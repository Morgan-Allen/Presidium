/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.util.Rand;



public class ActorHealth {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOVE_WALK  = 0,
    MOVE_RUN   = 1,
    MOVE_SNEAK = 2 ;
  
  final static float
    DEFAULT_PRIME    = 25,
    DEFAULT_LIFESPAN = 60,
    LIFE_EXTENDS     = 0.1f ;
  
  
  final Actor actor ;
  
  float birthDate, lifespan ;
  //  TODO:  have these as an array instead?
  /*
  private float lifespan ;
  private float energy, nutrition, bulk ;
  private float injury, fatigue, morale ;
  //*/
  
  private int moveType = MOVE_WALK ;
  private float baseSpeed = 1 ;//, moveRate = 1 ;
  
  
  
  ActorHealth(Actor actor) {
    this.actor = actor ;
    //  These are defaults I'm using for now.  Will be more individualised
    //  later.  TODO:  THAT
    birthDate = 0 - DEFAULT_PRIME ;
    lifespan = DEFAULT_LIFESPAN * (1 + Rand.range(0, LIFE_EXTENDS)) ;
  }
  
  
  void loadState(Session s) throws Exception {
  }
  
  
  void saveState(Session s) throws Exception {
  }
  
  
  public float moveRate() {
    float rate = baseSpeed ;
    //  You also have to account for the effects of fatigue and encumbrance...
    switch (moveType) {
      case (MOVE_SNEAK) : rate *= 0.50f ; break ;
      case (MOVE_WALK ) : rate *= 1.25f ; break ;
      case (MOVE_RUN  ) : rate *= 2.00f ; break ;
    }
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    return rate ;
  }
  
  
  public int agingStage() {
    final float time = actor.world().currentTime() / Planet.YEAR_LENGTH ;
    final float age = (time - birthDate) / lifespan ;
    return (int) (age * 4) ;
  }
  
  
  public int moveType() { return moveType ; }
  
  
  public boolean conscious() {
    return true ;
  }
  
  
  
  /**  Various short and long-term update methods-
    */
  void updatePerSecond() {
  }
  
  void updateOnGrowth() {
    //
    //  Check for disease.  Try healing injury.
    //  If you have enough food, convert some of it into growth.
    //  If you've reached maximum size, convert growth into offspring (for
    //  animals.)
    //  ...Also, check for aging.
    
  }
}









