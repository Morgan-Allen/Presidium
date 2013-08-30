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
  final public static int
    STAGE_INIT = -1,
    STAGE_BEGUN = 0,
    STAGE_COMPLETE = 1,
    STAGE_CANCELLED = 2 ;
  
  
  
  Behaviour nextStepFor(Actor actor) ;
  boolean monitor(Actor actor) ;  //Get rid of these.
  void setPriority(float priority) ;  //...Not used for Plans.
  
  float priorityFor(Actor actor) ;
  boolean complete() ;  //specify actor?  Return stage?
  void abortStep() ;    //specify actor?
  //boolean viable() ; ?
  //boolean begun() ; ?  ...No.  Return a Stage descriptor.
  
  void describeBehaviour(Description d) ;
}










