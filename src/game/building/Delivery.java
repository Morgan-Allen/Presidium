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
  
  
  /**  Assessing targets and priorities-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (stage >= STAGE_PICKUP || origin == null) return true ;
    for (Item i : items) {
      final float reserved = reservedForCollection(origin, i.type) ;
      final float excess = origin.stocks.amountOf(i) - reserved ;
      if (excess >= i.amount) return true ;
    }
    return false ;
  }
  
  
  public boolean complete() {
    return stage == STAGE_DONE ;
  }
  
  
  public static float reservedForCollection(Venue venue, Service goodType) {
    float sum = 0 ;
    for (Behaviour b : venue.world().activities.targeting(venue)) {
      if (b instanceof Delivery) for (Item i : ((Delivery) b).items) {
        if (i.type == goodType) sum += i.amount ;
      }
    }
    return sum ;
  }
  
  
  public static Venue findBestVenue(Actor actor, Item items[]) {
    //
    //  TODO:  Base this off the list of venues the actor is aware of.
    //  ...Which should, in turn, be updated gradually over time.
    
    final World world = actor.world() ;
    final int maxTried = 3 ;
    Venue best = null ;
    float bestRating = 0 ;
    
    for (Item item : items) {
      int numTried = 0 ;
      for (Object t : world.presences.matchesNear(item.type, actor, null)) {
        if (++numTried > maxTried) break ;
        final Venue v = (Venue) t ;
        float rating = 0 ;
        for (Service s : v.services()) for (Item i : items) {
          if (s == i.type) rating += i.amount * v.stocks.amountOf(i) ;
        }
        final float dist = Spacing.distance(actor, v) ;
        rating /= 1 + (dist / World.DEFAULT_SECTOR_SIZE) ;
        ///I.say("Rating for "+v+" was: "+rating) ;
        if (rating > bestRating) { best = v ; bestRating = rating ; }
      }
    }
    ///I.say("Returning "+best) ;
    return best ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    //
    //  Here, you need to check if you have enough of the goods?
    if (stage == STAGE_INIT) {
      stage = STAGE_PICKUP ;
    }
    if (stage == STAGE_PICKUP) {
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
    if (stage == STAGE_DROPOFF) {
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
    ///I.say(this+" returning NULL!") ;
    return null ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    
    boolean addBarge = true ;
    if (target == origin) {
      float sum = 0 ;
      for (Item i : items) {
        float TA = origin.stocks.transfer(i, actor) ;
        sum += TA ;
      }
      if (sum < 5) addBarge = false ;
    }
    if (target == passenger) {
      barge.passenger = passenger ;
    }
    
    if (addBarge) {
      final Barge barge = new Barge(actor, this) ;
      final Tile o = actor.origin() ;
      barge.enterWorldAt(o.x, o.y, o.world) ;
      I.say(actor+" Performing pickup...") ;
    }
    stage = STAGE_DROPOFF ;
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
    final Batch <Item> available = new Batch <Item> () ;
    for (Item i : items) {
      if ((! origin.stocks.hasItem(i)) && (! actor.gear.hasItem(i))) continue ;
      available.add(i) ;
    }
    d.appendList("", available) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}








