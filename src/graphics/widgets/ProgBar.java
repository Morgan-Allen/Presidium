

package src.graphics.widgets ;
import src.graphics.common.* ;
import src.util.* ;
import org.lwjgl.opengl.* ;



public class ProgBar extends UINode {
  
  
  public Colour backC = Colour.BLACK, barC = Colour.GREEN ;
  public float level = 0 ;
  
  
  public ProgBar(HUD myHUD) {
    super(myHUD) ;
  }


  protected void render() {
    renderSpectrum(this.bounds, 0, barC, backC, level) ;
  }
  
  
  public static void renderSpectrum(
    Box2D area, float zoff, Colour barC, Colour backC, float level
  ) {
    Texture.WHITE_TEX.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    final int
      x  = (int) area.xpos(), y  = (int) area.ypos(),
      xd = (int) area.xdim(), yd = (int) area.ydim() ;
    //
    //  First, do the background-
    backC.bindColour() ;
    UINode.drawQuad(
      x, y, x + xd, y + yd,
      0, 0, 1, 1, zoff
    ) ;
    //
    //  Then, the filled section-
    barC.bindColour() ;
    UINode.drawQuad(
      x, y, x + (int) (xd * level), y + yd,
      0, 0, 1, 1, zoff
    ) ;
    //
    //  And finish off-
    GL11.glEnd() ;
  }
}




