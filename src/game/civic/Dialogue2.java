


package src.game.civic ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;


/*
Okay.  During an introduction, you'll get three chances to make a good
impression... until the novelty wears off.  But if you fuck it up, you might
just trigger open combat.


Etiquette (based on subject) + Comm Skill (based on dialogue event).
  Humans:  Tribal, Commoner or Noble speech, plus Counsel, Command or Suasion
  Animals:  appropriate Ecology plus Zoologist
  Artilects:  Ancient Lore plus Inscription


Basic priority of Idle.  Higher for novel acquaintances.  Zero if actor is busy.
Central location for standing around, once begun.
Can talk with anyone currently talking (or trying to talk) to you.
Basic dialogue effects can also happen spontaneously.

//*/

public class Dialogue2 extends Plan implements Abilities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  final static int
    MAX_LINES = 3,
    LINE_INIT = -1 ;
  
  private static boolean verbose = true ;
  
  
  final Actor other ;
  private int numLines = LINE_INIT ;
  private Boardable location = null ;
  
  
  
  public Dialogue2(Actor actor, Actor other) {
    super(actor, other) ;
    this.other = other ;
  }
  
  
  public Dialogue2(Session s) throws Exception {
    super(s) ;
    other = (Actor) s.loadObject() ;
    numLines = s.loadInt() ;
    location = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other) ;
    s.saveInt(numLines) ;
    s.saveTarget(location) ;
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  public float priorityFor(Actor actor) {
    if (! canTalk(other)) return 0 ;
    float impetus = IDLE * actor.mind.relationValue(other) * 2 ;
    impetus = (impetus + (actor.mind.relationNovelty(other) * ROUTINE)) / 2f ;
    impetus *= actor.traits.scaleLevel(SOCIABLE) ;
    return impetus ;
  }
  
  
  private boolean canTalk(Actor other) {
    if (other.isDoing(Dialogue2.class, null)) return true ;
    final Dialogue2 d = new Dialogue2(other, actor) ;
    return other.mind.couldSwitchTo(d) ;
  }
  
  
  private Batch <Dialogue2> involved() {
    final World world = actor.world() ;
    final Batch <Dialogue2> batch = new Batch <Dialogue2> () ;
    for (Behaviour b : world.activities.targeting(actor)) {
      if (b instanceof Dialogue2) {
        final Dialogue2 d = (Dialogue2) b ;
        batch.add(d) ;
      }
    }
    return batch ;
  }
  
  
  private void setLocationFor(Action talkAction) {
    
    //  If a location has not already been assigned, look for one either used
    //  by existing conversants, or find a new spot nearby.
    if (location == null) {
      for (Dialogue2 d : involved()) if (d.location != null) {
        this.location = d.location ; break ;
      }
      if (location == null) {
        location = Spacing.nearestOpenTile(actor, actor) ;
      }
    }
    
    //  If a location has been found, walk over there.  If there doesn't seem
    //  to be any space, quit.
    if (location instanceof Tile) {
      talkAction.setMoveTarget(Spacing.pickFreeTileAround(location, actor)) ;
    }
    else if (location instanceof Boardable) {
      talkAction.setMoveTarget(location) ;
    }
    else abortBehaviour() ;
  }
  
  
  private Delivery gettingGiftFor(Actor other) {
    
    //  TODO:  Find the list of items needed (but not produced?) by the actor's
    //  home or work venues and see if those can be acquired locally.  Default
    //  to money, food or bling otherwise.
    return null ;
  }
  
  
  private Action assistanceFrom(Actor other) {
    
    //  TODO:  Clone actions from the actor's own behavioural repertoire and
    //  see if any of those suit the other actor.
    
    //Plan favour = actor.mind.createBehaviour() ;
    //favour.assignActor(other) ;
    //favour.priorityMod = ROUTINE * other.mind.relationValue(actor) ;
    //favour.priorityMod *= favour.priorityFor(other) / PARAMOUNT ;
    //return favour ;
    return null ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (numLines > MAX_LINES) return null ;
    
    if (numLines == LINE_INIT) {
      final Action greeting = new Action(
        actor, other,
        this, "actionGreet",
        Action.TALK, "Greeting "+other
      ) ;
      greeting.setProperties(Action.RANGED) ;
      return greeting ;
    }
    
    if (numLines < MAX_LINES) {
      //  TODO:  You'll want to try for a variety of different actions here,
      //  based on the train of association.  For now, I'll just employ a
      //  single generic 'chat' action...
      final Action chats = new Action(
        actor, other,
        this, "actionChats",
        Action.TALK_LONG, "Chatting with "+other
      ) ;
      setLocationFor(chats) ;
      return chats ;
    }
    
    if (numLines == MAX_LINES) {
      final Action farewell = new Action(
        actor, other,
        this, "actionFarewell",
        Action.TALK, "Saying farewell to "+other
      ) ;
      farewell.setProperties(Action.RANGED) ;
      return farewell ;
    }
    
    return null ;
  }
  
  
  private float talkResult(
    Skill language, Skill plea, Skill opposeSkill, Actor other
  ) {
    final float attitude = other.mind.relationValue(actor) ;
    final int DC = ROUTINE_DC - (int) (attitude * MODERATE_DC) ;
    float success = -1 ;
    success += actor.traits.test(language, null, null, ROUTINE_DC, 10, 2) ;
    success += actor.traits.test(plea, other, opposeSkill, 0 - DC, 10, 2) ;
    success /= 3 ;
    return success ;
  }
  
  
  public boolean actionIntro(Actor actor, Actor other) {
    //  Used when making a first impression.
    float success = talkResult(SUASION, SUASION, TRUTH_SENSE, other) ;
    if (other.mind.hasRelation(actor)) {
      other.mind.initRelation(actor, success / 2) ;
    }
    else other.mind.incRelation(actor, success) ;
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionGreet(Actor actor, Actor other) {
    //  Used when re-opening dialogue.
    if (! canTalk(other)) { abortBehaviour() ; return false ; }
    final Dialogue2 d = new Dialogue2(other, actor) ;
    other.mind.assignBehaviour(d) ;
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    //  Base on comparison of recent activities and associated traits, skills
    //  or actors involved.
    float success = talkResult(SUASION, SUASION, TRUTH_SENSE, other) ;
    other.mind.incRelation(actor, success / 10) ;
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionAdvise(Actor actor, Actor other) {
    //  Base on comparison of skills and relative expertise.
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionGossip(Actor actor, Actor other) {
    //  Based on comparison of acquaintances and relationships.
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    //  Used to close a dialogue.
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionGift(Actor actor, Actor other) {
    //  (Base on novelty.)
    numLines++ ;
    return true ;
  }
  
  
  public boolean actionAskFavour(Actor actor, Actor other) {
    //  (Base on relationship strength.)
    numLines++ ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}






