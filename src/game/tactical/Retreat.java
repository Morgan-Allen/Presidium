



package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Retreat extends Plan implements Abilities {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  static boolean verbose = true ;
  Boardable safePoint = null ;
  
  
  public Retreat(Actor actor) {
    super(actor) ;
  }
  
  
  public Retreat(Actor actor, Boardable safePoint) {
    super(actor) ;
    this.safePoint = safePoint ;
  }


  public Retreat(Session s) throws Exception {
    super(s) ;
    this.safePoint = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(safePoint) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float danger = dangerAtSpot(
      actor.origin(), actor, null, actor.mind.awareOf()
    ) ;
    danger += priorityMod / ROUTINE ;
    if (danger <= 0) return 0 ;
    
    final float stress = Visit.clamp(
      actor.health.stressPenalty() +
      actor.health.injuryLevel(), 0, 1
    ) ;
    danger = Math.max(danger, stress * 2) ;
    danger *= actor.traits.scaleLevel(NERVOUS) ;
    //danger = ((1 - stress) * danger) + (stress * 2) ;
    
    if (verbose && I.talkAbout == actor) {
      I.say("Perceived danger: "+danger+", stress: "+stress) ;
    }
    if (danger <= 0) return 0 ;
    return Visit.clamp(danger * PARAMOUNT, 0, PARAMOUNT * 2) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = nearestHaven(actor, null) ;
      priorityMod *= 0.5f ;
      if (priorityMod < 0.25f) priorityMod = 0 ;
    }
    if (safePoint == null) {
      abortBehaviour() ;
      return null ;
    }
    final Action flees = new Action(
      actor, safePoint,
      this, "actionFlee",
      Action.LOOK, "Fleeing to "+safePoint
    ) ;
    flees.setProperties(Action.QUICK) ;
    return flees ;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ") ;
    else d.append("Retreating to ") ;
    d.append(safePoint) ;
  }
  
  
  
  /**  These methods select safe points to withdraw to in a local area.
    */
  public static Target pickWithdrawPoint(
    Actor actor, float range,
    Target target, float salt
  ) {
    final int numPicks = 3 ;  // TODO:  Make this an argument, instead of range
    Target pick = actor.aboard() ;
    float bestRating = salt > 0 ?
      Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY ;
    for (int i = numPicks ; i-- > 0 ;) {
      //
      //  TODO:  Check by compass-point directions instead of purely at random?
      Tile tried = Spacing.pickRandomTile(actor, range, actor.world()) ;
      if (tried == null) continue ;
      tried = Spacing.nearestOpenTile(tried, target) ;
      ///I.say("Tried is: "+tried) ;
      if (tried == null || Spacing.distance(tried, target) > range) continue ;
      
      //
      //  Use the danger map if possible, since it's substantially cheaper
      //  TODO:  Have every actor assigned to a base?  ...Probably safer.
      float tryRating ;
      if (actor.base() != null) {
        tryRating = actor.base().dangerMap.shortTermVal(tried) ;
      }
      else {
        tryRating = dangerAtSpot(tried, actor, null, actor.mind.awareOf()) ;
      }
      
      tryRating += (Rand.num() - 0.5f) * salt ;
      if (salt < 0) tryRating *= -1 ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick ;
  }
  
  
  public static float dangerAtSpot(
    Target spot, Actor actor, Actor enemy, Batch <Element> seen
  ) {
    if (spot == null) return 0 ;

    // final boolean report = verbose && I.talkAbout == actor ;
    
    final float basePower = Combat.combatStrength(actor, enemy) ;
    float sumAllies = basePower, sumEnemies = 0 ;
    
    //
    //  TODO:  ...What about blending values from the danger map?
    //  float estimate = 0 - basePower ;

    for (Element m : seen) {
      if (m == actor || ! (m instanceof Actor)) continue ;
      final Actor near = (Actor) m ;
      if (near.indoors() || ! near.health.conscious()) continue ;
      
      final float relation = near.mind.relation(actor) ;
      final float power = Combat.combatStrength(near, enemy) ;
      if (relation > 0) {
        sumAllies += power ;
      }
      if (relation < 0) {
        sumEnemies += power ;
      }
    }
    
    return sumEnemies / (sumEnemies + sumAllies) ;
  }
  
  
  
  /**  These methods select safe venues to run to, over longer distances.
    */
  public static Boardable nearestHaven(Actor actor, Class prefClass) {
    //
    //  TODO:  Use the list of venues the actor is aware of?
    if (actor == null) I.say("NO ACTOR!") ;
    final Presences p = actor.world().presences ;
    int numC = 3 ;
    
    Object picked = null ;
    float bestRating = 0 ;
    int numChecked = 0 ;
    
    if (actor.mind.home() != null) {
      final Venue home = actor.mind.home() ;
      float rating = rateHaven(home, actor, prefClass) ;
      if (rating > bestRating) { bestRating = rating ; picked = home ; }
    }
    if (actor.base() != null) {
      for (Object t : p.matchesNear(actor.base(), actor, -1)) {
        if (numChecked++ > numC) break ;
        float rating = rateHaven(t, actor, prefClass) ;
        if (rating > bestRating) { bestRating = rating ; picked = t ; }
      }
    }
    if (prefClass != null) {
      numChecked = 0 ;
      for (Object t : p.matchesNear(prefClass, actor, -1)) {
        if (numChecked++ > numC) break ;
        float rating = rateHaven(t, actor, prefClass) ;
        if (rating > bestRating) { bestRating = rating ; picked = t ; }
      }
    }
    if (picked == null) picked = pickWithdrawPoint(
      actor, actor.health.sightRange(), actor, 0.1f
    ) ;
    
    return (Boardable) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    //
    //  TODO:  Don't pick anything too close by either.  That'll be in a
    //  dangerous area.
    if (! (t instanceof Venue)) return 1 ;
    final Venue haven = (Venue) t ;
    if (! haven.structure.intact()) return -1 ;
    float rating = 1 ;
    if (haven.getClass() == prefClass) rating *= 4 ;
    if (haven.base() == actor.base()) rating *= 3 ;
    if (haven == actor.mind.home()) rating *= 2 ;
    final int SS = World.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
    return rating ;
  }
}


