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
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;





public class BotanicalStation extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
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
  
  
  public static Model speciesModel(int varID, int growStage) {
    final Model seq[] = (Model[]) CROP_SPECIES[varID][2] ;
    return seq[Visit.clamp(growStage, seq.length)] ;
  }
  
  
  public static Service speciesYield(int varID) {
    return (Service) CROP_SPECIES[varID][1] ;
  }
  
  
  final static int
    MAX_PLANT_RANGE = 4 ;
  
  final List <Tile> toPlant = new List <Tile> () ;
  final List <Crop> planted = new List <Crop> () ;
  private int onceGrabbed = 0 ;
  
  
  
  public BotanicalStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs) ;
    structure.setupStats(
      150, 3, 250,
      VenueStructure.NORMAL_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(STATION_MODEL.makeSprite()) ;
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s) ;
    s.loadTargets((Series) toPlant) ;
    s.loadObjects(planted) ;
    onceGrabbed = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
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
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade>
    ALL_UPGRADES = new Index(BotanicalStation.class, "botanical_upgrades") ;
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      STARCHES, 2,
      null, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 2,
      null, ALL_UPGRADES
    ),
    FIELD_HAND_QUARTERS = new Upgrade(
      "Field Hand Quarters",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Vocation.FIELD_HAND, 2,
      null, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      CARBONS, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    ECOLOGIST_QUARTERS = new Upgrade(
      "Ecologist Quarters",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Vocation.ECOLOGIST, 1,
      TREE_FARMING, ALL_UPGRADES
    ) ;
  
  
  protected Index <Upgrade> allUpgrades() {
    return ALL_UPGRADES ;
  }
  

  public int numOpenings(Vocation v) {
    int num = super.numOpenings(v) ;
    if (v == Vocation.FIELD_HAND) return num + 2 ;
    if (v == Vocation.ECOLOGIST  ) return num + 0 ;
    return 0 ;
  }
  
  
  protected Service[] services() {
    return new Service[] { STARCHES, GREENS, PROTEIN } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.ECOLOGIST, Vocation.FIELD_HAND } ;
  }
  

  public Behaviour jobFor(Actor actor) {
    final Delivery d = Delivery.nextDeliveryFrom(this, actor, services()) ;
    if (d != null) return d ;
    
    //if (true) return null ;
    final Farming f = new Farming(actor, this) ;
    if (f.nextStep() != null) return f ;
    
    return null ;
  }
  
  
  float growBonus(Tile t, int varID) {
    if (varID == 4) {
      return (structure.upgradeBonus(PROTEIN) + 2) * 0.25f ;
    }
    float bonus = t.habitat().moisture() / 10f ;
    if (varID % 2 == 0) bonus = (1 - bonus) ;
    if (varID < 2) {
      return (structure.upgradeBonus(STARCHES) + 2) * 0.5f * bonus ;
    }
    else {
      return (structure.upgradeBonus(GREENS) + 2) * 0.5f * bonus ;
    }
  }
  
  
  public static int pickSpecies(Tile t, BotanicalStation parent) {
    final Float chances[] = {
      parent.growBonus(t, 0),
      parent.growBonus(t, 1),
      parent.growBonus(t, 2),
      parent.growBonus(t, 3),
      parent.growBonus(t, 4),
    } ;
    return (Integer) ((Object[]) Rand.pickFrom(CROP_SPECIES, chances))[0] ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/nursery_button.gif") ;
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}











