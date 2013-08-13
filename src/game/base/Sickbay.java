


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;



public class Sickbay extends Venue {
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  
  
  public Sickbay(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
  }
  
  
  public Sickbay(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  protected Vocation[] careers() {
    return null ;
  }
  
  protected Item.Type[] services() {
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "The Sickbay" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
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






