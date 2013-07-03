/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.user.BaseUI;
import src.util.* ;
import src.graphics.common.* ;
import org.lwjgl.opengl.* ;


public class Button extends Image {
  
  
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
  
  
  public Button(HUD myHUD, String norm, String infoS) {
    this(
      myHUD,
      Texture.loadTexture(norm),
      BaseUI.ICON_LIT_TEX,
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
  
  
  protected String info() {
    return info ;
  }
  
  
  protected void render() {
    super.render() ;
    final Texture realTex = texture ;
    final float realAlpha = alpha ;
    
    texture = highlit ;
    if (amPressed() || amDragged() || amClicked()) {
      alpha *= pressLit ;
      super.render() ;
    }
    if (amHovered()) {
      alpha *= hoverLit ;
      super.render() ;
    }
    alpha = realAlpha ;
    texture = realTex ;
  }
}

