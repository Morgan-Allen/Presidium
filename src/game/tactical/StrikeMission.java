/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.* ;
import src.util.* ;




public class StrikeMission extends Mission {
  
  
  
  public StrikeMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.STRIKE_MODEL.makeSprite(), "Striking at "+subject
    ) ;
  }
  
  
  public StrikeMission(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return Combat.combatPriority(
      actor, (Actor) subject,
      actor.psyche.greedFor(rewardAmount()) * ROUTINE,
      PARAMOUNT
    ) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (complete()) return null ;
    I.say("Getting next combat step for "+actor) ;
    return new Combat(actor, (Actor) subject) ;
  }


  public boolean complete() {
    final Actor target = (Actor) subject ;
    I.say(target+" is dead? "+target.health.deceased()) ;
    if (target.health.deceased()) return true ;
    return false ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Strike Mission against ") ;
    d.append(subject) ;
  }
}






