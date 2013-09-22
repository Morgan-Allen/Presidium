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
  
  
  final static int
    IS_TRADE    = 0,
    IS_SHOPPING = 1,
    IS_IMPORT   = 2,
    IS_EXPORT   = 3 ;  
  
  private static boolean verbose = false ;
  
  
  public static Delivery nextDeliveryFor(
    Actor actor, Owner origin, Service goods[],
    int sizeLimit, World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) nearbyCustomers(origin, world),
      sizeLimit, IS_TRADE
    ) ;
  }
  

  public static Delivery nextDeliveryFor(
    Actor actor, Owner origin, Service goods[],
    Batch <Venue> clients, int sizeLimit,
    World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) clients,
      sizeLimit, IS_TRADE
    ) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Actor actor, Owner client, Service goods[],
    int sizeLimit, Actor pays, World world
  ) {
    return Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) nearbyVendors(goods, client, world),
      sizeLimit, pays == null ? IS_TRADE : IS_SHOPPING
    ) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Actor actor, Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, Actor pays, World world
  ) {
    return Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) vendors,
      sizeLimit, pays == null ? IS_TRADE : IS_SHOPPING
    ) ;
  }
  
  
  public static Delivery nextImportDelivery(
    Actor actor, Owner origin, Service goods[], Batch <Venue> clients,
    int sizeLimit, World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) clients,
      sizeLimit, IS_IMPORT
    ) ;
  }
  
  
  public static Delivery nextExportCollection(
    Actor actor, Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, World world
  ) {
    return Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) vendors,
      sizeLimit, IS_EXPORT
    ) ;
  }
  
  
  private static Delivery bestOrigin(
    Actor actor, Service goods[], Owner client,
    Batch <Owner> origins, int sizeLimit, int tradeType
  ) {
    Delivery picked = null ;
    float bestRating = 0 ;
    for (Owner origin : origins) {
      Item order[] = Deliveries.configDelivery(
        goods, origin, client, actor,
        sizeLimit, actor.world(), tradeType
      ) ;
      if (order.length == 0) continue ;
      final Delivery d = new Delivery(order, origin, client) ;
      final float rating = d.priorityFor(actor) ;
      if (rating > bestRating) { bestRating = rating ; picked = d ; }
    }
    return picked ;
  }
  
  
  private static Delivery bestClient(
    Actor actor, Service goods[], Owner origin,
    Batch <Owner> clients, int sizeLimit, int tradeType
  ) {
    Delivery picked = null ;
    float bestRating = 0 ;
    for (Owner client : clients) {
      Item order[] = Deliveries.configDelivery(
        goods, origin, client, actor,
        sizeLimit, actor.world(), tradeType
      ) ;
      final Delivery d = new Delivery(order, origin, client) ;
      final float rating = d.priorityFor(actor) ;
      if (rating > bestRating) { bestRating = rating ; picked = d ; }
    }
    return picked ;
  }
  
  

  /**  Helper methods for squeezing orders down into manageable chunks-
    */
  private static float rateTrading(
    Service good, Owner origin, Owner client, int tradeType
  ) {
    //
    //  The basic purpose of this comparison is to ensure that deliveries are
    //  non-symmetric.
    if (tradeType == IS_SHOPPING) {
      final float rating = origin.inventory().amountOf(good) / 10f ;
      if (I.talkAbout == client && good == PARTS) {
        I.say("URGENCY for "+origin+" is "+rating) ;
      }
      return rating ;
    }
    else if (tradeType == IS_IMPORT) {
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
    
    if (clientUrgency <= originUrgency) return 0 ;
    float rating = 1 ;
    
    rating *= origin.inventory().amountOf(good) / 10f ;
    rating *= ((Venue) client).stocks.shortageOf(good) / 10f ;
    //  TODO:  Make sure the client inventory has space!
    
    return rating ;
  }
  
  
  private static Item[] configDelivery(
    Service goods[], Owner origin, Owner client,
    Actor pays, int sizeLimit, World world,
    int tradeType
  ) {
    //
    //  First, get the amount of each item available for trade at the point of
    //  origin, and desired by the destination/client, which constrains the
    //  quantities involved-
    final Object subject = pays ;
    
    if (verbose) I.sayAbout(
      subject, "Evaluating delivery from "+origin+" to "+client
    ) ;
    final int
      roundUnit = sizeLimit <= 5 ? 1 : 5,
      pickUnit  = sizeLimit <= 5 ? 0 : 3 ;
    
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
      
      if (verbose && I.talkAbout == subject) {
        I.say("  Service: "+good) ;
        I.say("    Available: "+origin.inventory().amountOf(good)) ;
        I.say("    Reserved: "+reservedForCollection(OD, good)) ;
        I.say("    Max buys/sold: "+maxBuys+"/"+maxSold) ;
        I.say("    Trade rating is: "+rateTrade) ;
        I.say("    Trade amount is: "+(amount * rateTrade)) ;
      }
      
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
      final float price = Delivery.purchasePrice(i, pays, origin) ;
      if (price <= 0) continue ;
      sumPrice += price ;
    }
    if (sumAmounts > sizeLimit) {
      scale = sizeLimit / sumAmounts ;
      sumPrice *= scale ;
    }
    final float priceLimit = tradeType != IS_SHOPPING ?
      Float.POSITIVE_INFINITY :
      pays.gear.credits() / 2f ;
    if (sumPrice > priceLimit) {
      scale *= priceLimit / sumPrice ;
    }
    
    if (verbose && I.talkAbout == subject) {
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
      float price ;
      i = 0 ; for (Item v : viable) {
        price = Math.max(0, Delivery.purchasePrice(v, pays, origin)) ;
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
    
    if (verbose && I.talkAbout == subject) {
      I.say("AFTER TRIM") ;
      i = 0 ;
      for (Item v : viable) {
        I.say("  "+v.type+" "+amounts[i++]) ;
      }
    }
    
    //
    //  Finally, we compile and return the quantities as items:
    final Batch <Item> trimmed = new Batch <Item> () ;
    i = viable.size() ;
    for (ListEntry <Item> LE = viable ; (LE = LE.lastEntry()) != viable ;) {
      final int amount = amounts[--i] ;
      if (amount > 0) trimmed.add(Item.withAmount(LE.refers, amount)) ;
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
  
  
  
  /**  Helper methods for getting viable targets-
    */
  public static Batch <Venue> nearbyDepots(Target t, World world) {
    final Batch <Venue> depots = new Batch <Venue> () ;
    world.presences.sampleFromKeys(
      t, world, 5, depots,
      SupplyDepot.class, StockExchange.class
    ) ;
    return depots ;
  }
  
  
  public static Batch <Venue> nearbyCustomers(Target target, World world) {
    final Batch <Venue> sampled = new Batch <Venue> () ;
    world.presences.sampleFromKey(
      target, world, 5, sampled, Venue.class
    ) ;
    final Batch <Venue> returned = new Batch <Venue> () ;
    for (Venue v : sampled) {
      if (v.privateProperty()) continue ;
      if ((v instanceof SupplyDepot) || (v instanceof StockExchange)) continue ;
      returned.add(v) ;
    }
    return returned ;
  }
  
  
  public static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleFromKey(target, world, 10, nearby, Dropship.class) ;
    return nearby ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service type, Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    world.presences.sampleFromKey(target, world, 5, vendors, type) ;
    return vendors ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service types[], Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    world.presences.sampleFromKeys(target, world, 5, vendors, (Object[]) types) ;
    return vendors ;
  }
}









