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




public class BulletinBoard extends Venue implements Economy {
  

  final public static Model MODEL = ImageModel.asSolidModel(
    Foundry.class, "media/Buildings/aesthete/bulletin_board.png", 2, 3
  ) ;
  
  
  public BulletinBoard(Base base) {
    super(2, 3, ENTRANCE_WEST, base) ;
    structure.setupStats(15, 2, 50, 0, Structure.TYPE_FIXTURE) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public BulletinBoard(Session s) throws Exception {
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
    return "Bulletin Board" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/bulletin_board_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Bulletin Board helps to draw citizens' attention to fresh news "+
      "and recruitment posting, and can help boost morale." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
}





