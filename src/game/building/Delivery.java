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
import src.game.building.Inventory.Owner ;



//
//  You may also want to specify the vehicle employed, and how or if you get
//  aboard it...
public class Delivery extends Plan {
  
  
  final static int
    STAGE_INIT = -1,
    STAGE_PICKUP = 0,
    STAGE_DROPOFF = 1,
    STAGE_DONE = 2 ;
  
  final public Owner origin, destination ;
  final public Item items[] ;
  final Actor passenger ;
  
  private byte stage = STAGE_INIT ;
  private Barge barge ;
  //private Vehicle driven ;  ...Use instead of the barge?
  
  
  
  public Delivery(Item item, Owner origin, Owner destination) {
    this(new Item[] { item }, origin, destination) ;
  }
  
  
  public Delivery(Item items[], Owner origin, Owner destination) {
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
    origin = (Owner) s.loadObject() ;
    destination = (Owner) s.loadObject() ;
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
    final World world = ((Element) origin).world() ;
    final Batch <Behaviour> doing = world.activities.targeting(origin) ;
    for (Item i : items) {
      final float reserved = reservedForCollection(doing, i.type) ;
      final float excess = origin.inventory().amountOf(i) - reserved ;
      if (excess >= i.amount) return true ;
    }
    return false ;
  }
  
  
  public boolean complete() {
    return stage == STAGE_DONE ;
  }
  
  
  private static float reservedForCollection(
    Batch <Behaviour> doing, Service goodType
  ) {
    float sum = 0 ;
    for (Behaviour b : doing) {
      if (b instanceof Delivery) for (Item i : ((Delivery) b).items) {
        if (i.type == goodType) sum += i.amount ;
      }
    }
    return sum ;
  }
  
  
  public static Venue findBestVendor(Actor actor, Item items[]) {
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
  
  
  public static Delivery nextDeliveryFrom(
    Inventory.Owner venue, Actor actor, Service types[]
  ) {
    //
    //  Otherwise we iterate over every nearby venue, and see if they need what
    //  we're selling, so to speak.
    float maxUrgency = 0 ;
    Delivery picked = null ;
    final Presences presences = actor.world().presences ;
    final float SEARCH_RADIUS = World.DEFAULT_SECTOR_SIZE * 2 ;
    final float ORDER_UNIT = VenueStocks.ORDER_UNIT ;
    
    for (Object o : presences.matchesNear(
      actor.base(), venue, SEARCH_RADIUS
    )) {
      final Venue client = (Venue) o ;
      final float distFactor = (SEARCH_RADIUS + Spacing.distance(
        venue, client
      )) / SEARCH_RADIUS ;
      //
      //  If we don't have enough of a given item to sell, we just pass over
      //  that item type.  Conversely, if a venue has no shortage, it is
      //  ignored.
      for (Service type : types) {
        if (venue.inventory().amountOf(type) < ORDER_UNIT) continue ;
        final float shortage = client.stocks.requiredShortage(type) ;
        float urgency = shortage ;
        if (urgency <= 0) continue ;
        final Delivery order = new Delivery(
          Item.withAmount(type, ORDER_UNIT), venue, client
        ) ;
        if (! order.valid()) continue ;
        urgency /= distFactor ;
        if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
      }
    }
    return picked ;
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
    I.say(actor+" Performing pickup...") ;
    
    boolean addBarge = true ;
    if (target == origin) {
      float sum = 0 ;
      for (Item i : items) {
        float TA = origin.inventory().transfer(i, actor) ;
        sum += TA ;
      }
      if (sum < 5) addBarge = false ;
    }
    if (passenger != null) addBarge = true ;
    if (addBarge) {
      final Barge barge = new Barge(actor, this) ;
      final Tile o = actor.origin() ;
      barge.enterWorldAt(o.x, o.y, o.world) ;
      this.barge = barge ;
    }
    
    if (target == passenger) {
      barge.passenger = passenger ;
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
      if ((! origin.inventory().hasItem(i)) && (! actor.gear.hasItem(i))) {
        continue ;
      }
      available.add(i) ;
    }
    d.appendList("", available) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}








