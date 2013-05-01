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


//
//  TODO:  Some of the methods employed here might be usefully extended to the
//  PathingCache class?
public class Paving implements TileConstants {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  //
  //  TODO:  You'll want to extend this beyond just venues, to allow for
  //  connection to things like mag nodes.
  //final Venue venue ;
  
  public static interface Hub extends Target {
    Paving paving() ;
    Tile origin() ;
    Tile[] entrances() ;
    Tile[] surrounds() ;
    boolean usesRoads() ;
    Base base() ;
    Box2D area() ;
  }
  
  private static class Route {
    Tile start, end, path[] ;
    private boolean fresh ;
  }
  
  //final Base base ;
  final Hub hub ;
  List <Route> routes = new List <Route> () ;
  
  
  public Paving(Hub hub) {
    this.hub = hub ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Route r = new Route() ;
      r.start = (Tile) s.loadTarget() ;
      r.end = (Tile) s.loadTarget() ;
      r.path = (Tile[]) s.loadTargetArray(Tile.class) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(routes.size()) ;
    for (Route r : routes) {
      s.saveTarget(r.start) ;
      s.saveTarget(r.end) ;
      s.saveTargetArray(r.path) ;
    }
  }
  
  
  
  /**  Updating routes to neighbours and perimeter paving-
    */
  public void onWorldEntry() {
    if (hub.base() == null || ! hub.usesRoads()) return ;
    world().terrain().maskAsPaved(hub.surrounds(), true) ;
    hub.base().toggleForService(hub, "paving", true) ;
  }
  
  
  public void onWorldExit() {
    if (hub.base() == null || ! hub.usesRoads()) return ;
    world().terrain().maskAsPaved(hub.surrounds(), false) ;
    hub.base().toggleForService(hub, "paving", false) ;
  }
  
  
  private World world() {
    return hub.base().world ;
  }
  
  
  //
  //  Restore this later once the essentials of road display are sorted...
  public void updateRoutes() {
    final float searchDist = Planet.SECTOR_SIZE ;
    final Box2D limit = new Box2D(), area = hub.area() ;
    for (Route r : routes) r.fresh = false ;
    //
    //  Refresh any routes to nearby venues in each of the cardinal directions-
    for (int d : N_ADJACENT) {
      
      final float
        xoff = (N_X[d] + 1) / 2f,
        yoff = (N_Y[d] + 1) / 2f ;
      limit.set(
        area.xpos() + (area.xdim() * xoff) - (searchDist * (1 - xoff)),
        area.ypos() + (area.ydim() * yoff) - (searchDist * (1 - yoff)),
        searchDist,
        searchDist
      ) ;
      
      for (Object o : hub.base().servicesNear("paving", hub, limit)) {
        ///I.say(o+" was nearby...") ;
        if (o == hub || ! (o instanceof Hub)) continue ;
        final Hub near = (Hub) o ;
        if (! near.usesRoads()) continue ;
        refreshRoute(hub, near) ;
        break ;
      }
    }
    for (Route r : routes) if (! r.fresh) {
      routes.remove(r) ;
    }
  }
  
  
  private void refreshRoute(Hub a, Hub b) {
    //
    //  Firstly, identify the previous and current routes between these venues-
    final Route
      newRoute = routeFor(a.origin(), b.origin()),
      oldRoute = currentMatch(newRoute, a) ;
    final RouteSearch search = new RouteSearch(
      a.entrances()[0], b.entrances()[0], Element.FIXTURE_OWNS
    ) ;
    ///search.verbose = true ;
    search.doSearch() ;
    newRoute.path = search.fullPath(Tile.class) ;
    //
    //  Then, check to see if the new path differs from the old.  If so, delete
    //  the old path and instate the new.
    final Terrain terrain = world().terrain() ;
    if (! routesEqual(newRoute, oldRoute)) {
      if (oldRoute != null) {
        terrain.maskAsPaved(oldRoute.path, false) ;
        a.paving().routes.remove(oldRoute) ;
      }
      if (newRoute.path != null) {
        terrain.maskAsPaved(newRoute.path, true) ;
        a.paving().routes.add(newRoute) ;
        clearRoute(newRoute.path) ;
        newRoute.fresh = true ;
      }
    }
    else oldRoute.fresh = true ;
  }
  
  
  //  You'll eventually want to replace this with explicit construction of
  //  roads...
  public static void clearRoute(Tile path[]) {
    for (Tile t : path) if (t.owningType() < Element.FIXTURE_OWNS) {
      if (t.owner() != null) t.owner().exitWorld() ;
    }
  }
  
  
  /**  Helper methods for initialising and comparing routes-
    */
  private boolean routesEqual(Route newRoute, Route oldRoute) {
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
  
  
  private Route currentMatch(Route r, Hub v) {
    for (Route m : v.paving().routes) {
      if (m.start == r.start && m.end == r.end) return m ;
    }
    return null ;
  }
  
  
  private Route routeFor(Tile a, Tile b) {
    //
    //  We must ensure the ordering of start/end tiles remains stable to ensure
    //  that pathing between them remains consistent.
    final Route route = new Route() ;
    final int s = world().size ;
    final boolean flip = ((a.x * s) + a.y) > ((b.x * s) + b.y) ;
    if (flip) { route.start = b ; route.end = b ; }
    else      { route.start = a ; route.end = b ; }
    return route ;
  }
}

