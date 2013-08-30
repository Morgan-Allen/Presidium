


package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.Description;



public class Supervision extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final float WAIT_TIME = 20f ;
  
  final Venue supervised ;
  private float beginTime = -1 ;
  
  
  public Supervision(Actor actor, Venue supervised) {
    super(actor, supervised) ;
    this.supervised = supervised ;
  }
  
  
  public Supervision(Session s) throws Exception {
    super(s) ;
    this.supervised = (Venue) s.loadObject() ;
    this.beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(supervised) ;
    s.saveFloat(beginTime) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    return CASUAL ;
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    if (actor.world().currentTime() - beginTime > WAIT_TIME) return null ;
    if (! (supervised.jobFor(actor) instanceof Supervision)) return null ;
    
    final Action supervise = new Action(
      actor, supervised,
      this, "actionSupervise",
      Action.LOOK, "Supervising"
    ) ;
    return supervise ;
  }
  
  
  public boolean actionSupervise(Actor actor, Venue venue) {
    //  TODO:  Various small chores?  Study?  Maintenance?  Gossip?  etc.
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Supervising ") ;
    d.append(supervised) ;
  }
}













