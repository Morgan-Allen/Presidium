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
//  TODO:  Barges need to be made more persistent.


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
  
  private static boolean verbose = false ;
  
  
  final public Owner origin, destination ;
  final public Item items[] ;
  final Actor passenger ;
  
  private byte stage = STAGE_INIT ;
  private Suspensor suspensor ;
  public Vehicle driven ;
  
  
  
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
  
  
  public boolean matchesPlan(Plan plan) {
    if (! super.matchesPlan(plan)) return false ;
    final Delivery d = (Delivery) plan ;
    if (d.origin != origin || d.destination != destination) return false ;
    boolean overlap = false;
    for (Item i : items) {
      for (Item dI : d.items) if (i.type == dI.type) overlap = true ;
    }
    return overlap ;
  }
  
  
  
  /**  Assessing targets and priorities-
    */
  private boolean isShopping() {
    return
      (destination instanceof Venue) &&
      ((Venue) destination).privateProperty() ;
  }
  
  
  private float purchasePrice(Item i) {
    final float TP = origin.priceFor(i.type) + destination.priceFor(i.type) ;
    return i.amount * TP / 2f ;
  }
  
  
  private Batch <Item> available() {
    final Batch <Item> available = new Batch <Item> () ;
    final boolean shopping = isShopping() ;
    
    if (stage <= STAGE_PICKUP) {
      float sumPrice = 0 ;
      for (Item i : items) {
        final float amount = origin.inventory().amountOf(i) ;
        if (amount <= 0) continue ;
        if (shopping) {
          sumPrice += purchasePrice(i) ;
          if (sumPrice > actor.gear.credits() / 2f) break ;
        }
        available.add(i) ;
      }
    }
    else {
      for (Item i : items) {
        if (! actor.gear.hasItem(i)) {
          final float amount = actor.gear.amountOf(i) ;
          if (amount > 0) available.add(Item.withAmount(i, amount)) ;
          continue ;
        }
        else available.add(i) ;
      }
    }
    return available ;
  }
  
  
  public float priorityFor(Actor actor) {
    final Batch <Item> available = available() ;
    if (available.size() == 0) return 0 ;
    
    final float rangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(actor, destination) +
      Plan.rangePenalty(origin, destination)
    ) / (driven == null ? 2f : 10f) ;

    float costVal = 0 ;
    if (isShopping() && stage <= STAGE_PICKUP) {
      int price = 0 ;
      for (Item i : available) price += purchasePrice(i) ;
      if (price > actor.gear.credits()) return 0 ;
      costVal = actor.AI.greedFor(price) * CASUAL ;
    }
    return Visit.clamp(
      ROUTINE + priorityMod - (costVal + rangePenalty), 0, URGENT
    ) ;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (driven != null) {
      if (driven.aboard() != origin && ! driven.inside().contains(actor)) {
        return false ;
      }
    }
    if (available().size() == 0) return false ;
    return true ;
  }
  
  
  public boolean finished() {
    return stage == STAGE_DONE ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    if (verbose && BaseUI.isPicked(actor)) {
      ///I.say("Delivery stage: "+stage+" "+this.hashCode()) ;
    }
    if (stage == STAGE_INIT) {
      stage = STAGE_PICKUP ;
    }
    if (stage == STAGE_PICKUP) {
      final Action pickup = new Action(
        actor, (passenger == null) ? origin : passenger,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      ) ;
      ///if (verbose && BaseUI.isPicked(actor)) I.say("Returning pickup") ;
      return pickup ;
    }
    if (stage == STAGE_DROPOFF) {
      ///if (BaseUI.isPicked(actor)) I.say("Returning dropoff action") ;
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
    if (a == null || b == null) return 0 ;
    float sumItems = 0 ;
    float totalPrice = 0 ;
    for (Item i : available()) {
      final float TA = a.inventory().transfer(i, b) ;
      totalPrice += TA * purchasePrice(i) / i.amount ;
      sumItems += TA ;
    }
    origin.inventory().incCredits(totalPrice) ;
    if (isShopping()) actor.gear.incCredits(0 - totalPrice) ;
    else destination.inventory().incCredits(0 - totalPrice) ;
    return sumItems ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    //
    //  Vehicles get special treatment-
    if (driven != null) {
      ///I.say("Performing vehicle pickup...") ;
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
    if (verbose && BaseUI.isPicked(actor)) I.say("Performing pickup!") ;
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
      driven.pilot = actor ;
      if (driven.aboard() == target) {
        I.say("Performing vehicle dropoff...") ;
        for (Item i : items) driven.cargo.transfer(i.type, target) ;
        stage = STAGE_RETURN ;
        return true ;
      }
      return false ;
    }
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i.type, target) ;
    if (passenger != null) {
      passenger.goAboard((Boardable) target, actor.world()) ;
    }
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionReturn(Actor actor, Venue target) {
    driven.pathing.updateTarget(target) ;
    if (driven.aboard() == target) {
      driven.pilot = null ;
      actor.goAboard(target, actor.world()) ;
      stage = STAGE_DONE ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN) {
      d.append("Returning to ") ;
      d.append(origin) ;
      return ;
    }
    
    d.append("Delivering ") ;
    final Batch <Item> available = available() ;
    d.appendList("", available) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}








  
  
  /**  Utility methods for generating deliveries to/from venues-
    */
  //  TODO:  These methods need to be replaced.
  
  /*
  public static Venue findBestVendor(Venue origin, Item items[]) {
    if (items == null || items.length == 0) return null ;
    //
    //  TODO:  Base this off the list of venues the actor is aware of.
    //  ...Which should, in turn, be updated gradually over time.
    float sumItems = 0 ;
    for (Item item : items) sumItems += item.amount ;
    ///if (sumItems < 0.5f) return null ;
    
    final World world = origin.world() ;
    final int maxTried = 3 ;
    Venue best = null ;
    float bestRating = 0 ;
    Batch <Venue> tried = new Batch <Venue> () ;
    
    for (Item item : items) {
      if (item.type.form != FORM_COMMODITY) continue ;
      int numTried = 0 ;
      for (Object t : world.presences.matchesNear(item.type, origin, null)) {
        final Venue v = (Venue) t ;
        if (v == origin || v.flaggedWith() != null) continue ;
        v.flagWith(tried) ;
        tried.add(v) ;
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
        
        if (verbose && I.talkAbout == origin) {
          I.say("\nRating for "+v+" was: "+rating) ;
          I.say("  Origin urgency: "+origin.stocks.shortageUrgency(item.type)) ;
          I.say("  Target urgency: "+v.stocks.shortageUrgency(item.type)) ;
        }
        if (rating > bestRating) { best = v ; bestRating = rating ; }
      }
    }
    
    for (Venue v : tried) v.flagWith(null) ;
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
      for (Service good : ALL_COMMODITIES) {
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
    Inventory.Owner venue, Actor actor, Service types[], int sizeLimit
  ) {
    final Batch <Venue> clients = new Batch <Venue> () ;
    final Presences presences = actor.world().presences ;
    final float SEARCH_RADIUS = World.DEFAULT_SECTOR_SIZE ;// * 2 ;
    for (Object o : presences.matchesNear(
      actor.base(), venue, SEARCH_RADIUS
    )) clients.add((Venue) o) ;
    return nextDeliveryFrom(
      venue, types,
      clients, sizeLimit,
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
      if (client == origin || client.privateProperty()) continue ;
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
        compressOrder(shortages.toArray(Item.class), orderLimit),
        origin, client
      ) ;
      if (! order.valid()) continue ;
      if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
    }
    return picked ;
  }
  
  
  public static Item[] compressOrder(
    Item order[], int sizeLimit, Venue supplies
  ) {
    final Batch <Item> culled = new Batch <Item> () ;
    for (int i = order.length ; i-- > 0 ;) {
      final Item o = order[i] ;
      final float amount = supplies.stocks.amountOf(o) / 2f ;
      if (amount == 0) continue ;
      culled.add(Item.withAmount(o, Math.min(o.amount, amount))) ;
    }
    if (culled.size() == 0) return new Item[0] ;
    return compressOrder(culled.toArray(Item.class), sizeLimit) ;
  }
  
  
  private static Item[] compressOrder(Item order[], int sizeLimit) {
    //
    //  Firstly, we check whether compression is needed-
    float sumAmounts = 0 ;
    for (Item i : order) sumAmounts += i.amount ;
    final float scale = (sumAmounts > sizeLimit) ?
      (sizeLimit / sumAmounts) : 1 ;
    //
    //  Round up the quantities-
    final int roundUnit = sizeLimit <= 5 ? 1 : 5 ;
    final int amounts[] = new int[order.length] ;
    sumAmounts = 0 ;
    for (int i = order.length ; i-- > 0 ;) {
      final float amount = order[i].amount * scale ;
      final int rounded = roundUnit * (int) Math.ceil(amount / roundUnit) ;
      sumAmounts += amounts[i] = rounded ;
    }
    //
    //  Then trim off excess-
    trimLoop: while (true) {
      for (int i = order.length ; i-- > 0 ;) {
        if (sumAmounts <= sizeLimit) break trimLoop ;
        if (amounts[i] > 0) {
          amounts[i] -= roundUnit ;
          sumAmounts -= roundUnit ;
        }
      }
    }
    //
    //  Compile and return results-
    for (int i = order.length ; i-- > 0 ;) {
      order[i] = Item.withAmount(order[i], amounts[i]) ;
    }
    return order ;
  }
  

  
  
  public static Batch <Venue> nearbyDepots(Target t, World world) {
    final Batch <Venue> depots = new Batch <Venue> () ;
    //  TODO:  Key this off the SERVICE_DEPOT service instead?
    world.presences.sampleTargets(SupplyDepot.class, t, world, 5, depots) ;
    world.presences.sampleTargets(StockExchange.class, t, world, 5, depots) ;
    return depots ;
  }
  
  
  public static Batch <Venue> nearbyCustomers(Target target, World world) {
    final Batch <Venue> nearby = new Batch <Venue> () ;
    //  TODO:  Skip over any depots.
    world.presences.sampleTargets(Venue.class, target, world, 10, nearby) ;
    return nearby ;
  }
  
  
  public static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleTargets(Dropship.class, target, world, 10, nearby) ;
    return nearby ;
  }
  //*/
//}








