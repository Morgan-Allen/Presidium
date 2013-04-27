


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.util.* ;



public class MagLine extends Installation.Line {
  
  
  final Base base ;
  private Tile path[] ;
  final private Tile tempB[] = new Tile[8] ;
  
  
  public MagLine(Base base) {
    super(base.world) ;
    this.base = base ;
  }
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  private Tile offsetFor(Tile t) {
    if (t == null) return null ;
    if (! (t.owner() instanceof Venue)) return t ;
    final Venue v = (Venue) t.owner() ;
    v.entrance().edgeAdjacent(tempB) ;
    for (int i = 4 ; i-- > 0 ;) {
      if (tempB[i] != null && tempB[i].owner() == v) {
        return tempB[(i + 2) % 4] ;
      }
    }
    return null ;
  }
  
  
  private Tile[] linePath(Tile from, Tile to, boolean full) {
    final Tile start = offsetFor(from), end = offsetFor(to) ;
    if (start == null || end == null) return null ;
    final RouteSearch search = new RouteSearch(start, end, Element.VENUE_OWNS) {
      protected boolean canEnter(Tile t) {
        for (Tile n : t.allAdjacent(tempB)) {
          if (n == null) return false ;
          if (n.owner() instanceof MagLineNode || super.canEnter(n)) continue ;
          return false ;
        }
        return t.owner() instanceof MagLineNode || super.canEnter(t) ;
      }
    } ;
    search.doSearch() ;
    if (full) return search.fullPath(Tile.class) ;
    else return search.bestPath(Tile.class) ;
  }
  
  
  protected Batch <Tile> toClear(Tile from, Tile to) {
    path = linePath(from, to, false) ;
    if (path == null) return null ;
    final Batch <Tile> clearB = new Batch <Tile> () ;
    for (Tile t : path) {
      if (t.flaggedWith() != null || t.owner() instanceof MagLineNode) continue ;
      t.flagWith(clearB) ;
      clearB.add(t) ;
      for (Tile n : t.allAdjacent(tempB)) if (n != null) {
        if (n.flaggedWith() != null || n.owner() instanceof MagLineNode) continue ;
        n.flagWith(clearB) ;
        clearB.add(n) ;
      }
    }
    for (Tile t : clearB) t.flagWith(null) ;
    return clearB ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = linePath(from, to, false) ;
    if (path == null) return null ;
    final Batch <MagLineNode> nodes = new Batch <MagLineNode> () ;
    for (Tile t : path) {
      final MagLineNode node = new MagLineNode() ;
      node.setPosition(t.x, t.y, t.world) ;
      nodes.add(node) ;
    }
    for (Tile t : path) t.flagWith(this) ;
    for (MagLineNode node : nodes) node.updateSprite() ;
    for (Tile t : path) t.flagWith(null) ;
    return (Batch <Element>) (Batch) nodes ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Mag Line" ;
  }
  
  
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/mag_line_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Mag Lines facilitate transport between distant sectors of your base,"+
      "along with distribution of water, power and life support." ;
  }
}





