/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.util.* ;






public abstract class ActorPsyche implements ActorConstants {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  final static int
    MAX_MEMORIES  = 100,
    MAX_RELATIONS = 100,
    MAX_VALUES    = 100 ;
  
  /*
  static class Memory {
    Class planClass ;
    Session.Saveable signature[] ;
    float timeBegun, timeEnded ;
  }
  //*/
  
  
  final protected Actor actor ;
  
  protected Stack <Behaviour> behaviourStack = new Stack <Behaviour> () ;
  //  TODO:  CREATE A LIST OF BEHAVIOURS TO GO BACK TO!  USE THAT FOR MISSIONS,
  //  FARMING, PURCHASES, ET CETERA!
  protected List <Behaviour> todoList = new List <Behaviour> () ;
  
  protected Table <Element, Element> seen = new Table <Element, Element> () ;
  protected Table <Actor, Relation> relations = new Table <Actor, Relation> () ;
  
  protected Mission mission ;
  protected Venue home ;
  protected Employment work ;
  
  
  
  protected ActorPsyche(Actor actor) {
    this.actor = actor ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(behaviourStack) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Element e = (Element) s.loadObject() ;
      seen.put(e, e) ;
    }
    
    mission = (Mission) s.loadObject() ;
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
    
    s.saveObject(mission) ;
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
  private void pushBehaviour(Behaviour b) {
    behaviourStack.addFirst(b) ;
    actor.world().activities.toggleActive(b, true) ;
  }
  
  
  private Behaviour popBehaviour() {
    final Behaviour b = behaviourStack.removeFirst() ;
    actor.world().activities.toggleActive(b, false) ;
    return b ;
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    I.say("Assigning behaviour "+behaviour+" to "+actor) ;
    actor.assignAction(null) ;
    cancelBehaviour(rootBehaviour()) ;
    pushBehaviour(behaviour) ;
    //  TODO:  Push unfinished behaviours onto the todoList, and return to them
    //  later if still valid.
  }
  
  
  public void cancelBehaviour(Behaviour b) {
    if (b == null) return ;
    if (! behaviourStack.includes(b)) I.complain("Behaviour not active.") ;
    while (behaviourStack.size() > 0) {
      final Behaviour popped = popBehaviour() ;
      if (popped == b) break ;
    }
    if (behaviourStack.includes(b)) I.complain("Duplicate behaviour!") ;
  }
  
  
  public void assignMission(Mission mission) {
    this.mission = mission ;
    //
    //  This might have to be done with all bases.
    for (Mission m : actor.assignedBase().allMissions()) if (m != mission) {
      m.setApplicant(actor, false) ;
    }
  }
  
  
  public boolean couldSwitchTo(Behaviour next) {
    return couldSwitch(rootBehaviour(), next) ;
  }
  
  
  public boolean couldSwitch(Behaviour last, Behaviour next) {
    if (next == null) return false ;
    if (last == null) return true ;
    if (! actor.health.conscious()) return false ;
    return next.priorityFor(actor) >= (last.priorityFor(actor) + 2) ;
  }
  
  
  
  /**  Updates and queries-
    */
  protected Action getNextAction() {
    while (true) {
      //
      //  TODO:  You'll have to try getting Behaviours from the todo-list here.
      if (behaviourStack.size() == 0) {
        final Behaviour root = nextBehaviour() ;
        if (root == null) return null ;
        pushBehaviour(root) ;
      }
      final Behaviour current = topBehaviour() ;
      final Behaviour next = current.nextStepFor(actor) ;
      if (current.complete() || next == null) popBehaviour() ;
      else if (current instanceof Action) return (Action) current ;
      else pushBehaviour(next) ;
    }
  }
  
  
  protected abstract Behaviour nextBehaviour() ;
  
  
  protected void updatePsyche(int numUpdates) {
    if (numUpdates % 10 == 0 && behaviourStack.size() > 0) {
      final Behaviour
        last = rootBehaviour(),
        next = nextBehaviour() ;
      if (couldSwitch(last, next)) assignBehaviour(next) ;
    }
    for (Behaviour b : behaviourStack) if (b.monitor(actor)) break ;
  }
  
  
  public Stack <Behaviour> agenda() {
    return behaviourStack ;
  }
  
  
  public Behaviour topBehaviour() {
    return behaviourStack.getFirst() ;
  }
  
  
  public Behaviour rootBehaviour() {
    return behaviourStack.getLast() ;
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
  
  
  
  /**  Methods related to the value system-
    *  Fear.  Love/Hate.  Aggression.
    *  Greed.  Curiosity.  Restlessness.
    *  Sociability.  Loyalty.  Stubbornness.
    *  
    *  These should all return values centred around 1.0f, 1 being typical,
    *  zero being low, 2.0f or more being high.  Obvious enough.  Point being
    *  it's a scalar operation.
    */
  public float greedFor(int credits) {
    float val = credits / 100f ;
    val = (float) Math.sqrt(val) ;
    val *= actor.traits.scaleLevel(ACQUISITIVE) ;
    
    //
    //  Cut this down based on how many thousand credits the actor has ATM.
    float reserves = actor.gear.credits() / 1000f ;
    reserves /= actor.traits.scaleLevel(ACQUISITIVE) ;
    val /= (0.5f + reserves) ;
    ///I.say("Greed value: "+val) ;
    return val ;
  }
}











//memories.addFirst(memory) ;
//if (memories.size() > MAX_MEMORIES) memories.removeLast() ;
/*
for (Memory memory : memories) {
  if (memory.equals(plan)) {
    memory.timeEnded = actor.world().currentTime() ;
    break ;
  }
}
//*/

/*
//  TODO:  You need to save and load memories.
//  TODO:  Just remember plans instead?  More direct, certainly.  ...Maybe.
protected List <Memory> memories = new List <Memory> () ;
//*/



//  TODO:  This may be too complicated, particularly for larger settlements.
//         Just make Dialogue something of Idle priority.
/*
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
//*/

/*
public Class[] recentActivities() {
  final Class recent[] = new Class[memories.size()] ;
  int n = 0 ; for (Memory memory : memories) {
    recent[n++] = memory.planClass ;
  }
  return recent ;
}
//*/

