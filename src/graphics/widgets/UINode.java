/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.*;

import org.lwjgl.opengl.* ;


public abstract class UINode {
  
  
  final public static byte
    HOVERED = 0,
    CLICKED = 1,
    PRESSED = 2,
    DRAGGED = 3 ;
  
  final public Box2D
    relBound = new Box2D(),
    absBound = new Box2D() ;
  public float relDepth = 0, absDepth = 0 ;
  final protected Box2D
    bounds = new Box2D() ;
  public boolean
    hidden = false ;
  
  final protected HUD myHUD ;
  private UIGroup parent ;
  private ListEntry <UINode> kidEntry ;
  
  
  public UINode(HUD myHUD) {
    this.myHUD = myHUD ;
  }
  
  protected abstract void render() ;
  
  protected String info() { return null ; }
  
  protected UINode selectionAt(Vec2D mousePos) {
    return (bounds.contains(mousePos.x, mousePos.y)) ? this : null ;
  }
  
  public float xpos() { return bounds.xpos() ; }
  public float ypos() { return bounds.ypos() ; }
  public float xdim() { return bounds.xdim() ; }
  public float ydim() { return bounds.ydim() ; }
  
  Box2D fullBounds() { return bounds ; }
  
  
  
  /**  Methods to control the order of rendering for UINodes.
    */
  public void attachTo(UIGroup group) {
    detach() ;
    parent = group ;
    kidEntry = parent.kids.addLast(this) ;
  }
  
  
  public void detach() {
    if (parent == null) return ;
    parent.kids.removeEntry(kidEntry) ;
    kidEntry = null ;
    parent = null ;
  }
  
  
  protected void updateState() {
    absDepth = relDepth + (parent == null ? 0 : parent.absDepth) ;
  }
  
  
  /**  Sets the absolute size and relative (to parent) position of this node.
    */
  protected void updateRelativeParent() {
    if (parent == null) updateRelativeParent(new Box2D()) ;
    else updateRelativeParent(parent.bounds) ;
  }
  
  void updateRelativeParent(Box2D base) {
    bounds.xdim(absBound.xdim() + (base.xdim() * relBound.xdim())) ;
    bounds.ydim(absBound.ydim() + (base.ydim() * relBound.ydim())) ;
    bounds.xpos(absBound.xpos()) ;
    bounds.ypos(absBound.ypos()) ;
  }
  
  /**  Sets the absolute position and bounds of this node.
    */
  protected void updateAbsoluteBounds() {
    if (parent == null) updateAbsoluteBounds(new Box2D()) ;
    else updateAbsoluteBounds(parent.bounds) ;
  }
  
  void updateAbsoluteBounds(Box2D base) {
    bounds.xpos(bounds.xpos() + base.xpos() + (relBound.xpos() * base.xdim())) ;
    bounds.ypos(bounds.ypos() + base.ypos() + (relBound.ypos() * base.ydim())) ;
  }
  
  
  protected void whenHovered() {}
  protected void whenClicked() {}
  protected void whenPressed() {}
  protected void whenDragged() {}
  protected boolean amHovered() { return myHUD.amSelected(this, HOVERED) ; }
  protected boolean amClicked() { return myHUD.amSelected(this, CLICKED) ; }
  protected boolean amPressed() { return myHUD.amSelected(this, PRESSED) ; }
  protected boolean amDragged() { return myHUD.amSelected(this, DRAGGED) ; }
  
  
  final protected void drawQuad(
    float xmin, float ymin,
    float xmax, float ymax,
    float umin, float vmin,
    float umax, float vmax
  ) {
    //I.say("\nquad bounds: " + xmin + " " + ymin + " " + xmax + " " + ymax);
    //GL rGL = UI.renderGL ;
    //if (absShade != -1)
      //GL11.glColor3f(absShade, absShade, absShade) ;
    GL11.glTexCoord2f(umin, vmax) ;
    GL11.glVertex3f(xmin, ymin, absDepth) ;
    GL11.glTexCoord2f(umin, vmin) ;
    GL11.glVertex3f(xmin, ymax, absDepth) ;
    GL11.glTexCoord2f(umax, vmin) ;
    GL11.glVertex3f(xmax, ymax, absDepth) ;
    GL11.glTexCoord2f(umax, vmax) ;
    GL11.glVertex3f(xmax, ymin, absDepth) ;
  }
}
