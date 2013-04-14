/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.util.* ;




public class PathingSearch extends AgendaSearch <Boardable> {
  
  
  
  /**  Field definitions and constructors-
    */
  final protected Boardable destination ;
  private Vec3D posA = new Vec3D(), posB = new Vec3D() ;
  private Boardable batch[] = new Boardable[8] ;
  
  
  public PathingSearch(Boardable init, Boardable dest) {
    super(init) ;
    if (dest == null) {
      I.complain("NO DESTINATION!") ;
    }
    this.destination = dest ;
  }
  
  
  
  /**  Actual search-execution methods-
    */
  protected Boardable[] adjacent(Boardable spot) {
    return spot.canBoard(batch) ;
  }
  
  
  protected float cost(Boardable prior, Boardable spot) {
    if (spot == null) return -1 ;
    switch(spot.pathType()) {
      case (Tile.PATH_CLEAR  ) : return 1.0f ;
      case (Tile.PATH_ROAD   ) : return 0.5f ;
      case (Tile.PATH_HINDERS) : return 5.0f ;
      default : return 0 ;
    }
  }
  
  
  protected float estimate(Boardable spot) {
    destination.position(posA) ;
    spot.position(posB) ;
    final float x = posA.x - posB.x, y = posA.y - posB.y ;
    final float dist = (float) Math.sqrt((x * x) + (y * y)) * 1.1f ;
    if (spot.pathType() == Tile.PATH_CLEAR) return dist * 0.55f ;
    return dist ;
  }
  
  
  protected boolean endSearch(Boardable best) {
    return best == destination ;
  }
}




