/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.graphics.cutout.* ;
import src.util.* ;


//
//  TODO:  You'll have to replace this with a GameLoop subclass later.
public class TestPlanet extends ViewLoop {
  
  
  public static void main(String args[]) {
    TestPlanet test = new TestPlanet() ;
    test.run() ;
  }
  
  private Terrain terrain ;
  private List <ImageSprite> sprites ;
  
  
  public void setup() {
    final int size = 64 ;
    final TerrainGen TG = new TerrainGen(
      64, 0.33f,
      Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    
    terrain = TG.generateTerrain() ;
    terrain.initPatchGrid(16) ;
    rendering.port.cameraPosition.set(size / 2, size / 2, 0) ;
    
    sprites = new List <ImageSprite> () {
      protected float queuePriority(ImageSprite r) {
        return r.depth ;
      }
    } ;
    
    //rendering.lighting.direct(new Vec3D(-1, -1, 1)) ;
    /*
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final Habitat h = terrain.habitatAt(c.x, c.y) ;
      if (h.floraModels == null) continue ;
      final float height = terrain.trueHeight(c.x, c.y) ;
      final float growChance = h.moisture / 10 ;
      final ImageModel model = h.floraModels[Rand.index(4)][Rand.index(4)] ;
      if (Rand.num() < (growChance * growChance)) {
        final ImageSprite sprite = new ImageSprite(model) ;
        sprite.position.set(c.x, c.y, height) ;
        sprites.add(sprite) ;
      }
    }
    //*/
  }
  
  
  
  protected void update() {
    final int size = terrain.mapSize ;
    final float time = System.currentTimeMillis() / 1000f ;
    terrain.renderFor(new Box2D().set(0, 0, size, size), rendering, time) ;
    Vec3D deep = new Vec3D() ;
    for (Sprite sprite : sprites) {
      Viewport.DEFAULT_VIEW.viewMatrix(deep.setTo(sprite.position)) ;
      sprite.depth = 0 - deep.z ;
    }
    sprites.queueSort() ;
    //rendering.clearDepth() ;
    for (ImageSprite sprite : sprites) {
      rendering.addClient(sprite) ;
    }
  }
}









