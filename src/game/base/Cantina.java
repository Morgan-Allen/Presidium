


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class Cantina extends Venue {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asIsometricModel(
    Cantina.class, "media/Buildings/merchant/cantina.gif", 4, 3
  ) ;
  
  
  public Cantina(Base base) {
    super(4, 3, Venue.ENTRANCE_SOUTH, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Cantina(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, services and economic functions-
    */

  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return null ;
  }
  
  
  protected Item.Type[] services() {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/cantina_button.gif") ;
  }
  
  
  public String fullName() { return "The Cantina" ; }
  
  
  public String helpInfo() {
    return
      "Citizens can seek lodgings or simply rest and relax at the Cantina, "+
      "which serves as both a social focal point and a potential breeding "+
      "ground for criminal activities." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}







