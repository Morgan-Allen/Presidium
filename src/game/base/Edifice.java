/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;




public class Edifice extends Venue implements Economy {
  

  final public static Model MODEL = ImageModel.asSolidModel(
    Foundry.class, "media/Buildings/aesthete/edifice.png", 3, 2
  ) ;
  
  
  public Edifice(Base base) {
    super(3, 2, ENTRANCE_NONE, base) ;
    structure.setupStats(
      500, 50, 800, 0, Structure.TYPE_FIXTURE
    ) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Edifice(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public Background[] careers() {
    return null ;
  }
  
  
  public Service[] services() {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Edifice" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/edifice_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Edifice commemorates significant events in the history of your "+
      "settlement, preserving a record beneath a frictionless tetra-carbon "+
      "facade." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
}





