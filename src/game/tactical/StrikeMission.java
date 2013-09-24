/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;




//  Merge this with the Combat class?


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
    //  TODO:  Try to unify these.
    if (subject instanceof Actor) return Combat.combatPriority(
      actor, (Actor) subject,
      actor.AI.greedFor(rewardAmount(actor)) * ROUTINE,
      PARAMOUNT
    ) ;
    if (subject instanceof Venue) {
      return actor.AI.greedFor(rewardAmount(actor)) * ROUTINE ;
    }
    return 0 ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null ;
    return new Combat(actor, (Element) subject) ;
  }


  public boolean finished() {
    if (Combat.isDead((Element) subject)) return true ;
    return false ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Strike Mission", this) ;
    d.append(" against ") ;
    d.append(subject) ;
  }
}






