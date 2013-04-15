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



public abstract class Actor extends Mobile implements Inventory.Owner {
  
  
  
  /**  Field definitions, constructors and save/load functionality-
    */
  final public ActorHealth health = new ActorHealth(this) ;
  final public ActorTraining training = new ActorTraining(this) ;
  final public ActorPathing pathing = new ActorPathing(this) ;
  final public ActorEquipment equipment = new ActorEquipment(this) ;
  
  private Action action ;
  private Stack <Behaviour> behaviourStack = new Stack <Behaviour> () ;
  
  Table <Element, Element> seen = new Table <Element, Element> () ;
  
  
  public Actor() {
    behaviourStack.add(initBehaviour()) ;
  }
  
  
  protected abstract Behaviour initBehaviour() ;
  
  
  public Actor(Session s) throws Exception {
    super(s) ;
    health.loadState(s) ;
    training.loadState(s) ;
    pathing.loadState(s) ;
    equipment.loadState(s) ;
    
    action = (Action) s.loadObject() ;
    s.loadObjects(behaviourStack) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Element e = (Element) s.loadObject() ;
      seen.put(e, e) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    health.saveState(s) ;
    training.saveState(s) ;
    pathing.saveState(s) ;
    equipment.saveState(s) ;
    
    s.saveObject(action) ;
    s.saveObjects(behaviourStack) ;
    
    s.saveInt(seen.size()) ;
    for (Element e : seen.keySet()) s.saveObject(e) ;
  }
  
  
  public ActorEquipment inventory() { return equipment ; }
  
  
  
  /**  Assigning behaviours and actions-
    */
  public void assignAction(Action action) {
    world.activities.toggleActive(this.action, false) ;
    this.action = action ;
    world.activities.toggleActive(action, true) ;
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    behaviourStack.addFirst(behaviour) ;
    assignAction(null) ;
  }
  
  
  private Action getNextAction() {
    while (true) {
      final Behaviour current = behaviourStack.getFirst() ;
      Behaviour step = null ;
      ///I.say(this+" getting next action from "+current) ;
      if (current.complete() || (step = current.nextStepFor(this)) == null) {
        if (behaviourStack.size() == 1) {
          //I.complain("ROOT BEHAVIOUR CANNOT RETURN NULL!") ;
          return null ;
        }
        behaviourStack.removeFirst() ;
      }
      else {
        behaviourStack.addFirst(step) ;
        if (step instanceof Action) {
          //I.say("Returning action: "+step) ;
          return (Action) step ;
        }
      }
    }
  }
  
  
  protected void abortAction() {
    if (action == null) return ;
    I.say(this+" aborting action...") ;
    behaviourStack.removeFirst() ;
    action = null ;
    behaviourStack.getFirst().abortStep() ;
    assignAction(getNextAction()) ;
  }
  
  
  public Action currentAction() {
    return action ;
  }
  
  
  public Stack <Behaviour> currentBehaviours() {
    return behaviourStack ;
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
    //  You'll have to skip this if you're KO or dead.
    if (action != null) action.updateAction() ;
  }
  
  public void updateAsScheduled() {
    super.updateAsScheduled() ;
    //  You'll have to skip this if you're KO or dead.
    if (action == null || action.complete()) {
      assignAction(getNextAction()) ;
    }
    for (Behaviour b : behaviourStack) if (b.monitor(this)) break ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void renderFor(Rendering rendering, Base base) {
    if (aboard() != null) return ;
    final Sprite s = sprite() ;
    if (action != null) {
      s.setAnimation(action.animName(), action.animProgress()) ;
    }
    else s.setAnimation(Model.AnimNames.STAND, 0) ;
    super.renderFor(rendering, base) ;
  }
  
  
  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).setSelection(this) ;
  }
  
  
  public String toString() {
    return fullName()+" (Actor)" ;
  }
}









