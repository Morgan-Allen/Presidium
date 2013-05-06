/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.Description;
import src.util.* ;
import java.lang.reflect.* ;



public class Action implements Behaviour, Model.AnimNames {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public Actor actor ;
  final Session.Saveable basis ;
  final Method toCall ;
  private float priority ;
  
  private byte inRange = -1 ;
  private Target actionTarget, moveTarget ;
  
  private float progress, oldProgress, duration ;
  final String animName, description ;
  
  
  public Action(
    Actor actor, Target target,
    Session.Saveable basis, String methodName,
    String animName, String description
  ) {
    if (actor == null || target == null)
      I.complain("Null arguments for action!") ;
    this.actor = actor ;
    this.basis = basis ;
    this.toCall = namedMethodFor(basis, methodName) ;
    this.priority = ROUTINE ;
    //  TODO:  Consider automatically looking for the nearest clear tile!
    this.actionTarget = this.moveTarget = target ;
    this.duration = 1.0f ;
    this.animName = animName ;
    this.description = description ;
  }
  
  
  public Action(Session s) throws Exception {
    s.cacheInstance(this) ;
    
    actor = (Actor) s.loadObject() ;
    basis = s.loadObject() ;
    toCall = namedMethodFor(basis, s.loadString()) ;
    priority = s.loadFloat() ;
    
    inRange = (byte) s.loadInt() ;
    actionTarget = s.loadTarget() ;
    moveTarget = s.loadTarget() ;
    
    progress = s.loadFloat() ;
    oldProgress = s.loadFloat() ;
    duration = s.loadFloat() ;
    animName = s.loadString() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(actor) ;
    s.saveObject(basis) ;
    s.saveString(toCall.getName()) ;
    s.saveFloat(priority) ;
    
    s.saveInt(inRange) ;
    s.saveTarget(actionTarget) ;
    s.saveTarget(moveTarget) ;
    
    s.saveFloat(progress) ;
    s.saveFloat(oldProgress) ;
    s.saveFloat(duration) ;
    s.saveString(animName) ;
    s.saveString(description) ;
  }
  
  
  
  public void setPriority(float p) {
    this.priority = p ;
  }
  
  
  public void setMoveTarget(Target t) {
    if (t == null) I.complain("MOVE TARGET MUST BE NON-NULL.") ;
    this.moveTarget = t ;
  }
  
  
  public void setDuration(float d) {
    if (d <= 0) I.complain("DURATION MUST BE POSITIVE.") ;
    this.duration = d ;
  }
  
  
  public Target target() {
    return actionTarget ;
  }
  
  
  public String methodName() {
    return toCall.getName() ;
  }
  
  
  
  /**  Implementing the Behaviour contract-
    */
  public float priorityFor(Actor actor) {
    return priority ;
  }
  
  
  public boolean complete() {
    return (inRange == 1) && (progress >= 1) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (complete()) return null ;
    return this ;
  }
  
  
  public boolean monitor(Actor actor) {
    return true ;
  }
  
  public void abortStep() {
    progress = -1 ;
    inRange = -1 ;
  }
  
  

  /**  Actual execution of associated behaviour-
    */
  protected float progressPerUpdate() {
    if (inRange == 1) return 1f / (duration * PlayLoop.UPDATES_PER_SECOND) ;
    if (inRange == 0) {
      return actor.health.moveRate() / PlayLoop.UPDATES_PER_SECOND ;
    }
    return 0 ;
  }
  
  
  protected void adjustMotion() {
    //
    //  Update the actor's pathing and current heading as required.
    ///I.say(actor+" move target is: "+moveTarget) ;
    actor.pathing.updateWithTarget(moveTarget) ;
    final boolean closeEnough = actor.pathing.closeEnough() ;
    if (closeEnough) {
      actor.projectHeading(actionTarget.position(null), 0) ;
      if (inRange != 1) {
        inRange = 1 ;
        progress = oldProgress = 0 ;
      }
    }
    else {
      final Target nextStep = actor.pathing.nextStep() ;
      if (nextStep == null) return ;
      actor.projectHeading(nextStep.position(null), actor.health.moveRate()) ;
      if (inRange != 0) {
        inRange = 0 ;
        progress = oldProgress = 0 ;
      }
    }
  }
  
  
  protected void updateAction() {
    if (complete()) {
      oldProgress = progress ;
      return ;
    }
    adjustMotion() ;
    //
    //  Iterate over progress made-
    oldProgress = progress ;
    progress += progressPerUpdate() ;
    //
    //  If you're actually in range, check to see if you can deliver the needed
    //  behaviour.
    if (actor.pathing.closeEnough()) {
      progress = Visit.clamp(progress, 0, 1) ;
      final float contact = (duration - 0.5f) / duration ;
      if (oldProgress <= contact && progress > contact) try {
        toCall.invoke(basis, actor, actionTarget) ;
      }
      catch (Exception e) {
        I.say("PROBLEM WITH ACTION: "+toCall.getName()) ;
        I.report(e) ;
      }
    }
  }
  
  
  
  /**  Caching methods for later execution-
    */
  final private static Table <
    Class <? extends Object>,
    Table <String, Method>
  > actionMethods = new Table <
    Class <? extends Object>,
    Table <String, Method>
  > (100) ;
  
  
  private static Method namedMethodFor(Object plans, String methodName) {
    if (plans == null || methodName == null) return null ;
    Table <String, Method> aM = actionMethods.get(plans.getClass()) ;
    if (aM == null) {
      aM = new Table <String, Method> (20) ;
      for (Method method : plans.getClass().getMethods()) {
        aM.put(method.getName(), method) ;
      }
      //  TODO:  Check to see if this method has an appropriate argument set?
      actionMethods.put(plans.getClass(), aM) ;
    }
    final Method method = aM.get(methodName) ;
    if (method == null) I.complain(
      "NO SUCH METHOD! "+methodName+" FOR PLAN: "+plans
    ) ;
    method.setAccessible(true) ;
    return method ;
  }
  
  
  
  /**  Methods to support rendering-
    */
  float animProgress() {
    final float alpha = PlayLoop.frameTime() ;
    return ((progress * alpha) + (oldProgress * (1 - alpha))) % 1 ;
  }
  
  
  String animName() {
    if (inRange == 1) return animName ;
    if (inRange == 0) {
      final int moveType = actor.health.moveType() ;
      if (moveType == ActorHealth.MOVE_WALK ) return MOVE       ;
      if (moveType == ActorHealth.MOVE_RUN  ) return MOVE_FAST  ;
      if (moveType == ActorHealth.MOVE_SNEAK) return MOVE_SNEAK ;
    }
    return STAND ;
  }
  
  
  public String toString() {
    return description ;
  }

  public void describeBehaviour(Description d) {
    d.append(description) ;
  }
}






