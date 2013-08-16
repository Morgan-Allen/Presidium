


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;



public class Hospice extends Venue {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Hospice.class, "media/Buildings/physician/physician_clinic.png", 3, 2
  ) ;
  
  
  public Hospice(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Hospice(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    //  If anyone is waiting for treatment, treat them.  Otherwise, just tend
    //  the desk.
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.MINDER, Vocation.PHYSICIAN } ;
  }
  
  
  protected Item.Type[] services() {
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "The Sickbay" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/hospice_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Sickbay allows your citizens' injuries, diseases and trauma to be"+
      "treated quickly and effectively.  It also helps to regulate "+
      "population growth and provide basic daycare and education facilities." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}






