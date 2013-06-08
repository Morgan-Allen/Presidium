/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;
import src.user.* ;


//  You may also want to specify the vehicle employed, and how or if you get
//  aboard it...
public class Delivery extends Plan {
  
  
  final static int
    STAGE_INIT = -1,
    STAGE_PICKUP = 0,
    STAGE_DROPOFF = 1,
    STAGE_DONE = 2 ;
  
  final public Venue origin, destination ;
  final public Item items[] ;
  private byte stage = STAGE_INIT ;
  

  public Delivery(Item item, Venue origin, Venue destination) {
    this(new Item[] {item}, origin, destination) ;
  }
  
  public Delivery(Item items[], Venue origin, Venue destination) {
    super(null, origin, destination) ;
    this.origin = origin ;
    this.destination = destination ;
    this.items = items ;
  }
  
  
  public Delivery(Session s) throws Exception {
    super(s) ;
    items = new Item[s.loadInt()] ;
    for (int n = 0 ; n < items.length ;) items[n++] = Item.loadFrom(s) ;
    origin = (Venue) s.loadObject() ;
    destination = (Venue) s.loadObject() ;
    stage = (byte) s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(items.length) ;
    for (Item i : items) Item.saveTo(s, i) ;
    s.saveObject((Session.Saveable) origin) ;
    s.saveObject((Session.Saveable) destination) ;
    s.saveInt(stage) ;
  }
  
  
  int stage() { return stage ; }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (stage >= STAGE_PICKUP) return true ;
    return origin.orders.canMeetOrder(this) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  public Behaviour getNextStep() {
    //  Here, you need to check if you have enough of the goods?
    if (stage == STAGE_INIT) {
      stage = STAGE_PICKUP ;
      final Action pickup = new Action(
        actor, origin,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      ) ;
      I.say(actor+" Scheduling new pickup...") ;
      return pickup ;
    }
    if (stage == STAGE_PICKUP) {
      stage = STAGE_DROPOFF ;
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      ) ;
      //  If the destination isn't complete, drop off at the entrance.
      return dropoff ;
    }
    return null ;
  }
  

  public boolean actionPickup(Actor actor, Venue target) {
    if (stage != STAGE_PICKUP) return false ;
    I.say(actor+" Performing pickup...") ;
    for (Item i : items) target.stocks.transfer(i, actor) ;
    stage = STAGE_PICKUP ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Venue target) {
    if (stage != STAGE_DROPOFF) return false ;
    I.say(actor+" Performing dropoff...") ;
    for (Item i : items) actor.gear.transfer(i, target) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Delivering ") ;
    for (Item i : items) {
      d.append(i) ;
      if (i == Visit.last(items)) d.append(" from ") ;
      else d.append(", ") ;
    }
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}








