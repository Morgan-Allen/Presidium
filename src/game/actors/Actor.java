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
  final public static Texture
    GROUND_SHADOW = Texture.loadTexture("media/SFX/ground_shadow.png") ;
  
  final public Healthbar healthBar = new Healthbar() ;
  final public TalkFX chat = new TalkFX() ;
  //  private ActorSprite actSprite ;
  //  private Healthbar healthBar ;
  //  private TalkFX chat ;
  
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
  
  public Vocation vocation() { return null ; }
  
  public Object species() { return null ; }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
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
    if (BaseUI.isPicked(this)) {
      I.say(this+" aborting "+actionTaken.methodName()) ;
    }
    AI.cancelBehaviour(AI.topBehaviour()) ;
    final Behaviour top = AI.topBehaviour() ;
    if (top != null) top.abortStep() ;
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
    if (actionTaken != null) {
      final boolean OK = health.conscious() ;
      actionTaken.updateMotion(OK) ;
      actionTaken.updateAction() ;
      if (actionTaken.complete() && OK) {
        world.schedule.scheduleNow(this) ;
      }
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    health.updateHealth(numUpdates) ;
    gear.updateGear(numUpdates) ;
    if (health.conscious()) {
      //
      //  Check to see if a new action needs to be decided on.
      if (actionTaken == null || actionTaken.complete()) {
        assignAction(AI.getNextAction()) ;
      }
      ///I.say("Updating pathing...") ;
      pathing.updatePathing() ;
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
    if (amDoing("actionFall")) return ;
    final Action falling = new Action(
      this, this, this, "actionFall",
      Action.FALL, "Stricken"
    ) ;
    AI.cancelBehaviour(AI.rootBehaviour()) ;
    this.assignAction(falling) ;
  }
  
  
  public boolean amDoing(String actionName) {
    if (actionTaken == null) return false ;
    return actionTaken.methodName().equals(actionName) ;
  }
  
  
  public boolean isDoing(Class planClass) {
    for (Behaviour b : AI.agenda()) {
      if (b.getClass().isInstance(planClass)) return true ;
    }
    return false ;
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public boolean visibleTo(Base base) {
    ///GameSettings.noFog = true ;
    ///if (BaseUI.isPicked(this)) I.say("Fog: "+base.intelMap.fogAt(origin())+" at: "+origin()) ;
    if (indoors()) return false ;
    return super.visibleTo(base) ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //
    //  Make a few basic sanity checks for visibility-
    final float scale = spriteScale() ;
    final Sprite s = sprite() ;
    
    if (! health.deceased()) {
      healthBar.level = (1 - health.injuryLevel()) * (1 - health.skillPenalty()) ;
      healthBar.size = 25 ;
      healthBar.matchTo(s) ;
      healthBar.position.z -= radius() ;
      rendering.addClient(healthBar) ;
    }
    
    //
    //  Render your shadow, either on the ground or on top of occupants-
    final float R2 = (float) Math.sqrt(2) ;
    final PlaneFX shadow = new PlaneFX(
      GROUND_SHADOW, radius() * scale * R2
    ) ;
    final Vec3D p = s.position ;
    shadow.position.setTo(p) ;
    shadow.position.z = shadowHeight(p) ;
    rendering.addClient(shadow) ;
    //
    //  In either case, set the sprite's scale factor and animations correctly-
    //
    //  ...Maybe include equipment/costume configuration here as well?
    s.scale = scale ;
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
  
  
  protected float spriteScale() {
    return 1 ;
  }
  
  
  protected float moveAnimStride() {
    return 1 ;
  }
  
  
  protected float shadowHeight(Vec3D p) {
    return world.terrain().trueHeight(p.x, p.y) ;
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










