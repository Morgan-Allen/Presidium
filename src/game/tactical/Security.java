


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.cutout.* ;
import src.user.* ;



//
//  This combines defence, rescue, and medical attention of the subject, as
//  the circumstances may require.

public class Security extends Mission {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  public Security(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.SECURITY_MODEL.makeSprite(),
      "Securing "+subject
    ) ;
  }
  
  
  public Security(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    return false ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    return null ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}














