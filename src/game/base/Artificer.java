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



public class Artificer extends Venue implements VenueConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Artificer.class, "media/Buildings/artificer aura/artificer.png", 4, 3
  ) ;
  
  
  public Artificer(Base base) {
    super(4, 3, ENTRANCE_EAST, base) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Artificer(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  
  
  public void updateAsScheduled() {
    super.updateAsScheduled() ;
    //
    //  TODO:  This is a temporary measure.  Remove later.
    //orders.updateDemand(PARTS, 10) ;
    orders.receiveDemand(PARTS, 10) ;
    
    orders.translateDemands(conversions()) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    
    final Delivery d = orders.nextDelivery(actor, itemsMade()) ;
    if (d != null) return d ;
    
    final Manufacture m = orders.nextManufacture(actor, conversions()) ;
    if (m != null) return m ;
    
    return null ;
  }
  
  
  protected Conversion[] conversions() {
    return new Conversion[] { METALS_TO_PARTS } ;
  }
  
  
  protected Item.Type[] itemsMade() {
    return new Item.Type[] { PARTS } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.ARTIFICER } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/artificer_button.gif") ;
  }

  public String fullName() { return "Artificer" ; }
  
  public String helpInfo() {
    return
      "The Artificer manufactures parts, inscriptions, devices and armour "+
      "for your citizens." ;
  }
}







