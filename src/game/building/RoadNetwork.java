/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.AgendaSearch ;
import src.game.planet.Planet ;
import src.graphics.terrain.TerrainMesh ;
import src.util.* ;



public class RoadNetwork implements TileConstants {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final World world ;
  private final byte mask[][] ;
  
  final public TerrainMesh.Mask meshMask = new TerrainMesh.Mask() {
    protected boolean maskAt(int x, int y) {
      return mask[x][y] > 0 ;
    }
  } ;
  
  static class Route {
    Tile start, end, path[] ;
    private boolean fresh ;
  }
  
  
  public RoadNetwork(World world) {
    this.world = world ;
    this.mask = new byte[world.size][world.size] ;
  }
  
  public void loadState(Session s) throws Exception {
    s.loadByteArray(mask) ;
  }
  
  static void loadRoutes(List <Route> routes, Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Route r = new Route() ;
      r.start = (Tile) s.loadTarget() ;
      r.end = (Tile) s.loadTarget() ;
      r.path = (Tile[]) s.loadTargetArray(Tile.class) ;
    }
  }
  
  public void saveState(Session s) throws Exception {
    s.saveByteArray(mask) ;
  }
  
  static void saveRoutes(List <Route> routes, Session s) throws Exception {
    s.saveInt(routes.size()) ;
    for (Route r : routes) {
      s.saveTarget(r.start) ;
      s.saveTarget(r.end) ;
      s.saveTargetArray(r.path) ;
    }
  }
  
  
  
  /**  Updating routes-
    */
  //
  //  Okay.  Now you just need to determine how often this updates, and whether
  //  that automatically requires an update to the terrain map, and how that
  //  graphical info gets passed on...
  //
  //  Once every 10 seconds or so seems fair, I guess.
  
  public void updateRoutes(Venue venue) {
    final float searchDist = Planet.SECTOR_SIZE ;
    final Base base = venue.base() ;
    final Box2D limit = new Box2D(), area = venue.area() ;
    for (Route r : venue.routes()) r.fresh = false ;
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
    for (Route r : venue.routes()) if (! r.fresh) {
      venue.routes().remove(r) ;
    }
  }
  
  
  private void refreshRoute(Venue a, Venue b) {
    Route route = routeFor(a.entrance(), b.entrance()) ;
    
    for (Route r : a.routes()) {
      if (r.start == route.start && r.end == route.end) {
        toggleRoute(r, false) ;
        a.routes.remove(r) ;
      }
    }
    
    final RouteSearch search = new RouteSearch(this, route) ;
    search.doSearch() ;
    route.path = search.getPath(Tile.class) ;
    
    if (route.path == null) return ;
    a.routes.add(route) ;
    toggleRoute(route, true) ;
    route.fresh = true ;
    
    //
    //  TODO:  If there's been any change along this route, have the terrain's
    //  road map update.
    /*
    for (Tile t : route.path) {
      world.terrain().updatePatchRoads(t.x, t.y, meshMask) ;
    }
    //*/
  }
  
  
  private Route routeFor(Tile a, Tile b) {
    final Route route = new Route() ;
    
    final int s = world.size ;
    final boolean flip = ((a.x * s) + a.y) > ((b.x * s) + b.y) ;
    
    if (flip) { route.start = b ; route.end = b ; }
    else      { route.start = a ; route.end = b ; }
    
    return route ;
  }
  
  
  private void toggleRoute(Route r, boolean is) {
    for (Tile t : r.path) mask[t.x][t.y] += is ? 1 : -1 ;
  }
  
  

  /**  A specialised search algorithm used especially for road connections.
    */
  static class RouteSearch extends AgendaSearch <Tile> {
    
    
    final RoadNetwork network ;
    final Tile destination ;
    final private Tile edges[] = new Tile[4] ;
    
    
    public RouteSearch(RoadNetwork network, Route route) {
      super(route.start) ;
      this.network = network ;
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
      if (network.mask[spot.x][spot.y] > 0) return 2 ;
      return 1.0f ;
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

