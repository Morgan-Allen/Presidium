


package src.game.building ;
import src.game.actors.* ;
import src.game.building.Paving.Route ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;



/**  A specialised search algorithm used especially for road connections.
  */
//  TODO:  Possibly introduce a 'safety feature' to ensure that the path won't
//  extend more than 3 or 4 times longer than the euclidean distance estimate-
//  I don't want stupidly convoluted or out-of-the-way routes being taken.
public class RouteSearch extends AgendaSearch <Tile> {
  
  
  final Terrain terrain  ;
  final Tile destination ;
  final int priority ;
  final private Tile edges[] = new Tile[4] ;
  
  
  public RouteSearch(Tile start, Tile end, int priority) {
    super(start) ;
    this.destination = end ;
    this.terrain = end.world.terrain() ;
    this.priority = priority ;
    ///I.say("Searching for route between "+start+" and "+end) ;
  }
  
  
  public RouteSearch(Paving paving, Route route) {
    super(route.start) ;
    this.destination = route.end ;
    this.terrain = paving.venue.world().terrain() ;
    this.priority = Element.FIXTURE_OWNS ;
    ///I.say("Searching for route between "+route.start+" and "+destination) ;
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
    ///if (t == null) I.complain("TILE IS NULL!") ;
    return t.habitat().pathClear && (
      t.owner() == null ||
      t.owner().owningType() < priority
    ) ;
  }
}

