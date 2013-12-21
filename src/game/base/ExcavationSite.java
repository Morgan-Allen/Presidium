/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
//import src.game.campaign.Scenario;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



//  TODO:  USE THESE UPGRADES-
//  Metal Ores Mining.  Fuel Cores Mining.  Artifact Assembly.
//  Safety Measures.  Excavator Station.  Mantle Drilling.

//
//  TODO:  Consider opening new shafts as you expand?  Or new smelters, based
//  on rock types encountered and total staff?  Yeah.



public class ExcavationSite extends Venue implements
  Economy, TileConstants
{
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String
    IMG_DIR = "media/Buildings/artificer/" ;
  final static ImageModel
    SHAFT_MODEL = ImageModel.asSolidModel(
      ExcavationSite.class, IMG_DIR+"excavation_shaft.gif", 4, 1
    ) ;
  
  private static boolean verbose = false ;
  
  final static int
    DIG_LIMITS[] = { 8, 12, 15, 16 },
    DIG_FACE_REFRESH = World.STANDARD_DAY_LENGTH / 10,
    SMELTER_REFRESH  = 10 ;
  
  
  private Tile underFaces[] ;
  private List <Smelter> smelters = new List <Smelter> () ;
  private Box2D stripArea = new Box2D() ;
  
  
  
  
  public ExcavationSite(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      200, 15, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachModel(SHAFT_MODEL) ;
  }


  public ExcavationSite(Session s) throws Exception {
    super(s) ;
    underFaces = (Tile[]) s.loadTargetArray(Tile.class) ;
    s.loadObjects(smelters) ;
    stripArea.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTargetArray(underFaces) ;
    s.saveObjects(smelters) ;
    stripArea.saveTo(s.output()) ;
  }
  
  
  
  /**  Presence in the world and boardability-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    return true ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
    //
    //  TODO:  Close all your shafts?  Eject occupants?
  }
  
  
  public void onDestruction() {
    super.onDestruction() ;
  }
  
  
  public void onCompletion() {
    super.onCompletion() ;
  }
  
  
  
  /**  Methods for sorting and returning mine-faces in order of promise.
    */
  public int digLimit() {
    final int level = structure.upgradeLevel(SAFETY_PROTOCOL) ;
    return DIG_LIMITS[level] ;
  }
  
  
  
  /**  Economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    ExcavationSite.class, "excavation_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    SAFETY_PROTOCOL = new Upgrade(
      "Safety Protocol",
      "Increases effective dig range while limiting pollution and reducing "+
      "the likelihood of artilect release.",
      100,
      ARTIFACTS, 1, null, ALL_UPGRADES
    ),
    
    METAL_ORES_MINING = new Upgrade(
      "Metal Ores Mining",
      "Allows veins of heavy metals to be detected and excavated more "+
      "reliably.",
      150,
      METAL_ORES, 2, null, ALL_UPGRADES
    ),
    
    FUEL_CORES_MINING = new Upgrade(
      "Fuel Cores Mining",
      "Allows deposits of radiactive isotopes to be sought out and extracted "+
      "more reliably.",
      200,
      FUEL_CORES, 2, null, ALL_UPGRADES
    ),
    
    EXCAVATOR_STATION = new Upgrade(
      "Excavator Station",
      "Excavators are responsible for seeking out subterranean mineral "+
      "deposits and bringing them to the surface.",
      50,
      Background.EXCAVATOR, 1, null, ALL_UPGRADES
    ),
    
    ARTIFACT_ASSEMBLY = new Upgrade(
      "Artifact Assembly",
      "Allows fragmentary artifacts to be reconstructed with greater skill "+
      "and confidence.",
      150,
      null, 1, EXCAVATOR_STATION, ALL_UPGRADES
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to bring up an indefinite supply of "+
      "metals and isotopes from the planet's molten core, at the cost of "+
      "heavy pollution.",
      350,
      null, 1, METAL_ORES_MINING, ALL_UPGRADES
    )
  ;
  
  
  
  public Background[] careers() {
    return new Background[] { Background.EXCAVATOR } ;
  }
  
  
  public Service[] services() {
    return new Service[] { METAL_ORES, FUEL_CORES, ARTIFACTS } ;
  }
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v) ;
    if (v == Background.EXCAVATOR) return NO + 2 ;
    return 0 ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    
    I.sayAbout(actor, "GETTING NEXT EXCAVATION TASK") ;
    
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 5, world
    ) ;
    if (d != null) return d ;
    final Choice choice = new Choice(actor) ;
    
    for (Smelter s : smelters) {
      choice.add(new OreProcessing(actor, s, s.output)) ;
    }
    if (structure.upgradeLevel(ARTIFACT_ASSEMBLY) > 0) {
      choice.add(new OreProcessing(actor, this, ARTIFACTS)) ;
    }
    
    final Target face = Mining.nextMineFace(this, underFaces) ;
    if (face != null) {
      choice.add(new Mining(actor, face, this)) ;
    }
    return choice.weightedPick() ;
  }
  
  
  protected int extractionBonus(Service mineral) {
    if (mineral == METAL_ORES) {
      return (1 + structure.upgradeLevel(METAL_ORES_MINING)) * 2 ;
    }
    if (mineral == FUEL_CORES) {
      return (1 + structure.upgradeLevel(FUEL_CORES_MINING)) * 2 ;
    }
    if (mineral == ARTIFACTS) {
      return (1 + structure.upgradeLevel(ARTIFACT_ASSEMBLY)) * 2 ;
    }
    return -1 ;
  }
  
  
  protected Venue smeltingSite(Service mineral) {
    if (mineral == ARTIFACTS ) return this ;
    for (Smelter s : smelters) {
      if (s.output == mineral) return s ;
    }
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    structure.setAmbienceVal(structure.upgradeLevel(SAFETY_PROTOCOL) - 3) ;
    
    checks: for (Smelter d : smelters) {
      for (Smelter kid : d.strip) if (kid.destroyed()) {
        smelters.remove(d) ; continue checks ;
      }
    }
    
    //
    //  TODO:  Come up with limits for each of the smelter types, based on
    //  staff size and underlying/surrounding terrain.
    
    //final int numDrills = structure.upgradeLevel(MANTLE_DRILLING) ;
    if (numUpdates % SMELTER_REFRESH == 0) {
      if (smeltingSite(METAL_ORES) == null) {
        final Smelter strip[] = Smelter.siteNewDrill(this, METAL_ORES) ;
        if (strip != null) smelters.add(strip[0]) ;
      }
      if (
        smeltingSite(FUEL_CORES) == null &&
        true //structure.upgradeLevel(FUEL_PROCESSING) > 0
      ) {
        final Smelter strip[] = Smelter.siteNewDrill(this, FUEL_CORES) ;
        if (strip != null) smelters.add(strip[0]) ;
      }
    }
    
    if (numUpdates % DIG_FACE_REFRESH == 0) {
      underFaces = Mining.getTilesUnder(this) ;
    }
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Site" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/excavation_button.gif") ;
  }


  public String helpInfo() {
    return
      "Excavation Sites expedite the extraction of mineral wealth and "+
      "buried artifacts from the terrain surrounding your settlement." ;
  }

  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}



