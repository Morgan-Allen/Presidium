


package src.game.building ;
import src.game.base.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;




//
//  TODO:  Allow slag to be of different sizes!  Up to 2x2, anyway.

public class Wreckage extends Fixture {
  
  
  /**  Construction and save/load routines-
    */
  final public static Model
  SLAG_MODELS[][] = ImageModel.fromTextureGrid(
    Wreckage.class,
    Texture.loadTexture("media/Buildings/lairs and ruins/all_wreckage.png"),
    3, 1.0f
  ) ;
  
  
  final boolean permanent ;
  private float spriteSize ;
  
  
  public Wreckage(boolean permanent, int size) {
    super(size, size / 2) ;
    this.permanent = permanent ;
    final int tier = size > 1 ? Rand.index(2) : Rand.index(2) ;
    final Model model = SLAG_MODELS[Rand.index(3)][tier] ;
    attachSprite(model.makeSprite()) ;
    spriteSize = (size + Rand.num()) / 2f ;
  }
  
  
  public Wreckage(Session s) throws Exception {
    super(s) ;
    permanent = s.loadBool() ;
    spriteSize = s.loadFloat() ;
  }
  

  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveBool(permanent) ;
    s.saveFloat(spriteSize) ;
  }
  
  
  public int owningType() {
    return Element.ELEMENT_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }


  
  /**  Physical properties, placement and behaviour-
    */
  public static void reduceToSlag(Box2D area, World world) {
    for (Tile t : world.tilesIn(area, true)) {
      //  TODO:  Allow for 2x2 wreckage here...
      final Wreckage heap = new Wreckage(false, 1) ;
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base) ;
    sprite().position.z -= 0.25f ;
    sprite().scale = spriteSize ;
  }
}



