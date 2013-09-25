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
    UPDATE_PERIOD = 1,
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
  
  
  
  
  /**  Overrides of standard inventory methods-
    */
  public boolean addItem(Item item) {
    final int oldAmount = (int) amountOf(item) ;
    if (super.addItem(item)) {
      final int inc = ((int) amountOf(item)) - oldAmount ;
      if (venue.inWorld() && inc != 0 && item.type.form != FORM_PROVISION) {
        String phrase = inc >= 0 ? "+" : "-" ;
        phrase+=" "+inc+" "+item.type.name ;
        venue.chat.addPhrase(phrase) ;
      }
      return true ;
    }
    return false ;
  }
  
  
  public void incCredits(float inc) {
    if (Float.isNaN(inc)) I.complain("INC IS NOT-A-NUMBER!") ;
    if (Float.isNaN(credits)) credits = 0 ;
    if (inc == 0) return ;
    final int oldC = (int) credits() ;
    super.incCredits(inc) ;
    final int newC = (int) credits() ;
    if (! venue.inWorld() || oldC == newC) return ;
    String phrase = inc >= 0 ? "+" : "-" ;
    phrase+=" "+(int) Math.abs(inc)+" credits" ;
    venue.chat.addPhrase(phrase) ;
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
    final Choice choice = new Choice(actor) ;
    for (Manufacture order : specialOrders) choice.add(order) ;
    return (Manufacture) choice.weightedPick(0) ;
  }
  
  
  public Manufacture nextManufacture(Actor actor, Conversion c) {
    final float shortage = shortageOf(c.out.type) ;
    if (shortage <= 0) return null ;
    //
    //  TODO:  Manufactures need to update the amount required...
    return new Manufacture(
      actor, venue, c,
      Item.withAmount(c.out, shortage + 5)
    ) ;
  }
  
  
  
  /**  Public accessor methods-
    */
  public float demandFor(Service type) {
    final Demand d = demands.get(type) ;
    if (d == null) return 0 ;
    return d.demandAmount ;
  }
  
  
  public boolean hasEnough(Service type) {
    return amountOf(type) < (demandFor(type) / 2) ;
  }
  
  
  public float shortagePenalty(Service type) {
    final float shortage = shortageOf(type) - 0.5f ;
    if (shortage <= 0) return 0 ;
    return shortage * 2 ;
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
    /*
    if (type == METAL_ORE && I.talkAbout == venue) {
      I.say("ORE SHORTAGE: "+shortage) ;
    }
    //*/

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
  
  
  //
  //  TODO:  Merge with the method below?
  public void incDemand(Service type, float amount, int period) {
    if (amount == 0) return ;
    final Demand d = demandRecord(type) ;
    incDemand(type, amount, d.demandTier, period) ;
  }
  
  
  private void incDemand(Service type, float amount, float tier, int period) {
    if (amount == 0) return ;
    final Demand d = demandRecord(type) ;
    final float inc = POTENTIAL_INC * period ;
    if (inc >= 1) I.complain("DEMAND INCREMENT TOO HIGH") ;
    d.amountInc += amount * inc ;
    if (verbose && type == PARTS) I.sayAbout(
      venue, "Demand inc is: "+d.amountInc+" bump: "+amount+
      " for: "+type
    ) ;
    d.demandTier *= (1 - inc) ;
    d.demandTier += (tier * inc) ;
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
  
  
  public void translateDemands(Conversion cons, int period) {
    //
    //  Firstly, we check to see if the output good is in demand, and if so,
    //  reset demand for the raw materials-
    final float demand = shortageOf(cons.out.type) ;
    if (demand <= 0) return ;
    float priceBump = 1 ;
    //
    //  We adjust our prices to ensure we can make a profit, and adjust demand
    //  for the inputs to match demand for the outputs-
    final Demand o = demandRecord(cons.out.type) ;
    o.pricePaid = o.type.basePrice * priceBump / (1f + cons.raw.length) ;
    for (Item raw : cons.raw) {
      ///I.sayAbout(venue, "Needs "+raw) ;
      final float needed = raw.amount * demand / cons.out.amount ;
      this.incDemand(raw.type, needed, 0, period) ;
      //forceDemand(raw.type, , 0) ;
    }
  }
  
  
  public void forceDemand(Service type, float amount, float tier) {
    if (amount < 0) amount = 0 ;
    final Demand d = demandRecord(type) ;
    d.demandAmount = amount ;
    d.demandTier = tier ;
    d.amountInc = 0 ;
    if (verbose && BaseUI.isPicked(venue) && type == PARTS) { I.say(
      "  "+type+" demand forced to: "+d.demandAmount
    ) ; }
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
      if (SU >= urgency) { i++ ; continue ; }
      float rating = 10 / (1 + SU) ;
      distances[i] = Spacing.distance(supplies, venue) / SEARCH_RADIUS ;
      rating /= 1 + distances[i] ;
      rating *= (supplies.stocks.amountOf(type) + 5) / 10f ;
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

      if (verbose && BaseUI.isPicked(venue)) I.say(
        "  Considering supplier: "+supplies+", rating: "+rating+
        "\n  BUMP IS: "+shortBump
      ) ;
      supplies.stocks.incDemand(
        type, shortBump, d.demandTier + distance + 1, (int) UPDATE_PERIOD
      ) ;
      avgPriceBump += (supplies.priceFor(type) + priceBump) * weight ;
    }
    
    incPrice(type, avgPriceBump) ;
  }
  
  
  public void diffuseDemand(Service type) {
    final Batch <Venue> suppliers = Deliveries.nearbyVendors(
      type, venue, venue.world()
    ) ;
    diffuseDemand(type, suppliers) ;
  }
  
  
  
  /**  Calling regular updates-
    */
  protected void updateStocks(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0 ;
    if (Float.isNaN(taxed)) taxed = 0 ;
    if (numUpdates % UPDATE_PERIOD == 0) diffuseExistingDemand() ;
    for (Manufacture m : specialOrders) {
      if (m.finished()) specialOrders.remove(m) ;
    }
  }
  
  
  protected void diffuseExistingDemand() {
    final Presences presences = venue.world().presences ;
    final Service services[] = venue.services() ;
    
    for (Demand d : demands.values()) {
      d.demandAmount *= (1 - POTENTIAL_INC) ;
      d.demandAmount += d.amountInc ;
      d.amountInc = 0 ;
      d.pricePaid -= d.type.basePrice ;
      d.pricePaid *= (1 - POTENTIAL_INC) ;
      d.pricePaid += d.type.basePrice ;
      if (verbose && d.type == PARTS) I.sayAbout(
        venue, d.type+" demand is: "+d.demandAmount
      ) ;
      
      if (services != null && Visit.arrayIncludes(services, d.type)) {
        continue ;
      }
      diffuseDemand(d.type) ;
    }
  }
}














