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
import src.user.* ;
import src.util.* ;





public class BotanicalStation extends Venue implements VenueConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist aura/" ;
  final static Model
    STATION_MODEL = ImageModel.asIsometricModel(
      BotanicalStation.class, IMG_DIR+"botanical_station.png", 4, 3
    ),
    NURSERY_MODEL = ImageModel.asIsometricModel(
      BotanicalStation.class, IMG_DIR+"nursery.png", 2, 2
    ),
    CROP_MODELS[][] = ImageModel.fromTextureGrid(
      BotanicalStation.class,
      Texture.loadTexture(IMG_DIR+"all_crops.png"),
      4, 4, 1, ImageModel.TYPE_FLAT
    ),
    GRUB_BOX_MODEL = ImageModel.asIsometricModel(
      BotanicalStation.class, IMG_DIR+"grub_box.png", 1, 1
    ) ;
  final static Object CROP_SPECIES[][] = {
    new Object[] { 0, STARCHES, CROP_MODELS[0] },
    new Object[] { 1, STARCHES, CROP_MODELS[1] },
    new Object[] { 2, GREENS  , CROP_MODELS[3] },
    new Object[] { 3, GREENS  , CROP_MODELS[2] },
    new Object[] { 4, PROTEIN , new Model[] { GRUB_BOX_MODEL }}
    //new Object[] { 0, TIMBER  , CROP_MODELS[0] },
  } ;
  
  public static int pickSpecies(Tile t, BotanicalStation parent) {
    final float proteinChance = parent.growProtein ? 0.2f : 0.1f ;
    if (Rand.num() < proteinChance) return 4 ;
    float moisture = t.habitat().moisture() / 10f ;
    final Float chances[] = {
      (parent.growStarches ? 1.5f : 0.5f) * moisture,
      (parent.growStarches ? 1.5f : 0.5f) * (1 - moisture),
      (parent.growGreens   ? 1.5f : 0.5f) * moisture,
      (parent.growGreens   ? 1.5f : 0.5f) * (1 - moisture),
      0f
    } ;
    return (Integer) ((Object[]) Rand.pickFrom(CROP_SPECIES, chances))[0] ;
  }
  
  public static Model speciesModel(int varID, int growStage) {
    final Model seq[] = (Model[]) CROP_SPECIES[varID][2] ;
    return seq[Visit.clamp(growStage, seq.length)] ;
  }
  
  public static Item.Type speciesYield(int varID) {
    return (Item.Type) CROP_SPECIES[varID][1] ;
  }
  
  
  final static int
    MAX_PLANT_RANGE = 4 ;

  boolean
    growStarches = false,
    growGreens   = true ,
    growProtein  = false ;
  
  final List <Tile> toPlant = new List <Tile> () ;
  final List <Crop> planted = new List <Crop> () ;
  private int onceGrabbed = 0 ;
  
  
  
  public BotanicalStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs) ;
    attachSprite(STATION_MODEL.makeSprite()) ;
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s) ;
    growStarches = s.loadBool() ;
    growGreens   = s.loadBool() ;
    growProtein  = s.loadBool() ;
    s.loadTargets((Series) toPlant) ;
    s.loadObjects(planted) ;
    onceGrabbed = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveBool(growStarches) ;
    s.saveBool(growGreens  ) ;
    s.saveBool(growProtein ) ;
    s.saveTargets((Series) toPlant) ;
    s.saveObjects(planted) ;
    s.saveInt(onceGrabbed) ;
  }
  
  
  
  /**  Grabbing areas suitable for plantation-
    */
  protected float ratePlantArea() {
    if (inWorld()) I.complain("Only intended for rating potential sites!") ;
    if (origin() == null) I.complain("Must set position first!") ;
    grabPlantArea() ;
    
    float rating = 0 ;
    for (Tile t : toPlant) {
      rating += t.habitat().moisture() ;
    }
    final int dim = 2 + (MAX_PLANT_RANGE * 2) ;
    return rating / (dim * dim) ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    toPlant.clear() ;
    onceGrabbed = 0 ;
  }
  
  
  protected void grabPlantArea() {
    final Tile o = origin() ;
    final int r = MAX_PLANT_RANGE, span = r + size + r ;
    final Box2D area = new Box2D().set(
      (o.x - r) - 0.5f, (o.y - r) - 0.5f,
      span, span
    ) ;
    toPlant.clear() ;
    
    //
    //  TODO:  You also want to put a Nursery someplace nearby.  But this will
    //  do for now.
    
    final BotanicalStation nursery = this ;
    final TileSpread spread = new TileSpread(o) {
      protected boolean canAccess(Tile t) {
        return nursery.canAccess(t, area) ;
      }
      protected boolean canPlaceAt(Tile t) {
        if ((t.y - o.y) % 3 == 0) return false ;
        if (nursery.canPlant(t)) toPlant.add(t) ;
        return false ;
      }
    } ;
    spread.doSearch() ;
    onceGrabbed = toPlant.size() + planted.size() ;
  }
  

  protected boolean canAccess(Tile t, Box2D area) {
    if (! t.habitat().pathClear) return false ;
    if (! area.contains(t.x, t.y)) return false ;
    if (t.owner() == this) return true ;
    if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
    return true ;
  }
  
  
  protected boolean canPlant(Tile t) {
    if (t.pathType() == Tile.PATH_ROAD) return false ;
    if (t.owner() == this) return false ;
    if (t.owner() instanceof Crop) return false ;
    if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
    return true ;
  }
  
  
  protected int onceGrabbed() {
    return onceGrabbed ;
  }
  
  
  
  /**  Economic functions-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  protected Item.Type[] services() {
    return new Item.Type[] { STARCHES, GREENS, PROTEIN } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] {
      Vocation.BOTANIST, Vocation.FIELD_HAND, Vocation.FIELD_HAND
    } ;
  }
  

  public Behaviour jobFor(Actor actor) {
    final Delivery d = orders.nextDelivery(actor, services()) ;
    if (d != null) return d ;
    
    //if (true) return null ;
    final Farming f = new Farming(actor, this) ;
    if (f.nextStep() != null) return f ;
    
    return null ;
  }
  
  
  //protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/ecologist_button.gif") ;
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    
    d.append(new Description.Link("\n[Grow Starches]") {
      public void whenClicked() {
        growStarches = ! growStarches ;
      }
    }, growStarches ? Colour.GREEN : Colour.RED) ;
    
    d.append(new Description.Link("\n[Grow Greens]") {
      public void whenClicked() {
        growGreens = ! growGreens ;
      }
    }, growGreens ? Colour.GREEN : Colour.RED) ;
    
    d.append(new Description.Link("\n[Grow Protein]") {
      public void whenClicked() {
        growProtein = ! growProtein ;
      }
    }, growProtein ? Colour.GREEN : Colour.RED) ;
    d.append("\n\n") ;
    
    super.writeInformation(d, categoryID, UI) ;
  }
}











