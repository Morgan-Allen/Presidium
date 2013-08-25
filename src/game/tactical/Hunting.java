/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;



public class Hunting extends Combat implements ActorConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS = 0,
    TYPE_HARVEST = 1,
    TYPE_SAMPLED = 2,
    
    MIN_ATTEMPTS = 3 ;
  
  
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
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    if (type == TYPE_FEEDS) {
      final float hunger = actor.health.hungerLevel() - 0.25f ;
      if (hunger < 0) return 0 ;
      final float reward = hunger * PARAMOUNT / 0.75f ;
      float priority = Combat.combatPriority(actor, prey, reward, PARAMOUNT) ;
      priority -= Plan.rangePenalty(actor, prey) ;
      
      if (prey instanceof Fauna && prey.AI.home() != null) {
        priority *= ((Lair) prey.AI.home()).crowding() ;
      }
      ///I.say(" BASE REWARD FOR HUNTING: "+reward+", APPEAL: "+CP) ;
      return priority ;
    }
    return super.priorityFor(actor) ;
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
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
      else I.complain("NON-FEEDING HUNTING BEHAVIOUR NOT IMPLEMENTED YET!") ;
    }
    //
    //  Otherwise, try tracking and downing the prey-
    if (++numAttempts > MIN_ATTEMPTS + actor.AI.persistance()) return null ;
    return super.getNextStep() ;
  }


  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Tear off chunks of meat and fill your belly.
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 2,
      maxDamage = actor.health.hungerLevel() * actor.health.maxHealth() / 10 ;
    prey.health.takeInjury(Math.min(damage, maxDamage)) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() * 10 ;
    actor.health.takeSustenance(taken, 1) ;
    return true ;
  }
}








