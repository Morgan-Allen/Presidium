/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.social.Relation;
import src.util.* ;



public class ActorPsyche implements ActorConstants {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  //  TODO:  Vary these values based on the intellect of the organism.
  final static int
    MAX_MEMORIES  = 100,
    MAX_RELATIONS = 100,
    MAX_VALUES    = 100 ;
  
  
  static class Memory {
    Class planClass ;
    Session.Saveable signature[] ;
    float timeBegun, timeEnded ;
  }
  
  
  final protected Actor actor ;
  
  protected Stack <Behaviour> behaviourStack = new Stack <Behaviour> () ;
  protected Table <Element, Element> seen = new Table <Element, Element> () ;
  
  protected Venue home ;
  protected Employment work ;
  
  //  TODO:  You need to save and load memories.
  //  TODO:  Just remember plans instead?  More direct, certainly.  ...Maybe.
  protected List <Memory> memories = new List <Memory> () ;
  protected Table <Actor, Relation> relations = new Table <Actor, Relation> () ;
  
  
  protected ActorPsyche(Actor actor) {
    this.actor = actor ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(behaviourStack) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Element e = (Element) s.loadObject() ;
      seen.put(e, e) ;
    }
    
    home = (Venue) s.loadObject() ;
    work = (Employment) s.loadObject() ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Relation r = Relation.loadFrom(s) ;
      relations.put((Actor) r.subject, r) ;
    }
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(behaviourStack) ;
    s.saveInt(seen.size()) ;
    for (Element e : seen.keySet()) s.saveObject(e) ;
    
    s.saveObject(home) ;
    s.saveObject(work) ;
    
    s.saveInt(relations.size()) ;
    for (Relation r : relations.values()) Relation.saveTo(s, r) ;
  }
  
  
  
  /**  Dealing with seen objects and reactions to them-
    *  TODO:  This needs to handle stealth effects and the like.  And implement
    *         the rest of it.
    */
  public boolean awareOf(Element e) {
    return actor.health.sightRange() >= Spacing.distance(actor, e) ;
    //Fix later.  Refer to stuff in local records.
  }
  
  
  /**  Setting home and work venues-
    */
  public static interface Employment extends Session.Saveable {
    Behaviour jobFor(Actor actor) ;
    void setWorker(Actor actor, boolean is) ;
  }
  
  
  public void setEmployer(Employment e) {
    if (work != null) work.setWorker(actor, false) ;
    work = e ;
    if (work != null) work.setWorker(actor, true) ;
  }
  
  
  public Employment work() {
    return work ;
  }
  
  
  public void setHomeVenue(Venue home) {
    final Venue old = this.home ;
    if (old != null) old.personnel.setResident(actor, false) ;
    this.home = home ;
    if (home != null) home.personnel.setResident(actor, true) ;
  }
  
  
  public Venue home() {
    return home ;
  }
  
  
  
  /**  Methods related to behaviours-
    */
  public boolean couldSwitchTo(Behaviour next) {
    return couldSwitch(rootBehaviour(), next) ;
  }
  
  
  public boolean couldSwitch(Behaviour last, Behaviour next) {
    if (next == null) return false ;
    if (last == null) return true ;
    return next.priorityFor(actor) >= (last.priorityFor(actor) + 2) ;
  }
  
  
  public Behaviour topBehaviour() {
    return behaviourStack.getFirst() ;
  }
  
  
  public Behaviour rootBehaviour() {
    return behaviourStack.getLast() ;
  }
  
  
  private void pushBehaviour(Behaviour b) {
    behaviourStack.addFirst(b) ;
    actor.world().activities.toggleActive(b, true) ;
    if (! (b instanceof Plan)) return ;
    //
    //  Create a memory of this plan, along with the starting time, etc.
    final Plan plan = (Plan) b ;
    final Memory memory = new Memory() ;
    memory.planClass = plan.getClass() ;
    memory.signature = plan.signature ;
    memory.timeBegun = actor.world().currentTime() ;
    memories.addFirst(memory) ;
    if (memories.size() > MAX_MEMORIES) memories.removeLast() ;
  }
  
  
  private Behaviour popBehaviour() {
    final Behaviour b = behaviourStack.removeFirst() ;
    actor.world().activities.toggleActive(b, false) ;
    if (! (b instanceof Plan)) return b ;
    //
    //  Find the corresponding memory of this plan, and note the ending time.
    final Plan plan = (Plan) b ;
    for (Memory memory : memories) {
      if (memory.equals(plan)) {
        memory.timeEnded = actor.world().currentTime() ;
        break ;
      }
    }
    return b ;
  }
  
  
  protected Action getNextAction() {
    //
    //  Drill down through the set of behaviours to get a concrete action-
    while (true) {
      Behaviour step = null ;
      if (behaviourStack.size() == 0) {
        step = nextBehaviour() ;
        if (step == null) return null ;
        pushBehaviour(step) ;
      }
      final Behaviour current = topBehaviour() ;
      if (current.complete() || (step = current.nextStepFor(actor)) == null) {
        popBehaviour() ;
      }
      else {
        pushBehaviour(step) ;
        if (step instanceof Action) return (Action) step ;
      }
    }
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    actor.assignAction(null) ;
    cancelBehaviour(rootBehaviour()) ;
    pushBehaviour(behaviour) ;
  }
  
  
  public void cancelBehaviour(Behaviour b) {
    if (b == null) return ;
    if (! behaviourStack.includes(b)) I.complain("Behaviour not active.") ;
    while (behaviourStack.size() > 0) {
      final Behaviour top = popBehaviour() ;
      if (top == b) break ;
    }
  }
  
  
  public Stack <Behaviour> currentBehaviours() {
    return behaviourStack ;
  }
  
  
  protected Behaviour nextBehaviour() {
    return null ;
  }
  
  
  protected void updatePsyche(int numUpdates) {
    if (numUpdates % 10 == 0 && behaviourStack.size() > 0) {
      final Behaviour
        last = rootBehaviour(),
        next = nextBehaviour() ;
      if (couldSwitch(last, next)) assignBehaviour(next) ;
    }
    for (Behaviour b : behaviourStack) if (b.monitor(actor)) break ;
  }
  
  
  
  
  /**  Methods related to relationships-
    */
  
  public float relationTo(Actor other) {
    Relation r = relations.get(other) ;
    if (r == null) return 0 ;
    return r.attitude() ;
  }
  
  
  public void incRelation(Actor other, float inc) {
    Relation r = relations.get(other) ;
    if (r == null) relations.put(other, r = new Relation(actor, other)) ;
    r.incRelation(inc) ;
  }
  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> () ;
    for (Relation r : relations.values()) all.add(r) ;
    return all ;
  }
  
  
  
  /**  Methods related to memories-
    */
  
  
  public float curiosity(Class planClass, Session.Saveable... assoc) {
    //
    //  Firstly, see if an existing memory/s match this one-
    Memory match = null ;
    for (Memory memory : memories) {
      if (memory.planClass != planClass) continue ;
      boolean matches = true ;
      for (int i = 0 ; i < assoc.length ; i++) {
        if (assoc[i] != memory.signature[i]) { matches = false ; break ; }
      }
      if (matches) { match = memory ; break ; }
    }
    //
    //  Then, calculate how curious about it the actor would be, based on how
    //  recently/often this event occured-
    
    //  More inquisitive actors have a higher initial attraction to novel
    //  stimuli, but take longer to recharge interest since the last event of
    //  this type.
    
    
    float curiosity = actor.traits.trueLevel(INQUISITIVE) ;
    if (match == null) {
      curiosity += 5 ;
    }
    else {
      final float timeGap = actor.world().currentTime() - match.timeEnded ;
      curiosity -= INQUISITIVE.maxVal ;
      curiosity += (timeGap * 2f / World.DEFAULT_DAY_LENGTH) ;
    }
    return Visit.clamp(curiosity / 10f, 0, 1) ;
  }
  
  
  public Class[] recentActivities() {
    final Class recent[] = new Class[memories.size()] ;
    int n = 0 ; for (Memory memory : memories) {
      recent[n++] = memory.planClass ;
    }
    return recent ;
  }
}









