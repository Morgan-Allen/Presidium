/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.Inventory.Owner ;
import src.util.* ;


//
//  The problem here is that for sufficiently finely-balanced demands, the act
//  of transport will itself change the balance of demands made.



/**  This class implements a big bunch of helper methods to search for
  *  optimal delivery venues and amounts-
  */
public class Deliveries implements BuildConstants {
  
  
  final private static int
    IS_TRADE = 0,
    IS_IMPORT = 1,
    IS_EXPORT = 2 ;  
  
  private static boolean verbose = false ;
  
  
  public static Delivery nextDeliveryFrom(
    Owner origin, Service goods[], int sizeLimit, World world
  ) {
    return nextDeliveryFrom(
      origin, goods, nearbyCustomers(origin, world), sizeLimit, world
    ) ;
  }
  

  public static Delivery nextDeliveryFrom(
    Owner origin, Service goods[],
    Batch <Venue> clients, int sizeLimit,
    World world
  ) {
    final Owner client = bestClient(
      goods, origin, (Batch) clients, IS_TRADE
    ) ;
    if (verbose && I.talkAbout == origin) {
      I.say("\nCLIENTS ARE:") ;
      for (Venue v : clients) I.say("  "+v) ;
      I.say("BEST CLIENT IS: "+client+"\n") ;
    }
    if (client == null) return null ;
    final Item order[] = configDelivery(
      goods, origin, client, null, sizeLimit, world, IS_TRADE
    ) ;
    if (order.length == 0) return null ;
    return new Delivery(order, origin, client) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Owner client, Service goods[], int sizeLimit, Actor pays, World world
  ) {
    final Batch <Venue> vendors = Deliveries.nearbyVendors(
      goods, client, world
    ) ;
    /*
    if (verbose && I.talkAbout == pays) {
      for (Venue v : vendors) I.say("Rating is: "+Deliveries.rateTrading(
        goods, v, client, IS_TRADE
      )+" FOR "+v) ;
    }
    //*/
    return nextCollectionFor(client, goods, vendors, sizeLimit, pays, world) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, Actor pays, World world
  ) {
    final Inventory.Owner origin = Deliveries.bestOrigin(
      goods, client, (Batch) vendors, IS_TRADE
    ) ;
    if (origin == null) return null ;
    Item order[] = Deliveries.configDelivery(
      goods, origin, client, pays, sizeLimit, world, IS_TRADE
    ) ;
    if (order.length == 0) return null ;
    return new Delivery(order, origin, client) ;
  }
  
  
  public static Delivery nextImportDelivery(
    Owner origin, Service goods[], Batch <Venue> clients,
    int sizeLimit, World world
  ) {
    final Owner client = bestClient(
      goods, origin, (Batch) clients, IS_IMPORT
    ) ;
    if (client == null) return null ;
    final Item order[] = configDelivery(
      goods, origin, client, null, sizeLimit, world, IS_IMPORT
    ) ;
    if (order.length == 0) return null ;
    return new Delivery(order, origin, client) ;
  }
  
  
  public static Delivery nextExportCollection(
    Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, World world
  ) {
    final Owner origin = Deliveries.bestOrigin(
      goods, client, (Batch) vendors, IS_EXPORT
    ) ;
    if (origin == null) return null ;
    Item order[] = Deliveries.configDelivery(
      goods, origin, client, null, sizeLimit, world, IS_EXPORT
    ) ;
    if (order.length == 0) return null ;
    return new Delivery(order, origin, client) ;
  }
  
  

  /**  Helper methods for squeezing orders down into manageable chunks-
    */
  private static Item[] configDelivery(
    Service goods[], Owner origin, Owner client,
    Actor pays, int sizeLimit, World world,
    int tradeType
  ) {
    //
    //  First, get the amount of each item available for trade at the point of
    //  origin, and desired by the destination/client, which constrains the
    //  quantities involved-
    if (verbose) I.sayAbout(
      origin, "Evaluating delivery from "+origin+" to "+client
    ) ;
    final int
      roundUnit = sizeLimit <= 5 ? 1 : 5,
      pickUnit  = sizeLimit <= 5 ? 0 : 3 ;
    final boolean ration =
      pays != null &&
      pays.vocation().guild == Background.GUILD_MILITANT ;
    
    
    final List <Item> viable = new List <Item> () {
      protected float queuePriority(Item i) {
        return (1f + i.type.basePrice) / (i.amount + 1) ;
      }
    } ;
    final Batch <Behaviour>
      OD = world.activities.targeting(origin),
      CD = world.activities.targeting(client) ;
    float maxSold, maxBuys ;
    //
    //  In the process, we deduct the sum of goods already due to be delivered/
    //  taken away.
    for (Service good : goods) if (good.form == FORM_COMMODITY) {
      if (tradeType == IS_IMPORT) {
        maxBuys = ((Service.Trade) client).importShortage(good) ;
        maxSold = maxBuys ;
      }
      else if (tradeType == IS_EXPORT) {
        maxSold = ((Service.Trade) origin).exportSurplus(good) ;
        maxBuys = maxSold ;
      }
      else {
        maxSold = origin.inventory().amountOf(good) ;
        maxBuys = ((Venue) client).stocks.shortageOf(good) ;
      }
      maxSold -= reservedForCollection(OD, good) ;
      maxBuys -= reservedForCollection(CD, good) ;
      final float amount = Math.min(maxSold, maxBuys) ;
      
      final float rateTrade = tradeType == IS_TRADE ? (Deliveries.rateTrading(
        good, origin, client, tradeType
      ) / (amount + 1)) : 2 ;
      
      /*
      if (origin == I.talkAbout && good == PARTS && tradeType == IS_TRADE) {
        I.say("Urgency at origin: "+((Venue) origin).stocks.shortageUrgency(good)) ;
        I.say("Urgency at client: "+((Venue) client).stocks.shortageUrgency(good)) ;
        I.sayAbout(origin, "Trade rating is: "+rateTrade) ;
        I.say("Trade amount is: "+(amount * rateTrade)) ;
      }
      //*/
      
      if ((amount * rateTrade) < pickUnit) continue ;
      if (rateTrade >= 1) {
        viable.queueAdd(Item.withAmount(good, amount)) ;
        continue ;
      }
      viable.queueAdd(Item.withAmount(good, amount * rateTrade)) ;
    }
    //
    //  We then compress the quantities of items to fit within prescribed size
    //  and price limits-
    final int amounts[] = new int[viable.size()] ;
    float sumAmounts = 0, sumPrice = 0, scale = 1 ;
    for (Item i : viable) {
      sumAmounts += i.amount ;
      float price = origin.priceFor(i.type) ;
      if (ration) price -= 50 ;
      if (price <= 0) continue ;
      sumPrice += price * i.amount ;
    }
    if (sumAmounts > sizeLimit) {
      scale = sizeLimit / sumAmounts ;
      sumPrice *= scale ;
    }
    final float priceLimit = pays == null ?
      Float.POSITIVE_INFINITY :
      pays.gear.credits() / 2f ;
    if (sumPrice > priceLimit) {
      scale *= priceLimit / sumPrice ;
    }
    
    if (verbose && I.talkAbout == origin) {
      I.say("Size/price limits: "+sizeLimit+" "+priceLimit+", goods:") ;
      for (Item v : viable) I.say("  "+v) ;
    }
    //
    //  In so doing, however, we must round up to the nearest order-unit...
    int i = 0 ;
    sumAmounts = 0 ;
    sumPrice = 0 ;
    for (Item v : viable) {
      amounts[i] = roundUnit * (int) Math.ceil(v.amount * scale / roundUnit) ;
      sumAmounts += amounts[i] ;
      sumPrice += amounts[i] * v.price() / v.amount ;
      i++ ;
    }
    //
    //  ...which then necessitates trimming off possible excess-
    if (viable.size() != 0) while (true) {
      boolean noneTrimmed = true ;
      i = 0 ; for (Item v : viable) {
        float price = origin.priceFor(v.type) ;
        if (ration) price -= 50 ;
        if (price <= 0) price = 0 ;
        
        final boolean mustTrim =
          sumAmounts > sizeLimit ||
          (sumPrice > priceLimit && price > 0) ||
          origin.inventory().amountOf(v) < amounts[i] ;
        if (amounts[i] > 0 && mustTrim) {
          amounts[i] -= roundUnit ;
          sumAmounts -= roundUnit ;
          sumPrice -= roundUnit * price / v.amount ;
          noneTrimmed = false ;
        }
        i++ ;
      }
      if (noneTrimmed) break ;
    }
    
    if (verbose && I.talkAbout == origin) {
      I.say("AFTER TRIM") ;
      i = 0 ;
      for (Item v : viable) {
        I.say("  "+v.type+" "+amounts[i++]) ;
      }
    }
    
    //
    //  Finally, we compile and return the quantities as items:
    final Batch <Item> trimmed = new Batch <Item> () ;
    i = 0 ; for (Item v : viable) {
      final int amount = amounts[i++] ;
      if (amount > 0) trimmed.add(Item.withAmount(v, amount)) ;
    }
    return trimmed.toArray(Item.class) ;
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
  
  
  //
  //  To evaluate priority, use the rateTrading method on available goods,
  //  scaled by quantity...  TODO:  That.
  
  
  
  /**  Helper methods for rating the attractiveness of trade with different
    *  venues-
    */
  private static Owner bestOrigin(
    Service goods[], Owner client, Batch <Owner> origins, int tradeType
  ) {
    Owner picked = null ;
    float bestRating = 0 ;
    for (Owner origin : origins) {
      final float rating = rateTrading(goods, origin, client, tradeType) ;
      if (rating > bestRating) { bestRating = rating ; picked = origin ; }
    }
    return picked ;
  }
  
  
  private static Owner bestClient(
    Service goods[], Owner origin, Batch <Owner> clients, int tradeType
  ) {
    Owner picked = null ;
    float bestRating = 0 ;
    for (Owner client : clients) {
      final float rating = rateTrading(goods, origin, client, tradeType) ;
      if (rating > bestRating) { bestRating = rating ; picked = client ; }
    }
    return picked ;
  }
  
  
  private static float rateTrading(
    Service goods[], Owner origin, Owner client, int tradeType
  ) {
    float sumRatings = 0 ;
    for (Service good : goods) {
      sumRatings += rateTrading(good, origin, client, tradeType) ;
    }
    return sumRatings ;
  }
  
  
  private static float rateTrading(
    Service good, Owner origin, Owner client, int tradeType
  ) {
    //
    //  The basic purpose of this comparison is to ensure that deliveries are
    //  non-symmetric.
    if (tradeType == IS_IMPORT) {
      //final int capacity = origin.spaceFor(good) ;
      return Math.min(
        ((Service.Trade) client).importShortage(good),
        origin.inventory().amountOf(good)
      ) / 10f ;//capacity ;
    }
    if (tradeType == IS_EXPORT) {
      final int capacity = client.spaceFor(good) ;
      return Math.min(
        ((Service.Trade) origin).exportSurplus(good),
        capacity
      ) / 10f ;//capacity ;
    }
    final float originUrgency = (origin instanceof Venue) ?
      ((Venue) origin).stocks.shortageUrgency(good) : 0 ;
    final float clientUrgency = (client instanceof Venue) ?
      ((Venue) client).stocks.shortageUrgency(good) : 0 ;
    
    /*
    if (I.talkAbout == origin) {
      I.say("ORE URGENCY: "+originUrgency+"/"+clientUrgency) ;
    }
    //*/
    if (clientUrgency <= originUrgency) return 0 ;
    /*
    if (clientUrgency + originUrgency == 0) return 0 ;
    //final float TP = client.priceFor(good) + origin.priceFor(good) ;
    float rating =
      (clientUrgency - originUrgency) * 2f /
      (clientUrgency + originUrgency) ;
    //*/
    float rating = 1 ;
    
    rating *= origin.inventory().amountOf(good) / 10f ;
    rating *= ((Venue) client).stocks.shortageOf(good) / 10f ;
    //  TODO:  Make sure the client inventory has space!
    
    return rating ;
  }
  
  
  
  /**  Helper methods for getting viable targets-
    */
  public static Batch <Venue> nearbyDepots(Target t, World world) {
    final Batch <Venue> depots = new Batch <Venue> () ;
    //  TODO:  Key this off the SERVICE_DEPOT service instead?
    world.presences.sampleTargets(SupplyDepot.class, t, world, 5, depots) ;
    world.presences.sampleTargets(StockExchange.class, t, world, 5, depots) ;
    return depots ;
  }
  
  
  public static Batch <Venue> nearbyCustomers(Target target, World world) {
    final Batch <Venue> nearby = new Batch <Venue> () ;
    world.presences.sampleTargets(Venue.class, target, world, 10, nearby) ;
    final Batch <Venue> returned = new Batch <Venue> () ;
    for (Venue v : nearby) {
      if (v.privateProperty()) continue ;
      if ((v instanceof SupplyDepot) || (v instanceof StockExchange)) continue ;
      returned.add(v) ;
    }
    return returned ;
  }
  
  
  public static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleTargets(Dropship.class, target, world, 10, nearby) ;
    return nearby ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service type, Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    world.presences.sampleTargets(type, target, world, 5, vendors) ;
    return vendors ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service types[], Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    for (Service type : types) {
      world.presences.sampleTargets(type, target, world, 5, vendors) ;
    }
    return vendors ;
  }
}









