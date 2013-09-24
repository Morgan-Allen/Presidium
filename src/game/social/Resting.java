



package src.game.social ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



public class Resting extends Plan implements BuildConstants {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean verbose = false ;
  
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
  
  
  public Resting(Actor actor, Target relaxesAt) {
    super(actor) ;
    this.restPoint = (Boardable) relaxesAt ;
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
    ///I.sayAbout(actor, "Considering rest...") ;
    if (restPoint == null) return -1 ;
    final float priority = ratePoint(actor, restPoint) ;
    //
    //  To ensure that the actor doesn't see-saw between relaxing and not
    //  relaxing as fatigue lifts, we establish a minimum priority-
    if (minPriority == -1) minPriority = Math.max(priority / 2, priority - 2) ;
    if (priority < minPriority) return minPriority ;
    if (verbose) I.sayAbout(actor, actor+" resting priority: "+priority) ;
    return priority ;
  }
  
  
  protected Behaviour getNextStep() {
    if (restPoint == null) return null ;
    final Behaviour eats = nextEating() ;
    if (eats != null) {
      if (verbose) I.sayAbout(actor, "Returning eat action...") ;
      return eats ;
    }
    //
    //  If you're tired, put your feet up.
    if (actor.health.fatigueLevel() > 0.1f) {
      final Action relax = new Action(
        actor, restPoint,
        this, "actionRest",
        Action.FALL, "Resting at "+restPoint
      ) ;
      currentMode = MODE_SLEEP ;
      return relax ;
    }
    return null ;
  }
  
  
  private Behaviour nextEating() {
    //
    //  If you're hungry, eat from local stocks.
    if (actor.health.hungerLevel() > 0.1f) {
      final Action eats = new Action(
        actor, restPoint,
        this, "actionEats",
        Action.BUILD, "Eating at "+restPoint
      ) ;
      final Batch <Service> menu = menuFor(restPoint) ;
      if (menu.size() == 0) {
        //
        //  TODO:  Make foraging/gathering into a separate behaviour.
        if (actor.health.hungerLevel() > 0.6f) {
          final Tile t = Spacing.pickFreeTileAround(restPoint, actor) ;
          eats.setMoveTarget(t) ;
          ///eats.setProperties(Action.CAREFUL) ;
          currentMode = MODE_SCAVENGE ;
          return eats ;
        }
        else return null ;
      }
      currentMode = MODE_DINE ;
      I.sayAbout(actor, "Menu size: "+menu.size()) ;
      return eats ;
    }
    return null ;
  }
  
  
  public boolean actionEats(Actor actor, Target place) {
    //
    //  If you're inside a venue, and it has food, then avail of it-
    final Batch <Service> menu = menuFor(place) ;
    final int numFoods = menu.size() ;
    if (numFoods > 0) {
      //
      //  FOOD TO BODY-MASS RATIO IS 1 TO 10.  So, 1 unit of food will last a
      //  typical person 5 days.
      final Venue venue = (Venue) place ;
      for (Service type : menu) {
        final Item portion = Item.withAmount(type, 0.1f * 1f / numFoods) ;
        venue.inventory().removeItem(portion) ;
      }
      actor.health.takeSustenance(1, numFoods / 2) ;
      return true ;
    }
    //
    //  Otherwise, grub whatever you can from the surroundings-
    else {
      final boolean
        canGrub = actor.traits.test(HARD_LABOUR, SCAVENGE_DC, 1),
        goodStuff = actor.traits.test(CULTIVATION, FORAGING_DC, 1) ;
      if (canGrub) actor.health.takeSustenance(1, goodStuff ? 0.5f : 0) ;
      return true ;
    }
  }
  
  
  public boolean actionRest(Actor actor, Boardable place) {
    //  TODO:  If you're in a Cantina, you'll have to pay the
    //  admission fee.
    ///I.sayAbout(actor, "ACTOR NOW RESTING") ;
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    return true ;
  }
  
  
  
  /**  Methods used for external assessment-
    */
  public static Target pickRestPoint(final Actor actor) {
    final Batch <Target> safePoints = new Batch <Target> () ;
    final Presences presences = actor.world().presences ;
    safePoints.add(Retreat.pickWithdrawPoint(actor, 16, actor, 0.1f)) ;
    safePoints.add(actor.AI.home()) ;
    safePoints.add(actor.AI.work()) ;
    safePoints.add(presences.nearestMatch(Cantina.class, actor, -1)) ;
    //
    //  Now pick whichever option is most attractive.
    final Target picked = new Visit <Target> () {
      public float rate(Target b) { return ratePoint(actor, b) ; }
    }.pickBest(safePoints) ;
    
    if (verbose) I.sayAbout(
      actor, "Have picked "+picked+", home rating: "+
      ratePoint(actor, actor.AI.home())
    ) ;
    return picked ;
  }
  
  
  private static Batch <Service> menuFor(Target place) {
    Batch <Service> menu = new Batch <Service> () ;
    if (! (place instanceof Venue)) return menu ;
    final Venue venue = (Venue) place ;
    for (Service type : ALL_FOOD_TYPES) {
      if (venue.inventory().amountOf(type) >= 0.1f) menu.add(type) ;
    }
    return menu ;
  }
  
  
  public static float ratePoint(Actor actor, Target point) {
    if (point == null) return -1 ;
    if (point instanceof Venue && ! ((Venue) point).structure.intact()) {
      return -1 ;
    }
    float baseRating = 0 ;
    if (point == actor.AI.home()) {
      baseRating += 4 ;
    }
    else if (point instanceof Cantina) {
      //  Modify this by the cost of paying for accomodations, and whether
      //  there's space available.
      baseRating += 3 ;
    }
    else if (point == actor.AI.work()) {
      baseRating += 2 ;
    }
    else if (point instanceof Tile) {
      baseRating += 1 ;
    }
    baseRating *= 1.5f - Planet.dayValue(actor.world()) ;
    //
    //  If the venue doesn't belong to the same base, reduce the attraction.
    if (point instanceof Venue) {
      final Venue venue = (Venue) point ;
      baseRating *= actor.AI.relation(venue) ;
      //
      //  Also, include the effects of hunger-
      float sumFood = 0 ;
      for (Service s : menuFor(point)) {
        sumFood += venue.stocks.amountOf(s) ;
      }
      if (sumFood > 1) sumFood = 1 ;
      baseRating += actor.health.hungerLevel() * sumFood * PARAMOUNT ;
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
      d.append("Eating at ") ;
      d.append(restPoint) ;
    }
    else if (currentMode == MODE_SCAVENGE) {
      d.append("Foraging around ") ;
      d.append(restPoint) ;
    }
    else {
      d.append("Resting at ") ;
      d.append(restPoint) ;
    }
  }
}










