

package src.debug ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.graphics.terrain.* ;
import src.util.* ;

import org.lwjgl.opengl.* ;




public class DebugTerrain extends ViewLoop implements TileConstants {
  

  
  public static void main(String a[]) {
    DebugTerrain DF = new DebugTerrain() ;
    DF.run() ;
  }
  
  
  final FogOverlay fogOver = new FogOverlay(32) ;
  final long initTime = System.nanoTime() ;
  private float lastTime = -1 ;
  
  Batch <Sprite> sprites = new Batch <Sprite> () ;
  
  
  
  
  protected void setup() {
    rendering.port.cameraPosition.set(12, 12, 0) ;
    final float newVals[][] = new float[32][32] ;
    for (Coord c : Visit.grid(0, 0, 32, 32, 1)) {
      newVals[c.x][c.y] = (float) Math.random() ;
    }
    fogOver.assignNewVals(newVals) ;
    
    final PassageFX passageFX = new PassageFX() ;
    final Box2D area = new Box2D().set(8.5f, 8.5f, 8, 8) ;
    final byte maskB[][] = {
        { 0, 1, 1, 1 },
        { 0, 1, 1, 1 },
        { 1, 1, 1, 1 },
        { 1, 1, 0, 0 }
    } ;
    final TileMask mask = new TileMask() {
      public boolean maskAt(int x, int y) {
        try { return maskB[x - 9][y - 9] == 1 ; }
        catch (Exception e) { return false ; }
        //return area.contains(x, y) ;
      }
    } ;
    final Texture tex = Texture.loadTexture(
      "media/Buildings/artificer/excavation_2.png"
    ) ;
    passageFX.setupWithArea(area, mask, tex) ;
    
    sprites.add(passageFX) ;
  }
  

  protected void update() {
    final long timeSpent = System.nanoTime() - initTime ;
    final float realTime = ((int) (timeSpent / 1000)) / 2000000f ;
    
    GL11.glClearColor(0.0f, 1.0f, 1.0f, 1.0f) ;
    //
    //  TODO:  Introduce some actual terrain here.
    
    /*
    rendering.lighting.direct(
      rendering.port.viewInvert(new Vec3D(0, 0, 1))
    ) ;
    //*/
    
    if (((int) lastTime) != ((int) realTime)) {
      final float newVals[][] = new float[32][32] ;
      for (Coord c : Visit.grid(0, 0, 32, 32, 1)) {
        newVals[c.x][c.y] = (float) Math.random() / 2 ;
      }
      fogOver.assignNewVals(newVals) ;
    }
    fogOver.assignFadeVal(realTime % 1) ;
    lastTime = realTime ;
    
    for (Sprite s : sprites) rendering.addClient(s) ;
    rendering.addClient(fogOver) ;
  }
}











