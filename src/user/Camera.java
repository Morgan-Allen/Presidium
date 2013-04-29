/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import org.lwjgl.input.Keyboard ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.KeyInput ;
import src.util.* ;


/**  This class handles camera motion around the world- also allowing for
  *  shifts in field-of-vision when an info-view is displayed.
  */
public class Camera {
  
  
  final BaseUI UI ;
  final Viewport port ;
  
  //private Element lockTarget = null ;
  private Target lockTarget = null ;
  private float lockX = 0, lockY = 0 ;
  
  
  Camera(BaseUI UI, Viewport port) {
    this.UI = UI ;
    this.port = port ;
  }
  
  void saveState(Session s) throws Exception {
    s.saveTarget(lockTarget) ;
    port.cameraPosition.saveTo(s.output()) ;
  }
  
  void loadState(Session s) throws Exception {
    lockTarget = s.loadTarget() ;
    port.cameraPosition.loadFrom(s.input()) ;
  }
  
  
  /**  Causes the camera to follow a given target and centre on the given
    *  screen coordinates.  If these are null, the screen centre is used by
    *  default.
    */
  protected void lockOn(Target target) {
    if (
      target == null
    ) {
      lockTarget = null ;
      UI.voidSelection() ;
    }
    else {
      if (
        (target instanceof Element) &&
        ((Element) target).sprite() == null
      ) { lockOn(null) ; return ; }
      lockTarget = target ;
    }
  }
  
  protected void setLockOffset(float lX, float lY) {
    lockX = lX ;
    lockY = lY ;
  }
  
  Target lockTarget() {
    return lockTarget ;
  }
  
  void pushCamera(int x, int y) {
    port.cameraPosition.x += x ;
    port.cameraPosition.y += y ;
    UI.setSelection(null) ;
    lockTarget = null ;
  }
  
  /*
  void zoomHome() {
    final Vec3D homePos = UI.homePos ;
    MainDisplay.MAIN_VIEW.cameraPosition.setTo(homePos) ;
    UI.setSelection(null) ;
    lockTarget = null ;
  }
  //*/
  
  
  /**  Updates general camera behaviour.
    */
  void updateCamera() {
    if (KeyInput.isKeyDown(Keyboard.KEY_UP   )) pushCamera(-1,  1) ;
    if (KeyInput.isKeyDown(Keyboard.KEY_DOWN )) pushCamera( 1, -1) ;
    if (KeyInput.isKeyDown(Keyboard.KEY_RIGHT)) pushCamera( 1,  1) ;
    if (KeyInput.isKeyDown(Keyboard.KEY_LEFT )) pushCamera(-1, -1) ;
    //if (KeyInput.isKeyDown(Keyboard.KEY_SPACE)) zoomHome() ;
    if (lockTarget != null) followLock() ;
  }
  
  
  /**  Zooms the camera onto the current lock-target.
    */
  private void followLock() {
    //
    //  Ascertain the difference between the current camera position and the
    //  the target's position.
    //final Vec3D lockOff = new Vec3D().set(lockX, lockY, 0) ;
    //port.flatToIso(lockOff).scale(-1f / port.screenScale()) ;
    final Vec3D
      lockPos = (lockTarget instanceof Element) ?
        ((Element) lockTarget).viewPosition(null) :
        lockTarget.position(null),
      viewPos = port.cameraPosition,
      targPos = new Vec3D().setTo(lockPos),//.add(lockOff),
      displace = targPos.sub(viewPos, new Vec3D()) ;
    final float distance = displace.length() ;
    //
    //  If distance is too large, just go straight to the point-
    if (distance > 32) {
      viewPos.add(displace) ;
    }
    else {
      //
      //  Otherwise, ascertain the rate at which one should 'drift' toward the
      //  target, and displace accordingly-
      final float drift = Math.min(1,
        ((distance + 2) * 5) / (PlayLoop.FRAMES_PER_SECOND * distance)
      ) ;
      viewPos.add(displace.scale(drift)) ;
    }
  }
}










