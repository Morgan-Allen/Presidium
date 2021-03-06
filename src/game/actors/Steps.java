


package src.game.actors ;
import src.game.common.* ;
import src.game.common.Session.Saveable ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Generalise this to arbitrary behaviours.

public class Steps extends Plan {
  
  
  final float priority ;
  final Stack <Action> actions = new Stack <Action> () ;
  
  
  public Steps(
    Actor actor, Saveable key, float priority,
    Action... actions
  ) {
    super(actor, key) ;
    this.priority = priority ;
    for (Action a : actions) this.actions.add(a) ;
  }
  
  
  public Steps(Session s) throws Exception {
    super(s) ;
    priority = s.loadFloat() ;
    s.loadObjects(actions) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(priority) ;
    s.saveObjects(actions) ;
  }
  
  
  
  /**  
    */
  public float priorityFor(Actor actor) {
    return priority ;
  }
  
  
  protected Behaviour getNextStep() {
    final int index = actions.indexOf((Action) lastStep) + 1 ;
    if (index >= actions.size()) return null ;
    return actions.atIndex(index) ;
  }
  
  
  public void describeBehaviour(Description d) {
    super.describedByStep(d) ;
  }
}








