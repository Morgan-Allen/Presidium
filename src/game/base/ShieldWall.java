


package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.util.* ;



public class ShieldWall extends Installation.Line {
  
  
  
  final Base base ;
  private Tile path[] ;
  final private Tile tempB[] = new Tile[8] ;
  
  
  public ShieldWall(Base base) {
    super(base.world) ;
    this.base = base ;
  }
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  
  private Tile[] linePath(Tile from, Tile to, boolean full) {
    if (from == null || to == null) return null ;
    final RouteSearch search = new RouteSearch(from, to, Element.VENUE_OWNS) {
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
      /*
      if (t.flaggedWith() != null || t.owner() instanceof MagNode) continue ;
      t.flagWith(clearB) ;
      clearB.add(t) ;
      for (Tile n : t.allAdjacent(tempB)) if (n != null) {
        if (n.flaggedWith() != null || n.owner() instanceof MagNode) continue ;
        n.flagWith(clearB) ;
        clearB.add(n) ;
      }
      //*/
    }
    for (Tile t : clearB) t.flagWith(null) ;
    return clearB ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = linePath(from, to, false) ;
    if (path == null) return null ;
    final Batch <Element> nodes = new Batch <Element> () ;
    /*
    for (Tile t : path) {
      final MagNode node = new MagNode() ;
      node.setPosition(t.x, t.y, t.world) ;
      nodes.add(node) ;
    }
    for (Tile t : path) t.flagWith(this) ;
    for (MagNode node : nodes) node.updateSprite() ;
    for (Tile t : path) t.flagWith(null) ;
    //*/
    return nodes ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Shield Wall" ;
  }
  
  
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/shield_wall_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Shield Walls are defensive emplacements that improve base security." ;
  }
}




