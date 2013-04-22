


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.cutout.ImageModel ;
import src.util.* ;



public class MagLine implements Placement {
  
  
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  final private Tile tempB[] = new Tile[8] ;
  
  
  private Tile offsetFor(Tile t) {
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
  
  private Tile[] linePath(Tile start, Tile end) {
    final RouteSearch search = new RouteSearch(start, end, Element.VENUE_OWNS) {
      protected boolean canEnter(Tile t) {
        for (Tile n : t.allAdjacent(tempB)) {
          if (t == null || ! super.canEnter(n)) return false ;
        }
        return super.canEnter(t) ;
      }
    } ;
    search.doSearch() ;
    return search.getPath(Tile.class) ;
  }
  
  
  
  /**  Actual placement of the line-
    */
  final static ImageModel
    NODE_MODEL = ImageModel.asIsometricModel(
      MagLine.class, "media/Buildings/vendor aura/mag_node_left.png", 1, 0.5f
    ) ;
  
  //  TODO:  You may actually want this to extend the Venue class, and behave
  //  similarly to a service hatch.  That way, you can maintain it, get
  //  automatic paving connections, and possibly link to an underground or 
  //  interior level like with excavations.  In theory.
  
  //  Or maybe mag nodes and service hatches need to be separate?
  static class MagNode extends Element {
    
    
    //final Paving paving = new Paving(this) ;
    
    MagNode() {
      super() ;
      this.attachSprite(NODE_MODEL.makeSprite()) ;
    }
    
    public MagNode(Session s) throws Exception {
      super(s) ;
    }
    
    public void saveState(Session s) throws Exception {
      super.saveState(s) ;
    }
    
    
    public int pathType() {
      return Tile.PATH_ROAD ;
    }
    
    
    public int owningType() {
      return Element.VENUE_OWNS ;
    }
    
    
    private Tile[] tilesAround() {
      final Tile around[] = new Tile[9] ;
      origin().allAdjacent(around) ;
      around[8] = this.origin() ;
      return around ;
    }
    
    
    public void enterWorldAt(int x, int y, World world) {
      super.enterWorldAt(x, y, world) ;
      world.terrain().maskAsPaved(tilesAround(), true) ;
    }
    
    
    public void exitWorld() {
      world.terrain().maskAsPaved(tilesAround(), false) ;
      super.exitWorld() ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public boolean pointsOkay(Tile points[]) {
    if (points.length != 2) return false ;
    final Tile start = offsetFor(points[0]), end = offsetFor(points[1]) ;
    final Tile path[] = linePath(start, end) ;
    return path != null ;
  }
  
  
  public void doPlace(Tile points[]) {
    final Tile start = offsetFor(points[0]), end = offsetFor(points[1]) ;
    final Tile path[] = linePath(start, end) ;
    //
    //  Having obtained the path between these points, we need to clear all
    //  affected tiles-
    final Batch <Tile> clearB = new Batch <Tile> () ;
    for (Tile t : path) {
      t.flagWith(clearB) ;
      clearB.add(t) ;
      for (Tile n : t.allAdjacent(tempB)) {
        if (n.flaggedWith() != null) continue ;
        n.flagWith(clearB) ;
        clearB.add(n) ;
      }
    }
    for (Tile t : clearB) t.flagWith(null) ;
    final Tile toClear[] = (Tile[]) clearB.toArray(Tile.class) ;
    Paving.clearRoute(toClear) ;
    //
    //  Then we install Mag Nodes at regular stops along the route-
    for (Tile t : path) {
      final MagNode node = new MagNode() ;
      node.enterWorldAt(t.x, t.y, t.world) ;
    }
  }
  
  
  public void previewPlaced(Tile points[], boolean canPlace) {
    //  TODO:  You'll have to generate the appropriate set of mag nodes, and a
    //  tile overlay for the relevant pathing area, and set that either green
    //  or red depending on whether it's legal or not.
  }
}









