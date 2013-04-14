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
  private float baseSpeed = 1, moveRate = 1 ;
  
  
  
  ActorHealth(Actor actor) {
    this.actor = actor ;
  }
  
  
  void loadState(Session s) throws Exception {
  }
  
  
  void saveState(Session s) throws Exception {
  }
  
  
  public float moveRate() { return baseSpeed * moveRate ; }
  public int moveType() { return moveType ; }
  
  
  
  
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









