/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;
import src.graphics.common.* ;
import org.lwjgl.opengl.* ;



/**  Not- images set their own dimensions to match that of their texture (times
  *  scale.)  If you wish to disable this behaviour, set scale to zero.
  */
public class Image extends UINode {
  
  
  public boolean stretch = false ;
  protected Texture texture ;
  
  
  public Image(HUD myHUD, String textureName) {
    this(myHUD, Texture.loadTexture(textureName)) ;
  }
  
  
  public Image(HUD myHUD, Texture t) {
    super(myHUD) ;
    texture = t ;
  }
  
  
  protected void render() {
    renderIn(bounds, texture, null) ;
  }
  
  
  protected void renderIn(Box2D area, Texture tex, Box2D UV) {
    final float scale = stretch ? 1 : Math.min(
      bounds.xdim() / texture.xdim(),
      bounds.ydim() / texture.ydim()
    ) ;
    GL11.glColor4f(1, 1, 1, absAlpha) ;
    
    final Box2D drawn = new Box2D().set(
      area.xpos(), area.ypos(),
      stretch ? area.xdim() : (texture.xdim() * scale),
      stretch ? area.ydim() : (texture.ydim() * scale)
    ) ;
    if (UV == null) UV = new Box2D().set(0, 0, tex.maxU(), tex.maxV()) ;
    
    tex.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    drawQuad(
      drawn.xpos(), drawn.ypos(),
      drawn.xmax(), drawn.ymax(),
      UV.xpos(), UV.ypos(),
      UV.xmax(), UV.ymax(),
      absDepth
    ) ;
    GL11.glEnd() ;
  }
}


