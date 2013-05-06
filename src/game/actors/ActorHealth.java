/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;



public class ActorHealth {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOVE_WALK  = 0,
    MOVE_RUN   = 1,
    MOVE_SNEAK = 2 ;
  
  
  final Actor actor ;
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
  }
}









