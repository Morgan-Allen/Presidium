

package src.game.actors ;
import src.game.common.* ;
import src.util.* ;


public class Route {
  
  
  //Junction from, to ;  //TODO:  Make these final.
  final public Tile start, end ;
  final private int hash ;
  
  public Tile path[] ;
  public float cost ;
  
  
  /*
  public Route(Junction a, Junction b) {
    this(a.core, b.core) ;
    from = a ;
    to   = b ;
  }
  //*/
  
  
  //  TODO:  Delete this constructor later.
  //
  //  We have to ensure a consistent ordering here so that the results of
  //  pathing searches between the two points remain stable.
  public Route(Tile a, Tile b) {
    final int s = a.world.size ;
    final boolean flip = ((a.x * s) + a.y) > ((b.x * s) + b.y) ;
    if (flip) { start = b ; end = a ; }
    else      { start = a ; end = b ; }
    hash = Table.hashFor(start, end) ;
  }
  
  
  public boolean equals(Object o) {
    if (! (o instanceof Route)) return false ;
    final Route r = (Route) o ;
    return r.start == start && r.end == end ;
  }
  
  
  public int hashCode() {
    return hash ;
  }
}