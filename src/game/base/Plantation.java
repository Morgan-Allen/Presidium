

package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Plantation extends Venue implements
  TileConstants, BuildConstants
{
  
  
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
  final static Model
    NURSERY_MODEL = ImageModel.asIsometricModel(
      Plantation.class, IMG_DIR+"curing_shed.png", 2, 2
    ),
    COVERING_LEFT = ImageModel.asIsometricModel(
      Plantation.class, IMG_DIR+"covering_left.png", 1, 1
    ),
    COVERING_RIGHT = ImageModel.asIsometricModel(
      Plantation.class, IMG_DIR+"covering_right.png", 1, 1
    ),
    CROP_MODELS[][] = ImageModel.fromTextureGrid(
      Plantation.class,
      Texture.loadTexture(IMG_DIR+"all_crops.png"),
      4, 4, 1, ImageModel.TYPE_FLAT
    ),
    GRUB_BOX_MODEL = ImageModel.asIsometricModel(
      Plantation.class, IMG_DIR+"grub_box.png", 1, 1
    ) ;
  final static Object CROP_SPECIES[][] = {
    new Object[] { 0, CARBS   , CROP_MODELS[0] },
    new Object[] { 1, CARBS   , CROP_MODELS[1] },
    new Object[] { 2, GREENS  , CROP_MODELS[3] },
    new Object[] { 3, GREENS  , CROP_MODELS[2] },
    new Object[] { 4, PROTEIN , new Model[] { GRUB_BOX_MODEL }}
  } ;
  
  final static int
    TYPE_NURSERY = 0,
    TYPE_BED     = 1,
    TYPE_COVERED = 2 ;
  
  
  final BotanicalStation belongs ;
  final int type ;
  final int facing ;
  final Plantation strip[] ;
  
  private Crop planted[] = new Crop[4] ;
  private float needsFarming = 0 ;
  
  
  
  public Plantation(
    BotanicalStation belongs, int type, int facing, Plantation strip[]
  ) {
    super(2, 1, (ENTRANCE_WEST + (facing / 2)) % 4, belongs.base()) ;
    final boolean IN = type == TYPE_NURSERY ;
    structure.setupStats(
      IN ? 50 : 20,  //integrity
      3,  //armour
      IN ? 30 : 10,  //build cost
      0,  //max upgrades
      false  //organic
    ) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    this.belongs = belongs ;
    this.type = type ;
    this.facing = facing ;
    this.strip = strip ;
  }
  

  public Plantation(Session s) throws Exception {
    super(s) ;
    belongs = (BotanicalStation) s.loadObject() ;
    type = s.loadInt() ;
    facing = s.loadInt() ;
    
    final int SS = s.loadInt() ;
    strip = new Plantation[SS] ;
    for (int p = SS ; p-- > 0 ;) strip[p] = (Plantation) s.loadObject() ;
    for (int i = 0 ; i < 4 ; i++) {
      planted[i] = (Crop) s.loadObject() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    s.saveInt(type) ;
    s.saveInt(facing) ;
    
    s.saveInt(strip.length) ;
    for (Plantation p : strip) s.saveObject(p) ;
    for (Crop c : planted) s.saveObject(c) ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
  
  
  public int pathType() {
    if (type == TYPE_NURSERY) return Tile.PATH_BLOCKS ;
    return Tile.PATH_HINDERS ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type == TYPE_NURSERY) super.updatePaving(inWorld) ;
    else {
      final Paving paving = base().paving ;
      paving.updatePerimeter(this, inWorld) ;
    }
  }
  
  
  /**  Establishing crop areas-
    */
  final static int
    STRIP_DIRS[]  = { N, E, S, W },
    CROPS_POS[] = { 0, 0, 0, 1, 1, 0, 1, 1 } ;
  
  
  protected float needForFarming() {
    if (type != TYPE_NURSERY || ! structure.intact()) return 0 ;
    float sum = 0 ;
    for (Plantation p : strip) sum += p.needsFarming ;
    return sum / (strip.length - 1) ;
  }
  
  
  protected Crop nextToFarm(boolean planting, Actor actor) {
    float minDist = Float.POSITIVE_INFINITY ;
    Crop picked = null ;
    final int stage = planting ? Crop.NOT_PLANTED : Crop.MAX_GROWTH ;
    for (Plantation p : strip) for (Crop c : p.planted) {
      if (c != null && c.growStage == stage) {
        final float dist = Spacing.distance(actor, c.tile) ;
        if (dist < minDist) { picked = c ; minDist = dist ; }
      }
    }
    return picked ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    if (type == TYPE_NURSERY) {
      attachModel(NURSERY_MODEL) ;
    }
    else {
      for (int c = 0, i = 0 ; c < 4 ; c++) {
        final Tile t = world.tileAt(x + CROPS_POS[i++], y + CROPS_POS[i++]) ;
        planted[c] = new Crop(this, pickSpecies(t, belongs), t) ;
      }
      refreshCropSprites() ;
    }
  }
  
  
  //
  //  TODO:  Move some of this back to the Crop class...
  public void onGrowth() {
    //
    //  TODO:  Average fertility/moisture over the whole strip?
    if (! structure.intact()) return ;
    boolean anyChange = false ;
    needsFarming = 0 ;
    
    for (Crop c : planted) if (c != null) {
      final int oldGrowth = (int) c.growStage ;
      if (oldGrowth == Crop.NOT_PLANTED) {
        needsFarming++ ;
        continue ;
      }
      
      float growInc = c.tile.habitat().moisture() / 10f ;
      growInc += belongs.growBonus(c.tile, c.varID, true) ;
      growInc *= Rand.num() * Planet.dayValue(world) ;
      growInc = Visit.clamp(growInc, 0.2f, 1.2f) / 2 ;
      I.sayAbout(this, "Grow increment: "+growInc) ;
      
      c.growStage = Visit.clamp(c.growStage + growInc, 0, Crop.MAX_GROWTH) ;
      
      final int newGrowth = (int) c.growStage ;
      if (newGrowth == Crop.NOT_PLANTED || newGrowth == Crop.MAX_GROWTH) {
        needsFarming++ ;
      }
      if (oldGrowth != newGrowth) anyChange = true ;
    }
    needsFarming /= planted.length ;
    if (anyChange) refreshCropSprites() ;
  }
  
  
  protected void refreshCropSprites() {
    
    if (sprite() != null) {
      final GroupSprite oldSprite = (GroupSprite) buildSprite().baseSprite() ;
      world.ephemera.addGhost(this, 2, oldSprite, 2.0f) ;
    }
    
    final GroupSprite s = new GroupSprite() ;
    final Tile o = origin() ;
    for (int i = 4 ; i-- > 0 ;) {
      final Crop c = planted[i] ;
      if (c == null) continue ;
      //
      //  Update the sprite-
      Model m = null ; if (type == TYPE_COVERED) {
        if ((facing == N || facing == S) && c.tile.x == o.x) {
          m = COVERING_LEFT  ;
        }
        if ((facing == E || facing == W) && c.tile.y == o.y + 1) {
          m = COVERING_RIGHT ;
        }
      }
      if (m == null) m = Plantation.speciesModel(c.varID, (int) c.growStage) ;
      s.attach(m,
        CROPS_POS[i * 2] - 0.5f,
        CROPS_POS[(i * 2) + 1] - 0.5f,
        0
      ) ;
    }
    attachSprite(s) ;
    setAsEstablished(false) ;
  }
  
  
  Crop[] planted() {
    return planted ;
  }
  
  
  public static Service speciesYield(int varID) {
    return (Service) CROP_SPECIES[varID][1] ;
  }
  
  
  public static Model speciesModel(int varID, int growStage) {
    final Model seq[] = (Model[]) CROP_SPECIES[varID][2] ;
    return seq[Visit.clamp(growStage, seq.length)] ;
  }
  
  
  public static int pickSpecies(Tile t, BotanicalStation parent) {
    final Float chances[] = {
      parent.growBonus(t, 0, false) / (50 + parent.stocks.amountOf(PROTEIN)),
      parent.growBonus(t, 1, false) / (50 + parent.stocks.amountOf(CARBS  )),
      parent.growBonus(t, 2, false) / (50 + parent.stocks.amountOf(GREENS )),
      parent.growBonus(t, 3, false) / (50 + parent.stocks.amountOf(CARBS  )),
      parent.growBonus(t, 4, false) / (50 + parent.stocks.amountOf(GREENS )),
    } ;
    return (Integer) ((Object[]) Rand.pickFrom(CROP_SPECIES, chances))[0] ;
  }
  
  
  public Service[] services() { return null ; }
  
  protected Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  
  /**  Finding space.
    */
  static Plantation[] placeAllotment(
    final BotanicalStation parent, final int minSize, boolean covered
  ) {
    final World world = parent.world() ;
    
    Plantation bestSite[] = null ;
    float bestRating = 0 ;
    
    for (int m = 10 ; m-- > 0 ;) {
      final Tile t = Spacing.pickRandomTile(parent, 12, world) ;
      if (t == null) continue ;
      final int off = Rand.index(4) ;
      for (int n = 4 ; n-- > 0 ;) {
        final Plantation allots[] = new Plantation[minSize] ;
        final int i = (n + off) % 4 ;
        if (tryPlacementAt(t, parent, allots, STRIP_DIRS[i], covered)) {
          final float rating = rateArea(allots, world) ;
          if (rating > bestRating) { bestSite = allots ; bestRating = rating ; }
        }
      }
    }
    if (bestSite != null) {
      for (Plantation p : bestSite) {
        final Tile o = p.origin() ;
        p.doPlace(o, o) ;
      }
      return bestSite ;
    }
    return null ;
  }
  
  
  static float rateArea(Plantation allots[], World world) {
    //
    //  Favour fertile, unpaved areas close to the parent botanical station but
    //  farther from other structures-
    float
      fertility = 0, num = 0,
      minDist = World.DEFAULT_SECTOR_SIZE, parentDist = 0 ;
    for (Plantation p : allots) {
      parentDist += Spacing.distance(p, p.belongs) ;
      Target close = world.presences.nearestMatch(Venue.class, p, minDist) ;
      if (
        close != null && close != p.belongs &&
        ! (close instanceof Plantation)
      ) {
        minDist = Spacing.distance(p, close) ;
      }
      for (Tile t : world.tilesIn(p.area(), false)) {
        fertility += t.habitat().moisture() ;
        if (t.pathType() == Tile.PATH_ROAD) fertility /= 2 ;
        num++ ;
      }
    }
    float rating = fertility / num ;
    rating *= 1 - (parentDist / (allots.length * World.DEFAULT_SECTOR_SIZE)) ;
    rating *= minDist / World.DEFAULT_SECTOR_SIZE ;
    return rating ;
  }
  
  
  private static boolean tryPlacementAt(
    Tile t, BotanicalStation parent, Plantation allots[],
    int dir, boolean covered
  ) {
    for (int i = 0 ; i < allots.length ; i++) try {
      final Plantation p = allots[i] = new Plantation(
        parent, i == 0 ? TYPE_NURSERY : (covered ? TYPE_COVERED : TYPE_BED),
        dir, allots
      ) ;
      p.setPosition(
        t.x + (N_X[dir] * 2 * i),
        t.y + (N_Y[dir] * 2 * i),
        t.world
      ) ;
      if (! p.canPlace()) return false ;
    } catch (Exception e) { return false ; }
    return true ;
  }
  

  protected boolean canTouch(Element e) {
    return e.owningType() < this.owningType() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/nursery_button.gif") ;
  }
  
  
  public String fullName() { return "Plantation" ; }
  
  
  public String helpInfo() {
    return
      "Plantations of managed, mixed-culture cropland secure a high-quality "+
      "food source for your base, but require space and constant attention." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}
















