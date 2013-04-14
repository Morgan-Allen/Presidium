/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.util.* ;



//  TODO:  Make this entirely separate from stocks?  ...You might have to.
public class VenueMaterial extends Inventory {
  
  
  /**  Fields, definitions and save/load methods-
    */
  final Venue venue ;
  
  int integrity ;
  Item materials[] ;
  
  
  
  VenueMaterial(Venue venue) {
    super(venue) ;
    this.venue = venue ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**
    */
}






/*
  
  
  static class Channel {
    Item.Type type ;
    int currentDemand ;
    float supplyPotential ;
    
    List <Delivery> ordersPlaced = new List(), ordersMet = new List() ;
  }
  
  
  Channel channelFor(Item.Type t) {
    for (Channel c : channels) if (c.type == t) return c ;
    Channel made = new Channel() ;
    made.type = t ;
    channels.addLast(made) ;
    return made ;
  }
  
  
  public float priceFor(Item.Type t) {
    return t.basePrice ;
  }
  
  
  public List <Delivery> ordersPlaced(Item.Type t) {
    return channelFor(t).ordersPlaced ;
  }

  public List <Delivery> ordersMet(Item.Type t) {
    return channelFor(t).ordersMet ;
  }
  

  void placeOrder(Venue demands, Venue supplies, Item.Type type) {
    I.say("  Placing order of "+type+" at "+venue+" for "+demands) ;
    
    final Delivery order = new Delivery(
      new Item(type, ORDER_UNIT), supplies, demands
    ) ;
    /*
    final Order order = new Order() ;
    order.client = demands ;
    order.fills = supplies ;
    order.ordered = new Item(type, ORDER_UNIT) ;
    //*/
/*
    final Channel
      demand = demands .stocks.channelFor(type),
      supply = supplies.stocks.channelFor(type) ;
    
    demand.ordersPlaced.addLast(order) ;
    supply.ordersMet.addLast(order) ;
    
    I.say("  Orders placed: "+ordersPlaced(type).size()) ;
  }
  
  
  void deleteOrder(Delivery order) {
    final Venue demands = (Venue) order.destination, supplies = order.origin ;
    final Channel
      demand = demands .stocks.channelFor(order.item.type),
      supply = supplies.stocks.channelFor(order.item.type) ;
    demand.ordersPlaced.remove(order) ;
    supply.ordersMet.remove(order) ;
    //if (order.activity != null) order.activity.abortStep() ;
  }
  
  
  
  /**  Updating and querying demands made at this venue-
  public void updateDemand(Item.Type type, int amount) {
    channelFor(type).currentDemand = amount ;
  }
  
  
  public void raiseDemand(Item.Type type, int amount) {
    final Channel channel = channelFor(type) ;
    if (channel.currentDemand < amount) channel.currentDemand = amount ;
  }
  
  
  public float queryDemand(Item.Type type) {
    final Channel channel = channelFor(type) ;
    return channel.currentDemand ;
  }
  
  
  public float queryShortage(Item.Type type) {
    return querySupplyLevel(type) - amountOf(type) ;
  }
  
  
  public float querySupplyLevel(Item.Type type) {
    final Channel channel = channelFor(type) ;
    return Math.max(
      channel.supplyPotential,
      channel.ordersMet.size() * ORDER_UNIT
    ) ;
  }
  
  
  
  /**  Convenience methods for the sake of getting actor behaviours-
  public void updateDemands(Conversion cons[]) {
    for (Conversion c : cons) {
      for (Item i : c.raw) updateDemand(i.type, 0) ;
      for (Item i : c.out) raiseDemand(i.type, 5) ;
    }
    for (Conversion c : cons) {
      for (Item out : c.out) {
        final float demand = queryShortage(out.type) / out.amount ;
        for (Item raw : c.raw) {
          raiseDemand(raw.type, (int) (raw.amount * demand) + 5) ;
        }
      }
    }
  }
  
  public Manufacture nextManufacture(Actor actor, Conversion cons[]) {
    Manufacture picked = null ;
    float maxUrgency = Float.NEGATIVE_INFINITY ;
    for (Conversion c : cons) {
      final Manufacture m = new Manufacture(actor, venue, c) ;
      if (m.viable()) return m ;
      float urgency = 0 ;
      for (Item i : c.out) urgency += queryShortage(i.type) ;
      if (urgency > maxUrgency) { picked = m ; maxUrgency = urgency ; }
    }
    return picked ;
  }
  
  
  public Delivery nextDeliveryMet(Actor actor) {
    return nextDelivery(actor, true) ;
  }

  public Delivery nextDeliveryPlaced(Actor actor) {
    return nextDelivery(actor, false) ;
  }
  
  
  private Delivery nextDelivery(Actor actor, boolean met) {
    //
    //  Returns the next unassigned delivery for this venue.
    //  TODO:  Rank in order of urgency?
    
    final World world = venue.world() ;
    
    for (Item.Type type : venue.itemsMade()) {
      for (Delivery order : (met ? ordersMet(type) : ordersPlaced(type))) {
      }
    }
    return null ;
  }
  
  
  /**  Updating the placement of supply and demand-
  void updateStocks() {
    for (Channel channel : channels) {
      //
      //  Clean out any completed orders.
      for (Delivery o : channel.ordersPlaced) if (! o.viable()) deleteOrder(o) ;
      for (Delivery o : channel.ordersMet) if (! o.viable()) deleteOrder(o) ;
      if (
        channel.ordersMet.size() == 0 &&
        channel.ordersPlaced.size() == 0 &&
        channel.currentDemand == 0 &&
        channel.supplyPotential < (POTENTIAL_INC * 0.5f)
      ) {
        channels.remove(channel) ;
        continue ;
      }
      updateChannel(channel) ;
      channel.supplyPotential *= (1 - POTENTIAL_INC) ;
    }
  }
  
  
  void onWorldExit() {
    for (Channel channel : channels) {
      for (Delivery d : channel.ordersPlaced) deleteOrder(d) ;
      for (Delivery d : channel.ordersMet   ) deleteOrder(d) ;
    }
  }
  
  
  
  //  TODO:  You might want to simplify this, based on some brute-force
  //  iteration through all venues.
  void updateChannel(Channel channel) {
    final Item.Type type = channel.type ;
    final Base base = venue.belongs() ;
    final World world = venue.world() ;
    ///I.say("  Updating channel of: "+type+" for "+venue) ;
    //
    //  Firstly, identify the weakest current order.
    Delivery worst = null ;
    float worstRating = Float.POSITIVE_INFINITY ;
    for (Delivery d : channel.ordersPlaced) {
      final float rating = rateSupplier(
        venue, d.origin, type
      ) ;
      if (rating < worstRating) { worstRating = rating ; worst = d ; }
    }
    //
    //  And identify a new candidate supplier.
    final Venue newSupplier = (Venue) (Rand.yes() ?
      base.nearestService(type, venue, -1) :
      base.randomServiceNear(type, venue, SEARCH_RADIUS)) ;
    final float newRating ;
    if (newSupplier != null) {
      newRating = rateSupplier(venue, newSupplier, type) ;
    }
    else newRating = 0 ;
    //
    //  Finally, assess current demand vs. current/anticipated supply.  If
    //  supply is in excess of demand, remove an order.  If demand is in excess
    //  of supply, add an order.  And if the two roughly match, consider
    //  replacing the weakest order with the new supplier, if superior enough-
    boolean addOrder = false, subOrder = false ;
    final float amountExpected =
      owner.inventory().amountOf(type) +
      (channel.ordersPlaced.size() * ORDER_UNIT) ;
    
    if (amountExpected < channel.currentDemand)
      addOrder = true ;
    else if (amountExpected > channel.currentDemand + ORDER_UNIT)
      subOrder = true ;
    else if (worstRating * 2 < newRating)
      addOrder = subOrder = true ;
    
    if (addOrder && newSupplier != null && newRating > 0) {
      placeOrder(venue, newSupplier, type) ;
    }
    if (subOrder && worst != null) {
      deleteOrder(worst) ;
    }
  }
  
  
  private float rateSupplier(Venue demands, Venue supplies, Item.Type type) {
    if (supplies.inventory().amountOf(type) == 0) {
      final Channel c = supplies.stocks.channelFor(type) ;
      c.supplyPotential += 5 * POTENTIAL_INC ;
      return 0 ;
    }
    float rating = 10.0f ;
    rating /= SEARCH_RADIUS + Spacing.distance(
      demands, supplies
    ) ;
    rating /= (channelFor(type).ordersMet.size() + 2) * ORDER_UNIT ;
    rating /= type.basePrice / (type.basePrice + supplies.stocks.priceFor(type)) ;
    return rating ;
  }


//*/




