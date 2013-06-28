/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import org.lwjgl.input.Keyboard ;
import src.util.* ;
import src.graphics.widgets.* ;



/**  A basic testing class used to try out viewing and rendering routines...
  */
public abstract class ViewLoop {
  
  
  public ViewLoop() {}
  
  
  protected Rendering rendering ;
  protected HUD HUD ;
  
  private boolean loop = true ;
  private int loopTime = 0 ;
  
  protected void doLoop(boolean l) { loop = l ; }
  protected int loopTime() { return loopTime ; }

  
  private boolean moused = false ;
  private float origX, origY, origR, origE ;
  
  
  public void run() {
    //
    //  Set up initial data for display:
    this.rendering = new Rendering(800, 640, 60, false) ;
    this.HUD = new HUD() ;
    setup() ;
    
    long
      lastTime,
      timeElapsed ;
    int
      loopNum = 0,
      totalTime = 0 ;
    
    while (loop) {
      lastTime = System.currentTimeMillis() ;
      //
      //  Perform the actual loop updates, as pledgeMade:
      rendering.updateViews() ;
      HUD.updateInput() ;
      final Viewport port = rendering.port ;
      if (KeyInput.isKeyDown(Keyboard.KEY_UP   )) {
        port.cameraPosition.x-- ;
        port.cameraPosition.y++ ;
      }
      if (KeyInput.isKeyDown(Keyboard.KEY_DOWN )) {
        port.cameraPosition.x++ ;
        port.cameraPosition.y-- ;
      }
      if (KeyInput.isKeyDown(Keyboard.KEY_RIGHT)) {
        port.cameraPosition.x++ ;
        port.cameraPosition.y++ ;
      }
      if (KeyInput.isKeyDown(Keyboard.KEY_LEFT )) {
        port.cameraPosition.x-- ;
        port.cameraPosition.y-- ;
      }
      if (HUD.mouseDown()) {
        if (! moused) {
          //I.add("\nBeginning mouse track...") ;
          moused = true ;
          origX = HUD.mousePos().x ;
          origY = HUD.mousePos().y ;
          origR = port.cameraRotation ;
          origE = port.cameraElevated ;
        }
        else {
          port.cameraRotation = origR + (
            (origX - HUD.mousePos().x) / 100
          ) ;
          port.cameraElevated = origE + (
            (HUD.mousePos().y - origY) / 100
          ) ;
        }
      }
      else moused = false ;
      update() ;
      rendering.assignHUD(HUD) ;
      rendering.renderDisplay() ;
      //
      //  ...Then go to sleep until the next update.
      timeElapsed = System.currentTimeMillis() - lastTime ;
      if (timeElapsed < 35) {
        try { Thread.sleep(35 - timeElapsed) ; }
        catch (InterruptedException e) {}
      }
      //
      //  Finally, periodically print out the time spent on updates:
      totalTime += timeElapsed ;
      if (loopNum++ % 100 == 0) {
        I.add("\nTime spent: " + totalTime / 100) ;
        totalTime = 0 ;
      }
      loopTime++ ;
    }
  }
  
  protected abstract void setup() ;
  protected abstract void update() ;
}
