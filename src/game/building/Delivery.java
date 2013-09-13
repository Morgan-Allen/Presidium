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
//  TODO:  Now you have to specify a vehicle.


public class Delivery extends Plan {
  
  
  final static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_DROPOFF =  1,
    STAGE_RETURN  =  2,
    STAGE_DONE    =  3 ;
  final static int
    MIN_BULK = 5 ;
  
  final public Owner origin, destination ;
  final public Item items[] ;
  final Actor passenger ;
  
  private byte stage = STAGE_INIT ;
  private Suspensor suspensor ;
  public Vehicle driven ;  //...Use instead of the suspensor?
  
  
  
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
    suspensor = (Suspensor) s.loadObject() ;
    driven = (Vehicle) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(items.length) ;
    for (Item i : items) Item.saveTo(s, i) ;
    s.saveObject(passenger) ;
    s.saveObject((Session.Saveable) origin) ;
    s.saveObject((Session.Saveable) destination) ;
    s.saveInt(stage) ;
    s.saveObject(suspensor) ;
    s.saveObject(driven) ;
  }
  
  
  int stage() { return stage ; }
  
  
  /**  Assessing targets and priorities-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE + priorityMod ;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (driven != null) {
      if (driven.aboard() != origin && ! driven.inside().contains(actor)) {
        return false ;
      }
    }
    
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
  
  
  
  /**  Utility methods for generating deliveries to/from venues-
    */
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
          if (s == i.type) rating += v.stocks.amountOf(i) ;
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
    final Batch <Venue> clients = new Batch <Venue> () ;
    final Presences presences = actor.world().presences ;
    final float SEARCH_RADIUS = World.DEFAULT_SECTOR_SIZE * 2 ;
    for (Object o : presences.matchesNear(
      actor.base(), venue, SEARCH_RADIUS
    )) clients.add((Venue) o) ;
    return nextDeliveryFrom(
      venue, types,
      clients, (int) VenueStocks.ORDER_UNIT,
      actor.world()
    ) ;
  }
  

  public static Delivery nextDeliveryFrom(
    Inventory.Owner origin, Service types[],
    Batch <Venue> clients, int orderLimit,
    World world
  ) {
    final Venue VO ;
    if (origin instanceof Venue) VO = (Venue) origin ;
    else VO = null ;
    //
    //  We iterate over every nearby venue, and see if they need our services-
    float maxUrgency = 0 ;
    Delivery picked = null ;
    for (Venue client : clients) {
      //
      //  First, we tally the total shortage of goods at this venue (which we
      //  can provide,) which determines the urgency of delivery.
      float totalShortage = 0 ;
      for (Service type : types) {
        if (origin.inventory().amountOf(type) < MIN_BULK) continue ;
        float shortage = client.stocks.shortageOf(type) ;
        if (shortage <= 0) continue ;
        totalShortage += shortage ;
      }
      if (totalShortage == 0) continue ;
      final float urgency = totalShortage / (1f + (
        Spacing.distance(client, origin) / World.DEFAULT_SECTOR_SIZE
      )) ;
      //
      //  Secondly, we assemble a batch of items within our load capacity, and
      //  in proportion to local shortages.
      final float batchLimit = Math.min(totalShortage, orderLimit) ;
      final Batch <Item> toDeliver = new Batch <Item> () ;
      for (Service type : types) {
        if (origin.inventory().amountOf(type) < MIN_BULK) continue ;
        final float shortage = client.stocks.shortageOf(type) ;
        if (shortage <= 0) continue ;
        float amount = shortage * batchLimit / totalShortage ;
        amount = 5 * (1 + (int) (amount / 5)) ;
        toDeliver.add(Item.withAmount(type, amount)) ;
      }
      //
      //  Initialise the order, and compare it with the others-
      final Delivery order = new Delivery(
        toDeliver.toArray(Item.class),
        origin, client
      ) ;
      if (! order.valid()) continue ;
      if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
    }
    return picked ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    if (stage == STAGE_INIT) {
      stage = STAGE_PICKUP ;
    }
    if (stage == STAGE_PICKUP) {
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
      if (driven != null) dropoff.setMoveTarget(driven) ;
      dropoff.setProperties(Action.CARRIES) ;
      return dropoff ;
    }
    if (stage == STAGE_RETURN) {
      final Action returns = new Action(
        actor, origin,
        this, "actionReturn",
        Action.REACH_DOWN, "Returning in vehicle"
      ) ;
      returns.setMoveTarget(driven) ;
      return returns ;
    }
    return null ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    
    
    if (driven != null) {
      I.say("Performing vehicle pickup...") ;
      if (driven.aboard() != origin) {
        I.say("VEHICLE UNAVAILABLE!") ;
        abortBehaviour() ;
      }
      ///actor.goAboard(driven, actor.world()) ;
      ///driven.pathing.updateTarget(destination) ;
      for (Item i : items) origin.inventory().transfer(i, driven) ;
      stage = STAGE_DROPOFF ;
      return true ;
    }
    
    
    boolean addSuspensor = true ;
    if (target == origin) {
      float sum = 0 ;
      for (Item i : items) {
        float TA = origin.inventory().transfer(i, actor) ;
        sum += TA ;
      }
      if (sum < 5) addSuspensor = false ;
    }
    if (passenger != null) addSuspensor = true ;
    if (addSuspensor) {
      final Suspensor suspensor = new Suspensor(actor, this) ;
      final Tile o = actor.origin() ;
      suspensor.enterWorldAt(o.x, o.y, o.world) ;
      this.suspensor = suspensor ;
    }
    
    if (target == passenger) {
      suspensor.passenger = passenger ;
    }
    stage = STAGE_DROPOFF ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Venue target) {
    if (stage != STAGE_DROPOFF) return false ;
    
    if (driven != null) {
      ///I.say("Performing vehicle dropoff...") ;
      driven.pilots = actor ;
      if (driven.aboard() == target) {
        I.say("Performing vehicle dropoff...") ;
        for (Item i : items) driven.cargo.transfer(i, target) ;
        stage = STAGE_RETURN ;
        return true ;
      }
      return false ;
    }
    
    ///I.say(actor+" Performing dropoff...") ;
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i, target) ;
    if (passenger != null) passenger.goAboard(target, target.world()) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionReturn(Actor actor, Venue target) {
    driven.pathing.updateTarget(target) ;
    if (driven.aboard() == target) {
      driven.pilots = null ;
      actor.goAboard(target, actor.world()) ;
      stage = STAGE_DONE ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    //
    //  TODO:  You need to vary the description here, depending on phase and
    //  type.
    
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








