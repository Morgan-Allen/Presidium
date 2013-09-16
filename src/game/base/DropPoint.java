/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;


//
//  TODO:  This should be attached to the Supply Depot, acting as a landing
//         strip for Dropships.

public class DropPoint extends Fixture {
  
  //
  //  TODO:  You need a sprite for this.
  
  
  public DropPoint(int size, Venue parent) {
    super(size, 0) ;
  }
  
  
  public DropPoint(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  public int owningType() {
    return VENUE_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
}








