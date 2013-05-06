


package src.game.building ;
import src.game.common.* ;
import src.game.building.Paving.* ;
import src.util.* ;



/**  This class specifically handles allocations of goods like power, water
  *  datalinks and/or life support that get delivered automatically through the
  *  network of roads and maglines that permeate the base.
  */
public class Distribution {
  
  
  public static void coverAll(Base base, Item.Type toDeliver[]) {
    Batch <Hub[]> allCovered = new Batch <Hub[]> () ;
    for (Object o : base.servicesNear(base, base.world.tileAt(0, 0), -1)) {
      if (! (o instanceof Venue)) continue ;
      final Venue venue = (Venue) o ;
      if (venue.flaggedWith() == allCovered) continue ;
      final Hub covered[] = coveredFrom(venue, toDeliver) ;
      for (Hub h : covered) h.flagWith(allCovered) ;
      allCovered.add(covered) ;
    }
    for (Hub covered[] : allCovered) for (Hub h : covered) h.flagWith(null) ;
  }
  
  
  private static Hub[] coveredFrom(Hub starts, Item.Type toDeliver[]) {
    //
    //  First, acquire a list of all the hubs connected here-
    final Hub hubBatch[] = new Hub[4] ;    
    final Search <Hub> spread = new Search <Hub> (starts, -1) {
      
      protected Hub[] adjacent(Hub spot) {
        int i = 0 ; for (Route r : spot.paving().routes) {
          hubBatch[i++] = (r.hubA == spot) ? r.hubB : r.hubA ;
        }
        while (i < hubBatch.length) hubBatch[i++] = null ;
        return hubBatch ;
      }
      
      protected boolean endSearch(Hub best) {
        return false ;
      }
      
      protected float cost(Hub prior, Hub spot) {
        return 0 ;
      }
      
      protected float estimate(Hub spot) {
        return 0 ;
      }
      
      protected void setEntry(Hub spot, Entry flag) {
        spot.flagWith(flag) ;
      }
      
      protected Entry entryFor(Hub spot) {
        return (Entry) spot.flaggedWith() ;
      }
    } ;
    spread.doSearch() ;
    //
    //  Then iterate over every venue accessed, and total supply and demand-
    final Hub[] reached = spread.allSearched(Hub.class) ;
    float
      supply[] = new float[toDeliver.length],
      demand[] = new float[toDeliver.length] ;
    for (Hub hub : reached) {
      if (! (hub instanceof Venue)) continue ;
      final Venue venue = (Venue) hub ;
      for (int i = toDeliver.length ; i-- > 0 ;) {
        final Item.Type type = toDeliver[i] ;
        supply[i] += venue.stocks.amountOf(type) ;
        venue.stocks.clearItems(type) ;
        demand[i] += venue.orders.requiredShortage(type) ;
      }
    }
    //
    //  Then top up demand in whole or in part, depending on how much supply
    //  is available-
    for (int i = toDeliver.length ; i-- > 0 ;) {
      final Item.Type type = toDeliver[i] ;
      final float supplyRatio = Visit.clamp(supply[i] / demand[i], 0, 1) ;
      for (Hub hub : reached) {
        if (! (hub instanceof Venue)) continue ;
        final Venue venue = (Venue) hub ;
        final float localDemand = venue.orders.requiredShortage(type) ;
        venue.stocks.addItem(new Item(type, localDemand * supplyRatio)) ;
      }
    }
    return reached ;
  }
}








