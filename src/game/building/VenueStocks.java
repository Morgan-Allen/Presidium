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
    ORDER_UNIT    = 5,
    POTENTIAL_INC = 0.15f,
    SEARCH_RADIUS = 16,
    MAX_CHECKED   = 5 ;
  
  private static boolean verbose = false ;
  
  static class Demand {
    Service type ;
    float demandAmount, demandTier ;
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
      d.demandAmount = s.loadFloat() ;
      d.demandTier   = s.loadFloat() ;
      demands.put(d.type, d) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(specialOrders) ;
    s.saveInt(demands.size()) ;
    for (Demand d : demands.values()) {
      s.saveInt(d.type.typeID) ;
      s.saveFloat(d.demandAmount) ;
      s.saveFloat(d.demandTier  ) ;
    }
  }
  
  
  
  /**  Assigning and producing jobs-
    */
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
    */
  private Demand demandFor(Service t) {
    final Demand d = demands.get(t) ;
    if (d != null) return d ;
    Demand made = new Demand() ;
    made.type = t ;
    demands.put(t, made) ;
    return made ;
  }
  
  
  public void clearDemands() {
    for (Demand d : demands.values()) {
      d.demandAmount = 0 ;
      d.demandTier = 0 ;
    }
  }
  
  
  public void translateDemands(Conversion cons) {
    for (Item raw : cons.raw) {
      final Demand d = demandFor(raw.type) ;
      d.demandAmount = 0 ;
      d.demandTier = 0 ;
    }
    final float demand = shortageOf(cons.out.type) ;
    if (demand <= 0) return ;
    
    for (Item raw : cons.raw) {
      final float inc = (raw.amount * demand / cons.out.amount) + 1 ;
      demandFor(raw.type).demandAmount += inc ;
    }
    
    if (verbose && BaseUI.isPicked(venue)) for (Item raw : cons.raw) {
      final Demand d = demandFor(raw.type) ;
      I.say(
        "  "+raw.type+" demand: "+d.demandAmount+
        " tier: "+d.demandTier
      ) ;
    }
  }
  
  
  public float incDemand(Service type, float amount, float tier) {
    if (amount == 0) return -1 ;
    final Demand d = demandFor(type) ;
    final float oldAmount = d.demandAmount, inc = amount * POTENTIAL_INC ;
    d.demandAmount += inc ;
    d.demandTier = (d.demandTier * oldAmount) + (tier * inc) ;
    if (verbose && BaseUI.isPicked(venue)) I.say(
      "  "+type+" demand: "+amount
    ) ;
    return d.demandAmount ;
  }
  
  
  public float shortageOf(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 - amountOf(type) ;
    return d.demandAmount - amountOf(type) ;
  }
  
  
  public float shortageUrgency(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    final float amount = amountOf(type), shortage = d.demandAmount - amount ;
    if (shortage < 0) return 0 ;
    final float urgency = shortage / ((amount + 5) * (2 + d.demandTier)) ;
    return urgency ;
  }
  
  
  public void updateStocks(int numUpdates) {
    final Presences presences = venue.world().presences ;
    final Service services[] = venue.services() ;
    
    for (Demand d : demands.values()) {
      d.demandAmount *= (1 - POTENTIAL_INC) ;
      if (Visit.arrayIncludes(services, d.type)) continue ;
      
      final Batch <Venue> suppliers = new Batch <Venue> () ;
      for (int n = (int) MAX_CHECKED / 2 ; n-- > 0 ;) {
        final Venue supplies = presences.randomMatchNear(
          d.type, venue, SEARCH_RADIUS
        ) ;
        if (supplies == null) break ;
        if (supplies == venue || supplies.flaggedWith() != null) continue ;
        suppliers.add(supplies) ;
        supplies.flagWith(suppliers) ;
      }
      
      for (Object o : presences.matchesNear(d.type, venue, SEARCH_RADIUS)) {
        final Venue supplies = (Venue) o ;
        if (supplies == venue || supplies.flaggedWith() != null) continue ;
        suppliers.add(supplies) ;
        if (suppliers.size() >= MAX_CHECKED) break ;
      }
      
      for (Venue s : suppliers) s.flagWith(null) ;
      diffuseDemand(d.type, suppliers) ;
    }
  }
  
  
  public void diffuseDemand(Service type, Batch <Venue> suppliers) {
    final Demand d = demands.get(type) ;
    if (d == null) return ;
    
    final float
      shortage = shortageOf(type),
      urgency = shortageUrgency(type) ;
    if (verbose && BaseUI.isPicked(venue)) I.say(
      venue+" has shortage of "+shortage+
      " for "+type+" of urgency "+urgency
    ) ;
    final float
      ratings[] = new float[suppliers.size()],
      distances[] = new float[suppliers.size()] ;
    float sumRatings = 0 ;
    
    int i = 0 ; for (Venue supplies : suppliers) {
      final float SU = supplies.stocks.shortageUrgency(type) ;
      if (verbose && BaseUI.isPicked(venue)) I.say(
        "  Considering supplier: "+supplies+", urgency: "+SU
      ) ;
      if (SU >= urgency) { i++ ; continue ; }
      float rating = 10 / (1 + SU) ;
      distances[i] = Spacing.distance(supplies, venue) / SEARCH_RADIUS ;
      rating /= 1 + distances[i] ;
      ratings[i++] = rating ;
      sumRatings += rating ;
    }
    if (sumRatings == 0) return ;
    
    i = 0 ; for (Venue supplies : suppliers) {
      final float rating = ratings[i], distance = distances[i++] ;
      if (rating == 0) continue ;
      final float amount = shortage * rating * 2 / sumRatings ;
      supplies.stocks.incDemand(type, amount, d.demandTier + distance) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Batch <String> ordersDesc() {
    final Batch <String> desc = new Batch <String> () ;
    for (Demand demand : demands.values()) {
      final int needed = (int) Math.ceil(demand.demandAmount) ;
      final int amount = (int) Math.ceil(amountOf(demand.type)) ;
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







