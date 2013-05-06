/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.util.* ;



//  TODO:  Make this entirely separate from stocks?  ...You might have to.
//  TODO:  Create a Manufacture.Output interface and have this implement it.
//  Use the Conversion class in the process.
public class VenueIntegrity extends Inventory {
  
  
  /**  Fields, definitions and save/load methods-
    */
  final Venue venue ;
  
  int integrity ;
  Item materials[] ;
  
  
  
  VenueIntegrity(Venue venue) {
    super(venue) ;
    this.venue = venue ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  /**
    */
}

