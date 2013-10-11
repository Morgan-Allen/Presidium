/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Smelter extends Venue implements EconomyConstants {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/" ;
  final static Model
    DRILL_MODELS[] = ImageModel.loadModels(
      Smelter.class, 3, 3, IMG_DIR, ImageModel.TYPE_HOLLOW_BOX,
      "isotopes_smelter_dark.gif",
      "metals_smelter_dark.gif",
      "carbons_smelter_dark.gif"
    ),
    DRILL_LIGHTS_MODELS[] = ImageModel.loadModels(
      Smelter.class, 3, 3, IMG_DIR, ImageModel.TYPE_HOLLOW_BOX,
      "isotopes_smelter_lights.gif",
      "metals_smelter_lights.gif",
      "carbons_smelter_lights.gif"
    ),
    ALL_MOLD_MODELS[][] = ImageModel.fromTextureGrid(
      Smelter.class, Texture.loadTexture(IMG_DIR+"all_molds.png"),
      4, 5, 1, ImageModel.TYPE_FLAT
    ) ;
  final static int
    MOLD_COORDS[] = {
      0, 0,
      0, 1,
      0, 2,
      0, 3,
      1, 3,
      2, 3,
      3, 3
    },
    NUM_MOLDS = MOLD_COORDS.length / 2,
    MAX_MOLD_LEVEL = 4 ;
  
  final static int
    MOLD_COOL_TIME = World.STANDARD_DAY_LENGTH / 10 ;
  final static Service
    MINED_TYPES[] = { FUEL_CORES, METAL_ORE, PETROCARBS } ;
  
  
  static class Mold {
    float coolTime = -1 ;
    float minerals = 0 ;
  }
  
  
  final ExcavationSite parent ;
  final Service type ;
  final int variant ;
  final Mold molds[] = new Mold[NUM_MOLDS] ;
  
  
  public Smelter(ExcavationSite parent, int variant) {
    super(4, 3, Venue.ENTRANCE_SOUTH, parent.base()) ;
    structure.setupStats(75, 6, 150, 0, Structure.TYPE_FIXTURE) ;
    this.parent = parent ;
    this.variant = variant ;
    this.type = MINED_TYPES[variant] ;
    initMolds() ;
    updateSprite() ;
  }
  
  
  public Smelter(Session s) throws Exception {
    super(s) ;
    parent = (ExcavationSite) s.loadObject() ;
    variant = s.loadInt() ;
    type = MINED_TYPES[variant] ;
    initMolds() ;
    for (Mold m : molds) {
      m.coolTime = s.loadFloat() ;
      m.minerals = s.loadFloat() ;
    }
  }
  
  
  private void initMolds() {
    for (int n = NUM_MOLDS ; n-- > 0 ;) molds[n] = new Mold() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(parent) ;
    s.saveInt(variant) ;
    for (Mold m : molds) {
      s.saveFloat(m.coolTime) ;
      s.saveFloat(m.minerals) ;
    }
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  
  /**  Behaviour implementation.
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    boolean anyChange = false ;
    for (Mold m : molds) {
      if (m.coolTime <= 0) continue ;
      final float inc = Rand.avgNums(2) * 2 ;
      m.coolTime -= inc ;
      anyChange = true ;
      if (m.coolTime <= 0) {
        m.coolTime = -1 ;
        stocks.bumpItem(type, 5) ;
      }
    }
    if (anyChange) updateSprite() ;
  }
  
  
  protected void fillMolds(float amount) {
    boolean anyChange = false ;
    for (Mold m : molds) {
      if (m.coolTime >= 0) continue ;
      anyChange = true ;
      m.coolTime += Math.min(1, amount / 5) ;
      if (m.coolTime > 0) m.coolTime = MOLD_COOL_TIME ;
      amount -= 5 ;
      if (amount < 0) break ;
    }
    if (anyChange) updateSprite() ;
  }
  
  
  protected boolean allCooled() {
    int num = 0 ; for (Mold m : molds) {
      if (m.coolTime <= 0) num++ ;
    }
    return num == molds.length ;
  }
  
  
  protected boolean hasRoom(Item toReceive) {
    int num = 0 ; for (Mold m : molds) {
      if (m.coolTime <= 0) num++ ;
    }
    return num * 5 > toReceive.amount ;
  }
  

  public Service[] services() {
    return new Service[] { type } ;
  }
  
  
  protected Background[] careers() {
    return null ;
  }
  

  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  
  /**  Rating potential placement sites-
    */
  static float rateSite(Smelter s, World world) {
    //
    //  Ideally, you want some place that's not too close to other structures,
    //  especially of the same type, but also close to the parent shaft, and
    //  rich in associated minerals.
    final int maxRange = World.SECTOR_SIZE ;
    float parentDist = Spacing.distance(s, s.parent) ;
    float minDist = maxRange ;
    
    for (Object o : world.presences.matchesNear(Venue.class, s, maxRange)) {
      final Venue v = (Venue) o ;
      if (v == s.parent) continue ;
      minDist = Spacing.distance(s, v) ;
      break ;
    }
    if (minDist <= s.size) return 0 ;
    
    float sumMinerals = 0 ;
    final Box2D area = s.area(null).expandBy(s.size) ;
    for (Tile under : world.tilesIn(area, true)) {
      sumMinerals += world.terrain().mineralsAt(under, (byte) s.variant) ;
    }
    
    float rating = (sumMinerals / area.area()) + 0.1f ;
    rating *= 1 - (parentDist / maxRange) ;
    rating *= minDist / maxRange ;
    I.say("Rating for "+s+" is- "+rating) ;
    return rating ;
  }
  
  
  static Smelter siteNewSmelter(ExcavationSite parent, Service mined) {
    final World world = parent.world() ;
    final int variant = variantFor(mined) ;
    Smelter bestSite = null ;
    float bestRating = 0 ;

    for (int m = 10 ; m-- > 0 ;) {
      final Tile t = Spacing.pickRandomTile(parent.origin(), 12, world) ;
      if (t == null) continue ;
      final Smelter site = new Smelter(parent, variant) ;
      site.setPosition(t.x, t.y, world) ;
      if (! site.canPlace()) continue ;
      I.say("Getting rating at "+t) ;
      final float rating = rateSite(site, world) ;
      if (rating > bestRating) { bestSite = site ; bestRating = rating ; }
    }
    if (bestSite != null) {
      final Tile o = bestSite.origin() ;
      bestSite.doPlace(o, o) ;
      return bestSite ;
    }
    return null ;
  }
  
  
  static Smelter cullWorst(List <Smelter> shafts, ExcavationSite parent) {
    final World world = parent.world() ;
    final Visit <Smelter> picks = new Visit <Smelter> () {
      public float rate(Smelter s) { return 0 - rateSite(s, world) ; }
    } ;
    return picks.pickBest(shafts) ;
  }
  
  
  static int variantFor(Service mined) {
    return Visit.indexOf(mined, MINED_TYPES) ;
  }
  
  
  


  /**  Rendering and interface methods-
    */
  protected boolean showLights() {
    return inside().size() > 0 || ! allCooled() ;
  }
  
  
  private void updateSprite() {
    final boolean inWorld = inWorld() && sprite() != null ;
    final float xo = (size - 1) / -2f, yo = (size - 1) / -2f ;
    final GroupSprite s ;
    if (inWorld) {
      s = (GroupSprite) buildSprite().baseSprite() ;
    }
    else {
      s = new GroupSprite() ;
      s.attach(DRILL_MODELS[variant], 2 + xo, 1 + yo, 0) ;
      attachSprite(s) ;
    }
    
    for (int i = 0, c = 0 ; i < NUM_MOLDS ; i++) {
      float moldLevel = molds[i].coolTime / MOLD_COOL_TIME ;
      moldLevel *= (MAX_MOLD_LEVEL - 1) ;
      final Model model = ALL_MOLD_MODELS[variant][(int) (moldLevel + 1)] ;
      
      if (inWorld) {
        final ImageSprite oldMold = (ImageSprite) s.atIndex(i + 1) ;
        if (oldMold != null && oldMold.model() == model) continue ;
        final Sprite ghost = oldMold.model().makeSprite() ;
        ghost.position.setTo(oldMold.position) ;
        world().ephemera.addGhost(null, 1, ghost, 2.0f) ;
        oldMold.setModel((ImageModel) model) ;
      }
      else s.attach(model,
        MOLD_COORDS[c++] + xo,
        MOLD_COORDS[c++] + yo,
      0) ;
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //I.sayAbout(this, "showing lights? "+showLights()) ;
    buildSprite().toggleLighting(
      DRILL_LIGHTS_MODELS[variant], showLights(), 0.5f, -0.5f, 0
    ) ;
    super.renderFor(rendering, base) ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[0] ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/excavation_button.gif") ;
  }
  
  
  public String fullName() {
    return type.name+" Smelter" ;
  }
  
  
  public String helpInfo() {
    return
      type.name+" Smelters extract larger quantities of "+type.name+
      " from subterranean mineral deposits." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}






