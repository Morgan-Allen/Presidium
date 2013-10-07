/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;

import java.lang.reflect.* ;



public class Action implements Behaviour, Model.AnimNames {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static int
    QUICK    = 1,
    CAREFUL  = 2,
    CARRIES  = 4,
    RANGED   = 8 ;
  
  private static boolean verbose = false ;
  
  
  final public Actor actor ;
  final public Session.Saveable basis ;
  final public Method toCall ;
  
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
  
  
  public boolean finished() {
    if (progress == -1) return true ;
    return (inRange == 1) && (progress >= 1) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null ;
    return this ;
  }
  
  
  public boolean monitor(Actor actor) {
    return true ;
  }
  
  
  public void abortBehaviour() {
    progress = -1 ;
    inRange = -1 ;
    actor.mind.cancelBehaviour(this) ;
  }
  
  
  public boolean valid() {
    return
      actor.inWorld() && moveTarget.inWorld() &&
      ! actionTarget.destroyed() ;
  }
  
  
  public void onSuspend() {
  }
  
  
  public boolean begun() {
    return actor.currentAction() == this ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  

  /**  Actual execution of associated behaviour-
    */
  private float duration() {
    float duration = 1 ;
    if ((properties & QUICK) != 0) duration /= 2 ;
    if ((properties & CAREFUL) != 0) duration *= 2 ;
    return duration ;
  }
  
  
  private float contactTime() {
    final float duration = duration() ;
    return (duration - 0.25f) / duration ;
  }
  
  
  private float moveRate() {
    float rate = actor.health.moveRate() * moveMultiple() ;
    //
    //  You also have to account for the effects of fatigue and encumbrance...
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    return rate ;
  }
  
  
  private float progressPerUpdate() {
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
  
  
  private void advanceAction() {
    progress = Visit.clamp(progress, 0, 1) ;
    final float contact = contactTime() ;
    if (oldProgress <= contact && progress > contact) applyEffect() ;
  }
  
  
  public void updateMotion(boolean moveOK) {
    //
    //  We don't chase targets which might have been themselves affected by
    //  the action.
    if (inRange == 1 && progress > contactTime()) {
      return ;
    }
    
    float minDist = 0 ;
    if ((properties & RANGED) != 0) {
      minDist = actor.health.sightRange() ;
      if (inRange == 1) minDist *= 2 ;
    }
    else if (inRange == 1) minDist += progress + 0.5f ;
    
    final float
      moveDist = Spacing.distance(actor, moveTarget) ;
    final boolean
      closed = actor.pathing.closeEnough(moveTarget, minDist),
      facing = actor.pathing.facingTarget(actionTarget),
      approach = (moveDist < 1) && ! (moveTarget instanceof Boardable) ;
    actor.pathing.updateTarget(moveTarget) ;
    final Target faced = closed ? actionTarget :
      (approach ? moveTarget : actor.pathing.nextStep()) ;
    
    if (verbose && I.talkAbout == actor) {
      I.say("Action target is: "+actionTarget) ;
      I.say("Move target is: "+moveTarget) ;
      I.say("Closed/facing: "+closed+"/"+facing+", move okay? "+moveOK) ;
      
      I.say("Distance: "+moveDist) ;
      I.say("Faced is: "+faced) ;
    }
    
    if (moveOK) actor.pathing.headTowards(faced, moveRate(), ! closed) ;
    //
    //  Check for state changes-
    final byte oldRange = inRange ;
    inRange = (byte) ((closed && facing) ? 1 : 0) ;
    if (inRange != oldRange) progress = oldProgress = 0 ;
  }
  
  
  protected void updateAction() {
    if (finished()) { oldProgress = progress = 1 ; return ; }
    oldProgress = progress ;
    progress += progressPerUpdate() ;
    if (inRange == 1) advanceAction() ;
  }
  
  
  public float moveMultiple() {
    float rate = 1 ;
    if ((properties & QUICK  ) != 0) {
      rate *= 2 * actor.health.moveLuck() ;
    }
    if ((properties & CAREFUL) != 0) {
      rate *= (2 - actor.health.moveLuck()) / 4 ;
    }
    return rate ;
  }
  
  
  public void applyEffect() {
    ///if (true) return ;
    try { toCall.invoke(basis, actor, actionTarget) ; }
    catch (Exception e) {
      I.say("PROBLEM WITH ACTION: "+toCall.getName()) ;
      I.report(e) ;
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
      }
      actionMethods.put(plans.getClass(), aM) ;
    }
    final Method method = aM.get(methodName) ;
    if (method == null) I.complain(
      "NO SUCH METHOD! "+methodName+" FOR CLASS: "+plans
    ) ;
    if (! method.isAccessible()) {
      final Class <? extends Object> params[] = method.getParameterTypes() ;
      if (
        params.length != 2 ||
        ! Actor.class.isAssignableFrom(params[0]) ||
        ! Target.class.isAssignableFrom(params[1])
      ) I.complain("METHOD HAS BAD ARGUMENT SET!") ;
      method.setAccessible(true) ;
    }
    return method ;
  }
  
  
  
  /**  Methods to support rendering-
    */
  void configSprite(Sprite sprite) {
    //
    //  In the case of a pushing animation, you actually need to set different
    //  animations for the upper and lower body.
    ///I.sayAbout(actor, "anim progress: "+animProgress());
    sprite.setAnimation(animName(), animProgress()) ;
  }
  
  
  protected float animProgress() {
    final float alpha = PlayLoop.frameTime() ;
    final float AP = ((progress * alpha) + (oldProgress * (1 - alpha))) ;
    if (AP > 1) return AP % 1 ;
    return AP ;
  }
  
  
  protected String animName() {
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






