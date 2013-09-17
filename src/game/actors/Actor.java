/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.base.Human;
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
  
  final public Healthbar healthBar = new Healthbar() ;
  final public TalkFX chat = new TalkFX() ;
  
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorTraits traits = new ActorTraits(this) ;
  final public ActorGear   gear   = new ActorGear  (this) ;
  
  final public ActorAI AI = initAI() ;
  private Action actionTaken ;
  private Base base ;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    
    health.loadState(s) ;
    traits.loadState(s) ;
    gear.loadState(s) ;
    AI.loadState(s) ;
    
    actionTaken = (Action) s.loadObject() ;
    base = (Base) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    health.saveState(s) ;
    traits.saveState(s) ;
    gear.saveState(s) ;
    AI.saveState(s) ;
    
    s.saveObject(actionTaken) ;
    s.saveObject(base) ;
  }
  
  
  protected abstract ActorAI initAI() ;
  
  protected MobilePathing initPathing() { return new MobilePathing(this) ; }
  
  public ActorGear inventory() { return gear ; }
  public float priceFor(Service service) { return service.basePrice ; }
  
  public Background vocation() { return null ; }
  
  public Object species() { return null ; }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    if (I.talkAbout == this) {
      I.sayIfAbout(verbose, this, "ASSIGNING ACTION: "+action) ;
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
    I.sayIfAbout(verbose, this, "Aborting "+actionTaken.methodName()) ;
    AI.cancelBehaviour(AI.topBehaviour()) ;
    final Behaviour top = AI.topBehaviour() ;
    if (top != null) top.abortBehaviour() ;
    assignAction(AI.getNextAction()) ;
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
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
  }
  
  
  public void exitWorld() {
    world.activities.toggleActive(actionTaken, false) ;
    AI.cancelBehaviour(AI.topBehaviour()) ;
    AI.onWorldExit() ;
    super.exitWorld() ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    final boolean OK = health.conscious() ;
    
    if (! OK) pathing.updateTarget(null) ;
    
    if (actionTaken != null && ! pathing.checkPathingOkay()) {
      world.schedule.scheduleNow(this) ;
    }
    if (actionTaken != null) {
      actionTaken.updateMotion(OK) ;
      actionTaken.updateAction() ;
      
      final Behaviour root = AI.rootBehaviour() ;
      if (root != null && root.finished() && OK) {
        if (root.begun()) AI.cancelBehaviour(root) ;
        world.schedule.scheduleNow(this) ;
      }
      else if (actionTaken.finished() && OK) {
        world.schedule.scheduleNow(this) ;
      }
      else if (! pathing.checkPathingOkay()) {
      }
    }
    
    if (aboard instanceof Mobile && (pathing.nextStep() == aboard || ! OK)) {
      ///I.sayAbout(this, "Tracking position: "+aboard) ;
      aboard.position(nextPosition) ;
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    health.updateHealth(numUpdates) ;
    gear.updateGear(numUpdates) ;
    if (health.conscious()) {
      //
      //  Check to see if a new action needs to be decided on.
      if (actionTaken == null || actionTaken.finished()) {
        final Action action = AI.getNextAction() ;
        I.sayIfAbout(verbose, this, "REFRESHING ACTION! "+action) ;
        assignAction(action) ;
      }
      if (! pathing.checkPathingOkay()) pathing.refreshPath() ;
      AI.updateAI(numUpdates) ;
      //
      //  Update the intel/danger maps associated with the world's bases.
      final float power = Combat.combatStrength(this, null) ;
      for (Base b : world.bases()) {
        if (b == base()) b.intelMap.liftFogAround(this, health.sightRange()) ;
        if (! visibleTo(b)) continue ;
        final float relation = AI.relation(b) ;
        b.dangerMap.impingeVal(origin(), power * relation) ;
      }
    }
    else if (health.decomposed()) setAsDestroyed() ;
  }
  
  
  
  /**  Dealing with state changes-
    */
  protected void enterStateKO() {
    ///I.say(this+" HAS BEEN KO'D") ;
    if (isDoing("actionFall", null)) return ;
    final Action falling = new Action(
      this, this, this, "actionFall",
      Action.FALL, "Stricken"
    ) ;
    pathing.updateTarget(null) ;
    AI.cancelBehaviour(AI.rootBehaviour()) ;
    this.assignAction(falling) ;
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true ;
  }
  
  
  public boolean isDoing(Class planClass, Target target) {
    if (target != null) {
      if (actionTaken == null || actionTaken.target() != target) return false ;
    }
    for (Behaviour b : AI.agenda()) {
      if (planClass.isAssignableFrom(b.getClass())) return true ;
    }
    return false ;
  }
  
  
  public boolean isDoing(String actionMethod, Target target) {
    if (actionTaken == null) return false ;
    if (target != null && actionTaken.target() != target) return false ;
    return actionTaken.methodName().equals(actionMethod) ;
  }
  
  
  public Target targetFor(Class planClass) {
    if (actionTaken == null) return null ;
    if (! isDoing(planClass, null)) return null ;
    return actionTaken.target() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    //
    //  Make a few basic sanity checks for visibility-
    final Sprite s = sprite() ;
    
    if (! health.deceased()) {
      healthBar.level =  (1 - health.injuryLevel()) ;
      healthBar.level *= (1 - health.skillPenalty()) ;
      healthBar.size = 25 ;
      healthBar.matchTo(s) ;
      healthBar.position.z -= radius() ;
      if (base() == null) healthBar.full = Colour.LIGHT_GREY ;
      else healthBar.full = base().colour ;
      rendering.addClient(healthBar) ;
    }
    //
    //  ...Maybe include equipment/costume configuration here as well?
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
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
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
    if (! inWorld()) { d.append("Is Offworld") ; return ; }
    final Behaviour rootB = AI.rootBehaviour() ;
    if (rootB != null) rootB.describeBehaviour(d) ;
    else d.append("Thinking") ;
  }
}










