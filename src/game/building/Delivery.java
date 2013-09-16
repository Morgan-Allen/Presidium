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
//  TODO:  You now need to ensure that actors factor price into the purchasing
//  decision when it comes to personal goods.  i.e, that it's the actor that
//  pays for it, not their home.


public class Delivery extends Plan implements BuildConstants {
  
  final public static int
    TYPE_SHOPPING  = 0,
    TYPE_DELIVERS  = 1,
    TYPE_STRETCHER = 2,
    TYPE_DRIVEN    = 3 ;
  
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
  private boolean isShopping() {
    return
      (destination instanceof Venue) &&
      ((Venue) destination).privateProperty() ;
  }
  
  
  private Batch <Item> available() {
    final Batch <Item> available = new Batch <Item> () ;
    for (Item i : items) {
      if ((origin.inventory().amountOf(i) <= 0) && (! actor.gear.hasItem(i))) {
        continue ;
      }
      available.add(i) ;
    }
    return available ;
  }
  
  
  public float priorityFor(Actor actor) {
    final float rangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(actor, destination) +
      Plan.rangePenalty(origin, destination)
    ) / (driven == null ? 2f : 10f) ;
    
    if (isShopping()) {
      final Batch <Item> available = available() ;
      int price = 0 ;
      for (Item i : available) {
        float TP = origin.priceFor(i.type) + destination.priceFor(i.type) ;
        price += TP * i.amount / 2f ;
      }
      if (price > actor.gear.credits()) return 0 ;
      float costVal = actor.AI.greedFor(price) * CASUAL ;
      return ROUTINE + priorityMod - (costVal + rangePenalty) ;
    }
    
    return ROUTINE + priorityMod - rangePenalty ;
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
      if (BaseUI.isPicked(actor)) I.say("Returning dropoff action") ;
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
  
  
  private float transferGoods(Owner a, Owner b) {
    float sum = 0 ;
    float totalPrice = 0 ;
    for (Item i : items) {
      float TP = origin.priceFor(i.type) + destination.priceFor(i.type) ;
      final float TA = a.inventory().transfer(i, b) ;
      totalPrice += TP * TA / 2f ;
      sum += TA ;
    }
    ///I.say("Total price: "+totalPrice) ;
    origin.inventory().incCredits(totalPrice) ;
    if (isShopping()) actor.gear.incCredits(0 - totalPrice) ;
    else destination.inventory().incCredits(0 - totalPrice) ;
    return sum ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP || target != origin) return false ;
    //
    //  Vehicles get special treatment-
    if (driven != null) {
      I.say("Performing vehicle pickup...") ;
      if (driven.aboard() != origin) {
        I.say("VEHICLE UNAVAILABLE!") ;
        abortBehaviour() ;
      }
      transferGoods(origin, driven) ;
      stage = STAGE_DROPOFF ;
      return true ;
    }
    //
    //  Perform the actual transfer of goods, make the payment required, and
    //  see if a suspensor is needed-
    final float sum = transferGoods(origin, actor) ;
    final boolean bulky = sum >= 5 || passenger != null ;
    //
    //  Passengers always require a suspensor.
    if (bulky) {
      final Suspensor suspensor = new Suspensor(actor, this) ;
      final Tile o = actor.origin() ;
      suspensor.enterWorldAt(o.x, o.y, o.world) ;
      this.suspensor = suspensor ;
    }
    if (target == passenger) suspensor.passenger = passenger ;
    stage = STAGE_DROPOFF ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Owner target) {
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
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i, target) ;
    if (passenger != null) {
      passenger.goAboard((Boardable) target, actor.world()) ;
    }
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
    final Batch <Item> available = available() ;
    d.appendList("", available) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
  

  
  
  
  /**  Utility methods for generating deliveries to/from venues-
    */
  public static Venue findBestVendor(Venue origin, Item items[]) {
    if (items == null || items.length == 0) return null ;
    //
    //  TODO:  Base this off the list of venues the actor is aware of.
    //  ...Which should, in turn, be updated gradually over time.
    final World world = origin.world() ;
    final int maxTried = 3 ;
    Venue best = null ;
    float bestRating = 0 ;
    
    for (Item item : items) {
      if (item.type.form != FORM_COMMODITY || item.amount < 1) continue ;
      int numTried = 0 ;
      for (Object t : world.presences.matchesNear(item.type, origin, null)) {
        final Venue v = (Venue) t ;
        if (v == origin) continue ;
        if (++numTried > maxTried) break ;
        
        float rating = 0 ;
        for (Service s : v.services()) for (Item i : items) {
          if (
            v.stocks.shortageUrgency(i.type) >=
            origin.stocks.shortageUrgency(i.type)
          ) continue ;
          if (s == i.type) rating += v.stocks.amountOf(i) ;
        }
        final float dist = Spacing.distance(origin, v) ;
        rating /= 1 + (dist / World.DEFAULT_SECTOR_SIZE) ;
        ///I.say("Rating for "+v+" was: "+rating) ;
        if (rating > bestRating) { best = v ; bestRating = rating ; }
      }
    }
    ///I.say("Returning "+best) ;
    return best ;
  }
  
  
  public static Delivery selectExports(
    Batch <Venue> traders, Dropship origin, int orderLimit
  ) {
    //
    //  TODO:  SELECT GOODS WHICH ARE MOST VALUABLE TO THE HOMEWORLD OF THE
    //  SHIP IN QUESTION
    Delivery picked = null ;
    float maxRating = 0 ;
    
    for (Venue venue : traders) {
      //
      //  TODO:  IMPLEMENT ORDER LIMITS- ROUNDED OFF TO NEAREST UNIT OF 5
      final Service.Trade trade = (Service.Trade) venue ;
      float rating = 0 ;
      Batch <Item> items = new Batch <Item> () ;
      for (Service good : ALL_CARRIED_ITEMS) {
        final float surplus = trade.exportSurplus(good) ;
        if (surplus <= 0) continue ;
        rating += surplus * origin.priceFor(good) ;
        items.add(Item.withAmount(good, surplus)) ;
      }
      
      if (rating > maxRating) {
        maxRating = rating ;
        picked = new Delivery(items.toArray(Item.class), venue, origin) ;
      }
    }
    
    return picked ;
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
      actor.world(), false
    ) ;
  }
  

  public static Delivery nextDeliveryFrom(
    Inventory.Owner origin, Service types[],
    Batch <Venue> clients, int orderLimit,
    World world, boolean offworld
  ) {
    final Venue VO ;
    if (origin instanceof Venue) VO = (Venue) origin ;
    else VO = null ;
    //
    //  We iterate over every nearby venue, and see if they need our services-
    float maxUrgency = 0 ;
    Delivery picked = null ;
    for (Venue client : clients) {
      if (client == origin) continue ;
      //
      //  First, we tally the total shortage of goods at this venue (which we
      //  can provide,) which determines the urgency of delivery.
      float totalShortage = 0 ;
      final Batch <Item> shortages = new Batch <Item> () ;
      
      for (Service type : types) {
        if (origin.inventory().amountOf(type) < MIN_BULK) continue ;
        if (
          VO != null && VO.stocks.shortageUrgency(type) >=
          client.stocks.shortageUrgency(type)
        ) continue ;
        
        final float shortage = offworld ?
          ((Service.Trade) client).importShortage(type) :
          client.stocks.shortageOf(type) ;
          
        if (shortage > 0) {
          totalShortage += shortage ;
          shortages.add(Item.withAmount(type, shortage)) ;
        }
      }
      
      if (totalShortage < 5) continue ;
      final float urgency = totalShortage / (1f + (
        Spacing.distance(client, origin) / World.DEFAULT_SECTOR_SIZE
      )) ;
      //
      //  Initialise the order, and compare it with the others-
      final Delivery order = new Delivery(
        compressOrder(shortages, orderLimit),
        origin, client
      ) ;
      if (! order.valid()) continue ;
      if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
    }
    return picked ;
  }
  

  public static Item[] compressOrder(Batch <Item> order, int sizeLimit) {
    return compressOrder(order.toArray(Item.class), sizeLimit) ;
  }
  
  
  public static Item[] compressOrder(Item order[], int sizeLimit) {
    float sumAmounts = 0 ;
    for (Item i : order) sumAmounts += i.amount ;
    if (sumAmounts <= sizeLimit) return order ;
    for (int i = order.length ; i-- > 0 ;) {
      final Item o = order[i] ;
      order[i] = Item.withAmount(o, o.amount * sizeLimit / sumAmounts) ;
    }
    return order ;
  }
}








