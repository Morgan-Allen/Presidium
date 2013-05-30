/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.Texture;
import src.user.BaseUI;
import src.user.Composite;



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


  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/supply_depot_button.gif") ;
  }
  
  public String helpInfo() {
    return "Experimental only." ;
  }
}
