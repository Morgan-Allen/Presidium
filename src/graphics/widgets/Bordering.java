/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;
import src.graphics.common.* ;
import org.lwjgl.opengl.GL11 ;


public class Bordering extends UINode {
  
  
  final Texture borderTex ;
  final public Box2D
    texInset = new Box2D().set(0.25f, 0.25f, 0.5f, 0.5f),
    drawInset = new Box2D().set(-10, -10, 20, 20) ;
  
  
  public Bordering(HUD myHUD, Texture tex) {
    super(myHUD) ;
    this.borderTex = tex ;
  }
  
  
  protected void render() {
    final float
      coordX[] = {
        drawInset.xpos(), 0,
        this.xdim(), this.xdim() + drawInset.xmax()
      },
      coordY[] = {
        drawInset.ypos(), 0,
        this.ydim(), this.ydim() + drawInset.ymax()
      },
      coordU[] = {
        0, texInset.xpos(),
        texInset.xmax(), 1
      },
      coordV[] = {
        0, texInset.ypos(),
        texInset.ymax(), 1
      } ;
    
    for (int i = 4 ; i-- > 0 ;) {
      coordX[i] += xpos() ;
      coordY[i] = ypos() + ydim() - coordY[i] ;
      coordU[i] *= borderTex.maxU() ;
      coordV[i] *= borderTex.maxV() ;
    }
    
    borderTex.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    for (int x = 3 ; x-- > 0 ;) for (int y = 3 ; y-- > 0 ;) {
      drawQuad(
        coordX[x], coordY[y], coordX[x + 1], coordY[y + 1],
        coordU[x], coordV[y + 1], coordU[x + 1], coordV[y]
      ) ;
    }
    GL11.glEnd() ;
  }
}



/*
public void enclose(UIGroup group) {
  this.absBound.set(
    0 - insetBox.xpos(),
    0 - insetBox.ypos(),
    insetBox.xpos() + borderTex.xdim() - insetBox.xmax(),
    insetBox.ypos() + borderTex.ydim() - insetBox.ymax()
  ) ;
  this.relBound.set(0, 0, 1, 1) ;
  this.attachTo(group) ;
}
//*/
/*
final private static float
  coordX[] = new float[4],
  coordY[] = new float[4],
  coordU[] = new float[4],
  coordV[] = new float[4] ;
//*/