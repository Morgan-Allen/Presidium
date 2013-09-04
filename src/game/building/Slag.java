


package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;




public class Slag extends Element {
  
  
  
  final public static Model
    SLAG_MODELS[][] = ImageModel.fromTextureGrid(
      Slag.class, Texture.loadTexture("media/terrain/slag_heaps.png"), 2, 1.20f
    ) ;
  
  
  public Slag() {
    super() ;
    attachSprite(SLAG_MODELS[Rand.index(2)][Rand.index(2)].makeSprite()) ;
  }
  
  public Slag(Session s) throws Exception {
    super(s) ;
  }

  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  public static void reduceToSlag(Box2D area, World world) {
    for (Tile t : world.tilesIn(area, true)) {
      final Slag heap = new Slag() ;
      heap.enterWorldAt(t.x, t.y, world) ;
    }
  }
  
  
  public void onGrowth() {
    setAsDestroyed() ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
  }
}






