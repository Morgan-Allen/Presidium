



package src.game.social ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



//
//  Various places can be used to relax- your home or workplace, the cantina,
//  the pleasure dome, the arena, the senate or the archives.
//
//  These have different properties in terms of morale-boost and lifting
//  fatigue/physical comfort.  Some can be used as a form of training.
//  Different personality types also tend to prefer different types of venue.
//
//  ...For now, just focus on the fatigue-relief aspect.



public class Resting extends Plan {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final Boardable relaxesAt ;
  
  
  public Resting(Actor actor, Boardable relaxesAt) {
    super(actor) ;
    this.relaxesAt = relaxesAt ;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s) ;
    this.relaxesAt = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(relaxesAt) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return rateVenue(actor, relaxesAt) ;
  }
  
  
  protected Behaviour getNextStep() {
    final Action relax = new Action(
      actor, relaxesAt,
      this, "actionRelax",
      Action.FALL, "Relaxing at "+relaxesAt
    ) ;
    return relax ;
  }
  
  
  public boolean actionRelax(Actor actor, Target place) {
    //
    //  Enter sleep mode-
    int relaxBonus = 3 ;
    float liftF = ActorHealth.FATIGUE_GROW_PER_DAY ;
    liftF *= relaxBonus / World.DEFAULT_DAY_LENGTH ;
    actor.health.liftFatigue(liftF) ;
    //
    //  TODO:  You should also relieve a certain amount of stress.
    return true ;
  }
  
  
  
  /**  Methods used for external assessment-
    */
  public static float rateVenue(Actor actor, Boardable venue) {
    float baseRating = 0 ;
    if (venue instanceof Tile) {
      baseRating += 1 ;
    }
    if (venue == actor.AI.work()) {
      baseRating += 2 ;
    }
    if (venue instanceof Cantina) {
      //  Modify this by the cost of paying for accomodations.
      baseRating += 3 ;
    }
    if (venue == actor.AI.home()) {
      //  Modify this by the upgrade level of your dwelling.
      baseRating += 4 ;
    }
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
    super.describeBehaviour(d) ;
  }
}








//
//  TODO:  Bear in mind half of these don't exist yet.
//  The Cantina is preferred by the indolent, debauched and gregarious.
//
//  The Arena is preferred by the optimistic, fearless or cruel.
//
//  The Senate is preferred by the stubborn, empathic or ambitious.
//  ...et cetera.



