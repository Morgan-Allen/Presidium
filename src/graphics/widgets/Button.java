/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;
import src.graphics.common.* ;
import org.lwjgl.opengl.* ;




public class Button extends Image {
  
  /**  
    */
  final public static Texture
    ICON_LIT_TEX     = Texture.loadTexture("media/GUI/iconLit.gif"),
    TRIANGLE_LIT_TEX = Texture.loadTexture("media/GUI/triangle_tab_glow.png"),
    TRI_INV_LIT_TEX  = Texture.loadTexture("media/GUI/tri_inv_tab_glow.png") ;
  
  //  selection modes.
  final public static byte
    MODE_RADIUS = 0,
    MODE_BOUNDS = 1,
    MODE_ALPHA = 2 ;
  
  
  public float
    hoverLit = 0.5f,
    pressLit = 0.75f ;  //default values...
  public byte selectMode = MODE_BOUNDS ;
  
  protected Texture highlit ;
  protected String info ;
  //private float litAlpha = 0 ;
  
  
  public Button(HUD myHUD, String norm, String infoS) {
    this(
      myHUD,
      Texture.loadTexture(norm),
      ICON_LIT_TEX,
      infoS
    ) ;
  }
  
  public Button(HUD myHUD, String path, String norm, String lit, String infoS) {
    this(
      myHUD,
      Texture.loadTexture(path+norm),
      Texture.loadTexture(path+lit),
      infoS
    ) ;
  }
  
  public Button(HUD myHUD, Texture norm, Texture lit, String infoS) {
    super(myHUD, norm) ;
    info = infoS ;
    highlit = lit ;
  }
  
  
  public void setHighlight(Texture h) {
    highlit = h ;
  }
  
  
  protected String info() {
    return info ;
  }
  
  
  
  /**  UI method overrides/implementations-
    */
  protected UINode selectionAt(Vec2D mousePos) {
    if (super.selectionAt(mousePos) == null) return null ;
    if (selectMode == MODE_BOUNDS) {
      return this ;
    }
    if (selectMode == MODE_RADIUS) {
      final float radius = Math.max(bounds.xdim(), bounds.ydim()) / 2 ;
      return (bounds.centre().pointDist(mousePos) < radius) ? this : null ;
    }
    if (selectMode == MODE_ALPHA) {
      final float
        tU = ((mousePos.x - bounds.xpos()) / bounds.xdim()),
        tV = ((mousePos.y - bounds.ypos()) / bounds.ydim()) ;
      final Colour texSample = texture.getColour(tU, tV) ;
      return (texSample.a > 0.5f) ? this : null ;
    }
    return null ;
  }
  

  protected void render() {
    final float FADE_TIME = 0.25f ;
    super.render() ;
    final Texture realTex = texture ;
    final float realAlpha = absAlpha ;
    texture = highlit ;
    if (amPressed() || amDragged() || amClicked()) {
      absAlpha *= pressLit ;
      super.render() ;
    }
    if (amHovered()) {
      absAlpha *= hoverLit ;
      absAlpha *= Visit.clamp(myHUD.timeHovered() / FADE_TIME, 0, 1) ;
      super.render() ;
    }
    absAlpha = realAlpha ;
    texture = realTex ;
  }
}





