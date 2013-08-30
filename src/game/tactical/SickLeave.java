


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.Description;



public class SickLeave extends Plan {
  
  
  final Sickbay sickbay ;
  
  
  public SickLeave(Actor actor) {
    super(actor) ;
    Object haven = Retreat.nearestHaven(actor, Sickbay.class) ;
    if (haven instanceof Sickbay) sickbay = (Sickbay) haven ;
    else sickbay = null ;
  }
  
  
  public SickLeave(Session s) throws Exception {
    super(s) ;
    sickbay = (Sickbay) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(sickbay) ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    if (sickbay == null) return 0 ;
    return Treatment.needForTreatment(actor) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (sickbay == null) return null ;
    if (Treatment.needForTreatment(actor) == 0) return null ;
    final Action leave = new Action(
      actor, sickbay,
      this, "actionLeave",
      Action.FALL, "Taking Sick Leave"
    ) ;
    return leave ;
  }
  
  
  public boolean actionLeave(Actor actor, Sickbay sickbay) {
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Seeking Treatment at ") ;
    d.append(sickbay) ;
  }
}














