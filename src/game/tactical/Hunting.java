/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;
import src.user.* ;




public class Hunting extends Combat implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS   = 0,
    TYPE_HARVEST = 1,
    TYPE_SAMPLE  = 2 ;
  //  TODO:  base willingness to quit on time spent, not tries made.
  /*
  final static int
    MIN_ATTEMPTS = 3 ;
  //*/
  
  
  final int type ;
  final Actor prey ;
  private float beginTime = -1 ;
  
  
  
  public Hunting(Actor actor, Actor prey, int type) {
    super(actor, prey) ;
    this.prey = prey ;
    this.type = type ;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s) ;
    prey = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(prey) ;
    s.saveInt(type) ;
    s.saveFloat(beginTime) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    float reward = 0 ;
    if (type == TYPE_FEEDS) {
      final float hunger = actor.health.hungerLevel() - 0.25f ;
      if (hunger < 0) return 0 ;
      reward = hunger * PARAMOUNT / 0.75f ;
    }
    if (type == TYPE_HARVEST) {
      reward = ROUTINE ;
    }
    reward += priorityMod ;
    //
    //  TODO:  Favour more abundant prey in general.  Use the Ecology class.
    
    //
    //  TODO:  You need to figure out why the chance of success is so low, and
    //  what the hell is causing that bizarre height-change bug.
    float priority = Combat.combatPriority(actor, prey, reward, PARAMOUNT) ;
    priority -= Plan.rangePenalty(actor, prey) ;
    I.say("Hunting priority is: "+priority) ;
    return priority ;
  }
  
  
  public boolean valid() {
    if (actor == null) return super.valid() ;
    if (type == TYPE_HARVEST) {
      if (actor.gear.amountOf(PROTEIN) > 0) return true ;
    }
    return super.valid() ;
  }
  
  
  public static Actor nextPreyFor(Actor actor, float sampleRange) {
    //
    //  TODO:  Base this off the list of stuff the actor is aware of, and
    //  favour more abundant prey.
    final int maxSampled = 10 ;
    Actor pickedPrey = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    final PresenceMap peers = actor.world().presences.mapFor(Mobile.class) ;
    int numSampled = 0 ;
    //
    //  
    for (Target t : peers.visitNear(actor, sampleRange, null)) {
      if (++numSampled > maxSampled) break ;
      if (! (t instanceof Actor)) continue ;
      final Actor f = (Actor) t ;
      if ((! f.health.organic()) || (! (t instanceof Fauna))) continue ;
      final Species s = (Species) f.species() ;
      //
      //  
      final float danger = Combat.combatStrength(f, actor) * Rand.num() ;
      float rating = ROUTINE / danger ;
      rating -= Plan.rangePenalty(actor, f) ;
      if (s.type != Species.Type.BROWSER) rating /= 2 ;
      //
      //  
      if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
    }
    return pickedPrey ;
  }
  
  
  
  /**  Actual implementation-
    */
  public boolean monitor(Actor actor) {
    //  Close at normal speed until you are near your prey.  Then enter stealth
    //  mode to get closer.  If they spot you, charge.
    //float dist = Spacing.distance(actor, prey) ;
    return super.monitor(actor) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    final float timeSpent = actor.world().currentTime() - beginTime ;
    if (timeSpent > World.DEFAULT_DAY_LENGTH / 3) {
      return null ;
    }
    //
    //  If the prey is dead, either feed or harvest the meat.
    if (! prey.health.conscious()) {
      if (type == TYPE_FEEDS) {
        if (actor.health.energyLevel() >= 1.5f) {
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
      else if (type == TYPE_HARVEST) {
        if (prey.destroyed()) {
          if (actor.gear.amountOf(PROTEIN) <= 0 || actor.AI.work() == null) {
            return null ;
          }
          final Action returning = new Action(
            actor, actor.AI.work(),
            this, "actionReturnHarvest",
            Action.REACH_DOWN, "Returning game meat"
          ) ;
          return returning ;
        }
        final Action harvest = new Action(
          actor, prey,
          this, "actionHarvest",
          Action.BUILD, "Harvesting from "+prey
        ) ;
        return harvest ;
      }
      else I.complain("BEHAVIOUR NOT IMPLEMENTED YET!") ;
    }
    //
    //  Otherwise, try tracking and downing the prey-
    return super.getNextStep() ;
  }


  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Tear off chunks of meat and fill your belly.
    if (! prey.health.deceased()) prey.health.setState(ActorHealth.STATE_DEAD) ;
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
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    if (! prey.health.deceased()) prey.health.setState(ActorHealth.STATE_DEAD) ;
    final float amountMeat = actor.health.maxHealth() / 5f ;
    actor.gear.addItem(Item.withAmount(PROTEIN, amountMeat)) ;
    prey.setAsDestroyed() ;
    return true ;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue store) {
    //
    //  TODO:  Try extracting a small amount of spice?  Or delivering the
    //  corpse back to the store for further treatment?
    actor.gear.transfer(PROTEIN, store) ;
    return true ;
  }
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (prey.health.deceased()) {
      if (type == TYPE_HARVEST) {
        if (prey.inWorld()) d.append("Harvesting meat from "+prey) ;
        else d.append("Returning meat to "+actor.AI.work()) ;
      }
    }
    else d.append("Hunting "+prey) ;
  }
}













