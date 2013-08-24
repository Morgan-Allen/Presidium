/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;



public class VenueStocks extends Inventory implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static float
    ORDER_UNIT = 5,
    POTENTIAL_INC = 0.15f,
    SEARCH_RADIUS = 32,
    MAX_CHECKED = 4 ;
  
  
  static class Demand {
    Service type ;
    float required, received, balance ;
  }
  
  
  final Venue venue ;
  final Table <Service, Demand> demands = new Table <Service, Demand> () ;
  final List <Manufacture> specialOrders = new List <Manufacture> () ;
  
  
  VenueStocks(Venue v) {
    super(v) ;
    this.venue = v ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    s.loadObjects(specialOrders) ;
    int numC = s.loadInt() ;
    while (numC-- > 0) {
      final Demand d = new Demand() ;
      d.type = BuildConstants.ALL_ITEM_TYPES[s.loadInt()] ;
      d.required = s.loadFloat() ;
      d.received = s.loadFloat() ;
      d.balance  = s.loadFloat() ;
      demands.put(d.type, d) ;
    }
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(specialOrders) ;
    s.saveInt(demands.size()) ;
    for (Demand d : demands.values()) {
      s.saveInt(d.type.typeID) ;
      s.saveFloat(d.required) ;
      s.saveFloat(d.received) ;
      s.saveFloat(d.balance ) ;
    }
  }
  
  
  
  /**  Assigning and producing jobs-
    */
  public void addSpecialOrder(Manufacture newOrder) {
    specialOrders.add(newOrder) ;
  }
  
  
  public Batch <Item> shortages(boolean requiredOnly) {
    final Batch <Item> batch = new Batch <Item> () ;
    for (Demand d : demands.values()) {
      float amount = requiredShortage(d.type) ;
      if (! requiredOnly) amount += receivedShortage(d.type) ;
      batch.add(Item.withAmount(d.type, amount)) ;
    }
    return batch ;
  }
  
  
  public Manufacture nextSpecialOrder(Actor actor) {
    for (Manufacture order : specialOrders) {
      if (order.actor() == null && ! order.complete()) {
        return order ;
      }
    }
    return null ;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = receivedShortage(c.out.type) ;
    if (shortage <= 0) return null ;
    return new Manufacture(
      actor, venue, c, Item.withAmount(c.out, ORDER_UNIT)
    ) ;
  }
  
  
  public Delivery nextDelivery(Actor actor, Service types[]) {
    //
    //  If an actor has already been assigned to this task, then don't assign
    //  anyone else.
    //
    //  TODO:  (Alternatively, iterate over all behaviours aimed at this venue,
    //  and the subtract the total of items reserved by other deliveries, to
    //  ensure there'll still be enough.)
    for (Actor works : venue.personnel.workers) {
      if (works.AI.rootBehaviour() instanceof Delivery) {
        final Delivery d = (Delivery) works.AI.rootBehaviour() ;
        if (d.origin == venue) return null ;
      }
    }
    //
    //  Otherwise we iterate over every nearby venue, and see if they need what
    //  we're selling, so to speak.
    float maxUrgency = 0 ;
    Delivery picked = null ;
    final Presences presences = venue.world().presences ;
    for (Object o : presences.matchesNear(venue.base(), venue, SEARCH_RADIUS)) {
      final Venue client = (Venue) o ;
      final float distFactor = (SEARCH_RADIUS + Spacing.distance(
        venue, client
      )) / SEARCH_RADIUS ;
      //
      //  If we don't have enough of a given item to sell, we just pass over
      //  that item type.  Conversely, if a venue has no shortage, it is
      //  ignored.
      for (Service type : types) {
        if (venue.stocks.amountOf(type) < ORDER_UNIT) continue ;
        float urgency = client.stocks.requiredShortage(type) ;
        if (urgency <= 0) continue ;
        final Delivery order = new Delivery(
          Item.withAmount(type, ORDER_UNIT), venue, client
        ) ;
        urgency /= distFactor ;
        if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
      }
    }
    return picked ;
  }
  
  
  
  /**  Internal and external updates-
    */
  Demand demandFor(Service t) {
    final Demand d = demands.get(t) ;
    if (d != null) return d ;
    Demand made = new Demand() ;
    made.type = t ;
    demands.put(t, made) ;
    return made ;
  }
  
  
  public void setRequired(Service type, float amount) {
    demandFor(type).required = amount ;
  }
  
  
  public void incRequired(Service type, float amount) {
    demandFor(type).required += amount ;
  }
  
  
  public void receiveDemand(Service type, float amount) {
    demandFor(type).received += amount * POTENTIAL_INC ;
  }
  
  
  public float receivedShortage(Service type) {
    return demandFor(type).received - venue.stocks.amountOf(type) ;
  }
  
  
  public float requiredShortage(Service type) {
    return demandFor(type).required - venue.stocks.amountOf(type) ;
  }
  
  

  /**  Updates and maintenance.
    */
  public void clearDemands() {
    for (Demand d : demands.values()) {
      d.required = 0 ;
      d.received = 0 ;
    }
  }
  
  
  public void translateDemands(Conversion... cons) {
    for (Conversion c : cons) {
      for (Item i : c.raw) demandFor(i.type).required = 0 ;
    }
    for (Conversion c : cons) {
      final float demand = receivedShortage(c.out.type) / c.out.amount ;
      for (Item raw : c.raw) {
        demandFor(raw.type).required += (raw.amount * demand) + 1 ;
      }
    }
  }
  
  
  public void updateStocks(int numUpdates) {
    if (numUpdates % 10 != 0) return ;
    //
    //  Clear out any special orders that are complete-
    for (Manufacture order : specialOrders) {
      if (order.complete() || ! order.valid()) specialOrders.remove(order) ;
    }
    //
    //  Find a random nearby supplier of this item type, and bump up demand
    //  received there.
    final Presences presences = venue.world().presences ;
    for (Demand d : demands.values()) {
      //
      //  TODO:  Search a little more thoroughly, and favour suppliers that are
      //  closer, less busy and/or have larger existing stocks.
      final Venue supplies = presences.randomMatchNear(
        d.type, venue, SEARCH_RADIUS
      ) ;
      if (supplies == null) continue ;
      supplies.stocks.receiveDemand(d.type, d.required) ;
      d.received *= 1 - POTENTIAL_INC ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Batch <String> ordersDesc() {
    final Batch <String> desc = new Batch <String> () ;
    for (Demand demand : demands.values()) {
      final int needed = (int) Math.max(demand.received, demand.required) ;
      final int amount = (int) amountOf(demand.type) ;
      if (needed == 0 && amount == 0) continue ;
      desc.add(demand.type.name+" ("+amount+"/"+needed+")") ;
    }
    return desc ;
  }
  
  
  protected List <Manufacture> specialOrders() {
    return specialOrders ;
  }
}






/*
private void checkBalance() {
  for (Demand d : demands.values()) {
    d.balance = venue.stocks.amountOf(d.type) ;
  }
  for (Plan p : orders) if (p instanceof Delivery) {
    final Delivery order = (Delivery) p ;
    final Actor a = order.actor() ;
    if (a == null) continue ;
    for (Item i : order.items) {
      final Demand d = demandFor(i.type) ;
      d.balance += a.inventory().amountOf(i) ;
      d.balance -= i.amount ;
    }
  }
}


public boolean canMeetOrder(Delivery d) {
  checkBalance() ;
  boolean included = orders.contains(d) ;
  for (Item i : d.items) {
    if (demandFor(i.type).balance < (included ? 0 : i.amount)) return false ;
  }
  return true ;
}
//*/

//
//  Remove any orders which have expired.
/*
final World world = venue.world() ;
for (Plan order : orders) {
  if (! world.activities.includes(order)) orders.remove(order) ;
}
//*/



/*
public Batch <Item> shortages(boolean requiredOnly) {
  final Batch <Item> batch = new Batch <Item> () ;
  for (Demand d : demands.values()) {
    float amount = requiredShortage(d.type) ;
    if (! requiredOnly) amount += receivedShortage(d.type) ;
    final Item i = new Item(d.type, amount) ;
    batch.add(i) ;
  }
  return batch ;
}
//*/


/*
public void writeInformation(Description d) {
  if (demands.size() == 0) {
    d.append("\n\n  No current demands.") ;
    return ;
  }
  for (Demand demand : demands.values()) {
    d.append("\n  Demand for: "+demand.type.name) ;
    d.append("\n    ("+(int) demand.required+" required)") ;
    d.append("\n    ("+(int) demand.received+" received)") ;
    d.append("\n    ("+(int) demand.balance +" balance)" ) ;
  }
}
//*/