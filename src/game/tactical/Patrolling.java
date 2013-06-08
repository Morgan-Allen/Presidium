/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.Description ;
import src.util.* ;


//
//  Maybe priority should be assigned externally?  Depending on how this
//  behaviour gets generated?


public class Patrolling extends Plan {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Actor actor ;
  private Target around ;
  private Tile goes ;
  private float range ;
  private int compassPoint, initPoint ;
  private int numCircuits = 0 ;
  
  
  
  public Patrolling(Actor actor, Target around, float range) {
    super(actor) ;
    this.actor = actor ;
    this.around = around ;
    this.range = range ;
    initPoint = compassPoint = Rand.index(4) ;
    numCircuits = 0 ;
  }
  
  
  public Patrolling(Session s) throws Exception {
    super(s) ;
    actor = (Actor) s.loadTarget() ;
    around = s.loadTarget() ;
    goes = (Tile) s.loadTarget() ;
    range = s.loadFloat() ;
    compassPoint = s.loadInt() ;
    initPoint = s.loadInt() ;
    numCircuits = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(actor) ;
    s.saveTarget(around) ;
    s.saveTarget(goes) ;
    s.saveFloat(range) ;
    s.saveInt(compassPoint) ;
    s.saveInt(initPoint) ;
    s.saveInt(numCircuits) ;
  }
  
  
  
  /**  Behaviour execution-
    */
  private boolean pickCompassPoint() {
    if (numCircuits == 2) {
      goes = null ;
      return false ;
    }
    final World world = actor.world() ;
    final Vec3D pick = around.position(null) ;
    switch (compassPoint) {
      case(0) : pick.y += range ; break ;
      case(1) : pick.x += range ; break ;
      case(2) : pick.y -= range ; break ;
      case(3) : pick.x -= range ; break ;
    }
    if ((compassPoint = (compassPoint + 1) % 4) == initPoint) numCircuits++ ;
    pick.x = Visit.clamp(pick.x, 0, world.size - 1) ;
    pick.y = Visit.clamp(pick.y, 0, world.size - 1) ;
    goes = Spacing.nearestOpenTile(world.tileAt(pick.x, pick.y), actor) ;
    if (goes == null || goes.blocked()) return false ;
    return true ;
  }
  
  
  public Behaviour getNextStep() {
    ///I.say("Getting next patrol action for: "+actor) ;
    if (goes == null) pickCompassPoint() ;
    if (goes == null) return null ;
    return new Action(
      actor, goes,
      this, "actionPatrol",
      Action.LOOK, "Patrolling"
    ) ;
  }
  
  
  public void abortStep() {
    I.say("Aborting patrol...") ;
    numCircuits = 2 ;
    goes = null ;
  }
  
  
  public float priorityFor(Actor actor) {
    //  Vary priority based on the observation skills of the actor in question?
    return ROUTINE ;
  }
  
  
  public boolean complete() {
    return numCircuits >= 2 ;
  }
  

  public boolean monitor(Actor actor) {
    //  You'll have to implement observation and chase behaviour here.
    return true ;
  }


  public boolean actionPatrol(Actor actor, Target spot) {
    //  You'll have to implement observation and chase behaviour here.
    if (pickCompassPoint()) return true ;
    else { numCircuits = 2 ; return false ; }
  }
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Patrolling around ") ;
    d.append(around) ;
  }
}





