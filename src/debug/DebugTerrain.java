

package src.debug ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.util.* ;
import org.lwjgl.opengl.* ;




public class DebugTerrain extends ViewLoop {
  

  
  public static void main(String a[]) {
    DebugTerrain DF = new DebugTerrain() ;
    DF.run() ;
  }
  
  
  final FogOverlay fogOver = new FogOverlay(32) ;
  final long initTime = System.nanoTime() ;
  private float lastTime = -1 ;
  
  
  protected void setup() {
    rendering.port.cameraPosition.set(16, 16, 0) ;
    final float newVals[][] = new float[32][32] ;
    for (Coord c : Visit.grid(0, 0, 32, 32, 1)) {
      newVals[c.x][c.y] = (float) Math.random() ;
    }
    fogOver.assignNewVals(newVals) ;
  }
  

  protected void update() {
    final long timeSpent = System.nanoTime() - initTime ;
    final float realTime = ((int) (timeSpent / 1000)) / 2000000f ;
    
    GL11.glClearColor(0.0f, 1.0f, 1.0f, 1.0f) ;
    
    if (((int) lastTime) != ((int) realTime)) {
      final float newVals[][] = new float[32][32] ;
      for (Coord c : Visit.grid(0, 0, 32, 32, 1)) {
        newVals[c.x][c.y] = (float) Math.random() / 2 ;
      }
      fogOver.assignNewVals(newVals) ;
    }
    fogOver.assignFadeVal(realTime % 1) ;
    lastTime = realTime ;
    
    rendering.addClient(fogOver) ;
  }
}











