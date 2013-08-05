

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;



public class Garrison extends Venue implements VenueConstants {
  
  
  
  /**  Fields, constants, and save/load methods-
    */
  final Model
    MODEL = ImageModel.asIsometricModel(
      Garrison.class, "media/Buildings/military aura/house_garrison.png", 4, 4
    ) ;
  
  
  public Garrison(Base base) {
    super(4, 4, ENTRANCE_EAST, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Garrison(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    //
    //  Grab a random building nearby and patrol around it.  Especially walls.
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.MILITANT } ;
  }
  
  
  protected Item.Type[] services() {
    return new Item.Type[] {} ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Garrison" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/garrison_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Garrison sends regular patrols of sentries to enforce the peace "+
      "and keep a watch for raiders or outlaws.  It also provides logistic "+
      "and engineering support to speed repairs and account for spending." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MILITANT ;
  }
}






