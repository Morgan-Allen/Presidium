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
  
  
  public float alpha = 1.0f ;
  public boolean stretch = false ;
  protected Texture texture ;
  
  
  public Image(HUD myHUD, String textureName) {
    this(myHUD, Texture.loadTexture(textureName)) ;
  }
  
  public Image(HUD myHUD, Texture t) {
    super(myHUD) ;
    texture = t ;
  }
  
  //  Scale the image to fit within the bounds.
  protected void render() {
    final float scale = stretch ? 1 : Math.min(
      bounds.xdim() / texture.xdim(),
      bounds.ydim() / texture.ydim()
    ) ;
    final float
      xmax = stretch ? bounds.xmax() : (bounds.xpos() + texture.xdim() * scale),
      ymax = stretch ? bounds.ymax() : (bounds.ypos() + texture.ydim() * scale) ;
    texture.bindTex() ;
    GL11.glColor4f(1, 1, 1, alpha) ;
    GL11.glBegin(GL11.GL_QUADS) ;
    drawQuad(
      xpos(), ypos(),
      xmax, ymax,
      0, 0,
      texture.maxU(), texture.maxV()
    ) ;
    GL11.glEnd() ;
  }
}


