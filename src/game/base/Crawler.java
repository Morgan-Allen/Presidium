


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Crawler extends Vehicle implements
  Inventory.Owner, BuildConstants
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model
    BARGE_MODEL = MS3DModel.loadMS3D(
      Crawler.class, FILE_DIR, "crawler.ms3d", 1.0f
    ).loadXMLInfo(XML_PATH, "DustCrawler") ;
  
  
  
  public Crawler() {
    super() ;
    attachSprite(BARGE_MODEL.makeSprite()) ;
  }
  
  
  public Crawler(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  public float height() { return 0.5f ; }
  public float radius() { return 0.5f ; }
  
  /*
  public boolean blockedBy(Boardable b) {
    boolean blocked = super.blockedBy(b) ;
    I.say("Blocked by "+b+"? "+blocked) ;
    return blocked ;
  }
  //*/
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Dust Crawler " ;//+this.hashCode() ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Dust crawlers perform automatic soil-sampling and terraforming "+
      "duties with the barest minimum of human supervision.\n\n"+
      "  'This one's called Bett, and this one's Geordi.  Geordi is pretty "+
      "friendly, but you just want to stand well back when Bett starts "+
      "making that humming noise- means she don't like you.'\n"+
      "  -Onyd Calgin, Former Engineer, prior to recall" ;
  }
}



