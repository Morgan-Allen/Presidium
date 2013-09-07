

package src.game.wild ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;




public class Ruins extends Venue {
  
  
  
  /**  Construction and save/load methods-
    */
  final static Model MODEL_RUINS[] = ImageModel.loadModels(
    Ruins.class, 4, 2, "media/Buildings/lairs and ruins/",
    "ruins_a.png",
    "ruins_b.png",
    "ruins_c.png"
  ) ;
  private static int NI = (int) (Math.random() * 3) ;
  
  
  public Ruins() {
    super(4, 2, ENTRANCE_EAST, null) ;
    structure.setupStats(1000, 100, 0, 0, false) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    final int index = (NI++ + Rand.index(1)) % 3 ;
    attachSprite(MODEL_RUINS[index].makeSprite()) ;
  }
  
  
  public Ruins(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behavioural routines-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  protected Vocation[] careers() { return null ; }
  protected Service[] services() { return null ; }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Ancient Ruins" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "The legacy of the wars of a vanished age, ancient ruins dot the "+
      "landscape of many worlds, in regions irradiated by nuclear fire or "+
      "decimated by biological weapons.  Backward primitives, deformed "+
      "mutants or stranger, more dangerous creatures often lurk in such "+
      "forsaken places." ;
  }
  

  public String buildCategory() { return UIConstants.TYPE_HIDDEN ; }
  
  
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







