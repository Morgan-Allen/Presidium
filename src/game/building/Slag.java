


package src.game.building ;
import src.game.base.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;




public class Slag extends Element {
  
  
  /**  Construction and save/load routines-
    */
  final public static Model
    SLAG_MODELS[][] = ImageModel.fromTextureGrid(
      Slag.class, Texture.loadTexture("media/terrain/slag_heaps.png"), 2, 1.2f
    ) ;
  
  
  final boolean permanent ;
  final float size ;
  
  
  public Slag(boolean permanent, float size) {
    super() ;
    this.permanent = permanent ;
    this.size = size ;
    final Model model = Rand.yes() ?
      SLAG_MODELS[Rand.index(2)][Rand.index(2)] :
      Holding.EXTRA_MODELS[0][Rand.index(3)] ;
    attachSprite(model.makeSprite()) ;
  }
  
  
  public Slag(Session s) throws Exception {
    super(s) ;
    permanent = s.loadBool() ;
    size = s.loadFloat() ;
  }
  

  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveBool(permanent) ;
    s.saveFloat(size) ;
  }
  
  
  
  /**  Physical properties, placement and behaviour-
    */
  public static void reduceToSlag(Box2D area, World world) {
    for (Tile t : world.tilesIn(area, true)) {
      final Slag heap = new Slag(false, 0.5f + Rand.num()) ;
      heap.enterWorldAt(t.x, t.y, world) ;
    }
  }
  
  
  public void onGrowth() {
    if (permanent) return ;
    setAsDestroyed() ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    sprite().scale = size ;
    super.renderFor(rendering, base) ;
  }
}






