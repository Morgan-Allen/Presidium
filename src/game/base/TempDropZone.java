

package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.Texture;



public class TempDropZone extends DropZone {
  
  
  
  public TempDropZone(Base base) {
    super(new Dropship(), base) ;
  }
  
  public TempDropZone(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    final Dropship ship = (Dropship) landing() ;
    ship.beginDescent(this) ;
  }


  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/supply_depot_button.gif") ;
  }
  
  public String helpInfo() {
    return "Experimental only." ;
  }
}
