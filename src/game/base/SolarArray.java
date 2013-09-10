


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;




public class SolarArray extends Segment {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/" ;
  final public static Model
    ARRAY_MODELS[] = ImageModel.loadModels(
      SolarArray.class, 2, 2, IMG_DIR,
      "solar_array_left.png",
      "solar_array_right.png",
      "windtrap_left.png",
      "windtrap_right.png",
      "power_hub.png"
    ),
    ARRAY_LEFT       = ARRAY_MODELS[0],
    ARRAY_RIGHT      = ARRAY_MODELS[1],
    ARRAY_TRAP_LEFT  = ARRAY_MODELS[2],
    ARRAY_TRAP_RIGHT = ARRAY_MODELS[3],
    ARRAY_CENTRE     = ARRAY_MODELS[4] ;
  
  
  
  public SolarArray(Base base) {
    super(2, 2, base) ;
    structure.setupStats(10, 5, 40, 0, false) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
  }
  
  
  public SolarArray(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  

  protected void configFromAdjacent(boolean[] near, int numNear) {

    final Tile o = origin() ;
    if (numNear > 0 && numNear <= 2) {
      if (near[N] || near[S]) {
        facing = Y_AXIS ;
        if (o.y % 8 == 0) {
          attachModel(ARRAY_TRAP_RIGHT) ;
        }
        else attachModel(ARRAY_RIGHT) ;
        return ;
      }
      if (near[W] || near[E]) {
        facing = X_AXIS ;
        if (o.x % 8 == 0) {
          attachModel(ARRAY_TRAP_LEFT) ;
        }
        else attachModel(ARRAY_LEFT) ;
        return ;
      }
    }
    
    facing = CORNER ;
    attachModel(ARRAY_LEFT) ;
  }
  
  //
  //  TODO:  Introduce road connections, and perhaps a secondary power hub.
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Solar Array" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/solar_array_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Solar Arrays provide clean power and a small amount of water to your "+
      "settlement, but also take up valuable space." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}





