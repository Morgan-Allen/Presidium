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



public class Paving implements TileConstants {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  //
  //  TODO:  You'll want to extend this beyond just venues, to allow for
  //  connection to things like mag nodes.
  final Venue venue ;
  List <Route> routes = new List <Route> () ;
  
  static class Route {
    Tile start, end, path[] ;
    private boolean fresh ;
  }
  
  
  public Paving(Venue venue) {
    this.venue = venue ;
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
  protected void onWorldEntry() {
    venue.world().terrain().maskAsPaved(venue.surrounds(), true) ;
  }
  
  
  protected void onWorldExit() {
    venue.world().terrain().maskAsPaved(venue.surrounds(), false) ;
  }
  
  
  //  Restore this later once the essentials of road display are sorted...
  protected void updateRoutes() {
    ///if (true) return ;
    
    final float searchDist = Planet.SECTOR_SIZE ;
    final Base base = venue.base() ;
    final Box2D limit = new Box2D(), area = venue.area() ;
    for (Route r : venue.paving.routes) r.fresh = false ;
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
      
      for (Object o : base.servicesNear(base, venue, limit)) {
        if (! (o instanceof Venue)) continue ;
        final Venue near = (Venue) o ;
        if (! near.usesRoads()) continue ;
        refreshRoute(venue, near) ;
      }
    }
    for (Route r : venue.paving.routes) if (! r.fresh) {
      venue.paving.routes.remove(r) ;
    }
  }
  
  
  private void refreshRoute(Venue a, Venue b) {
    //
    //  Firstly, identify the previous and current routes between these venues-
    final Route
      newRoute = routeFor(a.entrance(), b.entrance()),
      oldRoute = currentMatch(newRoute, a) ;
    final RouteSearch search = new RouteSearch(this, newRoute) ;
    search.doSearch() ;
    newRoute.path = search.getPath(Tile.class) ;
    //
    //  Then, check to see if the new path differs from the old.  If so, delete
    //  the old path and instate the new.
    final Terrain terrain = venue.world().terrain() ;
    if (! routesEqual(newRoute, oldRoute)) {
      if (oldRoute != null) {
        terrain.maskAsPaved(oldRoute.path, false) ;
        a.paving.routes.remove(oldRoute) ;
      }
      if (newRoute.path != null) {
        terrain.maskAsPaved(newRoute.path, true) ;
        a.paving.routes.add(newRoute) ;
        clearRoute(newRoute) ;
        newRoute.fresh = true ;
      }
    }
    else oldRoute.fresh = true ;
  }
  
  
  //  You'll eventually want to replace this with explicit construction of
  //  roads...
  private void clearRoute(Route r) {
    for (Tile t : r.path) if (t.owner() != null) t.owner().exitWorld() ;
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
  
  
  private Route currentMatch(Route r, Venue v) {
    for (Route m : v.paving.routes) {
      if (m.start == r.start && m.end == r.end) return m ;
    }
    return null ;
  }
  
  
  private Route routeFor(Tile a, Tile b) {
    //
    //  We must ensure the ordering of start/end tiles remains stable to ensure
    //  that pathing between them remains consistent.
    final Route route = new Route() ;
    final int s = venue.world().size ;
    final boolean flip = ((a.x * s) + a.y) > ((b.x * s) + b.y) ;
    if (flip) { route.start = b ; route.end = b ; }
    else      { route.start = a ; route.end = b ; }
    return route ;
  }
  
  
  
  /**  A specialised search algorithm used especially for road connections.
    */
  static class RouteSearch extends AgendaSearch <Tile> {
    
    
    final Paving paving ;
    final Terrain terrain  ;
    final Tile destination ;
    final private Tile edges[] = new Tile[4] ;
    
    
    public RouteSearch(Paving paving, Route route) {
      super(route.start) ;
      this.terrain = paving.venue.world().terrain() ;
      this.paving = paving ;
      this.destination = route.end ;
    }
    
    
    protected boolean endSearch(Tile t) {
      return t == destination ;
    }
    

    protected float estimate(Tile spot) {
      final float xd = spot.x - destination.x, yd = spot.y - destination.y ;
      return ((xd > 0 ? xd : -xd) + (yd > 0 ? yd : -yd)) * 2 ;
    }
    
    
    protected float cost(Tile prior, Tile spot) {
      if (terrain.isRoad(spot)) return 1 ;
      return 2 ;
    }
    
    
    protected Tile[] adjacent(Tile t) {
      return t.edgeAdjacent(edges) ;
    }
    
    
    protected boolean canEnter(Tile t) {
      return t.habitat().pathClear && (
        t.owner() == null ||
        t.owner().owningType() <= Element.ENVIRONMENT_OWNS
      ) ;
    }
  }
}

