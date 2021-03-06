

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class FleshStill extends Venue implements Economy {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    FleshStill.class, "media/Buildings/ecologist/flesh_still.png", 4, 2
  ) ;
  
  
  final SurveyStation parent ;
  GroupSprite camouflaged ;
  
  
  public FleshStill(SurveyStation parent) {
    super(4, 2, Venue.ENTRANCE_EAST, parent.base()) ;
    structure.setupStats(
      100, 4, 150, 0, Structure.TYPE_FIXTURE
    ) ;
    this.parent = parent ;
    attachSprite(MODEL.makeSprite()) ;
    camouflaged = new GroupSprite() ;
  }
  
  
  public FleshStill(Session s) throws Exception {
    super(s) ;
    parent = (SurveyStation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(parent) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  public Background[] careers() { return null ; }
  
  public Service[] services() {
    return new Service[] { WATER, PROTEIN, SPICE } ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    for (Tile t : Spacing.perimeter(area(), origin().world)) if (t != null) {
      if (t.owningType() >= this.owningType()) return false ;
    }
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base()) super.renderFor(rendering, base) ;
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      this.position(camouflaged.position) ;
      camouflaged.fog = this.fogFor(base) ;
      rendering.addClient(camouflaged) ;
    }
  }
  
  
  public String fullName() {
    return "Flesh Still" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/redoubt_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Flesh Stills help to extract protein and spice from culled specimens "+
      "of native wildlife." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}








