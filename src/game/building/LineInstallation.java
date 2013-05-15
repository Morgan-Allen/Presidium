


package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.util.* ;



public abstract class LineInstallation implements Installation, TileConstants {
  
  
  /**  Fields, constructors, and interface contract-
    */
  
  final World world ;
  protected Batch <Tile> toClear ;
  protected Batch <Element> toPlace ;
  
  //  TODO:  Have the exception-classes used specified in the constructor?
  private Tile tempB[] = new Tile[9] ;
  
  
  protected LineInstallation(World world) {
    this.world = world ;
  }
  
  
  protected abstract Batch <Tile> toClear(Tile from, Tile to) ;
  protected abstract Batch <Element> toPlace(Tile from, Tile to) ;
  
  
  /**  Utility methods specifically for handling element orientation-
    */
  /*
  protected int[] facingsFor(Tile path[], TileMask mask) {
    final int facings[] = new int[path.length] ;
    final boolean near[] = new boolean[8] ;
    for (Tile t : path) t.flagWith(this) ;
    for (int i = path.length ; i-- > 0 ;) {
      path[i].allAdjacent(tempB) ;
      for (int n : N_INDEX) {
        final Tile nT = tempB[n] ;
        near[n] = nT == null ? false : mask.maskAt(nT.x, nT.y) ;
      }
      facings[i] = TerrainPattern.simpleLineIndex(near) ;
    }
    for (Tile t : path) t.flagWith(null) ;
    return facings ;
  }
  //*/
  
  
  /**  Utility methods helpful for obtaining common results-
    */
  protected Tile[] lineVicinityPath(
    Tile from, Tile to, boolean full,
    final boolean allowNulls, final Class... exceptions
  ) {
    if (from == null || to == null) return null ;
    final RouteSearch search = new RouteSearch(from, to, Element.VENUE_OWNS) {
      protected boolean canEnter(Tile t) {
        if (t == null) return false ;
        for (Tile n : t.vicinity(tempB)) {
          if (n == null) {
            if (allowNulls) continue ;
            else return false ;
          }
          boolean skip = false ;
          if (n.owner() != null) for (Class c : exceptions) {
            if (n.owner().getClass() == c) { skip = true ; break ; }
          }
          if (skip || super.canEnter(n)) continue ;
          return false ;
        }
        return true ;
      }
    } ;
    search.doSearch() ;
    if (full) return search.fullPath(Tile.class) ;
    else return search.bestPath(Tile.class) ;
  }
  
  
  protected Batch <Tile> lineVicinity(
    Tile path[], Class... exceptions
  ) {
    if (path == null) return null ;
    final Batch <Tile> clearB = new Batch <Tile> () ;
    for (Tile t : path) for (Tile n : t.vicinity(tempB)) if (n != null) {
      if (n.flaggedWith() != null) continue ;
      boolean skip = false ;
      if (n.owner() != null) for (Class c : exceptions) {
        if (n.owner().getClass() == c) { skip = true ; break ; }
      }
      if (skip) continue ;
      n.flagWith(clearB) ;
      clearB.add(n) ;
    }
    for (Tile t : clearB) t.flagWith(null) ;
    return clearB ;
  }
  
  
  /**  Direct implementation of Installation interface-
    */
  public boolean pointsOkay(Tile from, Tile to) {
    toClear = toClear(from, to) ;
    toPlace = toPlace(from, to) ;
    if (toClear == null || toPlace == null) return false ;
    return true ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (toClear == null || toPlace == null) return ;
    Paving.clearRoute((Tile[]) toClear.toArray(Tile.class)) ;
    for (Element e : toPlace) {
      final Tile o = e.origin() ;
      if (o.owner() != null) o.owner().exitWorld() ;
      e.sprite().colour = null ;
      e.enterWorldAt(o.x, o.y, o.world) ;
    }
  }
  
  
  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {
    if (toClear == null || toPlace == null) return ;
    final TerrainMesh overlay = world.terrain().createOverlay(
      world, (Tile[]) toClear.toArray(Tile.class), true, Texture.WHITE_TEX
    ) ;
    overlay.colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(overlay) ;
    
    for (Element e : toPlace) {
      e.position(e.sprite().position) ;
      e.sprite().colour = canPlace ? Colour.GREEN : Colour.RED ;
      rendering.addClient(e.sprite()) ;
    }
  }
}
