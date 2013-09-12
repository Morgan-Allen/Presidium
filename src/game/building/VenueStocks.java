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




/*
//  Where do people earn money from?  Presumably, the sale of goods and
//  services.  You tack something on top of average buying price to determine
//  the selling price, and split the difference between the number of workers
//  over a given time frame, plus taxes to the state.  That's fair, right?
//  Okay.  Cool.



public class VenueStocks extends Inventory implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
  final public static float
    ORDER_UNIT    = 5,
    POTENTIAL_INC = 0.15f,
    SEARCH_RADIUS = 16,
    MAX_CHECKED   = 5 ;
  
  private static boolean verbose = true ;
  
  
  static class Demand {
    Service type ;
    float required, received, balance ;
    //float buyPrice, sellPrice ;
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
  public void addSpecialOrder(Manufacture newOrder) {
    specialOrders.add(newOrder) ;
  }
  
  
  public Batch <Item> shortages() {
    final Batch <Item> batch = new Batch <Item> () ;
    for (Demand d : demands.values()) {
      float amount = shortageOf(d.type) ;
      //if (! requiredOnly) amount += receivedShortage(d.type) ;
      batch.add(Item.withAmount(d.type, amount)) ;
    }
    return batch ;
  }
  
  
  public Manufacture nextSpecialOrder(Actor actor) {
    for (Manufacture order : specialOrders) {
      //I.say("Actor assigned "+order.actor()) ;
      if (order.actor() != actor || order.complete()) continue ;
      return order ;
    }
    return null ;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = shortageOf(c.out.type) ;
    ///I.say(c.out.type+" shortage is: "+shortage) ;
    if (shortage <= 0) return null ;
    return new Manufacture(
      actor, venue, c,
      Item.withAmount(c.out, shortage + ORDER_UNIT)
    ) ;
  }
  
  
  
  /**  Internal and external updates-
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
  
  /*
  public float receivedShortage(Service type) {
    return demandFor(type).received - amountOf(type) ;
  }
  
  
  public float requiredShortage(Service type) {
    return demandFor(type).required - amountOf(type) ;
  }
  //*/
  
  /*
  public float shortageOf(Service type) {
    final Demand d = demandFor(type) ;
    return d.received + d.required - amountOf(type) ;
  }
  
  

  /**  Updates and maintenance.
  public void clearDemands() {
    for (Demand d : demands.values()) {
      d.required = 0 ;
      d.received = 0 ;
    }
  }
  
  
  public void translateDemands(Conversion cons) {
    for (Item i : cons.raw) demandFor(i.type).required = 0 ;
    final float demand = shortageOf(cons.out.type) ;
    if (demand <= 0) return ;
    
    for (Item raw : cons.raw) {
      final float inc = (raw.amount * demand / cons.out.amount) + 1 ;
      if (verbose && BaseUI.isPicked(venue)) I.say(
        "Shortage of "+raw.type+" is: "+inc
      ) ;
      demandFor(raw.type).required += inc ;
    }
  }
  
  
  public void updateStocks(int numUpdates) {
    //if (numUpdates % 10 != 0) return ;
    //
    //  Clear out any special orders that are complete-
    for (Manufacture order : specialOrders) {
      if (order.complete() || ! order.valid()) specialOrders.remove(order) ;
    }
    
    
    final Presences presences = venue.world().presences ;
    for (Demand d : demands.values()) {
      final float shortage = d.required - amountOf(d.type) ;
      if (shortage <= 0) continue ;
      if (verbose && BaseUI.isPicked(venue)) I.say(
        venue+" demand for "+d.type+
        " received/required: "+d.received+"/"+d.required
      ) ;
      
      final Batch <Venue> tried = new Batch <Venue> () ;
      for (int n = (int) MAX_CHECKED / 2 ; n-- > 0 ;) {
        final Venue supplies = presences.randomMatchNear(
          d.type, venue, SEARCH_RADIUS
        ) ;
        if (supplies == null) break ;
        if (supplies == venue || supplies.flaggedWith() != null) continue ;
        tried.add(supplies) ;
        supplies.flagWith(tried) ;
      }
      
      for (Object o : presences.matchesNear(d.type, venue, SEARCH_RADIUS)) {
        final Venue supplies = (Venue) o ;
        if (supplies == venue || supplies.flaggedWith() != null) continue ;
        tried.add(supplies) ;
        if (tried.size() >= MAX_CHECKED) break ;
      }
      
      for (Venue supplies : tried) {
        supplies.flagWith(null) ;
        final float stimulus = shortage * rateSupplier(supplies, d) ;
        
        if (verbose && BaseUI.isPicked(venue)) I.say(
          venue+" conferring stimulus for "+d.type+
          " of "+stimulus+" to "+supplies
        ) ;
        supplies.stocks.receiveDemand(d.type, stimulus / tried.size()) ;
      }
      d.received *= 1 - POTENTIAL_INC ;
    }
  }
  
  
  
  
  
  private float rateSupplier(Venue supplies, Demand d) {
    float rating = 2.0f ;
    
    final float hasAmount = supplies.inventory().amountOf(d.type) ;
    if (hasAmount < d.required) {
      rating *= hasAmount / d.required ;
      rating = 0.5f + (rating * 0.5f) ;
    }
    rating /= 1 + (Spacing.distance(supplies, venue) / SEARCH_RADIUS) ;

    //rating /= (channelFor(supplies, type).ordersMet.size() + 2) * ORDER_UNIT ;
    //rating /= type.basePrice / (type.basePrice + supplies.priceFor(type)) ;
    return rating ;
  }
  
  
  
  /**  Rendering and interface methods-
  public Batch <String> ordersDesc() {
    final Batch <String> desc = new Batch <String> () ;
    for (Demand demand : demands.values()) {
      final int needed = (int) Math.max(demand.received, demand.required) ;
      final int amount = (int) amountOf(demand.type) ;
      if (needed == 0 && amount == 0) continue ;
      desc.add(demand.type.name+" ("+amount+"/"+needed+")") ;
    }
    for (Item item : super.allItems()) {
      if (demands.get(item.type) != null) continue ;
      desc.add(item+" (not needed)") ;
    }
    return desc ;
  }
  
  
  public List <Manufacture> specialOrders() {
    return specialOrders ;
  }
}
//*/











