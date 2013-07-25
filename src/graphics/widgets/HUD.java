/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.widgets ;
import src.util.* ;
import org.lwjgl.opengl.* ;
import org.lwjgl.input.Mouse ;



/**  This is the 'master' UI class.
  */
public class HUD extends UIGroup {

  
  
  final private static float
    DRAG_DIST = 3.0f,
    HOVER_DELAY = 0.5f ;
  
  private long
    hoverStart = -1 ;
  private Vec2D
    nextMP = new Vec2D(),
    dragMP = new Vec2D() ;
  private boolean
    nextMB ;
  
  private UINode
    selected ;
  private boolean
    mouseB ;
  private byte
    mouseState = HOVERED ;
  private Vec2D
    mousePos = new Vec2D() ;
  
  
  public HUD() {
    super(null) ;
  }
  
  
  public void updateInput() {
    KeyInput.updateKeyboard() ;
    
    nextMB = Mouse.isButtonDown(0) ;
    nextMP.x = Mouse.getX() ;
    nextMP.y = Mouse.getY() ;
    
    if (mouseB && (! nextMB)) {
      mouseState = HOVERED ;
    }
    if ((! mouseB) && nextMB) {
      mouseState = CLICKED ;
      dragMP.setTo(nextMP) ;
    }
    if (mouseB && nextMB && (mouseState != DRAGGED)) {
      mouseState = (dragMP.pointDist(nextMP) > DRAG_DIST) ? DRAGGED : PRESSED ;
    }
    mousePos.setTo(nextMP) ;
    mouseB = nextMB ;
  }
  
  //
  //  This is used for rendering GUI elements that share the viewport transform,
  //  but are superimposed on top of actual world-sprites.
  public void renderWorldFX() {} ;
  
  //
  //  This is used for two-dimensional GUI elements in the conventional drawing
  //  hierarchy.
  public void renderHUD(Box2D bounds) {
    relBound.set(0, 0, 1, 1) ;
    absBound.set(0, 0, 0, 0) ;
    updateAsBase(bounds) ;
    
    final UINode oldSelect = selected ;
    if ((selected == null) || (mouseState != DRAGGED)) {
      selected = selectionAt(mousePos) ;
    }
    if (mouseState != HOVERED) {
      //hoverStart = System.currentTimeMillis() ;
    }
    else if (selected != null && selected != oldSelect) {
      hoverStart = System.currentTimeMillis() ;
    }
    if (selected != null) switch (mouseState) {
      case (HOVERED) : selected.whenHovered() ; break ;
      case (CLICKED) : selected.whenClicked() ; break ;
      case (PRESSED) : selected.whenPressed() ; break ;
      case (DRAGGED) : selected.whenDragged() ; break ;
    }
    
    GL11.glMatrixMode(GL11.GL_PROJECTION) ;
    GL11.glLoadIdentity() ;
    GL11.glOrtho(
      0, bounds.xdim(),
      0, bounds.ydim(),
      -100, 100
    ) ;
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    GL11.glDisable(GL11.GL_LIGHTING) ;
    GL11.glDisable(GL11.GL_CULL_FACE) ;
    GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    GL11.glAlphaFunc(GL11.GL_GREATER, 0.05f) ;
    render() ;
  }
  
  
  
  public boolean amSelected(UINode node, byte state) {
    return (selected == node) && (mouseState == state) ;
  }
  
  
  public float timeHovered() {
    //if (mouseState != HOVERED) return -1 ;
    final long time = System.currentTimeMillis() - hoverStart ;
    return time / 1000f ;
  }
  
  
  
  public Vec2D mousePos() { return mousePos ; }
  public Vec2D dragOrigin() { return dragMP ; }
  
  public int mouseX() { return (int) mousePos.x ; }
  public int mouseY() { return (int) mousePos.y ; }
  
  
  public boolean mouseDown() { return mouseB ;  }
  public boolean mouseClicked() { return isMouseState(CLICKED) ; }
  public boolean mouseHovered() { return isMouseState(HOVERED) ; }
  public boolean mouseDragged() { return isMouseState(DRAGGED) ; }
  public boolean mousePressed() { return isMouseState(PRESSED) ; }
  
  
  public boolean isMouseState(final byte state) {
    return mouseState == state ;
  }
  
  
  public UINode selected() {
    return selected ;
  }
  
  
  public Box2D screenBounds() {
    return bounds ;
  }
}





/*
public static void attachAsListenerTo(Component component) {
  component.addMouseListener(MAIN_UI) ;
  component.addMouseMotionListener(MAIN_UI) ;
  component.addKeyListener(Keyboard.KEYBOARD) ;
}//*/
/*
private synchronized void eventGate(MouseEvent event) {
  if (event == null) {
    if (mouseB && (! nextMB)) mouseState = HOVERED ;
    if ((! mouseB) && nextMB) mouseState = CLICKED ;
    if (mouseB && nextMB && (mouseState != DRAGGED)) {
      if (mouseState != PRESSED) dragMP.setTo(nextMP) ;
      mouseState = (dragMP.dist(nextMP) > DRAG_DIST) ? DRAGGED : PRESSED ;
    }
    mousePos.setTo(nextMP) ;
    mouseB = nextMB ;

    if ((selected == null) || (mouseState != DRAGGED))
      selected = selectionAt(mousePos) ;
    if (selected != null) switch (mouseState) {
      case (HOVERED) : selected.whenHovered() ; break ;
      case (CLICKED) : selected.whenClicked() ; break ;
      case (PRESSED) : selected.whenPressed() ; break ;
      case (DRAGGED) : selected.whenDragged() ; break ;
    }
    return ;
  }
  else {
    final Point point = event.getPoint() ;
    nextMP.set(point.x, bounds.ydim() - point.y) ;
    switch (event.getID()) {
      case(MouseEvent.MOUSE_PRESSED)  : nextMB = true  ; break ;
      case(MouseEvent.MOUSE_RELEASED) : nextMB = false ; break ;
    }
  }
}

public void mouseDragged(MouseEvent event)  { eventGate(event) ; }
public void mouseMoved(MouseEvent event)    { eventGate(event) ; }
public void mouseClicked(MouseEvent event)  { eventGate(event) ; }
public void mouseEntered(MouseEvent event)  { eventGate(event) ; }
public void mouseExited(MouseEvent event)   { eventGate(event) ; }
public void mousePressed(MouseEvent event)  { eventGate(event) ; }
public void mouseReleased(MouseEvent event) { eventGate(event) ; }//*/