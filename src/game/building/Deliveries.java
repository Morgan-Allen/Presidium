


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.Inventory.Owner ;
import src.util.* ;



public class Deliveries implements BuildConstants {
  
  
  final private static int
    IS_TRADE = 0,
    IS_IMPORT = 1,
    IS_EXPORT = 2 ;  
  
  
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
    final List <Item> viable = new List <Item> () {
      protected float queuePriority(Item i) {
        return (i.amount + 1f) / (1 + i.type.basePrice) ;
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
      final float amount = Math.min(maxSold, maxBuys) / 2f ;
      if (amount <= 0) continue ;
      viable.queueAdd(Item.withAmount(good, amount)) ;
    }
    //
    //  We then compress the quantities of items to fit within prescribed size
    //  and price limits-
    final int amounts[] = new int[viable.size()] ;
    float sumAmounts = 0, sumPrice = 0, scale = 1 ;
    for (Item i : viable) {
      sumAmounts += i.amount ;
      sumPrice += i.price() ;
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
    //
    //  In so doing, however, we must round up to the nearest order-unit...
    final int roundUnit = sizeLimit <= 5 ? 1 : 5 ;
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
    //
    //  TODO:  You also have to fit under the actual amount of the given good
    //  available for transport at the origin!
    
    trimLoop: while (viable.size() != 0) {
      i = 0 ; for (Item v : viable) {
        if (sumAmounts <= sizeLimit && sumPrice <= priceLimit) break trimLoop ;
        if (amounts[i] > 0) {
          amounts[i] -= roundUnit ;
          sumAmounts -= roundUnit ;
          sumPrice -= roundUnit * v.price() / v.amount ;
        }
        i++ ;
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
      return Math.min(
        ((Service.Trade) client).importShortage(good),
        origin.inventory().amountOf(good)
      ) ;
    }
    if (tradeType == IS_EXPORT) {
      return Math.min(
        ((Service.Trade) origin).exportSurplus(good),
        1000 //  TODO:  Put in limit based on cargo capacity
      ) ;
    }
    final float originUrgency = (origin instanceof Venue) ?
      ((Venue) origin).stocks.shortageUrgency(good) : 0 ;
    final float clientUrgency = (client instanceof Venue) ?
      ((Venue) client).stocks.shortageUrgency(good) : 0 ;
      
    if (clientUrgency <= originUrgency) return 0 ;
    final float TP = client.priceFor(good) + origin.priceFor(good) ;
    float rating = clientUrgency - originUrgency ;
    return rating * TP / 2f ;
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
    //  TODO:  Skip over any depots.
    world.presences.sampleTargets(Venue.class, target, world, 10, nearby) ;
    return nearby ;
  }
  
  
  public static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleTargets(Dropship.class, target, world, 10, nearby) ;
    return nearby ;
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









