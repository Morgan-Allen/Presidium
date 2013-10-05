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


public abstract class ActorAI implements Aptitudes {
  
  
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
  
  protected Table <Element, Session.Saveable> seen = new Table() ;
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
      final Element e = (Element) s.loadObject() ;
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
    */
  private Session.Saveable reactionKey(Element seen) {
    if (seen instanceof Actor) {
      final Actor a = (Actor) seen ;
      if (a.currentAction() == null) return seen ;
      final Behaviour b = a.mind.rootBehaviour() ;
      if (b == null) return seen ;
      else return b ;
    }
    else return seen ;
  }
  
  
  public boolean awareOf(Element e) {
    return seen.get(e) != null ;
  }
  
  
  public Batch <Element> awareOf() {
    final Batch <Element> seen = new Batch <Element> () ;
    for (Element e : this.seen.keySet()) seen.add(e) ;
    return seen ;
  }
  
  
  protected boolean notices(Element e, float noticeRange) {
    final int roll = Rand.index(20) ;
    if (roll == 0 ) noticeRange *= 2 ;
    if (roll == 19) noticeRange /= 2 ;
    
    if (e instanceof Mobile) {
      final Mobile m = (Mobile) e ;
      if (m.indoors()) noticeRange /= 2 ;
    }
    if (e instanceof Actor) {
      final Actor a = (Actor) e ;
      if (a.targetFor(null) == actor) noticeRange *= 2 ;
    }
    if (e instanceof Fixture) {
      final Fixture f = (Fixture) e ;
      noticeRange += f.size * 2 ;
    }
    //
    //  TODO:  Incorporate line-of-sight considerations here.
    noticeRange -= Combat.stealthValue(e, actor) ;
    if (awareOf(e)) noticeRange += World.SECTOR_SIZE / 2f ;
    
    return Spacing.distance(actor, e) < noticeRange ;
  }
  
  
  protected void updateSeen() {
    
    final World world = actor.world() ;
    final float sightRange = actor.health.sightRange() ;
    final int reactLimit = 2 + (int) (actor.traits.traitLevel(INSIGHT) / 5) ;
    
    final Batch <Element>
      couldSee   = new Batch <Element> (),
      justSaw    = new Batch <Element> (),
      outOfSight = new Batch <Element> () ;
    //
    //  Firstly, cull anything you can't see any more-
    for (Element e : seen.keySet()) {
      if (! notices(e, sightRange)) outOfSight.add(e) ;
    }
    for (Element e : outOfSight) seen.remove(e) ;
    //
    //  Then, sample nearby objects you could react to-
    world.presences.sampleFromKeys(
      actor, world, reactLimit, couldSee,
      Mobile.class,
      Venue.class
    ) ;
    for (Behaviour b : world.activities.targeting(actor)) {
      if (b instanceof Action) {
        final Actor a = ((Action) b).actor ;
        if (Spacing.distance(a, actor) > World.SECTOR_SIZE) continue ;
        couldSee.add(a) ;
      }
    }
    //
    //  And check to see if they're anything new.
    for (Element m : couldSee) {
      if (! notices(m, sightRange)) continue ;
      final Session.Saveable after = reactionKey(m), before = seen.get(m) ;
      if (before != after) justSaw.add(m) ;
      seen.put(m, after) ;
    }
    //
    //  Finally, add reactions to anything novel-
    for (Element NS : justSaw) {
      final Behaviour reaction = reactionTo(NS) ;
      if (couldSwitchTo(reaction)) assignBehaviour(reaction) ;
    }
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
      I.say("  CURRENT FAVOURITE: "+taken) ;
      ///I.say("  Finished? "+taken.finished()) ;
    }
    return taken ;
  }
  
  
  protected abstract Behaviour createBehaviour() ;
  protected abstract Behaviour reactionTo(Element m) ;
  
  
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
        I.say("  Current action "+current) ;
        I.say("  Next step "+next) ;
        I.say("  Done "+isDone) ;
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
      final float priority = mission.priorityFor(actor) ;
      if (priority < Plan.ROUTINE && work != null) continue ;
      if (priority > chosen.priorityFor(actor)) {
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
      if (verbose) I.sayAbout(actor, " SAVING PLAN AS TODO: "+replaced) ;
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
    if (! actor.health.conscious()) return false ;
    return couldSwitch(rootBehaviour(), next) ;
  }
  
  
  protected boolean couldSwitch(Behaviour last, Behaviour next) {
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
    float baseUnit = actor.gear.credits() ;
    if (work instanceof Venue) {
      baseUnit += (100 + ((Venue) work).personnel.salaryFor(actor)) / 2f ;
    }
    baseUnit /= 3f ;
    return (credits / baseUnit) * actor.traits.scaleLevel(ACQUISITIVE) ;
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
    if (AB != null) {
      if (base == AB) return 1 ;
      if (base == null) return 0 ;
      return AB.relationWith(base) ;
    }
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












/*
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
//*/
