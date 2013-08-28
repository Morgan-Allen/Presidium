/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



//
//  ...This could be linked to an Order object at a venue, so that progress
//  can be tracked externally and the job taken up by other actors.


public class Manufacture extends Plan implements Behaviour {
  
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TIME_PER_UNIT = 10 ;
  
  final Venue venue ;
  final Conversion conversion ;
  final Item made, needed[] ;
  
  private float progress = 0, timeTaken = 0 ;
  
  
  
  public Manufacture(
    Actor actor, Venue venue, Conversion conversion, Item made
  ) {
    super(actor, venue) ;
    this.venue = venue ;
    this.made = made == null ? conversion.out : made ;
    this.conversion = conversion ;
    this.needed = conversion.raw ;
    timeTaken += this.made.amount ;
    timeTaken *= TIME_PER_UNIT ;
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
    conversion = Conversion.loadFrom(s) ;
    made = Item.loadFrom(s) ;
    progress = s.loadFloat() ;
    timeTaken = s.loadFloat() ;
    this.needed = conversion.raw ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    Conversion.saveTo(s, conversion) ;
    Item.saveTo(s, made) ;
    s.saveFloat(progress) ;
    s.saveFloat(timeTaken) ;
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  public float priorityFor(Actor actor) {
    if (GameSettings.hardCore && ! hasNeeded()) return 0 ;
    //
    //  Vary priority based on how qualified to perform the task you are.
    
    return ROUTINE ;
  }
  
  
  public boolean valid() {
    if (GameSettings.hardCore && ! hasNeeded()) return false ;
    return true ;
  }
  
  
  private boolean hasNeeded() {
    for (Item need : needed) {
      if (! venue.stocks.hasItem(need)) return false ;
    }
    return true ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    if (progress >= 1) {
      //  See if the venue has more orders for items of this type.
      return null ;
    }
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Working"
    ) ;
  }
  
  
  public boolean actionMake(Actor actor, Venue venue) {
    //
    //  First, check to make sure you have adequate raw materials.  (In hard-
    //  core mode, raw materials are strictly essential, and will be depleted
    //  regardless of success.)
    ///I.say(actor+" making "+made) ;
    final boolean hasNeeded = hasNeeded() ;
    if (GameSettings.hardCore && ! hasNeeded) {
      abortStep() ;
      return false ;
    }
    final Conversion c = conversion ;
    final int checkMod = hasNeeded ? 0 : 5 ;
    final float progInc = (hasNeeded ? 1 : 0.5f) / timeTaken ;
    //
    //  Secondly, make sure the skill tests all check out, and deplete any raw
    //  materials used up.
    boolean success = true ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      success &= actor.traits.test(c.skills[i], c.skillDCs[i] + checkMod, 1) ;
    }
    ///if (! success) I.say(" FAILED TEST") ;
    if (success || GameSettings.hardCore) {
      for (Item r : c.raw) {
        final Item used = Item.withAmount(r, r.amount * progInc) ;
        venue.inventory().removeItem(used) ;
      }
    }
    //
    //  Advance progress, and check if you're done yet.
    final int oldCount = (int) (progress * made.amount) ;
    progress += success ? progInc : (progInc / 10f) ;
    ///progress = Visit.clamp(progress, 0, 1) ;
    final int newCount = (int) (progress * made.amount) ;
    if (newCount > oldCount) {
      venue.stocks.addItem(Item.withAmount(made, newCount - oldCount)) ;
      if (progress >= 1) return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    d.append("Manufacturing "+made) ;
    d.append(" ("+((int) (progress * 100))+"%)") ;
    /*
    d.append(" at the ") ;
    d.append(venue) ;
    //*/
  }
}












