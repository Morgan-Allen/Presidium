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



public abstract class Actor extends Mobile implements
  Inventory.Owner, Accountable
{
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorPsyche psyche = new ActorPsyche(this) ;
  final public ActorTraits traits = new ActorTraits(this) ;
  final public ActorGear   gear   = new ActorGear  (this) ;
  
  final public MobilePathing pathing = new MobilePathing(this) ;
  private Action action ;
  private Stack <Behaviour> behaviourStack = new Stack <Behaviour> () ;
  private Base base ;
  Table <Element, Element> seen = new Table <Element, Element> () ;
  
  
  public Actor() {
  }
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    
    health.loadState(s) ;
    psyche.loadState(s) ;
    traits.loadState(s) ;
    gear.loadState(s) ;
    
    pathing.loadState(s) ;
    action = (Action) s.loadObject() ;
    s.loadObjects(behaviourStack) ;
    base = (Base) s.loadObject() ;
    
    //if (true) return ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Element e = (Element) s.loadObject() ;
      seen.put(e, e) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    health.saveState(s) ;
    psyche.saveState(s) ;
    traits.saveState(s) ;
    gear.saveState(s) ;
    
    pathing.saveState(s) ;
    s.saveObject(action) ;
    s.saveObjects(behaviourStack) ;
    s.saveObject(base) ;

    //if (true) return ;
    s.saveInt(seen.size()) ;
    for (Element e : seen.keySet()) s.saveObject(e) ;
  }
  
  
  public ActorGear inventory() { return gear ; }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    assignAction(null) ;
    cancelBehaviour(rootBehaviour()) ;
    psyche.planBegun(behaviour) ;
    behaviourStack.addFirst(behaviour) ;
  }
  
  
  public void cancelBehaviour(Behaviour b) {
    if (b == null) return ;
    if (! behaviourStack.includes(b)) I.complain("Behaviour not active.") ;
    while (behaviourStack.size() > 0) {
      final Behaviour top = behaviourStack.removeFirst() ;
      psyche.planEnded(top) ;
      if (top == b) break ;
    }
  }
  
  
  protected void assignAction(Action action) {
    world.activities.toggleActive(this.action, false) ;
    this.action = action ;
    world.activities.toggleActive(action, true) ;
  }
  
  
  protected abstract Behaviour nextBehaviour() ;
  public abstract boolean couldSwitch(Behaviour next, Behaviour last) ;
  
  
  private Action getNextAction() {
    //
    //  Firstly, check to see if a more compelling behaviour is due-
    if (behaviourStack.size() > 0) {
      final Behaviour
        last = behaviourStack.getLast(),
        next = nextBehaviour() ;
      if (couldSwitch(last, next)) assignBehaviour(next) ;
    }
    //
    //  Otherwise, drill down through the set of behaviours to get a concrete
    //  action-
    while (true) {
      Behaviour step = null ;
      if (behaviourStack.size() == 0) {
        step = nextBehaviour() ;
        if (step == null) return null ;
        psyche.planBegun(step) ;
        behaviourStack.add(step) ;
      }
      final Behaviour current = behaviourStack.getFirst() ;
      if (current.complete() || (step = current.nextStepFor(this)) == null) {
        psyche.planEnded(current) ;
        behaviourStack.removeFirst() ;
      }
      else {
        behaviourStack.addFirst(step) ;
        psyche.planBegun(step) ;
        if (step instanceof Action) return (Action) step ;
      }
    }
  }
  
  
  public void pathingAbort() {
    if (action == null) return ;
    I.say(this+" aborting action...") ;
    psyche.planEnded(behaviourStack.getFirst()) ;
    behaviourStack.removeFirst() ;
    action = null ;
    behaviourStack.getFirst().abortStep() ;
    assignAction(getNextAction()) ;
  }
  
  
  public Action currentAction() {
    return action ;
  }
  
  
  public Behaviour topBehaviour() {
    return behaviourStack.getFirst() ;
  }
  
  
  public Behaviour rootBehaviour() {
    return behaviourStack.getLast() ;
  }
  
  
  public Stack <Behaviour> currentBehaviours() {
    return behaviourStack ;
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
    world.activities.toggleActive(action, false) ;
    super.exitWorld() ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    if (! health.conscious()) return ;
    if (action != null) action.updateAction() ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    health.updateHealth(numUpdates) ;
    if (! health.conscious()) return ;
    //
    //  We check every 10 seconds to see if a more compelling behaviour has
    //  come up.  We also query new actions whenever the current action is
    //  missing or expires, and allow behaviours to monitor the actor at
    //  regular intervals-
    if (numUpdates % 10 == 0 && behaviourStack.size() > 0) {
      final Behaviour
        last = rootBehaviour(),
        next = nextBehaviour() ;
      if (couldSwitch(last, next)) assignBehaviour(next) ;
    }
    if (action == null || action.complete()) {
      assignAction(getNextAction()) ;
    }
    for (Behaviour b : behaviourStack) if (b.monitor(this)) break ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void renderFor(Rendering rendering, Base base) {
    if (indoors()) return ;
    final Sprite s = sprite() ;
    if (action != null) {
      ///I.say("Name/progress "+action.animName()+"/"+action.animProgress()) ;
      s.setAnimation(action.animName(), action.animProgress()) ;
    }
    else s.setAnimation(Action.STAND, 0) ;
    super.renderFor(rendering, base) ;
  }
  
  
  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).setSelection(this) ;
  }

  
  public InfoPanel createPanel(BaseUI UI) {
    return new ActorPanel(UI, this) ;
  }
  
  
  public String toString() {
    return fullName() ;
  }
}









