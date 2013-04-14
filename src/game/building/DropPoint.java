/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;



/**  This is a special brand of 'venue' specifically intended to allow ease of
  *  interface with vehicles.  It occupies a single tile, just outside the
  *  vehicle door.
  */
public class DropPoint extends Venue implements VenueConstants {
  
  
  
  Vehicle landing ;
  
  
  public DropPoint(Vehicle landing, Base base) {
    super(1, 0, ENTRANCE_NONE, base) ;
    this.landing = landing ;
  }


  /**  Satisfying supply and demand-
    */
  protected Vocation[] careers() {
    return new Vocation[0] ;
  }
  
  protected Item.Type[] itemsMade() {
    return ALL_ITEM_TYPES ;
  }
  
  
  /**  Entry and exit methods, that similarly need to be overwritten.
    */
  
  
  
  public int owningType() {
    return NOTHING_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_CLEAR ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return landing.fullName() ;
  }

  public Texture portrait() {
    return landing.portrait() ;
  }
  
  public String helpInfo() {
    return landing.helpInfo() ;
  }
}








