/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.* ;




public class StrikeMission extends Mission {
  
  
  public StrikeMission(Target subject) {
    super(
      subject,
      MissionsTab.STRIKE_MODEL.makeSprite(), "Striking at "+subject
    ) ;
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour nextStepFor(Actor actor) {
    return null ;
  }
  
  
  public boolean complete() {
    return false ;
  }
  
  
  public void describeBehaviour(Description d) {
  }
  
  
}



