


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;



/**  A specialised search algorithm used especially for road connections.
  */
public class RoadSearch extends Search <Tile> {
  
  
  final static float
    MAX_DISTANCE_MULT = 4.0f ;
  
  final Terrain terrain  ;
  final Tile destination ;
  final int priority ;
  final private Tile edges[] = new Tile[4] ;
  
  
  public RoadSearch(Tile start, Tile end, int priority) {
    super(start, (Spacing.axisDist(start, end) * 20) + 20) ;
    this.destination = end ;
    this.terrain = end.world.terrain() ;
    this.priority = priority ;
  }
  
  
  protected boolean endSearch(Tile t) {
    return t == destination ;
  }
  
  
  protected float estimate(Tile spot) {
    final float
      xd = spot.x - destination.x,
      yd = spot.y - destination.y ;
    return ((xd > 0 ? xd : -xd) + (yd > 0 ? yd : -yd)) / 2f ;
  }
  
  
  protected float cost(Tile prior, Tile spot) {
    if (terrain.isRoad(spot)) return 1 ;
    return 2 ;
  }
  
  
  protected Tile[] adjacent(Tile t) {
    return t.edgeAdjacent(edges) ;
  }
  
  
  protected boolean canEnter(Tile t) {
    if (t == init || t == destination) return true ;
    ///if (t == null) I.complain("TILE IS NULL!") ;
    return t.habitat().pathClear && (
      t.owner() == null ||
      t.owner().owningType() < priority
    ) ;
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith() ;
  }
}






