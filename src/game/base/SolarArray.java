


package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;




public class SolarArray extends Segment implements BuildConstants {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/" ;
  final public static Model
    ARRAY_MODELS[] = ImageModel.loadModels(
      SolarArray.class, 2, 2, IMG_DIR, ImageModel.TYPE_HOLLOW_BOX,
      "solar_array_left.png",
      "solar_array_right.png",
      "windtrap_left.png",
      "windtrap_right.png",
      "power_hub_left.png",
      "power_hub_right.png"
    ),
    MODEL_LEFT       = ARRAY_MODELS[0],
    MODEL_RIGHT      = ARRAY_MODELS[1],
    MODEL_TRAP_LEFT  = ARRAY_MODELS[2],
    MODEL_TRAP_RIGHT = ARRAY_MODELS[3],
    MODEL_HUB_LEFT   = ARRAY_MODELS[4],
    MODEL_HUB_RIGHT  = ARRAY_MODELS[5] ;
  
  final int
    TYPE_SOLAR = 0,
    TYPE_WIND  = 1,
    TYPE_HUB   = 2 ;
  
  
  
  public SolarArray(Base base) {
    super(2, 2, base) ;
    structure.setupStats(10, 5, 40, 0, Structure.TYPE_FIXTURE) ;
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
    type = TYPE_SOLAR ;
    
    if (numNear > 0 && numNear <= 2) {
      if (near[N] || near[S]) {
        facing = Y_AXIS ;
        if (o.y % 8 == 0) {
          type = TYPE_WIND ;
          attachModel(MODEL_TRAP_RIGHT) ;
        }
        else attachModel(MODEL_RIGHT) ;
        return ;
      }
      if (near[W] || near[E]) {
        facing = X_AXIS ;
        if (o.x % 8 == 0) {
          type = TYPE_WIND ;
          attachModel(MODEL_TRAP_LEFT) ;
        }
        else attachModel(MODEL_LEFT) ;
        return ;
      }
    }
    
    facing = CORNER ;
    attachModel(MODEL_LEFT) ;
  }
  

  protected List <Segment> installedBetween(Tile start, Tile end) {
    final List <Segment> installed = super.installedBetween(start, end) ;
    if (installed == null) return installed ;
    
    final int hubIndex = installed.size() / 2 ;
    final SolarArray hub = (SolarArray) installed.atIndex(hubIndex) ;
    hub.type = TYPE_HUB ;
    Model model = hub.facing == X_AXIS ? MODEL_HUB_LEFT : MODEL_HUB_RIGHT ;
    hub.attachModel(model) ;
    
    return installed ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  TODO:  Power must be stockpiled by day and released slowly at night.
    //  ...Maybe just a constant output could be assumed, for simplicity's
    //  sake?  Or maybe the hub could detect shortages and release more power
    //  to satisfy demand?
    
    final float dayVal = Planet.dayValue(world) ;
    stocks.bumpItem(POWER, 5 * dayVal / 10f, type == TYPE_HUB ? 100 : 10) ;
    stocks.bumpItem(WATER, 1 * dayVal / 10f, 5 ) ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type != TYPE_HUB) return ;
    base().paving.updatePerimeter(this, inWorld) ;
    
    final Tile o = origin() ;
    base().paving.updateJunction(this, o, false) ;
    
    final Tile perim[] = Spacing.perimeter(area(), world) ;
    for (int n = 0 ; n < perim.length ; n += 4) {
      final Tile t = perim[n] ;
      if (t != null) base().paving.updateJunction(this, t, inWorld) ;
    }
  }
  
  
  
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





