/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;
import src.game.planet.Terrain;
import src.game.social.Accountable ;
import src.graphics.sfx.Healthbar;
import src.graphics.sfx.PlaneFX ;



public abstract class Actor extends Mobile implements
  Inventory.Owner, Accountable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  final public static Texture
    GROUND_SHADOW = Texture.loadTexture("media/SFX/ground_shadow.png") ;
  
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorTraits traits = new ActorTraits(this) ;
  final public ActorGear   gear   = new ActorGear  (this) ;
  
  final public MobilePathing pathing = initPathing() ;
  final public ActorPsyche psyche = initPsyche() ;
  
  private Action actionTaken, reflexAction ;
  private Base base ;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    
    health.loadState(s) ;
    traits.loadState(s) ;
    gear.loadState(s) ;
    
    pathing.loadState(s) ;
    psyche.loadState(s) ;
    
    actionTaken = (Action) s.loadObject() ;
    base = (Base) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    health.saveState(s) ;
    traits.saveState(s) ;
    gear.saveState(s) ;
    
    pathing.saveState(s) ;
    psyche.saveState(s) ;
    
    s.saveObject(actionTaken) ;
    s.saveObject(base) ;
  }
  
  
  protected abstract ActorPsyche initPsyche() ;
  
  protected MobilePathing initPathing() { return new MobilePathing(this) ; }
  
  public ActorGear inventory() { return gear ; }
  
  public Vocation vocation() { return null ; }
  
  public Object species() { return null ; }
  
  
  //  TODO:  Should this be moved to the Psyche class?
  public float attraction(Actor otherA) {
    return 0 ;
  }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    world.activities.toggleActive(this.actionTaken, false) ;
    this.actionTaken = action ;
    if (action != null) actionTaken.updateAction() ;
    world.activities.toggleActive(action, true) ;
  }
  
  
  protected void onMotionBlock() {
    final boolean canRoute = pathing.refreshPath() ;
    if (! canRoute) pathingAbort() ;
  }
  
  
  public void pathingAbort() {
    if (actionTaken == null) return ;
    ///I.say(this+" aborting actionTaken...") ;
    psyche.cancelBehaviour(psyche.topBehaviour()) ;
    final Behaviour top = psyche.topBehaviour() ;
    if (top != null) top.abortStep() ;
    assignAction(psyche.getNextAction()) ;
  }
  
  
  public Action currentAction() {
    return actionTaken ;
  }
  
  
  public void assignBase(Base base) {
    this.base = base ;
  }
  
  
  public Base assignedBase() {
    return base ;
  }
  
  
  
  /**  Life cycle and updates-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
  }
  
  
  public void exitWorld() {
    world.activities.toggleActive(actionTaken, false) ;
    psyche.cancelBehaviour(psyche.topBehaviour()) ;
    super.exitWorld() ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    if (actionTaken != null) actionTaken.updateAction() ;
    if (health.conscious()) {
      if (actionTaken == null || actionTaken.complete()) {
        assignAction(psyche.getNextAction()) ;
        I.say(this+" got next action: "+actionTaken) ;
      }
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    health.updateHealth(numUpdates) ;
    if (health.conscious()) {
      psyche.updatePsyche(numUpdates) ;
      if (assignedBase() != null) {
        assignedBase().intelMap.liftFogAround(this, health.sightRange()) ;
      }
    }
    else if (health.decomposed()) setAsDestroyed() ;
  }
  
  
  
  /**  Dealing with state changes-
    */
  protected void enterStateKO() {
    final Action falling = new Action(
      this, this, this, "actionFall",
      Action.FALL, "Stricken"
    ) ;
    psyche.cancelBehaviour(psyche.rootBehaviour()) ;
    this.assignAction(falling) ;
  }
  
  
  public boolean actionFall(Actor actor, Actor fallen) {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected boolean visibleTo(Base base) {
    if (indoors()) return false ;
    return super.visibleTo(base) ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //
    //  Make a few basic sanity checks for visibility-
    //if (indoors()) return ;
    final float scale = spriteScale() ;
    final Sprite s = sprite() ;
    
    final Healthbar bar = new Healthbar() ;
    bar.level = 1 - health.stressLevel() ;
    bar.size = health.maxHealth() * 2 ;
    bar.matchTo(s) ;
    bar.position.z -= radius() ;
    rendering.addClient(bar) ;
    
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
    if (actionTaken != null) {
      /*
      if (BaseUI.isPicked(this)) I.say(
        this+" action name/progress "+
        actionTaken.animName()+" "+actionTaken.animProgress()
      ) ;
      //*/
      s.setAnimation(actionTaken.animName(), actionTaken.animProgress()) ;
    }
    super.renderFor(rendering, base) ;
    //
    //  TODO:  Last but not least, you need to render any weapon or shield FX
    //  associated with your gear.
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
}

