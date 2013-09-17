


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.Inventory.Owner ;
import src.util.* ;


//
//  TODO:  I think the whole passenger-delivery thing needs to be made
//  separate.  It's not actually closely related.
//  TODO:  Also, barges need to be made more persistent.


public class Delivery2 implements BuildConstants {
  

  
  

  /**  Helper methods for squeezing orders down into manageable chunks-
    */
  static Item[] configDelivery(
    Service goods[], Owner origin, Owner client,
    Actor pays, int sizeLimit, World world
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
      maxSold = origin.inventory().amountOf(good) ;
      maxBuys = (client instanceof Service.Trade) ?
        ((Service.Trade) client).importShortage(good) :
        ((Venue) client).stocks.shortageOf(good) ;
      maxSold -= reservedForCollection(OD, good) ;
      maxBuys -= reservedForCollection(CD, good) ;
      final float amount = Math.min(maxSold, maxBuys) ;
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
    trimLoop: while (true) {
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
  //  scaled by quantity.
  
  
  
  /**  Helper methods for rating the attractiveness of trade with different
    *  venues-
    */
  static Owner bestOrigin(
    Service goods[], Owner client, Batch <Owner> origins
  ) {
    Owner picked = null ;
    float bestRating = 0 ;
    for (Owner origin : origins) {
      final float rating = rateTrading(goods, origin, client) ;
      if (rating > bestRating) { bestRating = rating ; picked = origin ; }
    }
    return picked ;
  }
  
  
  static Owner bestClient(
    Service goods[], Owner origin, Batch <Owner> clients
  ) {
    Owner picked = null ;
    float bestRating = 0 ;
    for (Owner client : clients) {
      final float rating = rateTrading(goods, origin, client) ;
      if (rating > bestRating) { bestRating = rating ; picked = client ; }
    }
    return picked ;
  }
  
  
  static float rateTrading(Service goods[], Owner origin, Owner client) {
    float sumRatings = 0 ;
    for (Service good : goods) sumRatings += rateTrading(good, origin, client) ;
    return sumRatings ;
  }
  
  
  static float rateTrading(Service good, Owner origin, Owner client) {
    //
    //  The basic purpose of this comparison is to ensure that deliveries are
    //  non-symmetric.
    //  TODO:  This also needs to use different supply/demand metrics for
    //         dropship origins/clients.
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
  static Batch <Venue> nearbyDepots(Target t, World world) {
    final Batch <Venue> depots = new Batch <Venue> () ;
    //  TODO:  Key this off the SERVICE_DEPOT service instead?
    world.presences.sampleTargets(SupplyDepot.class, t, world, 5, depots) ;
    world.presences.sampleTargets(StockExchange.class, t, world, 5, depots) ;
    return depots ;
  }
  
  
  static Batch <Venue> nearbyCustomers(Target target, World world) {
    final Batch <Venue> nearby = new Batch <Venue> () ;
    //  TODO:  Skip over any depots.
    world.presences.sampleTargets(Venue.class, target, world, 10, nearby) ;
    return nearby ;
  }
  
  
  static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleTargets(Dropship.class, target, world, 10, nearby) ;
    return nearby ;
  }
}









