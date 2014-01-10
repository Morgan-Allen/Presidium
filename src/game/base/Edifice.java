/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;




public class Edifice extends Venue implements Economy {
  

  final public static Model MODEL = ImageModel.asSolidModel(
    Foundry.class, "media/Buildings/aesthete/edifice.png", 4, 2
  ) ;
  //
  //  Events include:
  //    First landing.  1000 citizens.
  //    Full exploration of map.
  //    Full conquest of an enemy base.
  //    Birth of an heir.  Marriage alliance.
  //    Acquiring a relic or artifact.
  //    etc.
  //  (Effects become less powerful as you repeat an event to commemmorate.)
  //
  //  Styles include:  Representative, Geometric and Surreal
  //    Representative encourages/appeals to tradition
  //    Geometric encourages/appeals to logic
  //    Surreal encourages/appeals to creativity
  
  final public static int
    
    EVENT_POPULATION =  0,
    EVENT_FAMILY     =  1,
    EVENT_FIND_RELIC =  3,
    EVENT_CONQUEST   =  2,
    EVENT_ALLIANCE   =  3,
    EVENT_EXPLORE    =  4,
    
    STYLE_REPRESENTATIVE = 0,
    STYLE_GEOMETRIC      = 1,
    STYLE_SURREALISTIC   = 2 ;
  
  
  int eventCode = -1, styleCode = -1 ;
  
  
  public Edifice(Base base) {
    super(4, 2, ENTRANCE_NONE, base) ;
    structure.setupStats(
      500, 50, 800, 0, Structure.TYPE_FIXTURE
    ) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Edifice(Session s) throws Exception {
    super(s) ;
    eventCode = s.loadInt() ;
    styleCode = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(eventCode) ;
    s.saveInt(styleCode) ;
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
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    ///I.sayAbout(this, "Ambience value: "+structure.ambienceVal()) ;
    structure.setAmbienceVal(10) ;
  }
  


  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 3) {
      //
      //  TODO:  You need to pick an event to commemorate and the style of
      //  decoration.  (Implement as hidden upgrades.)
    }
    else super.writeInformation(d, categoryID, UI) ;
  }
  
  
  public String fullName() {
    return "Edifice" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/edifice_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Edifice commemorates significant events in the history of your "+
      "settlement, preserving a record thereof beneath a frictionless "+
      "tetra-carbon facade." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
}





