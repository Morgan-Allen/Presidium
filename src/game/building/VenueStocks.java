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


//
//  TODO:  Merge this with the stocks object.

public class VenueStocks extends Inventory {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static float
    ORDER_UNIT = 5,
    POTENTIAL_INC = 0.15f,
    SEARCH_RADIUS = 32,
    MAX_CHECKED = 4 ;
  
  
  final Venue venue ;
  Table <Item.Type, Demand> demands = new Table() ;
  List <Plan> orders = new List <Plan> () ;
  
  
  static class Demand {
    Item.Type type ;
    float required, received, balance ;
  }
  
  
  VenueStocks(Venue v) {
    super(v) ;
    this.venue = v ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    s.loadObjects(orders) ;
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
    s.saveObjects(orders) ;
    s.saveInt(demands.size()) ;
    for (Demand d : demands.values()) {
      s.saveInt(d.type.typeID) ;
      s.saveFloat(d.required) ;
      s.saveFloat(d.received) ;
      s.saveFloat(d.balance ) ;
    }
  }
  
  
  
  
  /**  Internal and external updates-
    */
  Demand demandFor(Item.Type t) {
    final Demand d = demands.get(t) ;
    if (d != null) return d ;
    Demand made = new Demand() ;
    made.type = t ;
    demands.put(t, made) ;
    return made ;
  }
  
  
  public void setRequired(Item.Type type, float amount) {
    demandFor(type).required = amount ;
  }
  
  
  public void incRequired(Item.Type type, float amount) {
    demandFor(type).required += amount ;
  }
  
  
  public void receiveDemand(Item.Type type, float amount) {
    demandFor(type).received += amount * POTENTIAL_INC ;
  }
  
  
  public float receivedShortage(Item.Type type) {
    return demandFor(type).received - venue.stocks.amountOf(type) ;
  }
  
  
  public float requiredShortage(Item.Type type) {
    return demandFor(type).required - venue.stocks.amountOf(type) ;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion... cons) {
    Manufacture picked = null ;
    float maxUrgency = 0 ;
    for (Conversion c : cons) {
      final Manufacture m = new Manufacture(actor, venue, c) ;
      if (! m.valid()) continue ;
      float urgency = 0 ;
      for (Item i : c.out) urgency += receivedShortage(i.type) ;
      if (urgency > maxUrgency) { picked = m ; maxUrgency = urgency ; }
    }
    ///I.say("Urgency was: "+maxUrgency) ;
    if (picked != null) orders.add(picked) ;
    return picked ;
  }
  
  
  public Delivery nextDelivery(Actor actor, Item.Type types[]) {
    float maxUrgency = 0 ;
    Delivery picked = null ;
    final Presences presences = venue.base().world.presences ;
    //
    //  We iterate over every nearby venue, and see if they need what we're
    //  selling, so to speak.
    for (Object o : presences.matchesNear(venue.base(), venue, SEARCH_RADIUS)) {
      final Venue client = (Venue) o ;
      final float distFactor = (SEARCH_RADIUS + Spacing.distance(
        venue, client
      )) / SEARCH_RADIUS ;
      //
      //  If we don't have enough of a given item to sell, we just pass over
      //  that item type.  Conversely, if a venue has no shortage, it is
      //  ignored.
      for (Item.Type type : types) {
        if (venue.stocks.amountOf(type) < ORDER_UNIT) continue ;
        float urgency = client.stocks.requiredShortage(type) ;
        if (urgency <= 0) continue ;
        final Delivery order = new Delivery(
          new Item(type, ORDER_UNIT), venue, client
        ) ;
        if (! canMeetOrder(order)) continue ;
        urgency /= distFactor ;
        if (urgency > maxUrgency) { picked = order ; maxUrgency = urgency ; }
      }
    }
    if (picked != null) orders.add(picked) ;
    return picked ;
  }
  
  
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
      for (Item out : c.out) {
        final float demand = receivedShortage(out.type) / out.amount ;
        for (Item raw : c.raw) {
          demandFor(raw.type).required += (raw.amount * demand) + 1 ;
        }
      }
    }
  }
  
  
  public void updateOrders() {
    //for (Demand d : demands.values()) d.reserved = 0 ;
    final Presences presences = venue.base().world.presences ;
    //
    //  Remove any orders which have expired, and calculate what goods are
    //  being held in reserve.
    final World world = venue.world() ;
    for (Plan order : orders) {
      if (! world.activities.includes(order)) {
        orders.remove(order) ;
      }
      /*
      if (order instanceof Delivery) {
        final Delivery d = (Delivery) order ;
        if (d.stage() > Delivery.STAGE_PICKUP) continue ;
        for (Item i : d.items) demandFor(i.type).reserved += i.amount ;
      }
      //*/
    }
    //
    //  Find a random nearby supplier of this item type, and bump up demand
    //  received there.
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
      String s = demand.type.name ;
      s+=" ("+(int) amountOf(demand.type)+"/"+(int) demand.received+")" ;
      desc.add(s) ;
    }
    return desc ;
  }
  
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
}








