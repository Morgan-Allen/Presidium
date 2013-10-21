



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
  static boolean verbose = false ;
  Target safePoint = null ;
  
  
  public Retreat(Actor actor) {
    super(actor) ;
  }
  
  
  public Retreat(Actor actor, Target safePoint) {
    super(actor) ;
    this.safePoint = safePoint ;
  }


  public Retreat(Session s) throws Exception {
    super(s) ;
    this.safePoint = s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(safePoint) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float danger = dangerAtSpot(actor.origin(), actor, actor.mind.awareOf()) ;
    danger *= actor.traits.scaleLevel(NERVOUS) ;
    if (danger <= 0) return 0 ;
    I.sayAbout(actor, "Danger is: "+danger); 
    return Visit.clamp(danger * ROUTINE, 0, PARAMOUNT) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (safePoint == null || actor.aboard() == safePoint) {
      safePoint = nearestHaven(actor, null) ;
    }
    if (safePoint == null) {
      abortBehaviour() ;
      return null ;
    }
    //if (actor.aboard() == safePoint) return null ;
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
  public static Tile pickWithdrawPoint(
    Actor actor, float range,
    Target target, float salt
  ) {
    final int numPicks = 3 ;  //Make this an argument, instead of range?
    Tile pick = actor.origin() ;
    float bestRating = dangerAtSpot(pick, actor, actor.mind.awareOf()) ;
    for (int i = numPicks ; i-- > 0 ;) {
      
      //
      //  TODO:  Check by compass-point directions instead of purely at random.
      final Tile tried = Spacing.pickRandomTile(actor, range, actor.world()) ;
      if (tried == null) continue ;
      if (Spacing.distance(tried, target) > range) continue ;
      
      //
      //  TODO:  USE THE DANGER MAP INSTEAD.  Significantly cheaper.
      float tryRating = dangerAtSpot(tried, actor, actor.mind.awareOf()) ;
      tryRating += (Rand.num() - 0.5f) * salt ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick ;
  }
  
  
  public static float dangerAtSpot(
    Target spot, Actor actor, Batch <Element> seen
  ) {
    //
    //  Get a reading of threats based on all actors visible to this one, and
    //  their distance from the spot in question.  TODO:  Retain awareness
    //  longer?
    final boolean report = verbose && I.talkAbout == actor ;
    if (report) I.say("\n"+actor+" GETTING DANGER AT "+spot) ;
    
    //float sumDanger = 0, minDanger = 0 ;
    float sumThreats = 0, sumAllies = Combat.combatStrength(actor, null) ;
    
    final float range = World.SECTOR_SIZE ;
    for (Element m : seen) {
      if (m == actor || ! (m instanceof Actor)) continue ;
      final Actor near = (Actor) m ;
      if (near.indoors() || ! near.health.conscious()) continue ;
      final float threat = Combat.threatFrom(actor, near) ;
      float danger = threat ;
      //
      //  More distant foes are less threatening.
      final float dist = Spacing.distance(spot, near) / range ;
      if (dist > 1) danger /= 0.5f + dist ;
      else danger /= 1 + (dist / 2) ;
      //
      //  Adjust danger estimate based on allegiance-
      if (threat > 0) {
        danger *= Combat.combatStrength(near, actor) ;
        sumThreats += danger ;
      }
      if (threat < 0) {
        danger *= Combat.combatStrength(near, null) ;
        sumAllies += danger ;
      }
      if (report) {
        I.say("Danger from "+near+" is "+danger+", threat: "+threat) ;
      }
    }
    if (report) I.say("Sum of allies/enemies: "+sumAllies+"/"+sumThreats) ;
    if (sumThreats == 0) return 0 ;
    if (sumAllies == 0) return 100 ;
    final float estimate = sumThreats / (sumThreats + sumAllies) ;
    if (report) I.say("Total danger is: "+estimate) ;
    return estimate ;
  }
  
  
  
  /**  These methods select safe venues to run to, over longer distances.
    */
  public static Target nearestHaven(Actor actor, Class prefClass) {
    //
    //  TODO:  Use the list of venues the actor is aware of?
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
    
    return (Target) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    //
    //  TODO:  Don't pick anything too close by either.  That'll be in a
    //  dangerous area.
    if (! (t instanceof Venue)) return 1 ;
    final Venue haven = (Venue) t ;
    float rating = 1 ;
    if (haven.getClass() == prefClass) rating *= 4 ;
    if (haven.base() == actor.base()) rating *= 3 ;
    if (haven == actor.mind.home()) rating *= 2 ;
    final int SS = World.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
    return rating ;
  }
}


