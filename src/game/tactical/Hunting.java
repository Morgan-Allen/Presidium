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
import src.game.social.* ;
import src.util.* ;
import src.user.* ;




public class Hunting extends Combat implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS   = 0,
    TYPE_HARVEST = 1,
    TYPE_PROCESS = 2,
    TYPE_SAMPLE  = 3 ;
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
  final Employment depot ;
  private int stage = STAGE_INIT ;
  private float beginTime = -1 ;
  
  
  
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null) ;
  }
  
  
  public static Hunting asHarvest(Actor actor, Actor prey, Employment depot) {
    if (depot == null) return asFeeding(actor, prey) ;
    return new Hunting(actor, prey, TYPE_HARVEST, depot) ;
  }
  
  
  public static Hunting asProcess(Actor actor, Actor prey, Employment depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!") ;
    return new Hunting(actor, prey, TYPE_PROCESS, depot) ;
  }
  
  
  public static Hunting asSample(Actor actor, Actor prey, Employment depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!") ;
    return new Hunting(actor, prey, TYPE_SAMPLE, depot) ;
  }
  
  
  
  private Hunting(Actor actor, Actor prey, int type, Employment depot) {
    super(actor, prey) ;
    this.prey = prey ;
    this.type = type ;
    this.depot = depot ;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s) ;
    prey = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    depot = (Employment) s.loadObject() ;
    stage = s.loadInt() ;
    beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(prey) ;
    s.saveInt(type) ;
    s.saveObject(depot) ;
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
    
    float priority = Combat.combatPriority(
      actor, prey, reward, PARAMOUNT, false
    ) ;
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
  
  
  public static Actor nextPreyFor(
    Actor actor, float sampleRange, boolean conserve
  ) {
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
      final float abundance = ecology.globalAbundance(s) ;
      if (conserve && abundance < 1) continue ;
      rating *= abundance * Rand.avgNums(2) ;
      //
      //  
      if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
    }
    //if (pickedPrey == null) I.say("NO PREY FOUND FOR "+actor) ;
    //else I.say("PREY IS: "+pickedPrey) ;
    return pickedPrey ;
  }
  
  
  
  /**  Actual implementation-
    */
  public int motionType(Actor actor) {
    //
    //  Close at normal speed until you are near your prey.  Then enter stealth
    //  mode to get closer.  If they spot you, charge.
    if (prey.mind.awareOf(actor)) {
      return MOTION_FAST ;
    }
    else if (actor.mind.awareOf(prey)) {
      return MOTION_SNEAK ;
    }
    else return super.motionType(actor) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    final float timeSpent = actor.world().currentTime() - beginTime ;
    if (timeSpent > World.STANDARD_DAY_LENGTH / 3) {
      return null ;
    }

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
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    if (type == TYPE_SAMPLE) {
      final Item sample = Item.withReference(SAMPLES, prey) ;
      actor.gear.addItem(sample) ;
      return true ;
    }
    //
    //  Determine just how large a chunk you can take out of the prey-
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 10 ;
    prey.health.takeInjury(damage) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() ;
    //
    //  Then dispose of it appropriately-
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING) ;
    if (type == TYPE_FEEDS) {
      actor.health.takeSustenance(taken * 10, 1) ;
    }
    if (type == TYPE_HARVEST) {
      actor.gear.bumpItem(PROTEIN, taken) ;
    }
    if (type == TYPE_PROCESS) {
      final Item sample = Item.withReference(SAMPLES, prey) ;
      actor.gear.addItem(Item.withAmount(sample, taken)) ;
    }
    return true ;
  }
  
  
  public boolean actionProcess(Actor actor, Actor prey) {
    if (type == TYPE_HARVEST) {
      actor.gear.transfer(PROTEIN, depot) ;
    }
    
    if (type == TYPE_PROCESS || type == TYPE_SAMPLE) {
      final Item
        sample = Item.withReference(SAMPLES, prey),
        carried = actor.gear.matchFor(sample) ;
      if (carried != null) actor.gear.transfer(carried, depot) ;
      if (type == TYPE_SAMPLE) return true ;
      
      final Inventory stocks = depot.inventory() ;
      final float remaining = stocks.amountOf(sample) ;
      if (remaining > 0) {
        float success = 1 ;
        if (actor.traits.test(DOMESTICS  , SIMPLE_DC  , 1)) success++ ;
        if (actor.traits.test(XENOZOOLOGY, MODERATE_DC, 1)) success++ ;
        
        final Species species = (Species) prey.species() ;
        float baseAmount = 0.1f, spiceAmount = 0.1f ;
        if (species.type == Species.Type.BROWSER ) spiceAmount  = 0 ;
        if (species.type != Species.Type.PREDATOR) spiceAmount /= 4 ;
        baseAmount = Math.min(baseAmount, remaining) ;
        
        stocks.removeItem(Item.withAmount(sample, baseAmount)) ;
        baseAmount *= success ;
        spiceAmount *= baseAmount * (1 + (success / 2)) ;
        stocks.bumpItem(PROTEIN, baseAmount) ;
        stocks.bumpItem(SPICE, spiceAmount) ;
      }
    }
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (prey.health.dying()) {
      if (type == TYPE_HARVEST) {
        if (! prey.destroyed()) d.append("Harvesting meat from "+prey) ;
        else d.append("Returning meat to "+actor.mind.work()) ;
      }
    }
    else d.append("Hunting "+prey) ;
  }
}

