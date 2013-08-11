


package src.game.social ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;
import src.user.* ;




//
//  This plan allows actors to 'upgrade' their weapons, armour, outfits or
//  devices.
public class Purchase extends Plan {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  Venue ordersAt ;
  
  
  public Purchase(Actor actor, Venue ordersAt) {
    super(actor, ordersAt) ;
  }
  
  
  public Purchase(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}











