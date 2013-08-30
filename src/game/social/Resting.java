



package src.game.social ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Base this exclusively on fatigue!

public class Resting extends Plan implements BuildConstants {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final static int
    SCAVENGE_DC = 5,
    FORAGING_DC = 10 ;
  final static int
    MODE_NONE     = -1,
    MODE_DINE     =  0,
    MODE_SCAVENGE =  1,
    MODE_SLEEP    =  2 ;
  
  final Boardable restPoint ;
  int currentMode = MODE_NONE ;
  float minPriority = -1 ;
  
  
  public Resting(Actor actor, Boardable relaxesAt) {
    super(actor) ;
    this.restPoint = relaxesAt ;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s) ;
    this.restPoint = (Boardable) s.loadTarget() ;
    this.currentMode = s.loadInt() ;
    this.minPriority = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(restPoint) ;
    s.saveInt(currentMode) ;
    s.saveFloat(minPriority) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (restPoint == null) return -1 ;
    final float priority = ratePoint(actor, restPoint) ;
    //
    //  To ensure that the actor doesn't see-saw between relaxing and not
    //  relaxing as fatigue lifts, we establish a minimum priority-
    if (minPriority == -1) minPriority = Math.max(priority / 2, priority - 2) ;
    if (priority < minPriority) return minPriority ;
    return priority ;
  }
  
  
  protected Behaviour getNextStep() {
    if (restPoint == null) return null ;
    //
    //  If you're hungry, eat from local stocks.
    if (actor.health.hungerLevel() > 0.1f) {
      final Action eats = new Action(
        actor, restPoint,
        this, "actionEats",
        Action.BUILD, "Eating at "+restPoint
      ) ;
      final Batch <Service> menu = menuFor(restPoint) ;
      if (menu.size() == 0 && actor.health.hungerLevel() > 0.6f) {
        final Tile t = Spacing.pickFreeTileAround(restPoint, actor) ;
        eats.setMoveTarget(t) ;
        eats.setProperties(Action.CAREFUL) ;
        currentMode = MODE_SCAVENGE ;
      }
      else currentMode = MODE_DINE ;
      return eats ;
    }
    //
    //  If you're tired, put your feet up.
    if (actor.health.fatigueLevel() > 0.1f) {
      final Action relax = new Action(
        actor, restPoint,
        this, "actionRelax",
        Action.STAND, "Relaxing at "+restPoint
      ) ;
      currentMode = MODE_SLEEP ;
      return relax ;
    }
    return null ;
  }
  
  
  public boolean actionEats(Actor actor, Target place) {
    //
    //  If you're inside a venue, and it has food, then avail of it-
    final Batch <Service> menu = menuFor(place) ;
    if (menu.size() > 0) {
      final Venue venue = (Venue) place ;
      int numFoods = 0 ;
      for (Service type : menu) {
        venue.inventory().removeItem(Item.withAmount(type, 1f / numFoods)) ;
      }
      actor.health.takeSustenance(5, numFoods / ActorHealth.MAX_FOOD_TYPES) ;
      return true ;
    }
    //
    //  Otherwise, grub whatever you can from the surroundings-
    else {
      final boolean
        canGrub = actor.traits.test(HARD_LABOUR, SCAVENGE_DC, 1),
        goodStuff = actor.traits.test(XENOBIOLOGY, FORAGING_DC, 1) ;
      if (canGrub) actor.health.takeSustenance(1, goodStuff ? 0.5f : 0) ;
      return true ;
    }
  }
  
  
  private Batch <Service> menuFor(Target place) {
    Batch <Service> menu = new Batch <Service> () ;
    if (! (place instanceof Venue)) return menu ;
    final Venue venue = (Venue) place ;
    for (Service type : ALL_FOOD_TYPES) {
      if (venue.inventory().amountOf(type) >= 1) menu.add(type) ;
    }
    return menu ;
  }
  
  
  public boolean actionRelax(Actor actor, Target place) {
    //
    //  TODO:  If you're in a public venue, you'll have to pay the
    //  admission fee.  Also, should the actor just enter sleep mode?  
    ///actor.health.setState(ActorHealth.STATE_RESTING) ;
    int relaxBonus = 3 ;
    float liftF = ActorHealth.FATIGUE_GROW_PER_DAY * actor.health.maxHealth() ;
    liftF *= relaxBonus * 1f / World.DEFAULT_DAY_LENGTH ;
    actor.health.liftFatigue(liftF) ;
    return true ;
  }
  
  
  
  /**  Methods used for external assessment-
    */
  public static Boardable pickRestPoint(final Actor actor) {
    //
    //  Consider outsourcing this to the Resting class?  Yeah.
    final Batch <Boardable> safePoints = new Batch <Boardable> () ;
    final Presences presences = actor.world().presences ;
    //
    //  TODO:  Try picking the safest known point on the map.
    //safePoints.add(Retreat.withdrawPoint(actor)) ;
    safePoints.add(actor.AI.home()) ;
    safePoints.add(actor.AI.work()) ;
    safePoints.add(presences.nearestMatch(Cantina.class, actor, -1)) ;
    //
    //  Okay, now pick whichever option is most attractive.
    final Boardable picked = new Visit <Boardable> () {
      public float rate(Boardable b) { return ratePoint(actor, b) ; }
    }.pickBest(safePoints) ;
    return picked ;
  }
  
  
  public static float ratePoint(Actor actor, Boardable point) {
    if (point == null) return -1 ;
    float baseRating = 0 ;
    if (point instanceof Tile) {
      baseRating += 1 ;
    }
    else if (point == actor.AI.work()) {
      baseRating += 2 ;
    }
    else if (point instanceof Cantina) {
      //  Modify this by the cost of paying for accomodations, and whether
      //  there's space available.
      baseRating += 3 ;
    }
    else if (point == actor.AI.home()) {
      //  Modify this by the upgrade level of your dwelling?
      baseRating += 4 ;
    }
    //
    //  If the venue doesn't belong to the same base, reduce the attraction.
    if (point instanceof Venue) {
      float relation = actor.AI.relation(((Venue) point).base()) ;
      baseRating *= relation ;
    }
    baseRating -= Plan.rangePenalty(actor, point) ;
    if (baseRating < 0) return 0 ;
    //
    //  Okay.  If you're only of average fatigue, these are the ratings.  With
    //  real exhaustion, however, these all converge to being paramount.
    final float fatigue = actor.health.fatigueLevel() ;
    if (fatigue < 0.5f) {
      return baseRating * fatigue * 2 ;
    }
    else {
      final float f = (fatigue - 0.5f) * 2 ;
      return (baseRating * f) + (PARAMOUNT * (1 - f)) ;
    }
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (currentMode == MODE_DINE) {
      d.append("Dining at ") ;
      d.append(restPoint) ;
    }
    else if (currentMode == MODE_SCAVENGE) {
      d.append("Scrounging food around ") ;
      d.append(restPoint) ;
    }
    else {
      d.append("Relaxing at ") ;
      d.append(restPoint) ;
    }
  }
}










