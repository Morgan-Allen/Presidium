/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.actors.* ;
import src.graphics.common.Texture ;
import src.user.Description ;



public class Creature extends Actor {
  
  
  
  public boolean switchBehaviour(Behaviour next, Behaviour last) {
    return next.priorityFor(this) >= (last.priorityFor(this) + 2) ;
  }
  
  public Behaviour nextBehaviour() {
    return null ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    // TODO Auto-generated method stub
    return null;
  }

  public Texture portrait() {
    // TODO Auto-generated method stub
    return null;
  }

  public String helpInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  public String[] infoCategories() {
    // TODO Auto-generated method stub
    return null;
  }

  public void writeInformation(Description d, int categoryID) {
  }
  
  public void describeBehaviour(Description d) {
    d.append(fullName()) ;
  }
}









