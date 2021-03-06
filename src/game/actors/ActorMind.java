/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Create a separate 'ActorSenses' class to handle some of this stuff.


public abstract class ActorMind implements Abilities {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    reactionsVerbose = false ,
    updatesVerbose   = false ;
  
  
  final protected Actor actor ;
  
  final Stack <Behaviour> agenda = new Stack() ;
  final List <Behaviour> todoList = new List() ;
  
  final Table <Element, Session.Saveable> seen = new Table() ;
  final Table <Accountable, Relation> relations = new Table() ;
  protected float anger, fear, solitude, libido, boredom ;
  
  protected Mission mission ;
  protected Employment home, work ;
  protected Application application ;
  protected Actor master ;
  
  
  
  protected ActorMind(Actor actor) {
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
    anger    = s.loadFloat() ;
    fear     = s.loadFloat() ;
    solitude = s.loadFloat() ;
    libido   = s.loadFloat() ;
    boredom  = s.loadFloat() ;
    
    mission = (Mission) s.loadObject() ;
    home = (Employment) s.loadObject() ;
    work = (Employment) s.loadObject() ;
    application = (Application) s.loadObject() ;
    master = (Actor) s.loadObject() ;
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
    s.saveFloat(anger   ) ;
    s.saveFloat(fear    ) ;
    s.saveFloat(solitude) ;
    s.saveFloat(libido  ) ;
    s.saveFloat(boredom ) ;
    
    s.saveObject(mission) ;
    s.saveObject(home) ;
    s.saveObject(work) ;
    s.saveObject(application) ;
    s.saveObject(master) ;
  }
  
  
  protected void onWorldExit() {
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
  
  
  public boolean hasSeen(Element e) {
    return seen.get(e) != null ;
  }
  
  
  public Batch <Element> awareOf() {
    final Batch <Element> seen = new Batch <Element> () ;
    for (Element e : this.seen.keySet()) seen.add(e) ;
    return seen ;
  }
  
  
  protected boolean notices(Element e, float noticeRange) {
    if (e == actor || ! e.inWorld()) return false ;
    if (e == home || e == work) return true ;
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
    final int reactLimit = 3 + (int) (actor.traits.traitLevel(INSIGHT) / 5) ;
    
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
    if (home != null) couldSee.include((Element) home) ;
    if (work != null) couldSee.include((Element) work) ;
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
    if (justSaw.size() > 0) {
      final Choice choice = new Choice(actor) ;
      for (Element NS : justSaw) addReactions(NS, choice) ;
      final Behaviour reaction = choice.pickMostUrgent() ;
      if (couldSwitchTo(reaction)) assignBehaviour(reaction) ;
    }
  }
  
  
  
  
  /**  Calling regular, periodic updates and triggering AI refreshments-
    */
  protected void updateAI(int numUpdates) {
    updateSeen() ;
    updateDrives() ;
    if (numUpdates % 10 != 0) return ;
    //
    //  Remove any expired behaviour-sources:
    if (home != null && home.destroyed()) {
      setHome(null) ;
    }
    if (work != null && work.destroyed()) {
      setWork(null) ;
    }
    if (application != null && ! application.valid()) {
      switchApplication(null) ;
    }
    if (mission != null && mission.finished()) {
      assignMission(null) ;
    }
    //
    //  Cull any expired items on the to-do list, and see if it's worth
    //  switching to a different behaviour-
    for (Behaviour b : todoList) if (b.finished()) todoList.remove(b) ;
    final Behaviour last = rootBehaviour() ;
    final Behaviour next = nextBehaviour() ;
    if (updatesVerbose && I.talkAbout == actor) {
      I.say("\nPerformed periodic AI update.") ;
      final float
        lastP = last == null ? -1 : last.priorityFor(actor),
        nextP = next == null ? -1 : next.priorityFor(actor) ;
      I.say("  LAST PLAN: "+last+" "+lastP) ;
      I.say("  NEXT PLAN: "+next+" "+nextP) ;
      I.say("\n") ;
    }
    if (couldSwitch(last, next)) assignBehaviour(next) ;
  }
  
  
  private Behaviour nextBehaviour() {
    final Behaviour
      notDone = new Choice(actor, todoList).pickMostUrgent(),
      newChoice = createBehaviour(),
      taken = couldSwitch(notDone, newChoice) ? newChoice : notDone ;
    
    if (updatesVerbose && I.talkAbout == actor) {
      //I.say("  Persistance: "+persistance()) ;
      I.say("  LAST PLAN: "+rootBehaviour()) ;
      I.say("  NOT DONE: "+notDone) ;
      I.say("  NEW CHOICE: "+newChoice) ;
      I.say("  CURRENT FAVOURITE: "+taken) ;
      ///I.say("  Finished? "+taken.finished()) ;
    }
    return taken ;
  }
  
  
  protected abstract Behaviour createBehaviour() ;
  protected abstract void addReactions(Element m, Choice choice) ;
  
  
  protected Action getNextAction() {
    final int MAX_LOOP = 100 ;  // Safety feature, see below...
    for (int loop = MAX_LOOP ; loop-- > 0 ;) {
      if (updatesVerbose) I.sayAbout(actor, "...in action loop.") ;
      //
      //  If all current behaviours are complete, generate a new one.
      if (agenda.size() == 0) {
        if (updatesVerbose && I.talkAbout == actor) {
          I.say("Current agenda is empty!") ;
        }
        final Behaviour taken = nextBehaviour() ;
        if (taken == null) {
          if (updatesVerbose) I.sayAbout(actor, "No next behaviour!") ;
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
      if (updatesVerbose && I.talkAbout == actor) {
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
        if (updatesVerbose && I.talkAbout == actor) {
          I.say("Next action: "+current) ;
          I.say("Agenda size: "+agenda.size()) ;
        }
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
    I.say("  Valid/finished "+next.valid()+"/"+next.finished()) ;
    new Exception().printStackTrace() ;
    return null ;
  }
  
  
  
  /**  Setting home and work venues & applications, plus missions-
    */
  public void switchApplication(Application a) {
    if (this.application == a) return ;
    if (application != null) {
      application.employer.personnel().setApplicant(application, false) ;
    }
    application = a ;
    if (application != null) {
      application.employer.personnel().setApplicant(application, true) ;
    }
  }
  
  
  public Application application() {
    return application ;
  }
  
  
  public void setWork(Employment e) {
    if (work == e) return ;
    if (work != null) work.personnel().setWorker(actor, false) ;
    work = e ;
    if (work != null) work.personnel().setWorker(actor, true) ;
  }
  
  
  public Employment work() {
    return work ;
  }
  
  
  public void setHome(Employment home) {
    final Employment old = this.home ;
    if (old == home) return ;
    if (old != null) old.personnel().setResident(actor, false) ;
    this.home = home ;
    if (home != null) home.personnel().setResident(actor, true) ;
  }
  
  
  public Employment home() {
    return home ;
  }
  
  
  public void assignMission(Mission mission) {
    if (this.mission == mission) return ;
    if (this.mission != null) {
      this.mission.setApplicant(actor, false) ;
    }
    this.mission = mission ;
    if (this.mission != null) {
      this.mission.setApplicant(actor, true) ;
    }
  }
  
  
  public Mission mission() {
    return mission ;
  }
  
  
  public void assignMaster(Actor master) {
    this.master = master ;
  }
  
  
  public Actor master() {
    return master ;
  }
  
  
  
  /**  Methods related to maintaining the agenda stack-
    */
  private void pushBehaviour(Behaviour b) {
    if (todoList.includes(b)) todoList.remove(b) ;
    agenda.addFirst(b) ;
    if (updatesVerbose && I.talkAbout == actor) {
      I.say("PUSHING BEHAVIOUR: "+b) ;
    }
    actor.world().activities.toggleBehaviour(b, true) ;
  }
  
  
  private Behaviour popBehaviour() {
    final Behaviour b = agenda.removeFirst() ;
    if (updatesVerbose && I.talkAbout == actor) {
      I.say("POPPING BEHAVIOUR: "+b) ;
    }
    actor.world().activities.toggleBehaviour(b, false) ;
    return b ;
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.") ;
    if (updatesVerbose) I.sayAbout(actor, "Assigning behaviour "+behaviour) ;
    actor.assignAction(null) ;
    final Behaviour replaced = rootBehaviour() ;
    cancelBehaviour(replaced) ;
    pushBehaviour(behaviour) ;
    if (replaced != null && ! replaced.finished()) {
      if (updatesVerbose) I.sayAbout(actor, " SAVING PLAN AS TODO: "+replaced) ;
      todoList.include(replaced) ;
    }
  }
  
  
  public void pushFromParent(Behaviour b, Behaviour parent) {
    if (! agenda.includes(parent)) {
      //I.complain("Behaviour not active.") ;
      return ;
    }
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
  
  
  public boolean mustIgnore(Behaviour next) {
    if (! actor.health.conscious()) return true ;
    return couldSwitch(next, rootBehaviour()) ;
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
      persist = (
        Choice.DEFAULT_PRIORITY_RANGE +
        (actor.traits.relativeLevel(STUBBORN) * Choice.DEFAULT_TRAIT_RANGE)
      ),
      threshold = persist + lastPriority,
      /*
      threshold = Math.min(
        lastPriority + persist,
        lastPriority * (1 + (persist / 2))
      ),
      //*/
      nextPriority = next.priorityFor(actor) ;
    if (reactionsVerbose && I.talkAbout == actor) {
      I.say("Last/next priority is: "+lastPriority+"/"+nextPriority) ;
      I.say("Threshold is: "+threshold) ;
    }
    return nextPriority >= threshold ;
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
  
  
  public Series <Behaviour> agenda() {
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
    float baseUnit = actor.gear.credits() / 2f ;
    if (actor.base() != null) {
      final Profile p = actor.base().profiles.profileFor(actor) ;
      baseUnit += (100 + p.salary()) / 2f ;
    }
    baseUnit /= 2f ;
    //if (updatesVerbose) I.sayAbout(actor, actor+" greed unit is: "+baseUnit) ;
    return (credits / baseUnit) * actor.traits.scaleLevel(ACQUISITIVE) ;
  }
  
  
  
  /**  Supplementary methods for relationships and attitudes-
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
  
  
  public void setRelation(Accountable other, float level, int initTime) {
    final Relation r = new Relation(actor, other, level, initTime) ;
    relations.put(other, r) ;
  }
  
  
  public void incRelation(Accountable other, float inc) {
    Relation r = relations.get(other) ;
    if (r == null) r = initRelation(other, 0) ;
    r.incValue(inc) ;
  }
  
  
  public float relationValue(Base base) {
    final Base AB = actor.base() ;
    if (AB != null) {
      if (base == AB) return 1 ;
      if (base == null) return 0 ;
      return AB.relationWith(base) ;
    }
    else return 0 ;
  }
  
  
  public float relationValue(Venue venue) {
    if (venue == null) return 0 ;
    if (venue == home) return 1.0f ;
    if (venue == work) return 0.5f ;
    return relationValue(venue.base()) / 2f ;
  }
  
  
  public float relationValue(Actor other) {
    final Relation r = relations.get(other) ;
    if (r == null) {
      return relationValue(other.base()) / 2 ;
    }
    if (r.subject == actor) return Visit.clamp(r.value() + 1, 0, 1) ;
    return r.value() + (relationValue(other.base()) / 2) ;
  }
  
  
  public float relationValue(Target other) {
    if (other instanceof Venue) return relationValue((Venue) other) ;
    if (other instanceof Actor) return relationValue((Actor) other) ;
    return 0 ;
  }
  
  
  public float relationNovelty(Actor other) {
    final Relation r = relations.get(other) ;
    if (r == null) return 1 ;
    return r.novelty(actor.world()) ;
  }
  
  
  public Relation initRelation(Accountable other, float value) {
    final Relation r = new Relation(
      actor, other, value, (int) actor.world().currentTime()
    ) ;
    relations.put(other, r) ;
    return r ;
  }
  
  
  public Batch <Relation> relations() {
    final Batch <Relation> all = new Batch <Relation> () ;
    for (Relation r : relations.values()) all.add(r) ;
    return all ;
  }
  
  
  public boolean hasRelation(Accountable other) {
    return relations.get(other) != null ;
  }
  
  
  
  /**  Updates associated with general emotional drives.
    */
  //  TODO:  These might only be suitable for humans?
  //  TODO:  Also, include evaluation of career ambitions here.
  protected void updateDrives() {
    float sumFriends = 0 ;
    for (Relation r : relations.values()) {
      sumFriends += Math.max(0, r.value()) ;
    }
    sumFriends /= Relation.BASE_NUM_FRIENDS ;
    sumFriends /= actor.traits.scaleLevel(SOCIABLE) ;
    solitude = Visit.clamp(1 - sumFriends, 0, 1) ;
  }
  
  
  public float solitude() {
    return solitude ;
  }
}










