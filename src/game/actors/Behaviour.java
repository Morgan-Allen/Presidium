/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.user.* ;



public interface Behaviour extends Session.Saveable {
  
  final public static float
    IDLE     = 1,
    CASUAL   = 3,
    ROUTINE  = 5,
    URGENT   = 7,
    CRITICAL = 9,
    PARAMOUNT = 10 ;
  
  
  Behaviour nextStepFor(Actor actor) ;
  boolean monitor(Actor actor) ;
  
  float priorityFor(Actor actor) ;
  boolean finished() ;
  void abortBehaviour() ;
  //boolean viable() ;
  boolean begun() ;
  
  void describeBehaviour(Description d) ;
}










