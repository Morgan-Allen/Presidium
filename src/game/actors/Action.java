/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.common.Session.Saveable;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;

import java.lang.reflect.* ;


//
//  ALLOW FOR RANGED ACTIONS.


public class Action implements Behaviour, Model.AnimNames {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static int
    QUICK   = 1,
    CAREFUL = 2,
    STEADY  = 4,
    RANGED  = 8 ;
  
  final public Actor actor ;
  final Session.Saveable basis ;
  final Method toCall ;
  
  private float priority ;

  private int properties ;
  private byte inRange = -1 ;
  private Target actionTarget, moveTarget ;
  private float progress, oldProgress ;
  
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
    this.actionTarget = this.moveTarget = target ;
    //this.duration = 1.0f ;
    this.animName = animName ;
    this.description = description ;
  }
  
  
  public Action(Session s) throws Exception {
    s.cacheInstance(this) ;
    
    actor = (Actor) s.loadObject() ;
    basis = s.loadObject() ;
    toCall = namedMethodFor(basis, s.loadString()) ;
    priority = s.loadFloat() ;
    
    properties = s.loadInt() ;
    inRange = (byte) s.loadInt() ;
    actionTarget = s.loadTarget() ;
    moveTarget = s.loadTarget() ;
    
    progress = s.loadFloat() ;
    oldProgress = s.loadFloat() ;
    ///duration = s.loadFloat() ;
    animName = s.loadString() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(actor) ;
    s.saveObject(basis) ;
    s.saveString(toCall.getName()) ;
    s.saveFloat(priority) ;
    
    s.saveInt(properties) ;
    s.saveInt(inRange) ;
    s.saveTarget(actionTarget) ;
    s.saveTarget(moveTarget) ;
    
    s.saveFloat(progress) ;
    s.saveFloat(oldProgress) ;
    ///s.saveFloat(duration) ;
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
  
  
  public void setProperties(int p) {
    this.properties = p ;
  }
  
  /*
  public void setDuration(float d) {
    if (d <= 0) I.complain("DURATION MUST BE POSITIVE.") ;
    this.duration = d ;
  }
  //*/
  
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
    actor.psyche.cancelBehaviour(this) ;
  }
  
  

  /**  Actual execution of associated behaviour-
    */
  protected float duration() {
    float duration = 1 ;
    if ((properties & QUICK) != 0) duration /= 2 ;
    if ((properties & CAREFUL) != 0) duration *= 2 ;
    return duration ;
  }
  
  
  protected float moveRate() {
    float rate = actor.health.moveRate() ;
    //  You also have to account for the effects of fatigue and encumbrance...
    if ((properties & QUICK  ) != 0) rate *= 2 ;
    if ((properties & CAREFUL) != 0) rate /= 2 ;
    
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    return rate ;
  }
  
  
  protected void adjustMotion() {
    //
    //  Update the actor's pathing and current heading as required.
    ///I.say(actor+" move target is: "+moveTarget) ;
    float minDist = 0 ;
    if ((properties & RANGED) != 0) minDist = actor.health.sightRange() ;
    actor.pathing.updateWithTarget(moveTarget, minDist) ;
    
    //  TODO:  The 'Close Enough' test needs to ensure that the actor is facing
    //  toward to target as well.
    if (actor.pathing.closeEnough()) {
      actor.headTowards(actionTarget, 0) ;
      if (inRange != 1) {
        inRange = 1 ;
        progress = oldProgress = 0 ;
      }
    }
    else {
      final Target nextStep = actor.pathing.nextStep() ;
      if (nextStep == null) return ;
      actor.headTowards(nextStep, moveRate()) ;
      if (inRange != 0) {
        inRange = 0 ;
        progress = oldProgress = 0 ;
      }
    }
  }
  
  
  protected float progressPerUpdate() {
    if (inRange == 1) {
      return 1f / (duration() * PlayLoop.UPDATES_PER_SECOND) ;
    }
    if (inRange == 0) {
      return
        actor.health.moveRate() *
        actor.moveAnimStride() /
        PlayLoop.UPDATES_PER_SECOND ;
    }
    return 0 ;
  }
  
  
  //  TODO:  Having to include both motion and actual action-updates here is a
  //  little awkward.  Consider moving some of this to the MobilePathing or
  //  Actor classes?
  protected void updateAction() {
    if (complete()) {
      oldProgress = progress = 1 ;
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
      final float duration = duration() ;
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
  > (1000) ;
  
  
  private static Method namedMethodFor(Object plans, String methodName) {
    if (plans == null || methodName == null) return null ;
    Table <String, Method> aM = actionMethods.get(plans.getClass()) ;
    if (aM == null) {
      aM = new Table <String, Method> (20) ;
      for (Method method : plans.getClass().getMethods()) {
        aM.put(method.getName(), method) ;
        if (method.getName().equals(methodName)) {
          final Class <? extends Object> params[] = method.getParameterTypes() ;
          if (
            params.length != 2 ||
            ! Actor.class.isAssignableFrom(params[0]) ||
            ! Target.class.isAssignableFrom(params[1])
          ) I.complain("METHOD HAS BAD ARGUMENT SET!") ;
        }
      }
      actionMethods.put(plans.getClass(), aM) ;
    }
    final Method method = aM.get(methodName) ;
    if (method == null) I.complain(
      "NO SUCH METHOD! "+methodName+" FOR CLASS: "+plans
    ) ;
    method.setAccessible(true) ;
    return method ;
  }
  
  
  
  /**  Methods to support rendering-
    */
  float animProgress() {
    final float alpha = PlayLoop.frameTime() ;
    final float AP = ((progress * alpha) + (oldProgress * (1 - alpha))) ;
    if (AP > 1) return AP % 1 ;
    return AP ;
  }
  
  
  String animName() {
    if (inRange == 1) {
      return animName ;
    }
    else {
      if ((properties & QUICK  ) != 0) return MOVE_FAST  ;
      if ((properties & CAREFUL) != 0) return MOVE_SNEAK ;
      return MOVE ;
    }
  }
  
  
  public String toString() {
    return description ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(description) ;
  }
}






