/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.common.Session.Saveable ;
import src.util.* ;


public abstract class Plan implements Saveable, Behaviour {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Saveable signature[] ;
  final int hash ;
  protected Actor actor ;
  protected Behaviour nextStep = null ;
  
  //
  //  These can be returned by default, or overridden if required.
  private float priority = -1 ;
  private String description = null ;
  
  
  protected Plan(Actor actor, Saveable... signature) {
    this.actor = actor ;
    this.signature = signature ;
    this.hash = Table.hashFor((Object[]) signature) ;
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this) ;
    actor = (Actor) s.loadObject() ;
    final int numS = s.loadInt() ;
    signature = new Saveable[numS] ;
    for (int i = 0 ; i < numS ; i++) signature[i] = s.loadObject() ;
    this.hash = Table.hashFor((Object[]) signature) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor) ;
    s.saveInt(signature.length) ;
    for (Saveable o : signature) s.saveObject(o) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    for (int i = 0 ; i < signature.length ; i++) {
      final Object s = signature[i], pS = p.signature[i] ;
      if (s == null && pS != null) return false ;
      if (! s.equals(pS)) return false ;
    }
    return true ;
  }
  
  public int planHash() {
    return hash ;
  }
  
  
  public boolean valid() {
    for (Saveable o : signature) if (o instanceof Target) {
      final Target t = (Target) o ;
      if (! t.inWorld()) return false ;
    }
    if (actor != null  && ! actor.inWorld()) return false ;
    return true ;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public boolean monitor(Actor actor) {
    return false ;
  }
  

  public void abortStep() {
    I.say("Aborting plan! "+this) ;
    actor.cancelBehaviour(this) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (this.actor != actor) {
      this.actor = actor ;
      nextStep = null ;
    }
    if (nextStep == null || nextStep.complete()) {
      if (valid()) nextStep = getNextStep() ;
      else { I.say("Plan no longer valid: "+this) ; nextStep = null ; }
    }
    return nextStep ;
  }
  
  
  protected abstract Behaviour getNextStep() ;
  
  
  public Behaviour nextStep() {
    return nextStepFor(actor) ;
  }
  
  
  public boolean complete() {
    return begun() && nextStep() == null ;
  }
  
  public boolean begun() {
    return actor != null ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
}









