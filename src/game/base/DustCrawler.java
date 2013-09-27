


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class DustCrawler extends Vehicle implements
  Inventory.Owner, BuildConstants
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model
    BARGE_MODEL = MS3DModel.loadMS3D(
      DustCrawler.class, FILE_DIR, "DustCrawler.ms3d", 1.0f
    ).loadXMLInfo(XML_PATH, "DustCrawler") ;
  
  
  
  public DustCrawler() {
    super() ;
    attachSprite(BARGE_MODEL.makeSprite()) ;
  }
  
  
  public DustCrawler(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  public float height() { return 0.5f ; }
  public float radius() { return 0.5f ; }
  
  
  
  
  /**  Behavioural methods-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    //
    //  TODO:  Restore once building/salvage of vehicles is complete-
    ///if (! structure.intact()) return ;
    
    base().intelMap.liftFogAround(this, 3.0f) ;
    
    final FormerPlant plant = (FormerPlant) hangar() ;
    final Target going = pathing.target() ;
    
    if (going == null || going == aboard()) {
      if (plant == aboard()) {
        for (Item sample : cargo.matches(SAMPLES)) {
          final Tile t = (Tile) sample.refers ;
          plant.soilSamples += (t.habitat().minerals() / 10f) + 0.5f ;
          cargo.removeItem(sample) ;
        }
        if (plant.soilSamples < 10) {
          pathing.updateTarget(plant.pickSample()) ;
        }
      }
      else {
        cargo.addItem(Item.withType(SAMPLES, origin())) ;
        pathing.updateTarget(plant) ;
      }
      final JointSprite sprite = (JointSprite) sprite() ;
      sprite.toggleGroup("soil bed", cargo.amountOf(SAMPLES) > 0) ;
    }
  }
  
  
  protected void pathingAbort() {
    ///I.say("PATHING ABORTED!") ;
    super.pathingAbort() ;
    if (! pathing.checkPathingOkay()) {
      final FormerPlant plant = (FormerPlant) hangar() ;
      if (cargo.amountOf(SAMPLES) > 0) pathing.updateTarget(plant) ;
      else pathing.updateTarget(plant.pickSample()) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Dust Crawler " ;
  }


  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Dust crawlers perform automatic soil-sampling and terraforming "+
      "duties with the barest minimum of human supervision.\n\n"+
      "  'This one's called Bett, and this one's Jordi.  Jordi is pretty "+
      "friendly, but you just want to stand well back when Bett starts "+
      "making that humming noise- means she don't like you.'\n"+
      "  -Anonymous Former Engineer prior to recall" ;
  }
}



