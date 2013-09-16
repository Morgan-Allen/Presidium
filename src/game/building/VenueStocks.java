/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.sfx.TalkFX;
import src.user.* ;
import src.util.* ;



//
//  Implement budgeting effects, and paying for offworld trade.

//  *  Paying for offworld goods.
//  *  Selling goods offworld.
//  *  Actors paying for home purchases.
//  *  Venues and actors paying tax, or getting debts paid off.

//  *  Actors receiving a basic salary.  (Tax is after expenses.)  That will
//     have to be keyed off the venues too.

//
//  You may need a generalised system for raising prices based on the cost of
//  imported goods, so that you make some minimum degree of profit.


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
    
    private float amountInc, demandAmount, demandTier ;
    private float pricePaid ;
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
      d.amountInc    = s.loadFloat() ;
      d.demandAmount = s.loadFloat() ;
      d.demandTier   = s.loadFloat() ;
      d.pricePaid    = s.loadFloat() ;
      demands.put(d.type, d) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(specialOrders) ;
    s.saveInt(demands.size()) ;
    for (Demand d : demands.values()) {
      s.saveInt(d.type.typeID) ;
      s.saveFloat(d.amountInc   ) ;
      s.saveFloat(d.demandAmount) ;
      s.saveFloat(d.demandTier  ) ;
      s.saveFloat(d.pricePaid   ) ;
    }
  }
  
  
  public List <Manufacture> specialOrders() {
    return specialOrders ;
  }
  
  
  public boolean addItem(Item item) {
    final int oldAmount = (int) amountOf(item) ;
    if (super.addItem(item)) {
      final int inc = ((int) amountOf(item)) - oldAmount ;
      if (venue.inWorld() && inc != 0) {
        String phrase = inc >= 0 ? "+" : "-" ;
        phrase+=" "+inc+" "+item.type.name ;
        venue.chat.addPhrase(phrase, TalkFX.NOT_SPOKEN) ;
      }
      return true ;
    }
    return false ;
  }
  
  
  public void incCredits(int inc) {
    super.incCredits(inc) ;
    if (! venue.inWorld()) return ;
    String phrase = inc >= 0 ? "+" : "-" ;
    phrase+=" "+Math.abs(inc)+" credits" ;
    venue.chat.addPhrase(phrase, TalkFX.NOT_SPOKEN) ;
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
      if (amount <= 0) continue ;
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
  
  
  
  /**  Public accessor methods-
    */
  public float demandFor(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    return d.demandAmount ;
  }
  
  
  public float shortageOf(Service type) {
    return demandFor(type) - amountOf(type) ;
  }
  
  
  public float surplusOf(Service type) {
    return amountOf(type) - demandFor(type) ;
  }
  
  
  public float shortageUrgency(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    final float amount = amountOf(type), shortage = d.demandAmount - amount ;
    if (shortage < 0) return 0 ;
    final float urgency = shortage / ((amount + 5) * (2 + d.demandTier)) ;
    return urgency ;
  }
  
  
  public float priceFor(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return type.basePrice ;
    return d.pricePaid ;
  }
  
  
  
  /**  Utility methods for setting and propagating various types of demand-
    */
  private Demand demandRecord(Service t) {
    final Demand d = demands.get(t) ;
    if (d != null) return d ;
    Demand made = new Demand() ;
    made.type = t ;
    made.pricePaid = t.basePrice ;
    demands.put(t, made) ;
    return made ;
  }
  
  
  private void incDemand(Service type, float amount, float tier) {
    if (amount == 0) return ;
    final Demand d = demandRecord(type) ;
    final float oldAmount = d.demandAmount, inc = amount * POTENTIAL_INC ;
    d.amountInc += amount ;
    d.demandTier = (d.demandTier * oldAmount) + (tier * inc) ;
    /*
    final float oldAmount = d.demandAmount, inc = amount * POTENTIAL_INC ;
    d.demandAmount += inc ;
    d.demandTier = (d.demandTier * oldAmount) + (tier * inc) ;
    if (verbose && BaseUI.isPicked(venue)) I.say(
      "  "+type+" demand: "+amount
    ) ;
    return d.demandAmount ;
    //*/
  }
  
  
  private void incPrice(Service type, float toPrice) {
    final Demand d = demandRecord(type) ;
    d.pricePaid += (toPrice - type.basePrice) * POTENTIAL_INC ;
  }
  
  
  public void clearDemands() {
    for (Demand d : demands.values()) {
      d.demandAmount = 0 ;
      d.demandTier = 0 ;
    }
  }
  
  
  public void translateDemands(Conversion cons) {
    //
    //  Firstly, we check to see if the output good is in demand, and if so,
    //  reset demand for the raw materials-
    final float demand = shortageOf(cons.out.type) ;
    if (demand <= 0) return ;
    float priceBump = 1 ;
    for (Item raw : cons.raw) {
      final Demand d = demandRecord(raw.type) ;
      d.demandAmount = 0 ;
      d.demandTier = 0 ;
      priceBump += priceFor(raw.type) / raw.type.basePrice ;
    }
    //
    //  We adjust our prices to ensure we can make a profit, and adjust demand
    //  for the inputs to match demand for the outputs-
    final Demand o = demandRecord(cons.out.type) ;
    o.pricePaid = o.type.basePrice * priceBump / (1f + cons.raw.length) ;
    for (Item raw : cons.raw) {
      final float inc = (raw.amount * demand / cons.out.amount) + 1 ;
      demandRecord(raw.type).demandAmount += inc ;
    }
    //
    //  (If desired, report the aftermath-)
    if (verbose && BaseUI.isPicked(venue)) for (Item raw : cons.raw) {
      final Demand d = demandRecord(raw.type) ;
      I.say(
        "  "+raw.type+" demand: "+d.demandAmount+
        " tier: "+d.demandTier
      ) ;
    }
  }
  
  
  public void forceDemand(Service type, float amount) {
    if (amount < 0) amount = 0 ;
    final Demand d = demandRecord(type) ;
    d.demandAmount = amount ;
    d.demandTier = 0 ;
    if (verbose && BaseUI.isPicked(venue)) I.say(
      "  "+type+" demand: "+d.demandAmount
    ) ;
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
    float avgPriceBump = 0 ;
    i = 0 ;
    
    for (Venue supplies : suppliers) {
      final float rating = ratings[i], distance = distances[i++] ;
      if (rating == 0) continue ;
      final float
        weight = rating / sumRatings,
        shortBump = shortage * weight,
        priceBump = type.basePrice * distance / 10f ;
      supplies.stocks.incDemand(type, shortBump, d.demandTier + distance) ;
      avgPriceBump += (supplies.priceFor(type) + priceBump) * weight ;
    }
    
    incPrice(type, avgPriceBump) ;
  }
  
  
  
  /**  Calling regular updates-
    */
  protected void updateStocks(int numUpdates) {
    if (numUpdates % 10 == 0) diffuseExistingDemand() ;
  }
  
  
  protected void diffuseExistingDemand() {
    final Presences presences = venue.world().presences ;
    final Service services[] = venue.services() ;
    
    for (Demand d : demands.values()) {
      d.demandAmount += d.amountInc * POTENTIAL_INC ;
      d.amountInc = 0 ;
      d.demandAmount *= (1 - POTENTIAL_INC) ;
      d.pricePaid -= d.type.basePrice ;
      d.pricePaid *= (1 - POTENTIAL_INC) ;
      d.pricePaid += d.type.basePrice ;
      
      if (services != null && Visit.arrayIncludes(services, d.type)) continue ;
      
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
}














