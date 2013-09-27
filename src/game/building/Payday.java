


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;
import src.game.actors.ActorAI.Employment ;



//  TODO:  Have this plan trigger auditing if no-one else does it first?


public class Payday extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final Employment pays ;
  
  
  public Payday(Actor actor, ActorAI.Employment pays) {
    super(actor, pays) ;
    this.pays = pays ;
  }
  
  
  public Payday(Session s) throws Exception {
    super(s) ;
    this.pays = (Employment) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(pays) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    if (! (pays instanceof Venue)) return 0 ;
    final int wages = ((Venue) pays).personnel.wagesFor(actor) ;
    if (wages <= 0) return 0 ;
    final float impetus = actor.mind.greedFor(wages) * ROUTINE ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final Action getPaid = new Action(
      actor, pays,
      this, "actionGetPaid",
      Action.TALK_LONG, "Getting Paid"
    ) ;
    return getPaid ;
  }
  
  
  public boolean actionGetPaid(Actor actor, Venue venue) {
    venue.personnel.dispenseWages(actor) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Collecting wages at ") ;
    d.append(pays) ;
  }
}













