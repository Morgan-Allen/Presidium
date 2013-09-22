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
  final static int
    STAGE_INIT          = 0,
    STAGE_HUNT          = 1,
    STAGE_FEED          = 2,
    STAGE_HARVEST_MEAT  = 3,
    STAGE_RETURN_MEAT   = 4,
    STAGE_SAMPLE_GENE   = 5,
    STAGE_RETURN_SAMPLE = 6,
    STAGE_COMPLETE      = 7 ;
  
  
  final int type ;
  final Actor prey ;
  private int stage = STAGE_INIT ;
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
    stage = s.loadInt() ;
    beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(prey) ;
    s.saveInt(type) ;
    s.saveInt(stage) ;
    s.saveFloat(beginTime) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    float reward = 0 ;
    if (type == TYPE_FEEDS) {
      final float hunger = actor.health.hungerLevel() - 0.25f ;
      if (hunger < 0) return 0 ;
      reward = CASUAL + (hunger * URGENT / 0.75f) ;
      if (BaseUI.isPicked(actor)) I.say("Base feeding priority: "+reward) ;
    }
    if (type == TYPE_HARVEST) {
      float abundance = actor.world().ecology().relativeAbundance(prey) ;
      ///I.say("Abundance of prey is: "+abundance) ;
      if (abundance < 0.75f) return 0 ;
      reward = ROUTINE ;
    }
    reward += priorityMod ;
    
    float priority = Combat.combatPriority(actor, prey, reward, PARAMOUNT) ;
    priority -= Plan.rangePenalty(actor, prey) ;
    if (BaseUI.isPicked(actor)) I.say("Hunting "+prey+" priority: "+priority) ;
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
    ///I.say("FINDING PREY FOR: "+actor) ;
    //
    //  TODO:  Base this off the list of stuff the actor is aware of?
    final Ecology ecology = actor.world().ecology() ;
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
      if (s == actor.species()) continue ;
      //
      //  
      final float danger = Combat.combatStrength(f, actor) * Rand.num() ;
      float rating = 1f / danger ;
      if (s.type != Species.Type.BROWSER) rating /= 2 ;
      rating -= Plan.rangePenalty(actor, f) ;
      if (rating < 0) continue ;
      rating *= ecology.globalAbundance(s) * Rand.avgNums(2) ;
      //
      //  
      if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
    }
    if (pickedPrey == null) I.say("NO PREY FOUND FOR "+actor) ;
    //else I.say("PREY IS: "+pickedPrey) ;
    return pickedPrey ;
  }
  
  
  
  /**  Actual implementation-
    */
  public boolean monitor(Actor actor) {
    //  Close at normal speed until you are near your prey.  Then enter stealth
    //  mode to get closer.  If they spot you, charge.
    //float dist = Spacing.distance(actor, prey) ;
    
    if (prey.AI.canSee(actor)) {
      
    }
    else if (actor.AI.canSee(prey)) {
      
    }
    else {
      
    }
    
    //if (oldStage != newStage) actor.AI.swapActionFor(this, nextStep()) ;
    return super.monitor(actor) ;
  }
  
  
  protected Behaviour getNextClosing() {
    //  Either close normally, or make it slow.  Then charge.
    return null ;
  }
  
  
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    final float timeSpent = actor.world().currentTime() - beginTime ;
    if (timeSpent > World.STANDARD_DAY_LENGTH / 3) {
      return null ;
    }

    if (type == TYPE_FEEDS) {
      return nextFeeding() ;
    }
    if (type == TYPE_HARVEST) {
      return nextHarvest() ;
    }
    I.complain("Behaviour not implemented yet!") ;
    return null ;
  }
  
  
  
  /**  Routines for feeding on meat directly-
    */
  protected Behaviour nextFeeding() {
    if (prey.health.conscious()) return super.getNextStep() ;
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


  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Tear off chunks of meat and fill your belly.
    if (! prey.health.deceased()) prey.health.setState(ActorHealth.STATE_DEAD) ;
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 2 ;
    prey.health.takeInjury(damage) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() * 10 ;
    actor.health.takeSustenance(taken, 1) ;
    ///I.say("Energy level: "+actor.health.energyLevel()) ;
    return true ;
  }
  
  
  
  /**  Routines for harvesting meat and bringing back to the depot-
    */
  protected Behaviour nextHarvest() {
    if (prey.health.conscious()) return super.getNextStep() ;
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
        if (! prey.destroyed()) d.append("Harvesting meat from "+prey) ;
        else d.append("Returning meat to "+actor.AI.work()) ;
      }
    }
    else d.append("Hunting "+prey) ;
  }
}













