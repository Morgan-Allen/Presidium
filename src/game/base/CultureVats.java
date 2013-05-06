/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;



public class CultureVats extends Venue implements VenueConstants {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    CultureVats.class, "media/Buildings/physician aura/culture_vats.png", 4, 3
  ) ;
  
  
  public CultureVats(Base base) {
    super(4, 3, ENTRANCE_EAST, base) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public CultureVats(Session s) throws Exception {
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
  
  
  public Behaviour jobFor(Citizen actor) {
    
    final Delivery d = orders.nextDelivery(actor, goods()) ;
    if (d != null) return d ;
    
    final Manufacture m = orders.nextManufacture(actor, conversions()) ;
    if (m != null) return m ;
    
    return null ;
  }
  
  
  protected Conversion[] conversions() {
    return new Conversion[] { NONE_TO_SOMA } ;
  }
  
  
  protected Item.Type[] goods() {
    return new Item.Type[] { SOMA } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.VAT_BREEDER } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/culture_vats_button.gif") ;
  }

  public String fullName() { return "Culture Vats" ; }
  
  public String helpInfo() {
    return
      "The Culture Vats manufacture soma, medicines, tissue cultures and even "+
      "basic foodstuffs." ;
  }
  
}