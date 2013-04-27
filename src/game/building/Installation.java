

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.TerrainMesh ;
import src.util.* ;



/**  This class is intended specifically to work with the BuildingsTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Installation {
  
  
  boolean pointsOkay(Tile from, Tile to) ;
  void doPlace(Tile from, Tile to) ;
  void preview(boolean canPlace, Rendering rendering, Tile from, Tile to) ;
  
  String fullName() ;
  Texture portrait() ;
  String helpInfo() ;
  
  
  
  public abstract static class Line implements Installation {
    
    final World world ;
    protected Batch <Tile> toClear ;
    protected Batch <Element> toPlace ;
    
    
    protected Line(World world) {
      this.world = world ;
    }
    
    
    protected abstract Batch <Tile> toClear(Tile from, Tile to) ;
    protected abstract Batch <Element> toPlace(Tile from, Tile to) ;
    

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
       (Tile[]) toClear.toArray(Tile.class), Texture.WHITE_TEX
      ) ;
      /*
      final Table <Tile, Tile> pathTable = new Table(toClear.size()) ;
      Box2D area = null ;
      for (Tile t : toClear) {
        if (area == null) area = new Box2D().set(t.x, t.y, 0, 0) ;
        pathTable.put(t, t) ;
        area.include(t.x, t.y, 0.5f) ;
      }
      final TerrainMesh overlay = world.terrain().createOverlay(
        area, Texture.WHITE_TEX,
        new TerrainMesh.Mask() { protected boolean maskAt(int x, int y) {
          final Tile t = world.tileAt(x, y) ;
          return (t == null) ? false : (pathTable.get(t) != null) ;
        } }
      ) ;
      //*/
      overlay.colour = canPlace ? Colour.GREEN : Colour.RED ;
      rendering.addClient(overlay) ;
      
      for (Element e : toPlace) {
        e.position(e.sprite().position) ;
        e.sprite().colour = canPlace ? Colour.GREEN : Colour.RED ;
        rendering.addClient(e.sprite()) ;
      }
    }
  }
}












