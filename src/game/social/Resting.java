



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
    MODE_NONE     = -1,
    MODE_DINE     =  0,
    //MODE_SCAVENGE =  1,
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
    if (restPoint instanceof Venue && menuFor((Venue) restPoint).size() > 0) {
      if (actor.health.hungerLevel() > 0.1f) {
        final Action eats = new Action(
          actor, restPoint,
          this, "actionEats",
          Action.BUILD, "Eating at "+restPoint
        ) ;
        currentMode = MODE_DINE ;
        if (verbose) I.sayAbout(actor, "Returning eat action...") ;
        return eats ;
      }
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
  
  
  public boolean actionEats(Actor actor, Venue place) {
    return dineFrom(actor, place) ;
  }
  
  
  public static boolean dineFrom(Actor actor, Inventory.Owner stores) {
    final Batch <Service> menu = menuFor(stores) ;
    final int numFoods = menu.size() ;
    if (numFoods > 0 && actor.health.hungerLevel() > 0.1f) {
      //
      //  FOOD TO BODY-MASS RATIO IS 1 TO 10.  So, 1 unit of food will last a
      //  typical person 5 days.
      for (Service type : menu) {
        final Item portion = Item.withAmount(type, 0.1f * 1f / numFoods) ;
        stores.inventory().removeItem(portion) ;
      }
      actor.health.takeSustenance(1, numFoods / 2) ;
      return true ;
    }
    return false ;
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
    safePoints.add(actor.mind.home()) ;
    safePoints.add(actor.mind.work()) ;
    safePoints.add(presences.nearestMatch(Cantina.class, actor, -1)) ;
    //
    //  Now pick whichever option is most attractive.
    final Target picked = new Visit <Target> () {
      public float rate(Target b) { return ratePoint(actor, b) ; }
    }.pickBest(safePoints) ;
    
    if (verbose) I.sayAbout(
      actor, "Have picked "+picked+", home rating: "+
      ratePoint(actor, actor.mind.home())
    ) ;
    return picked ;
  }
  
  
  private static Batch <Service> menuFor(Inventory.Owner place) {
    Batch <Service> menu = new Batch <Service> () ;
    for (Service type : ALL_FOOD_TYPES) {
      if (place.inventory().amountOf(type) >= 0.1f) menu.add(type) ;
    }
    return menu ;
  }
  
  
  public static float ratePoint(Actor actor, Target point) {
    if (point == null) return -1 ;
    if (point instanceof Venue && ! ((Venue) point).structure.intact()) {
      return -1 ;
    }
    float baseRating = 0 ;
    if (point == actor.mind.home()) {
      baseRating += 4 ;
    }
    else if (point instanceof Cantina) {
      //  Modify this by the cost of paying for accomodations, and whether
      //  there's space available.
      baseRating += 3 ;
    }
    else if (point == actor.mind.work()) {
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
      baseRating *= actor.mind.relation(venue) ;
      //
      //  Also, include the effects of hunger-
      float sumFood = 0 ;
      for (Service s : menuFor(venue)) {
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
    /*
    else if (currentMode == MODE_SCAVENGE) {
      d.append("Foraging around ") ;
      d.append(restPoint) ;
    }
    //*/
    else {
      d.append("Resting at ") ;
      d.append(restPoint) ;
    }
  }
}










