

package src.game.base ;
import src.game.common.* ;
import src.game.actors.Vocation;
import src.game.building.* ;
import src.game.building.Item.Type;
import src.graphics.common.* ;
import src.util.* ;



public class ShieldWallBlastDoors extends Venue {
  
  
  int facing = Venue.ENTRANCE_NONE ;
  
  
  //  TODO:  You have to assign the correct sprite, and allow for venues with
  //  multiple entrances.
  
  
  public ShieldWallBlastDoors(Base base) {
    super(3, 2, Venue.ENTRANCE_NONE, base) ;
  }
  
  
  public ShieldWallBlastDoors(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  
  /**  Life cycle and placement-
    */
  protected Vocation[] careers() {
    return null ;
  }
  
  
  protected Type[] itemsMade() {
    return null ;
  }
  
  
  
  
  
  
  /**  Rendering and interface methods-
    */
  


  public String fullName() {
    return "Blast Doors" ;
  }


  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/shield_wall_button.gif") ;
  }


  public String helpInfo() {
    return
      "Blast Doors grant your citizens access to enclosed sector of your "+
      "base." ;
  }
}









