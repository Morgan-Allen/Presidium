/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.user.* ;
import src.util.* ;



public abstract class Actor extends Mobile implements
  Inventory.Owner, Accountable, Selectable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  private static boolean verbose = false ;
  
  final public Healthbar healthbar = new Healthbar() ;
  final public TalkFX chat = new TalkFX() ;
  
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorTraits traits = new ActorTraits(this) ;
  final public ActorGear   gear   = new ActorGear  (this) ;
  
  final public ActorMind mind = initAI() ;
  private Action actionTaken ;
  private Base base ;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    
    health.loadState(s) ;
    traits.loadState(s) ;
    gear.loadState(s) ;
    mind.loadState(s) ;
    
    actionTaken = (Action) s.loadObject() ;
    base = (Base) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    health.saveState(s) ;
    traits.saveState(s) ;
    gear.saveState(s) ;
    mind.saveState(s) ;
    
    s.saveObject(actionTaken) ;
    s.saveObject(base) ;
  }
  
  
  protected abstract ActorMind initAI() ;
  
  protected MobilePathing initPathing() { return new MobilePathing(this) ; }
  
  public Background vocation() { return null ; }
  public void setVocation(Background b) {}
  
  public Object species() { return null ; }
  
  
  
  /**  Dealing with items and inventory-
    */
  public ActorGear inventory() {
    return gear ;
  }
  
  
  public float priceFor(Service service) {
    return service.basePrice * 2 ;
  }
  
  
  public int spaceFor(Service good) {
    return (int) health.maxHealth() / 2 ;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    if (verbose && I.talkAbout == this) {
      I.say("  ASSIGNING ACTION: "+action) ;
      if (action != null) I.add("  "+action.hashCode()+"\n") ;
    }
    world.activities.toggleActive(this.actionTaken, false) ;
    this.actionTaken = action ;
    if (actionTaken != null) {
      actionTaken.updateMotion(false) ;
      actionTaken.updateAction() ;
    }
    world.activities.toggleActive(action, true) ;
  }  
  
  
  protected void pathingAbort() {
    if (actionTaken == null) return ;
    
    final Behaviour top = mind.topBehaviour() ;
    if (top != null) {
      top.abortBehaviour() ;
      mind.cancelBehaviour(top) ;
    }
    
    final Behaviour root = mind.rootBehaviour() ;
    if (verbose) {
      I.sayAbout(this, "Aborting "+actionTaken) ;
      if (actionTaken != null) I.add("  "+actionTaken.hashCode()+"\n") ;
      I.sayAbout(this, "Root behaviour "+root) ;
    }
    //
    //  TODO:  Not good enough.  You need to work down the chain from the top
    //  behaviour, and allow each behaviour a chance to handle the pathing
    //  failure 'gracefully' (whatever that means.)  The default response,
    //  however, should be outright cancellation.
    if (root != null && root.finished()) {
      if (verbose) I.sayAbout(this, "  ABORTING ROOT") ;
      mind.cancelBehaviour(root) ;
    }
    
    assignAction(mind.getNextAction()) ;
  }
  
  
  public Action currentAction() {
    return actionTaken ;
  }
  
  
  public void assignBase(Base base) {
    this.base = base ;
  }
  
  
  public Base base() {
    return base ;
  }
  
  
  
  /**  Life cycle and updates-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    return true ;
  }
  
  
  public void exitWorld() {
    world.activities.toggleActive(actionTaken, false) ;
    mind.cancelBehaviour(mind.topBehaviour()) ;
    mind.onWorldExit() ;
    super.exitWorld() ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    
    final boolean OK = health.conscious() ;
    if (! OK) pathing.updateTarget(null) ;
    
    if (actionTaken != null && ! pathing.checkPathingOkay()) {
      world.schedule.scheduleNow(this) ;
    }

    final Behaviour root = mind.rootBehaviour() ;
    if (root != null && root != actionTaken && root.finished() && OK) {
      if (verbose && I.talkAbout == this) {
        I.say("  ROOT BEHAVIOUR COMPLETE... "+root) ;
        I.say("  PRIORITY: "+root.priorityFor(this)) ;
        I.say("  NEXT STEP: "+root.nextStepFor(this)) ;
      }
      mind.cancelBehaviour(root) ;
      //world.schedule.scheduleNow(this) ;
    }
    if (actionTaken != null && actionTaken.finished() && OK) {
      if (verbose) I.sayAbout(this, "  ACTION COMPLETE: "+actionTaken) ;
      //world.schedule.scheduleNow(this) ;
    }
    
    if (actionTaken != null) {
      actionTaken.updateMotion(OK) ;
      actionTaken.updateAction() ;
    }
    
    if (aboard instanceof Mobile && (pathing.nextStep() == aboard || ! OK)) {
      aboard.position(nextPosition) ;
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    long realStart = System.currentTimeMillis() ;
    long startTime, timeTaken ;
    
    //
    //  Update our basic statistics and physical properties-
    startTime = System.currentTimeMillis() ;
    health.updateHealth(numUpdates) ;
    gear.updateGear(numUpdates) ;
    traits.updateTraits(numUpdates) ;
    timeTaken = System.currentTimeMillis() - startTime ;
    
    if (verbose && timeTaken > 0 && I.talkAbout == this) {
      I.say("Time taken for stat updates: "+timeTaken) ;
    }
    
    if (health.conscious()) {
      //
      //  Check to see if our current action has expired-
      if ((actionTaken == null || actionTaken.finished())) {
        
        final Behaviour oldRoot = mind.rootBehaviour() ;
        startTime = System.currentTimeMillis() ;
        final Action action = mind.getNextAction() ;
        timeTaken = System.currentTimeMillis() - startTime ;
        final Behaviour newRoot = mind.rootBehaviour() ;
        
        if (verbose && timeTaken > 0 && I.talkAbout == this) {
          I.say("Time taken for getting action was "+timeTaken) ;
        }
        if (verbose && oldRoot != newRoot && I.talkAbout == this) {
          I.say("Root behaviour has changed from:") ;
          I.say("  "+oldRoot+" to\n  "+newRoot) ;
        }
        if (verbose) I.sayAbout(this, "REFRESHING ACTION! "+action) ;
        
        assignAction(action) ;
      }
      //
      //  Ensure our current pathing route is valid-
      if (! pathing.checkPathingOkay()) {
        
        startTime = System.currentTimeMillis() ;
        pathing.refreshPath() ;
        timeTaken = System.currentTimeMillis() - startTime ;

        if (verbose && timeTaken > 0 && I.talkAbout == this) {
          I.say("Time taken for getting path was "+timeTaken) ;
        }
      }
      //
      //  Update the AI at regular intervals-
      startTime = System.currentTimeMillis() ;
      mind.updateAI(numUpdates) ;
      timeTaken = System.currentTimeMillis() - startTime ;

      if (verbose && timeTaken > 0 && I.talkAbout == this) {
        I.say("Time taken for AI update was "+timeTaken) ;
      }
      //
      //  Update the intel/danger maps associated with the world's bases.
      startTime = System.currentTimeMillis() ;
      
      final float power = Combat.combatStrength(this, null) * 10 ;
      for (Base b : world.bases()) {
        if (b == base()) {
          //
          //  Actually lift fog in an area slightly ahead of the actor-
          final Vec2D heads = new Vec2D().setFromAngle(rotation) ;
          heads.scale(health.sightRange() / 3f) ;
          heads.x += position.x ;
          heads.y += position.y ;
          b.intelMap.liftFogAround(heads.x, heads.y, health.sightRange()) ;
        }
        if (! visibleTo(b)) continue ;
        final float relation = mind.relation(b) ;
        b.dangerMap.impingeVal(origin(), 0 - power * relation, true) ;
      }
      timeTaken = System.currentTimeMillis() - startTime ;
      
      if (verbose && timeTaken > 0 && I.talkAbout == this) {
        I.say("Time taken for fog/danger updates was "+timeTaken) ;
      }
    }
    else {
      if (health.asleep() && numUpdates % 10 == 0) {
        //
        //  Check to see if you need to wake up-
        Behaviour root = mind.rootBehaviour() ;
        if (root != null) mind.cancelBehaviour(root) ;
        mind.updateAI(numUpdates) ;
        mind.getNextAction() ;
        root = mind.rootBehaviour() ;
        
        final float
          wakePriority  = root == null ? 0 : root.priorityFor(this),
          sleepPriority = Resting.ratePoint(this, aboard(), 0) ;
        if (verbose && I.talkAbout == this) {
          I.say("Wake priority: "+wakePriority) ;
          I.say("Sleep priority: "+sleepPriority) ;
          I.say("Root behaviour: "+root) ;
        }
        
        final float margin = Math.max(Plan.ROUTINE / 2f, sleepPriority) ;
        if ((Rand.num() * sleepPriority) < (wakePriority - margin)) {
          health.setState(ActorHealth.STATE_ACTIVE) ;
          if (verbose) I.sayAbout(this, "Waking up for: "+root) ;
        }
      }
      if (health.isDead()) setAsDestroyed() ;
    }
    
    //
    //  Finally, report on total time taken-
    timeTaken = System.currentTimeMillis() - realStart ;
    if (verbose && timeTaken > 0 && I.talkAbout == this) {
      I.say(this+" Time taken for all updates: "+timeTaken) ;
    }
  }
  
  
  
  /**  Dealing with state changes-
    */
  //
  //  TODO:  Consider moving these elsewhere?
  
  public void enterStateKO(String animName) {
    ///I.say(this+" HAS BEEN KO'D") ;
    if (isDoing("actionFall", null)) return ;
    final Action falling = new Action(
      this, this, this, "actionFall",
      animName, "Stricken"
    ) ;
    pathing.updateTarget(null) ;
    mind.cancelBehaviour(mind.rootBehaviour()) ;
    this.assignAction(falling) ;
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true ;
  }
  
  
  public boolean isDoing(Class planClass, Target target) {
    if (target != null) {
      if (actionTaken == null || actionTaken.target() != target) return false ;
    }
    for (Behaviour b : mind.agenda()) {
      if (planClass.isAssignableFrom(b.getClass())) return true ;
    }
    return false ;
  }
  
  
  public Plan matchFor(Plan matchPlan) {
    for (Behaviour b : mind.agenda()) if (b instanceof Plan) {
      if (matchPlan.matchesPlan((Plan) b)) return (Plan) b ;
    }
    return null ;
  }
  
  
  public boolean isDoing(String actionMethod, Target target) {
    if (actionTaken == null) return false ;
    if (target != null && actionTaken.target() != target) return false ;
    return actionTaken.methodName().equals(actionMethod) ;
  }
  
  
  public Target targetFor(Class planClass) {
    if (actionTaken == null) return null ;
    if (planClass != null && ! isDoing(planClass, null)) return null ;
    return actionTaken.target() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    //
    //  ...Maybe include equipment/costume configuration here as well?
    final Sprite s = sprite() ;
    renderHealthbars(rendering, base) ;
    if (actionTaken != null) actionTaken.configSprite(s) ;
    ///I.say("Sprite height: "+s.position.z) ;
    super.renderFor(rendering, base) ;
    //
    //  Finally, if you have anything to say, render the chat bubbles!
    if (chat.numPhrases() > 0) {
      chat.position.setTo(s.position) ;
      chat.position.z += height() ;
      chat.update() ;
      rendering.addClient(chat) ;
    }
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    healthbar.level =  (1 - health.injuryLevel()) ;
    healthbar.level *= (1 - health.stressPenalty()) ;
    
    final BaseUI UI = (BaseUI) PlayLoop.currentUI() ;
    if (
      UI.selection.selected() != this &&
      UI.selection.hovered()  != this &&
      healthbar.level > 0.5f
    ) return ;
    
    if (health.dying()) return ;
    healthbar.size = 25 ;
    healthbar.matchTo(sprite()) ;
    healthbar.position.z -= radius() ;
    if (base() == null) healthbar.full = Colour.LIGHT_GREY ;
    else healthbar.full = base().colour ;
    rendering.addClient(healthbar) ;
  }
  
  
  protected float moveAnimStride() {
    return 1 ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }

  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return ;
    final boolean t = aboard() instanceof Tile ;
    Selection.renderPlane(
      rendering, viewPosition(null), (radius() + 0.5f) * (t ? 1 : 0.5f),
      Colour.transparency((hovered ? 0.5f : 1.0f) * (t ? 1 : 0.5f)),
      Selection.SELECT_CIRCLE
    ) ;
  }
  
  
  public Target subject() {
    return this ;
  }
  

  public String toString() {
    return fullName() ;
  }
  
  
  public void whenClicked() {
    if (PlayLoop.currentUI() instanceof BaseUI) {
      ((BaseUI) PlayLoop.currentUI()).selection.pushSelection(this, false) ;
    }
  }
  
  
  public void describeStatus(Description d) {
    if (! health.conscious()) { d.append(health.stateDesc()) ; return ; }
    if (! inWorld()) { d.append("Offworld") ; return ; }
    final Behaviour rootB = mind.rootBehaviour() ;
    if (rootB != null) rootB.describeBehaviour(d) ;
    else d.append("Thinking") ;
  }
}










