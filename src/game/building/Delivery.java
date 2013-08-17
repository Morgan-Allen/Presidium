/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.util.* ;
import src.user.* ;



//
//  You may also want to specify the vehicle employed, and how or if you get
//  aboard it...
public class Delivery extends Plan {
  
  
  final static int
    STAGE_INIT = -1,
    STAGE_PICKUP = 0,
    STAGE_DROPOFF = 1,
    STAGE_DONE = 2 ;
  
  final public Venue origin, destination ;  //Allow arbitrary Owners instead.
  final public Item items[] ;
  final Actor passenger ;
  
  private byte stage = STAGE_INIT ;
  private Barge barge ;
  
  
  
  public Delivery(Item item, Venue origin, Venue destination) {
    this(new Item[] { item }, origin, destination) ;
  }
  
  
  public Delivery(Item items[], Venue origin, Venue destination) {
    super(null, origin, destination) ;
    this.origin = origin ;
    this.destination = destination ;
    this.items = items ;
    this.passenger = null ;
  }
  
  
  public Delivery(Actor passenger, Venue destination) {
    super(null, passenger, destination) ;
    this.origin = null ;
    this.destination = destination ;
    this.items = new Item[0] ;
    this.passenger = passenger ;
  }
  
  
  public Delivery(Session s) throws Exception {
    super(s) ;
    items = new Item[s.loadInt()] ;
    for (int n = 0 ; n < items.length ;) items[n++] = Item.loadFrom(s) ;
    passenger = (Actor) s.loadObject() ;
    origin = (Venue) s.loadObject() ;
    destination = (Venue) s.loadObject() ;
    stage = (byte) s.loadInt() ;
    barge = (Barge) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(items.length) ;
    for (Item i : items) Item.saveTo(s, i) ;
    s.saveObject(passenger) ;
    s.saveObject((Session.Saveable) origin) ;
    s.saveObject((Session.Saveable) destination) ;
    s.saveInt(stage) ;
    s.saveObject(barge) ;
  }
  
  
  int stage() { return stage ; }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (stage >= STAGE_PICKUP || origin == null) return true ;
    return origin.stocks.canMeetOrder(this) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public Behaviour getNextStep() {
    //
    //  Here, you need to check if you have enough of the goods?
    if (stage == STAGE_INIT) {
      stage = STAGE_PICKUP ;
      //
      //  Create a barge to follow the actor (which will dispose of itself
      //  once the behaviour completes.)
      I.say(actor+" Scheduling new pickup...") ;
      final Action pickup = new Action(
        actor, (passenger == null) ? origin : passenger,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      ) ;
      return pickup ;
    }
    if (stage == STAGE_PICKUP) {
      stage = STAGE_DROPOFF ;
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      ) ;
      dropoff.setProperties(Action.CARRIES) ;
      //
      //  If the destination isn't complete, drop off at the entrance?
      return dropoff ;
    }
    return null ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    final Barge barge = new Barge(actor, this) ;
    final Tile o = actor.origin() ;
    barge.enterWorldAt(o.x, o.y, o.world) ;
    I.say(actor+" Performing pickup...") ;
    
    if (target == origin) {
      for (Item i : items) origin.stocks.transfer(i, actor) ;
    }
    if (target == passenger) {
      barge.passenger = passenger ;
    }
    stage = STAGE_PICKUP ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Venue target) {
    if (stage != STAGE_DROPOFF) return false ;
    I.say(actor+" Performing dropoff...") ;
    if (barge != null && barge.inWorld()) barge.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i, target) ;
    if (passenger != null) passenger.goAboard(target, target.world()) ;
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








