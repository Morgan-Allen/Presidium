/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.Description ;
import src.util.* ;



public class Manufacture extends Plan implements Behaviour {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Venue venue ;
  final Conversion conversion ;
  
  private float progress = 0, timeTaken = 0 ;
  private Item[] needed ;
  
  
  public Manufacture(Actor actor, Venue venue, Conversion conversion) {
    super(actor, venue) ;
    this.venue = venue ;
    this.conversion = conversion ;
    this.needed = conversion.raw ;
    for (Item made : conversion.out) timeTaken += made.amount ;
  }
  
  public Manufacture(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
    conversion = (Conversion) s.loadObject() ;
    progress = s.loadFloat() ;
    timeTaken = s.loadFloat() ;
    this.needed = conversion.raw ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    s.saveObject(conversion) ;
    s.saveFloat(progress) ;
    s.saveFloat(timeTaken) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    if (progress >= 1) return null ;
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Working"
    ) ;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    for (Item need : needed) {
      if (! venue.stocks.hasItem(need)) return false ;
    }
    return true ;
  }
  
  
  
  //  This should be adaptable to construction as well.
  
  public boolean actionMake(Actor actor, Venue venue) {
    //
    //  First, check to make sure you have adequate raw materials.
    final Conversion c = conversion ;
    final float inc = 1 / timeTaken ;
    if (progress == 0) {
      for (Item r : c.raw) {
        if (! venue.inventory().hasItem(r)) {
          abortStep() ;
          return false ;
        }
      }
      for (Item r : c.raw) venue.inventory().removeItem(r) ;
    }
    //
    //  Secondly, make sure the skill tests all check out.
    boolean success = true ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      success &= actor.traits.test(c.skills[i], c.skillDCs[i], 1) ;
    }
    progress += inc * (success ? 1.5f : 0.5f) ;
    //
    //  Advance progress, and check if you're done yet.
    if (progress >= 1) {
      for (Item made : conversion.out) {
        venue.stocks.addItem(made) ;
      }
      return true ;
    }
    return false ;
  }
  
  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    d.append("Manufacturing ") ;
    for (Item i : conversion.out) {
      d.append(i.type.name) ;
      if (i != Visit.last(conversion.out)) d.append(" and ") ;
    }
    d.append(" at the ") ;
    d.append(venue) ;
  }
}











