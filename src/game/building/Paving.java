/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.planet.* ;
import src.util.* ;



public class Paving {
  
  
  
  /**  Field definitions, constructor and save/load methods-
    */
  final static int PATH_RANGE = World.SECTION_RESOLUTION * 2 ;
  
  final World world ;
  PresenceMap junctions ;
  Table <Tile, List <Route>> tileRoutes = new Table(1000) ;
  Table <Route, Route> allRoutes = new Table <Route, Route> (1000) ;
  
  
  public Paving(World world) {
    this.world = world ;
    junctions = new PresenceMap(world, "junctions") ;
  }
  
  
  public void loadState(Session s) throws Exception {
    junctions = (PresenceMap) s.loadObject() ;
    
    int numR = s.loadInt() ;
    for (int n = numR ; n-- > 0 ;) {
      final Route r = Route.loadRoute(s) ;
      allRoutes.put(r, r) ;
      toggleRoute(r, r.start, true) ;
      toggleRoute(r, r.end  , true) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(junctions) ;
    
    s.saveInt(allRoutes.size()) ;
    for (Route r : allRoutes.keySet()) Route.saveRoute(r, s) ;
  }
  
  
  
  
  /**  Methods related to installation, updates and deletion of junctions-
    */
  public void updatePerimeter(Venue v, boolean isMember) {
    final Tile o = v.origin() ;
    final Route key = new Route(o, o), match = allRoutes.get(key) ;
    if (match != null) world.terrain().maskAsPaved(match.path, false) ;
    if (isMember) {
      final Batch <Tile> around = new Batch <Tile> () ;
      for (Tile t : Spacing.perimeter(v.area(), world)) if (t != null) {
        if (t.owningType() <= Element.ELEMENT_OWNS) around.add(t) ;
      }
      key.path = around.toArray(Tile.class) ;
      key.cost = -1 ;
      world.terrain().maskAsPaved(key.path, true) ;
      clearRoad(key.path) ;
      allRoutes.put(key, key) ;
    }
  }
  
  
  public void updateJunction(Tile t, boolean isMember) {
    junctions.toggleMember(t, isMember) ;
    if (isMember) {
      ///I.say("Updating road junction "+t) ;
      for (Target o : junctions.visitNear(t, PATH_RANGE, null)) {
        final Tile jT = (Tile) o ;
        routeBetween(t, jT) ;
      }
    }
    else {
      ///I.say("Deleting road junction "+t) ;
      for (Route r : tileRoutes.get(t)) deleteRoute(r) ;
    }
  }
  
  
  private boolean routeBetween(Tile a, Tile b) {
    //
    //  Firstly, determine the correct current route.
    final Route route = new Route(a, b) ;
    final RoadSearch search = new RoadSearch(a, b, Element.FIXTURE_OWNS) ;
    search.doSearch() ;
    route.path = search.fullPath(Tile.class) ;
    route.cost = search.totalCost() ;
    //
    //  If the new route differs from the old, delete it.  Otherwise return.
    final Route oldRoute = allRoutes.get(route) ;
    if (roadsEqual(route, oldRoute)) return false ;
    if (oldRoute != null) deleteRoute(oldRoute) ;
    if (! search.success()) return false ;
    //
    //  If the route needs an update, clear the tiles and store the data.
    allRoutes.put(route, route) ;
    toggleRoute(route, route.start, true) ;
    toggleRoute(route, route.end  , true) ;
    world.terrain().maskAsPaved(route.path, true) ;
    clearRoad(route.path) ;
    return true ;
  }
  
  
  private boolean roadsEqual(Route newRoute, Route oldRoute) {
    if (newRoute.path == null || oldRoute == null) return false ;
    boolean match = true ;
    for (Tile t : newRoute.path) t.flagWith(newRoute) ;
    int numMatched = 0 ;
    for (Tile t : oldRoute.path) {
      if (t.flaggedWith() != newRoute) {
        match = false ;
        break ;
      }
      else numMatched++ ;
    }
    for (Tile t : newRoute.path) t.flagWith(null) ;
    if (numMatched != newRoute.path.length) match = false ;
    return match ;
  }
  
  
  private void deleteRoute(Route route) {
    world.terrain().maskAsPaved(route.path, false) ;
    allRoutes.remove(route) ;
    toggleRoute(route, route.start, false) ;
    toggleRoute(route, route.end  , false) ;
  }
  
  
  private void toggleRoute(Route route, Tile t, boolean is) {
    List <Route> atTile = tileRoutes.get(t) ;
    if (atTile == null) tileRoutes.put(t, atTile = new List <Route> ()) ;
    if (is) atTile.add(route) ;
    else atTile.remove(route) ;
    if (atTile.size() == 0) tileRoutes.remove(t) ;
  }
  
  
  /**  Methods related to physical road construction-
    */
  //
  //  You'll eventually want to replace this with explicit construction of
  //  roads...
  public static void clearRoad(Tile path[]) {
    for (Tile t : path) if (t.owningType() < Element.FIXTURE_OWNS) {
      if (t.owner() != null) t.owner().exitWorld() ;
    }
  }
  
  
  
  /**  Methods related to distribution of provisional goods-
    */
  class Junction {
    Target source ;
    Tile tile ;
    Stack <Route> routes = new Stack <Route> () ;
  }
  
  
  private Tile[] junctionsReached(Base base, Tile init) {
    final Batch <Tile> reached = new Batch <Tile> () ;
    final Stack <Tile> working = new Stack <Tile> () ;
    
    working.add(init) ;
    reached.add(init) ;
    init.flagWith(reached) ;
    
    while (working.size() > 0) {
      Tile next = working.removeFirst() ;
      for (Route r : tileRoutes.get(next)) {
        Tile toAdd = r.start == next ? r.end : r.start ;
        if (toAdd.flaggedWith() != null) continue ;
        working.add(toAdd) ;
        reached.add(toAdd) ;
        toAdd.flagWith(reached) ;
      }
    }
    
    for (Tile t : reached) t.flagWith(null) ;
    return reached.toArray(Tile.class) ;
  }
}




/*
  public void distribute(Item.Type toDeliver[]) {
    Batch <Junction[]> allCovered = new Batch <Junction[]> () ;
    for (Object o : base.servicesNear(base, base.world.tileAt(0, 0), -1)) {
      if (! (o instanceof Venue)) continue ;
      final Venue venue = (Venue) o ;
      if (venue.flaggedWith() == allCovered) continue ;
      final Junction init = junctionAt(venue.mainEntrance()) ;
      if (init == null) continue ;
      final Junction reached[] = reachedFrom(init) ;
      distributeTo(reached, toDeliver) ;
      for (Junction j : reached) j.parent.flagWith(allCovered) ;
      allCovered.add(reached) ;
    }
    for (Junction covered[] : allCovered) for (Junction j : covered) {
      j.parent.flagWith(null) ;
    }
  }
  
  
  private Junction[] reachedFrom(Junction init) {
    final JunctionPathSearch search = new JunctionPathSearch(this, init, null) ;
    search.doSearch() ;
    final Junction reached[] = search.allSearched(Junction.class) ;
    return reached ;
  }
  
  
  private void distributeTo(Junction reached[], Item.Type toDeliver[]) {
    float
      supply[] = new float[toDeliver.length],
      demand[] = new float[toDeliver.length] ;
    for (Junction j : reached) {
      if (! (j.parent instanceof Venue)) continue ;
      final Venue venue = (Venue) j.parent ;
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
      for (Junction j : reached) {
        if (! (j.parent instanceof Venue)) continue ;
        final Venue venue = (Venue) j.parent ;
        final float localDemand = venue.orders.requiredShortage(type) ;
        venue.stocks.addItem(new Item(type, localDemand * supplyRatio)) ;
      }
    }
  }

//*/