


package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.* ;
import src.util.* ;


//
//  TODO:  Just make this a general 'Feeding' plan?


public class Hunting extends Plan implements ActorConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS = 0,
    TYPE_HARVEST = 1,
    TYPE_SAMPLED = 2,
    
    MAX_ATTEMPTS = 5 ;  //  Modify using Persistant trait?
  
  
  final int type ;
  final Actor prey ;
  int numAttempts = 0 ;
  
  
  public Hunting(Actor actor, Actor prey, int type) {
    super(actor, prey) ;
    this.prey = prey ;
    this.type = type ;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s) ;
    prey = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    numAttempts = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(prey) ;
    s.saveInt(type) ;
    s.saveInt(numAttempts) ;
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
    if (numAttempts > MAX_ATTEMPTS) {
      I.say(actor+" abandoning hunt of "+prey) ;
      return null ;
    }
    //
    //  If the prey is dead, either feed or harvest the meat.
    if (prey.health.deceased()) {
      if (type == TYPE_FEEDS) {
        if (actor.health.energyLevel() >= 1) {
          I.say(actor+" has eaten their fill of "+prey) ;
          return null ;
        }
        final Action feeding = new Action(
          actor, prey,
          this, "actionFeed",
          Action.STRIKE, "Feeding on "+prey
        ) ;
        return feeding ;
      }
      else {
        I.complain("NON-FEEDING HUNTING BEHAVIOUR NOT IMPLEMENTED YET!") ;
      }
    }
    //
    //  If you're close to the prey, attack it.
    if (actor.psyche.awareOf(prey)) {
      final Action strike = new Action(
        actor, prey,
        this, "actionStrike",
        Action.STRIKE, "Striking at "+prey
      ) ;
      strike.setProperties(Action.QUICK) ;
      return strike ;
    }
    //
    //  If the prey is out of range, track it down.
    final Action tracking = new Action(
      actor, prey,
      this, "actionTrack",
      Action.LOOK, "Tracking "+prey
    ) ;
    return tracking ;
  }
  
  
  protected void onceInvalid() {
    if (! prey.inWorld()) {
      I.say(actor+" has finished the carcasse of "+prey+"?") ;
    }
  }
  
  
  public boolean monitor(Actor actor) {
    //
    //  If you're in the middle of tracking and you catch sight of the prey,
    //  cancel the action.
    final Action current = actor.currentAction() ;
    if (current == null) return true ;
    if (current.methodName().equals("actionTrack")) {
      if (actor.psyche.awareOf(prey)) {
        actor.psyche.cancelBehaviour(current) ;
        
      }
    }
    return false ;
  }


  public boolean actionTrack(Actor actor, Actor prey) {
    numAttempts++ ;
    return true ;
  }
  
  
  public boolean actionStrike(Actor actor, Actor prey) {
    //
    //  Outsource this to the Combat class, using specified offensive and
    //  defensive skills.
    if (prey.health.deceased()) return false ;
    if (type == TYPE_FEEDS) {
      Combat.performStrike(actor, prey, REFLEX, REFLEX) ;
    }
    else I.complain("UNSUPPORTED HUNTING TYPE.") ;
    numAttempts++ ;
    return true ;
  }
  
  
  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Inflict injury, extract meat, fill your belly.
    //actor.health.takeSustenance(actor.health.maxHealth(), 1) ;
    //*
    final float damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 2 ;
    final float before = prey.health.injuryLevel() ;
    prey.health.takeInjury(damage) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() * 4 ;
    ///I.say("Eaten: "+taken+" calories from "+prey) ;
    actor.health.takeSustenance(taken, 1) ;
    //*/
    return true ;
  }
  
  
  /*
  public boolean actionHarvestMeat(Actor actor, Actor prey) {
    return true ;
  }
  
  
  public boolean actionReturnMeat(Actor actor, Actor prey) {
    return true ;
  }
  //*/
}








