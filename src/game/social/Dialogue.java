/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Dialogue extends Plan implements ActorConstants {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  final static int
    STAGE_GREETING = 0,
    STAGE_TALKING  = 1,
    STAGE_GOODBYE  = 3,
    
    TRANSCRIPT_SIZE = 5 ;
  final static float
    FINISH_CHANCE = 0.1f ;
  
  final Actor other ;
  protected Boardable location = null ;
  
  
  //  TODO:  Introduce various stages (above) instead.
  boolean finished = false ;
  Stack <String> transcript = new Stack <String> () ;
  
  
  
  public Dialogue(Actor actor, Actor other) {
    super(actor, actor, other) ;
    this.other = other ;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s) ;
    this.other = (Actor) s.loadObject() ;
    this.location = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other) ;
    s.saveTarget(location) ;
  }
  
  
  
  /**  Static helper methods.  (I don't make these local, since I want to
    *  emphasise that these are supposed to be independant of any particular
    *  conversation, or decide whether it even happens.)
    */
  //
  //  Returns whether Actor can talk to Other.
  public static boolean canTalk(Actor actor, Actor other) {
    if (actor == other) return false ;
    if (isListening(actor, other)) return true ;
    return actor.psyche.couldSwitch(
      actor.psyche.rootBehaviour(), new Dialogue(actor, other)
    ) ;
  }
  
  //
  //  Returns whether Actor is listening to Other.
  static boolean isListening(Actor actor, Actor other) {
    final Behaviour root = actor.psyche.rootBehaviour() ;
    if (! (root instanceof Dialogue)) return false ;
    return ((Dialogue) root).other == other ;
  }
  
  
  static Boardable pickLocation(Dialogue dialogue) {
    final Actor A = dialogue.actor, O = dialogue.other ;
    if (A.aboard() == O.aboard()) return A.aboard() ;
    final Vec3D p = A.position(null).add(O.position(null)).scale(0.5f) ;
    final Tile t = A.world().tileAt(p.x, p.y) ;
    return Spacing.nearestOpenTile(t, O) ;
  }
  
  
  static Skill mannersFor(Actor actor) {
    if (actor instanceof Human) {
      final Vocation birth = ((Human) actor).career().birth() ;
      if (birth == Vocation.NATIVE_BIRTH) return NATIVE_TABOO ;
      if (birth == Vocation.HIGH_BIRTH) return NOBLE_ETIQUETTE ;
      return COMMON_CUSTOM ;
    }
    //
    //  TODO:  Create/re-locate/use species traits.  And use a wider variety of
    //  skills for non-human actors.
    return SUASION ;
  }
  
  
  static float firstImpression(Actor actor, Actor other) {
    float impression = -10 * Rand.num() ;
    if (other.traits.test(mannersFor(other), ROUTINE_DC, 1)) impression += 10 ;
    if (other.traits.test(SUASION, ROUTINE_DC, 1)) impression += 10 ;
    impression += actor.attraction(other) * Rand.avgNums(2) ;
    return impression ;
  }
  
  
  public float priorityFor(Actor actor) {
    float priority = ROUTINE / 4f ;
    priority += actor.attraction(other) / 4f ;
    priority += actor.psyche.relationTo(other) / 2f ;
    priority += actor.traits.trueLevel(SOCIABLE) ;
    
    priority += actor.psyche.curiosity(Dialogue.class, actor, other) * 5 ;
    I.say(actor+" priority for dialogue with "+other+" is "+priority) ;
    
    return Visit.clamp(priority, 0, ROUTINE) ;
  }
  
  
  
  /**  Implementing the actual sequence of actions.
    */
  //
  //  The attraction here really needs to be based on how much information the
  //  two have to exchange.
  //  Basic info- homeworld, birth, vocation.
  //  Recent events- last couple of behaviours known.
  //  Fields of interest- skills and traits.
  //  Acquaintances- friends, foes and family.
  //  Trigger based on association?
  protected Behaviour getNextStep() {
    if (! canTalk(actor, other)) {
      BaseUI.logFor(actor, actor+" can't talk right now, breaking dialogue.") ;
      finishDialogue() ;
      return null ;
    }
    if (finished) return null ;
    //
    //  TODO:  Base this selection on a Stage field (above.)
    if (location == null) {
      final Action greet = new Action(
        actor, other,
        this, "actionGreet",
        Action.TALK, "Greeting "+other
      ) ;
      greet.setMoveTarget(actor) ;
      return greet ;
    }
    //
    //  Otherwise, stand around and chat-
    final Action talk = new Action(
      actor, other,
      this, "actionTalk",
      Action.TALK_LONG, "Talking to "+other
    ) ;
    talk.setMoveTarget(location) ;
    return talk ;
  }
  
  
  private void joinDialogue(Actor other) {
    BaseUI.logFor(actor, actor+" starting dialogue with "+other) ;
    final Dialogue forOther = new Dialogue(other, actor) ;
    if (location instanceof Tile) {
      forOther.location = Spacing.nearestOpenTile((Tile) location, other) ;
    }
    else forOther.location = location ;
    other.psyche.assignBehaviour(forOther) ;
  }

  
  private void finishDialogue() {
    BaseUI.logFor(actor, actor+" finished dialogue.") ;
    this.finished = true ;
    final Behaviour root = other.psyche.rootBehaviour() ;
    if (root instanceof Dialogue) {
      final Dialogue d = ((Dialogue) root) ;
      if (d.other == actor) d.finished = true ;
    }
  }
  
  
  public boolean actionGreet(Actor actor, Actor other) {
    if (canTalk(other, actor)) {
      location = pickLocation(this) ;
      joinDialogue(other) ;
      return true ;
    }
    else {
      BaseUI.logFor(actor, other+" can't talk right now, breaking dialogue.") ;
      finishDialogue() ;
      return false ;
    }
  }
  
  
  public boolean actionTalk(Actor actor, Actor other) {
    if (finished || ! canTalk(other, actor)) {
      finishDialogue() ;
      return false ;
    }

    //*
    I.say(actor+" talking to "+other) ;
    final float attLevel = other.psyche.relationTo(actor) / Relation.MAX_ATT ;
    float success = 0 ;
    if (actor.traits.test(mannersFor(actor), ROUTINE_DC, 1)) success += 5 ;
    if (actor.traits.test(SUASION, attLevel * -20, 1)) success += 5 ;
    ///success += other.psyche.attraction(actor) * Rand.num() ;
    //success += (other.psyche.relationTo(other) / Relation.MAX_ATT) * Rand.num() ;
    success /= 2 ;
    
    other.psyche.incRelation(actor, success) ;
    actor.health.liftStress(success / 5f) ;
    //*/
    //float success = 0.9f ;
    
    if (Rand.num() < FINISH_CHANCE && Rand.index(10) > success) {
      finishDialogue() ;
    }
    return true ;
  }
  
  
  private void introduce(Actor actor, Actor other) {
    //
    //  Compare vocations, physical traits and appearance, and proper manners.
    //  Set up initial relationships.
  }
  
  private void gossip(Actor actor, Actor other) {
    //
    //  Pick a random recent activity and see if the other also indulged in it.
    //  If the activity is similar, or was undertaken for similar reason,
    //  improve relations.
  }
  
  private void advise(Actor actor, Actor other) {
    //
    //  Pick a random skill and see if the other knows it.  Possibly instruct
    //  the other in the skill in question, if their level is lower.  If so,
    //  improve relations.
  }
  
  private void request(Actor actor, Actor other) {
    //
    //  Pick something the actor wants, and ask the other for it.  If it's a
    //  reasonable request, grant it.
  }
  
  
  /**  Rendering and interface methods-
    */
  private void transcribe(Actor actor, String phrase) {
    transcript.addLast(phrase) ;
    if (transcript.size() > TRANSCRIPT_SIZE) transcript.removeFirst() ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(actor) ;
    d.append(" talking to ") ;
    d.append(other) ;
  }
}






//
//  There should be a few basic 'moves' here-
//    First Impressions/Attraction
//    Small Talk/Introduction
//    Anecdotes/Gossip
//    Advice/Assistance
//    Dispute/Sex Escalation
//    Kinship Modifiers/Exceptions


/*
  static int testManners(Dialogue dialogue) {
    final Actor A = dialogue.actor, O = dialogue.other ;
    int result = 0 ;
    final Skill MA = mannersFor(A), MO = mannersFor(O) ;
    result += A.traits.test(MO, ROUTINE_DC, 1) ? 1 : -1 ;
    result += O.traits.test(MA, ROUTINE_DC, 1) ? 1 : -1 ;
    return result * 5 ;
  }
//*/

/*
    //
    //  Select a recent plan undertaken by the actor, and tell the other actor
    //  about it.  Approval tends to rise based on good manners, and points in
    //  common- traits, skills, vocations or activities.
    final Class
      actorAC[] = actor.psyche.recentActivities(),
      otherAC[] = other.psyche.recentActivities() ;
    final Class pick = (Class) Rand.pickFrom(actorAC) ;
    //
    //  TODO:  Get a list of actual plans so you can work off associations.
    if (Visit.arrayIncludes(otherAC, pick)) {
      actor.psyche.incRelation(other, 5) ;
      other.psyche.incRelation(actor, 5) ;
    }
//*/


