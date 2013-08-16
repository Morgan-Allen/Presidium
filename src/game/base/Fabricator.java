/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class Fabricator extends Venue implements BuildConstants {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Fabricator.class, "media/Buildings/aesthete/fabricator.png", 4, 2
  ) ;
  
  
  public Fabricator(Base base) {
    super(4, 2, ENTRANCE_EAST, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Fabricator(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    orders.translateDemands(conversions()) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    
    final Delivery d = orders.nextDelivery(actor, services()) ;
    if (d != null) return d ;
    
    final Manufacture m = orders.nextManufacture(actor, conversions()) ;
    if (m != null) return m ;
    
    return null ;
  }
  
  
  protected Conversion[] conversions() {
    return new Conversion[] { CARBONS_TO_PLASTICS } ;
  }
  
  
  protected Item.Type[] services() {
    return new Item.Type[] { PLASTICS } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.FABRICATOR } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/fabricator_button.gif") ;
  }

  public String fullName() { return "Fabricator" ; }
  
  public String helpInfo() {
    return
      "The Fabricator manufactures plastics, pressfeed, decor and outfits "+
      "for your citizens." ;
  }
  
  public String buildCategory() {
    return InstallTab.TYPE_AESTHETE ;
  }
}




