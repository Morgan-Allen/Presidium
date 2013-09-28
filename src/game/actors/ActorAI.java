/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.user.* ;
import src.util.* ;




//  Include a list of past behaviours as well, or their types?  As memories?
//  What about a separate list of elements currently 'aware of'?  This is
//  going to be the basis of a bunch of activities.


public abstract class ActorAI implements ActorConstants {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  final static int
    MAX_MEMORIES  = 100,
    MAX_RELATIONS = 100,
    MAX_VALUES    = 100 ;
  
  private static boolean verbose = false ;
  
  
  final protected Actor actor ;
  
  protected Stack <Behaviour> agenda = new Stack() ;
  protected List <Behaviour> todoList = new List() ;
  
  protected Table <Mobile, Session.Saveable> seen = new Table() ;
  
  protected Table <Accountable, Relation> relations = new Table() ;
  
  protected Mission mission ;
  protected Venue home ;
  protected Employment work ;
  
  
  
  protected ActorAI(Actor actor) {
    this.actor = actor ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(agenda) ;
    s.loadObjects(todoList) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Mobile e = (Mobile) s.loadObject() ;
      seen.put(e, s.loadObject()) ;
    }
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Relation r = Relation.loadFrom(s) ;
      relations.put((Actor) r.subject, r) ;
    }
    
    mission = (Mission) s.loadObject() ;
    home = (Venue) s.loadObject() ;
    work = (Employment) s.loadObject() ;
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(agenda) ;
    s.saveObjects(todoList) ;
    s.saveInt(seen.size()) ;
    for (Element e : seen.keySet()) {
      s.saveObject(e) ;
      s.saveObject(seen.get(e)) ;
    }
    s.saveInt(relations.size()) ;
    for (Relation r : relations.values()) Relation.saveTo(s, r) ;
    
    s.saveObject(mission) ;
    s.saveObject(home) ;
    s.saveObject(work) ;
  }
  
  
  
  /**  Dealing with seen objects and reactions to them-
    *  TODO:  This needs to handle stealth effects, which can affect how
    *  easily an actor is spotted and how easily they can be missed.
    */
  //
  //    TODO:  Use this as the basis for all other behaviours that work with
  //    'batches' of potential targets... which means the actor can sometimes
  //    'see' things quite far away, albeit in a randomised fashion.
  
  
  protected void updateSeen() {
    final PresenceMap mobiles = actor.world().presences.mapFor(Mobile.class) ;
    final float sightMod = actor.indoors() ? 0.5f : 1 ;
    final float sightRange = actor.health.sightRange() * sightMod ;
    final float lostRange = sightRange * 1.5f ;
    final int reactLimit = (int) (actor.traits.traitLevel(INSIGHT) / 2) ;
    //
    //  Firstly, remove any elements that have escaped beyond sight range.
    final Batch <Element> outOfRange = new Batch <Element> () ;
    for (Mobile e : seen.keySet()) {
      if (
        (! e.inWorld()) || e.indoors() ||
        Spacing.distance(e, actor) > lostRange
      ) {
        outOfRange.add(e) ;
        if (verbose) I.sayAbout(actor, "Can no longer see: "+e) ;
      }
    }
    for (Element e : outOfRange) seen.remove(e) ;
    //
    //  Secondly, add any elements that have entered the requisite range-
    final Batch <Mobile> newSeen = new Batch <Mobile> () ;
    int numR = 0 ; for (Target t : mobiles.visitNear(actor, -1, null)) {
      if (t == actor) continue ;
      if (++numR > reactLimit) break ;
      if (Spacing.distance(actor, t) <= sightRange) {
        final Mobile m = (Mobile) t ;
        final Session.Saveable after = activityFor(m), before = seen.get(m) ;
        if (before != after) newSeen.add(m) ;
        seen.put(m, after) ;
      }
      else break ;
    }
    //
    //  And react to anything fresh-
    for (Mobile NS : newSeen) {
      ///if (BaseUI.isPicked(actor)) I.say("  "+actor+" REACTING TO: "+NS) ;
      final Behaviour reaction = reactionTo(NS) ;
      if (couldSwitchTo(reaction)) assignBehaviour(reaction) ;
    }
  }
  
  
  private Session.Saveable activityFor(Mobile m) {
    if (m instanceof Actor) {
      final Actor a = (Actor) m ;
      if (a.currentAction() == null) return m ;
      final Behaviour b = a.mind.rootBehaviour() ;
      if (b == null) return m ;
      else return b ;
    }
    else return m ;
  }
  
  
  public boolean canSee(Element e) {
    return seen.get(e) != null ;
  }
  
  
  public Batch <Element> seen() {
    final Batch <Element> seen = new Batch <Element> () ;
    for (Element e : this.seen.keySet()) seen.add(e) ;
    return seen ;
  }
  
  
  
  
  /**  Calling regular, periodic updates and triggering AI refreshments-
    */
  protected void updateAI(int numUpdates) {
    updateSeen() ;
    if (home != null && home.destroyed()) home = null ;
    if (work != null && work.destroyed()) work = null ;
    if (numUpdates % 10 == 0) {
      for (Behaviour b : todoList) if (b.finished()) todoList.remove(b) ;
      final Behaviour
        last = rootBehaviour(),
        next = nextBehaviour() ;
      if (couldSwitch(last, next)) assignBehaviour(next) ;
    }
    for (Behaviour b : agenda) if (b.monitor(actor)) break ;
  }
  
  
  private Behaviour nextBehaviour() {
    final Behaviour
      notDone = new Choice(actor, todoList).weightedPick(0),
      newChoice = createBehaviour(),
      taken = couldSwitch(notDone, newChoice) ? newChoice : notDone ;
    if (verbose && I.talkAbout == actor) {
      I.say("  Persistance: "+persistance()) ;
      I.say("  NOT DONE: "+notDone) ;
      I.say("  NEW CHOICE: "+newChoice) ;
      I.say("  CURRENT FAVOURITE: "+taken+" "+taken.hashCode()) ;
      I.say("  Finished? "+taken.finished()) ;
    }
    return taken ;
  }
  
  
  protected abstract Behaviour createBehaviour() ;
  protected abstract Behaviour reactionTo(Mobile m) ;
  
  
  
  /**  Setting home and work venues-
    */
  public static interface Employment extends Session.Saveable, Boardable {
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
  
  
  protected void onWorldExit() {
  }
  
  
  
  /**  Handling missions-
    */
  //
  //  TODO:  This may have to be thought over.
  public void assignMission(Mission mission) {
    this.mission = mission ;
    //
    //  This might have to be done with all bases.
    for (Mission m : actor.base().allMissions()) if (m != mission) {
      m.setApplicant(actor, false) ;
    }
  }
  
  
  protected void applyForMissions(Behaviour chosen) {
    if (mission != null || actor.base() == null) return ;
    for (Mission mission : actor.base().allMissions()) {
      if (! mission.open()) continue ;
      if (couldSwitch(chosen, mission)) {
        mission.setApplicant(actor, true) ;
      }
      else mission.setApplicant(actor, false) ;
    }
  }
  
  
  
  /**  Methods related to maintaining the agenda stack-
    */
  private void pushBehaviour(Behaviour b) {
    if (todoList.contains(b)) todoList.remove(b) ;
    agenda.addFirst(b) ;
    actor.world().activities.toggleActive(b, true) ;
  }
  
  
  private Behaviour popBehaviour() {
    final Behaviour b = agenda.removeFirst() ;
    actor.world().activities.toggleActive(b, false) ;
    if (b != null) b.onSuspend() ;
    return b ;
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    if (verbose) I.sayAbout(actor, "Assigning behaviour "+behaviour) ;
    actor.assignAction(null) ;
    final Behaviour replaced = rootBehaviour() ;
    cancelBehaviour(replaced) ;
    pushBehaviour(behaviour) ;
    if (replaced != null && ! replaced.finished()) {
      //if (verbose)
      I.sayAbout(actor, " SAVING PLAN AS TODO: "+replaced) ;
      todoList.include(replaced) ;
    }
  }
  
  
  public void pushFromParent(Behaviour b, Behaviour parent) {
    if (! agenda.includes(parent)) I.complain("Behaviour not active.") ;
    cancelBehaviour(parent) ;
    pushBehaviour(parent) ;
    pushBehaviour(b) ;
    actor.assignAction(null) ;
  }
  
  
  public void cancelBehaviour(Behaviour b) {
    if (b == null) return ;
    if (! agenda.includes(b)) return ;
    while (agenda.size() > 0) {
      final Behaviour popped = popBehaviour() ;
      if (popped == b) break ;
    }
    if (agenda.includes(b)) I.complain("Duplicate behaviour!") ;
    actor.assignAction(null) ;
  }
  
  
  public boolean couldSwitchTo(Behaviour next) {
    return couldSwitch(rootBehaviour(), next) ;
  }
  
  
  public boolean couldSwitch(Behaviour last, Behaviour next) {
    if (! actor.health.conscious()) return false ;
    if (next == null) return false ;
    if (last == null) return true ;
    //
    //  TODO:  CONSIDER GETTING RID OF THIS CLAUSE?  It's handy in certain
    //  situations, (e.g, where completing the current plan would come at 'no
    //  cost' to the next plan,) but may be more trouble than it's worth.
    final Target NT = targetFor(next) ;
    if (NT != null && targetFor(last) == NT && NT != actor.aboard()) {
      return false ;
    }
    final float
      lastPriority = last.priorityFor(actor),
      persist = persistance(),
      margin = Math.min(
        lastPriority + persist,
        lastPriority * (1 + (persist / 2))
      ) ;
    return next.priorityFor(actor) >= margin ;
  }
  
  
  private Target targetFor(Behaviour b) {
    final Behaviour n = b.nextStepFor(actor) ;
    if (n instanceof Action) return ((Action) n).target() ;
    else if (n == null || n.finished()) return null ;
    else return targetFor(n) ;
  }
  
  
  public void clearAgenda() {
    if (rootBehaviour() != null) cancelBehaviour(rootBehaviour()) ;
    todoList.clear() ;
  }
  
  
  
  /**  Updates and queries-
    */
  protected Action getNextAction() {
    final int MAX_LOOP = 100 ;  // Safety feature, see below...
    for (int loop = MAX_LOOP ; loop-- > 0 ;) {
      if (verbose) I.sayAbout(actor, actor+" in action loop.") ;
      //
      //  If all current behaviours are complete, generate a new one.
      if (agenda.size() == 0) {
        final Behaviour taken = nextBehaviour() ;
        if (taken == null) {
          if (verbose) I.sayAbout(actor, "No next behaviour!") ;
          return null ;
        }
        pushBehaviour(taken) ;
      }
      //
      //  Root behaviours which return null, but aren't complete, should be
      //  stored for later.  Otherwise, unfinished behaviours should return
      //  their next step.
      final Behaviour current = topBehaviour() ;
      final Behaviour next = current.nextStepFor(actor) ;
      final boolean isDone = current.finished() ;
      if (verbose && I.talkAbout == actor) {
        I.say("Current action "+current) ;
        I.say("Next step "+next) ;
        I.say("Done "+isDone) ;
      }
      if (isDone || next == null) {
        if (current == rootBehaviour() && ! isDone) {
          todoList.add(current) ;
        }
        popBehaviour() ;
      }
      else if (current instanceof Action) {
        if (verbose) I.sayAbout(actor, "Next action: "+current) ;
        return (Action) current ;
      }
      else {
        pushBehaviour(next) ;
      }
    }
    //
    //  If you exhaust the maximum number of iterations (which I assume *would*
    //  be enough for any reasonable use-case,) report the problem.
    I.say("  "+actor+" COULD NOT DECIDE ON NEXT STEP.") ;
    final Behaviour root = rootBehaviour() ;
    final Behaviour next = root.nextStepFor(actor) ;
    I.say("Root behaviour: "+root) ;
    I.say("Next step: "+next) ;
    I.say("Valid/finished "+next.valid()+"/"+next.finished()) ;
    new Exception().printStackTrace() ;
    return null ;
  }
  
  
  public Stack <Behaviour> agenda() {
    return agenda ;
  }
  
  
  public Behaviour topBehaviour() {
    return agenda.getFirst() ;
  }
  
  
  public Behaviour rootBehaviour() {
    return agenda.getLast() ;
  }
  
  
  public boolean hasToDo(Class planClass) {
    for (Behaviour b : agenda) if (b.getClass() == planClass) return true ;
    for (Behaviour b : todoList) if (b.getClass() == planClass) return true ;
    return false ;
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
    float baseUnit = actor.gear.credits() + 100f ;
    if (work instanceof Venue) {
      baseUnit += ((Venue) work).personnel.salaryFor(actor) ;
    }
    baseUnit /= 3f ;
    return (credits / baseUnit) * actor.traits.scaleLevel(ACQUISITIVE) ;
    /*
    float val = credits / 100f ;
    val = (float) Math.sqrt(val) ;
    val *= actor.traits.scaleLevel(ACQUISITIVE) ;
    //
    //  Cut this down based on how many thousand credits the actor has ATM.
    float reserves = actor.gear.credits() / 1000f ;
    ///I.sayFor(actor, "reserves are:" +reserves+" "+actor.gear.credits()) ;
    reserves /= actor.traits.scaleLevel(ACQUISITIVE) ;
    if (reserves < 0) return val * 2 ;
    else val /= (0.5f + reserves) ;
    return val ;
    //*/
  }
  
  
  public float persistance() {
    return 2 * actor.traits.scaleLevel(STUBBORN) ;
  }
  
  
  public float whimsy() {
    return 2 / actor.traits.scaleLevel(STUBBORN) ;
  }
  
  
  
  /**  Supplementary methods for behaviour-
    */
  public float attraction(Actor other) {
    if (this.actor.species() != Species.HUMAN) return 0 ;
    if (other.species() != Species.HUMAN) return 0 ;
    //
    //  TODO:  Create exceptions based on age and kinship modifiers.
    //
    //  First, we establish a few facts about each actor's sexual identity:
    float actorG = 0, otherG = 0 ;
    if (actor.traits.hasTrait(GENDER, "Male"  )) actorG = -1 ;
    if (actor.traits.hasTrait(GENDER, "Female")) actorG =  1 ;
    if (other.traits.hasTrait(GENDER, "Male"  )) otherG = -1 ;
    if (other.traits.hasTrait(GENDER, "Female")) otherG =  1 ;
    float attraction = other.traits.traitLevel(HANDSOME) * 3.33f ;
    attraction += otherG * other.traits.traitLevel(FEMININE) * 3.33f ;
    attraction *= (actor.traits.scaleLevel(DEBAUCHED) + 1f) / 2 ;
    //
    //  Then compute attraction based on orientation-
    final String descO = actor.traits.levelDesc(ORIENTATION) ;
    float matchO = 0 ;
    if (descO.equals("Heterosexual")) {
      matchO = (actorG * otherG < 0) ? 1 : 0.33f ;
    }
    else if (descO.equals("Bisexual")) {
      matchO = 0.66f ;
    }
    else if (descO.equals("Homosexual")) {
      matchO = (actorG * otherG > 0) ? 1 : 0.33f ;
    }
    return attraction * matchO / 10f ;
  }
  
  
  public String preferredGender() {
    final boolean male = actor.traits.male() ;
    if (actor.traits.hasTrait(ORIENTATION, "Heterosexual")) {
      return male ? "Female" : "Male" ;
    }
    if (actor.traits.hasTrait(ORIENTATION, "Homosexual")) {
      return male ? "Male" : "Female" ;
    }
    return Rand.yes() ? "Male" : "Female" ;
  }
  
  
  public void setRelation(Actor other, float level, int initTime) {
    final Relation r = new Relation(actor, other, level, initTime) ;
    relations.put(other, r) ;
  }
  
  
  public float relation(Base base) {
    final Base AB = actor.base() ;
    if (base == AB) return 1 ;
    else if (AB != null && base != null) return AB.relationWith(base) ;
    else return 0 ;
  }
  
  
  public float relation(Venue venue) {
    if (venue == null) return 0 ;
    if (venue == home) return 1.0f ;
    if (venue == work) return 0.5f ;
    return relation(venue.base()) / 2f ;
  }
  
  
  public float relation(Actor other) {
    final Relation r = relations.get(other) ;
    if (r == null) {
      return relation(other.base()) / 2 ;
    }
    return r.value() + (relation(other.base()) / 2) ;
  }
  
  
  public float novelty(Actor other) {
    final Relation r = relations.get(other) ;
    if (r == null) return 1 ;
    return r.novelty(actor.world()) ;
  }
  
  
  public void incRelation(Accountable other, float inc) {
    Relation r = relations.get(other) ;
    if (r == null) {
      r = new Relation(actor, other, 0, (int) actor.world().currentTime()) ;
      relations.put(other, r) ;
    }
    r.incValue(inc) ;
  }
  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> () ;
    for (Relation r : relations.values()) all.add(r) ;
    return all ;
  }
}









