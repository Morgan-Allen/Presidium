/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.social ;
import src.game.common.* ;
import src.game.planet.Species;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.graphics.sfx.TalkFX ;
import src.user.* ;
import src.util.* ;



public class Dialogue extends Plan implements ActorConstants {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  final static int
    STAGE_GREETING = 0,
    STAGE_TALKING  = 1,
    STAGE_GOODBYE  = 3 ;
  final static float
    FINISH_CHANCE = 0.1f ;
  
  final Actor other ;
  final boolean inits ;
  private Boardable location = null ;
  private boolean speaking = false, finished = false ;
  
  
  
  public Dialogue(Actor actor, Actor other, boolean inits) {
    super(actor, actor, other) ;
    this.other = other ;
    this.inits = inits ;
    this.speaking = inits ;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s) ;
    this.other = (Actor) s.loadObject() ;
    this.location = (Boardable) s.loadTarget() ;
    this.inits = s.loadBool() ;
    this.speaking = s.loadBool() ;
    this.finished = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other) ;
    s.saveTarget(location) ;
    s.saveBool(inits) ;
    s.saveBool(speaking) ;
    s.saveBool(finished) ;
  }
  
  
  
  /**  Static helper methods.  (I don't make these local, since I want to
    *  emphasise that these are supposed to be independant of any particular
    *  conversation, or decide whether it even happens.)
    */
  //
  //  Returns whether Actor is listening to Other.
  static boolean isListening(Actor actor, Actor other) {
    final Behaviour root = actor.AI.rootBehaviour() ;
    if (! (root instanceof Dialogue)) return false ;
    final Dialogue OD = (Dialogue) root ;
    return OD.other == other && ! OD.inits ;
  }
  
  
  //
  //  Returns whether Actor can talk to Other.
  //  TODO:  Base this off possession of mutual etiquette/language skills?
  public static boolean canTalk(Actor actor, Actor other) {
    if (actor == other || other.species() != Species.HUMAN) return false ;
    if (isListening(actor, other)) return true ;
    return actor.AI.couldSwitch(
      actor.AI.rootBehaviour(), new Dialogue(actor, other, false)
    ) ;
  }
  
  
  static Boardable pickLocation(Dialogue dialogue) {
    final Actor A = dialogue.actor, O = dialogue.other ;
    if (A.aboard() == O.aboard()) return A.aboard() ;
    final Vec3D p = A.position(null).add(O.position(null)).scale(0.5f) ;
    final Tile t = A.world().tileAt(p.x, p.y) ;
    return Spacing.nearestOpenTile(t, O) ;
  }
  
  
  private void joinDialogue(Actor other) {
    BaseUI.logFor(actor, actor+" starting dialogue with "+other) ;
    final Dialogue forOther = new Dialogue(other, actor, false) ;
    if (location instanceof Tile) {
      final Box2D a = location.area(null) ;
      forOther.location = Spacing.nearestOpenTile(a, other, actor.world()) ;
    }
    else forOther.location = location ;
    other.AI.assignBehaviour(forOther) ;
  }
  
  
  private void finishDialogue() {
    BaseUI.logFor(actor, actor+" finished dialogue.") ;
    ///actor.chat.addPhrase(Wording.VOICE_GOODBYE, true) ;
    this.finished = true ;
    final Behaviour root = other.AI.rootBehaviour() ;
    if (root instanceof Dialogue) {
      final Dialogue d = ((Dialogue) root) ;
      if (d.other == actor) d.finished = true ;
    }
  }
  
  
  public float priorityFor(Actor actor) {
    if (actor.species() != Species.HUMAN || other.species() != Species.HUMAN) {
      return 0 ;
    }
    if (finished || (inits && ! canTalk(other, actor))) return 0 ;
    
    final float
      relation = actor.AI.relation(other),
      attraction = actor.AI.attraction(other),
      novelty = actor.AI.novelty(other) ;
    
    float appeal = 0 ;
    appeal += relation ;
    appeal += attraction / 2 ;
    appeal += novelty * actor.traits.scaleLevel(INQUISITIVE) ;
    appeal *= actor.traits.scaleLevel(SOCIABLE) * CASUAL ;
    ///I.say("____ Final appeal of dialogue: "+appeal) ;
    return Visit.clamp(appeal, 0, ROUTINE) ;
  }
  
  
  
  /**  Implementing the actual sequence of actions.
    */
  protected Behaviour getNextStep() {
    if (priorityFor(actor) <= 0) {
      ///BaseUI.logFor(actor, actor+" can't talk right now, breaking dialogue.") ;
      finishDialogue() ;
      return null ;
    }
    //
    //  TODO:  Base this selection on a Stage field (above)?
    if (inits && location == null) {
      final Action greet = new Action(
        actor, other,
        this, "actionGreet",
        Action.TALK, "Greeting "+other
      ) ;
      greet.setProperties(Action.RANGED) ;
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
  
  
  public boolean actionGreet(Actor actor, Actor other) {
    if (finished) return false ;
    if (canTalk(other, actor)) {
      location = pickLocation(this) ;
      joinDialogue(other) ;
      return true ;
    }
    else {
      //BaseUI.logFor(actor, other+" can't talk right now, breaking dialogue.") ;
      finishDialogue() ;
      return false ;
    }
  }
  
  
  public boolean actionTalk(Actor actor, Actor other) {
    if (finished) return false ;
    if (! canTalk(other, actor)) {
      I.say("____ Other actor CANNOT talk: "+other) ;
      finishDialogue() ;
      return false ;
    }
    speaking = ! speaking ;
    if (speaking) return false ;
    //
    //  Try factoring this in a bit more thoroughly.
    final float SE = suasionEffect(actor, other) ;
    other.AI.incRelation(actor, SE) ;
    if (SE > 0) actor.health.liftStress(SE) ;
    else actor.health.takeStress(0 - SE) ;
    //
    //  Pick one of three approaches to take at random-
    switch (Rand.index(3)) {
      case(0): anecdote(actor, other) ;
      case(1): gossip(actor, other) ;
      case(2): advise(actor, other) ;
    }
    return true ;
  }
  
  
  
  /**
    */
  private float suasionEffect(Actor actor, Actor other) {
    final float attLevel = other.AI.relation(actor) ;
    float effect = 0 ;
    if (actor.traits.test(SUASION, ROUTINE_DC, 1)) effect += 5 ;
    else effect -= 5 ;
    if (actor.traits.test(SUASION, attLevel * -20, 1)) effect += 5 ;
    else effect -= 5 ;
    effect /= 25 ;
    return effect ;
  }
  
  
  private void anecdote(Actor actor, Actor other) {
    //
    //  Pick a random recent activity and see if the other also indulged in it.
    //  If the activity is similar, or was undertaken for similar reasons,
    //  improve relations.
    //
    //  TODO:  At the moment, we just compare traits.  Fix later.
    final Trait comp = (Trait) Rand.pickFrom(actor.traits.personality()) ;
    final float
      levelA = actor.traits.scaleLevel(comp),
      levelO = actor.traits.scaleLevel(comp),
      effect = 0.5f - (Math.abs(levelA - levelO) / 1.5f) ;
    final String desc = actor.traits.levelDesc(comp) ;
    
    other.AI.incRelation(actor, effect) ;
    actor.AI.incRelation(other, effect) ;
    
    utters(actor, "It's important to be "+desc+".") ;
    if (effect > 0) utters(other, "Absolutely.") ;
    if (effect == 0) utters(other, "Yeah, I guess...") ;
    if (effect < 0) utters(other, "No way!") ;
  }
  
  
  private void gossip(Actor actor, Actor other) {
    //
    //  Pick an acquaintance, see if it's mutual, and if so compare attitudes
    //  on the subject.  TODO:  Include memories of recent activities?
    final Relation r = (Relation) Rand.pickFrom(actor.AI.relations()) ;
    if (r == null) {
      utters(actor, "Nice weather, huh?") ;
      utters(other, "Uh-huh.") ;
      return ;
    }
    final float attA = r.value(), attO = other.AI.relation(actor) ;
    if (r.subject == other) {
      if (attA > 0) utters(actor, "I think we get along.") ;
      else utters(actor, "We don't get along.") ;
    }
    else {
      if (attA > 0) utters(actor, "I get on well with "+r.subject+".") ;
      else utters(actor, "I don't get on with "+r.subject+".") ;
    }
    final boolean agrees = attO * attA > 0 ;
    if (agrees) utters(other, "I can see that.") ;
    else utters(other, "Really?") ;
    
    final float effect = 0.2f * (agrees ? 1 : -1) ;
    other.AI.incRelation(actor, effect) ;
    actor.AI.incRelation(other, effect) ;
  }
  
  
  private void advise(Actor actor, Actor other) {
    final Skill tested = (Skill) Rand.pickFrom(other.traits.skillSet()) ;
    if (tested == null) return ;
    final float level = actor.traits.useLevel(tested) ;
    
    utters(other, "I'm interested in "+tested.name+".") ;
    if (level < other.traits.useLevel(tested)) {
      utters(actor, "Don't know much about that.") ;
      return ;
    }
    utters(actor, "Well, here's what you do...") ;
    utters(other, "You mean like this?") ;
    
    //  Use the Counsel skill here.  And/or possibly restrict to intellectual
    //  skills?
    
    float effect = 0 ;
    if (other.traits.test(tested, level / 2, 0.5f)) effect += 5 ;
    else effect -= 5 ;
    if (other.traits.test(tested, level * Rand.num(), 0.5f)) effect += 5 ;
    else effect -= 5 ;
    effect /= 25 ;
    other.AI.incRelation(actor, effect) ;
    actor.AI.incRelation(other, effect) ;
    
    if (effect > 0) utters(actor, "Yes, exactly!") ;
    if (effect == 0) utters(actor, "Close. Try again.") ;
    if (effect < 0) utters(actor, "No, that's not it...") ;
  }
  
  
  
  /**  You need to have methods for convincing actors to switch allegiance,
    *  chiefly for the sake of Contact missions.
    */
  
  
  
  /**  Rendering and interface methods-
    */
  final static Vec3D forwardVec = new Vec3D(1, 1, 0) ;
  
  private boolean onRight(Actor a, Actor b) {
    final Vec3D disp = a.position(null).sub(b.position(null)) ;
    return disp.dot(forwardVec) > 0 ;
  }
  
  
  private void utters(Actor a, String s) {
    if (a.chat.numPhrases() > 3) return ;
    final Actor opposite = a == actor ? other : actor ;
    final boolean onRight = onRight(a, opposite) ;
    a.chat.addPhrase(s, onRight ? TalkFX.FROM_RIGHT : TalkFX.FROM_LEFT) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Talking to ") ;
    d.append(other) ;
  }
}


//
//  TODO:  REINTRODUCE THESE ASAP.


/*
//
//  The attraction here really needs to be based on how much information the
//  two have to exchange.
//  Basic info- homeworld, birth, vocation.
//  Recent events- last couple of behaviours known.
//  Fields of interest- skills and traits.
//  Acquaintances- friends, foes and family.
//  Trigger based on association?


private void introduce(Actor actor, Actor other) {
  //
  //  Compare vocations, physical traits and appearance, and proper manners.
  //  Set up initial relationships.
}

//*/










/*
private void request(Actor actor, Actor other) {
  //
  //  Pick something the actor wants, and ask the other for it.  If it's a
  //  reasonable request, grant it.
}
//*/
/*
static Skill mannersFor(Actor actor) {
  if (actor instanceof Human) {
    final Vocation birth = ((Human) actor).career().birth() ;
    if (birth == Vocation.NATIVE_BIRTH) return NATIVE_TABOO ;
    if (birth == Vocation.HIGH_BIRTH) return NOBLE_ETIQUETTE ;
    return COMMON_CUSTOM ;
  }
  else return null ;
}


static float firstImpression(Actor actor, Actor other) {
  float impression = -10 * Rand.num() ;
  if (other.traits.test(mannersFor(other), ROUTINE_DC, 1)) impression += 10 ;
  if (other.traits.test(SUASION, ROUTINE_DC, 1)) impression += 10 ;
  impression += actor.attraction(other) * Rand.avgNums(2) ;
  return impression ;
}
//*/



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


