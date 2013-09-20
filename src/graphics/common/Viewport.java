/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package src.graphics.common ;
import org.lwjgl.opengl.* ;
import src.graphics.widgets.HUD ;
import src.util.* ;



/**  The Viewport represents a given portion of the screen, and the various view
  *  transforms for 'looking into' the world through a virtual camera.  The
  *  viewport is used to set up OpenGL perspective transforms, as well as
  *  provide basic view-culling functionality.
  */
public class Viewport {
  
  
  final public static float
    DEFAULT_SCALE = 40.0F,
    DEFAULT_ROTATE = (float) Math.toRadians(45),
    DEFAULT_ELEVATE = (float) Math.toRadians(65) ;
  final public static Viewport
    DEFAULT_VIEW = new Viewport() ;
  
  
  final public Vec3D
    cameraPosition = new Vec3D() ;
  public float
    cameraRotation = DEFAULT_ROTATE,
    cameraElevated = DEFAULT_ELEVATE,
    cameraZoom = 1.0f ;
  final Box2D viewBounds = new Box2D() ;
  
  private float
    screenX,  //middle-of-the-screen coordinates.
    screenY,
    screenS,  //screen scale,
    invertS ;  //and, finally, 1-over-screen-scale.
  private Mat3D
    viewMatrix = new Mat3D(),
    viewInvert = new Mat3D() ;
  
  
  Viewport() {
    this.updateView() ;
  }
  
  /**  Initialises this viewport for use prior to actual scene rendering (to
    *  ensure that scenegraph culling is performed correctly.  See the Display
    *  class for details.)
    */
  void updateView() {
    viewMatrix.setIdentity() ;
    viewMatrix.rotateX(cameraElevated) ;
    viewMatrix.rotateZ(cameraRotation) ;
    viewMatrix.inverse(viewInvert) ;
    screenX = (viewBounds.xpos() + viewBounds.xmax()) / 2 ;
    screenY = (viewBounds.ypos() + viewBounds.ymax()) / 2 ;  //-centre of view on screen.
    screenS = cameraZoom * DEFAULT_SCALE ;
    invertS = 1f / screenS ;
    projectMode = MODE_INIT ;
  }
  

  /**  Checks to see whether the given Box3D intersects the view bounds of this
    *  viewport.  This does not use a precise intersection formula- instead, it
    *  employs a 'bounding sphere' for the box, which is then in turn bounded
    *  with a box2d, and checked for intersection with the view bounds.  Only
    *  intended for very rough culling.
    */
  private void sphereBound(Box3D box) {
    centre.set(box.xdim(), box.ydim(), box.zdim()) ;
    centre.scale(0.5f) ;
    radius = centre.length() ;
    centre.x += box.xpos() ; centre.y += box.ypos() ; centre.z += box.zpos() ;
  }

  private static Vec3D centre = new Vec3D() ;
  private static float radius ;
  private static Box2D boxB = new Box2D() ;
  
  
  /**  Used to check if mouse position roughly intersects the given box, etc.
    */
  public boolean intersects(Box3D box, int sX, int sY) {
    sphereBound(box) ;
    isoToScreen(centre) ;
    final float x = centre.x - sX, y = centre.y - sY ;
    return Math.sqrt((x * x) + (y * y)) < radius * screenS ;
  }
  

  /**  Checks if mouse position exactly overlaps the given point+radius...
    */
  public boolean mouseIntersects(Vec3D point, float radius, HUD UI) {
    isoToScreen(centre.setTo(point)) ;
    final float x = centre.x - UI.mouseX(), y = centre.y - UI.mouseY() ;
    return Math.sqrt((x * x) + (y * y)) < radius * screenS ;
  }
  
  
  /**  Used to check if the given box roughly intersects the screen...
    */
  public boolean intersects(Box3D box) {
    sphereBound(box) ;
    return intersects(centre, radius) ;
  }
  
  
  /**  Used to check if the given point (+ radius) roughly intersects the
    *  screen.
    */
  public boolean intersects(Vec3D point, float radius) {
    isoToScreen(centre.setTo(point)) ;
    radius *= screenS ;
    boxB.set(centre.x - radius, centre.y - radius, radius * 2, radius * 2) ;
    return viewBounds.intersects(boxB) ;
  }
  
  
  /**  returns the exact ratio of in-world unit distance to pixels on-screen.
    */
  public float screenScale() {
    return screenS ;
  }
  
  
  /**  Return the current view bounds:
    */
  public int viewWide() { return (int) viewBounds.xdim() ; }
  public int viewHigh() { return (int) viewBounds.ydim() ; }
  public Box2D viewBounds() { return viewBounds ; }
  
  
  /**  Flags used to record the current projection mode.
    */
  private final static byte
    MODE_FLAT = 0,
    MODE_SCREEN = 2,
    MODE_ISOMETRIC  = 3,
    MODE_INIT = -1 ;
  private byte projectMode = MODE_INIT ;
  

  /**  Called immediately prior to sprite rendering, setting up exact OpenGL
    *  viewport bounds:
    */
  void applyView() {
    GL11.glViewport(
      (int) viewBounds.xpos(),
      (int) viewBounds.ypos(),
      (int) viewBounds.xdim(),
      (int) viewBounds.ydim()
    ) ;
  }
  
  
  /**  Used to render 2D image-based sprites, flat mode disables lighting and
    *  face culling and eliminates rotation transforms.
    */
  public void setFlatMode() {
    if (projectMode == MODE_FLAT) return ;
    GL11.glDisable(GL11.GL_CULL_FACE) ;
    doOrtho() ;
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    projectMode = MODE_FLAT ;
  }
  
  
  /**  Used to render 3D joint-based sprites, static model sprites, etc- Iso
    *  mode applies isometric view transforms and enables lighting and face
    *  culling.
    */
  public void setIsoMode() {
    if (projectMode == MODE_ISOMETRIC) return ;
    GL11.glEnable(GL11.GL_CULL_FACE) ;
    GL11.glEnable(GL11.GL_LIGHTING) ;
    doOrtho() ;
    GL11.glRotatef((float) (0 - cameraElevated * 180 / Math.PI), 1, 0, 0) ;
    GL11.glRotatef((float) (0 - cameraRotation  * 180 / Math.PI), 0, 0, 1) ;
    GL11.glTranslatef(
      0 - cameraPosition.x,
      0 - cameraPosition.y,
      0 - cameraPosition.z
    ) ;
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    projectMode = MODE_ISOMETRIC ;
  }
  
  
  /**  Similar to flat mode, but the coordinates used correspond directly with
    *  screen-pixel coordinates.
    */
  public void setScreenMode() {
    if (projectMode == MODE_SCREEN) return ;
    GL11.glDisable(GL11.GL_LIGHTING ) ;
    GL11.glDisable(GL11.GL_CULL_FACE) ;
    GL11.glMatrixMode(GL11.GL_PROJECTION) ;
    GL11.glLoadIdentity() ;
    GL11.glOrtho(
      0, viewBounds.xdim(),
      0, viewBounds.ydim(),
      -100 * screenS, 100 * screenS
    ) ;
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    projectMode = MODE_SCREEN ;
  }
  
  
  /**  Sets the OpenGL projection matrix to the correct height and width.
   */
  private void doOrtho() {
    final float
      wide = viewBounds.xdim() * 0.5f / screenS,
      high = viewBounds.ydim() * 0.5f / screenS ;
    GL11.glMatrixMode(GL11.GL_PROJECTION) ;
    GL11.glLoadIdentity() ;
    GL11.glOrtho(
      0 - wide, wide,
      0 - high, high,
      -100 / cameraZoom, 100 / cameraZoom
    ) ;
  }
  
  
  public Vec3D viewMatrix(Vec3D v) { return viewMatrix.trans(v) ; }
  public Vec3D viewInvert(Vec3D v) { return viewInvert.trans(v) ; }
  
  
  /**  Transforms the given vector from in-world isometric to flat-mode
    *  coordinates.
    */
  public Vec3D isoToFlat(Vec3D ItF) {
    ItF.sub(cameraPosition) ;
    viewMatrix.trans(ItF) ;
    return ItF ;
  }
  
  
  /**  Transforms the given vector from flat-mode to in-world isometric
    *  coordinates.
    */
  public Vec3D flatToIso(Vec3D ItF) {
    viewInvert.trans(ItF) ;
    ItF.add(cameraPosition) ;
    return ItF ;
  }
  
  
  /**  Transforms the given vector from screen-pixel to isometric coordinates.
    */
  public Vec3D screenToIso(Vec3D StI) {
    StI.x = (StI.x - screenX) * invertS ;
    StI.y = (StI.y - screenY) * invertS ;
    StI.z = StI.z * invertS / cameraZoom ;
    viewInvert.trans(StI) ;
    StI.add(cameraPosition) ;
    return StI ;
  }
  

  /**  Transforms the given vector from isometric to screen-pixel coordinates.
    */
  public Vec3D isoToScreen(Vec3D ItS) {
    ItS.sub(cameraPosition) ;
    viewMatrix.trans(ItS) ;
    ItS.x = (ItS.x * screenS) + screenX ;
    ItS.y = (ItS.y * screenS) + screenY ;
    ItS.z = ItS.z * cameraZoom * screenS ;
    return ItS ;
  }
}



