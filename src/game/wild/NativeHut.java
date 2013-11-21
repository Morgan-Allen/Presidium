


package src.game.wild ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class NativeHut extends Venue {
  
  
  final static Model HUT_MODELS[] = ImageModel.loadModels(
    Ruins.class, 4, 2, "media/Buildings/lairs and ruins/",
    ImageModel.TYPE_SOLID_BOX,
    "desert_huts.png",
    "forest_huts.png",
    "wastes_huts.png",
    "tundra_huts.png"
  ) ;
  final static ImageModel[][][] MODEL_KEYS = {
    Habitat.FOREST_FLORA_MODELS,
    Habitat.DESERT_FLORA_MODELS,
    Habitat.WASTES_FLORA_MODELS,
    Habitat.TUNDRA_FLORA_MODELS
  } ;
  
  private static Model modelFor(Habitat h) {
    int index = Visit.indexOf(h.floraModels, MODEL_KEYS) ;
    if (index == -1) index = 0 ;
    return HUT_MODELS[index] ;
  }
  
  
  
  public NativeHut(Habitat h, Base base) {
    super(4, 1, ENTRANCE_SOUTH, base) ;
    structure.setupStats(100, 4, 0, 0, Structure.TYPE_CRAFTED) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    attachModel(modelFor(h)) ;
  }
  
  
  public NativeHut(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    //
    //  Hunting, crafting, gathering, and housekeeping.  (Or are those covered
    //  already under human behaviours?  ...Well, they should be.)  In essence,
    //  humans without access to a base should default to these behaviours.
    return null ;
  }
  
  
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  protected void updatePaving(boolean inWorld) {}



  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Native Hut" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return "" ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_HIDDEN ;
  }

  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.append(helpInfo()) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}





