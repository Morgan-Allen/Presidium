/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.cutout ;
import src.graphics.common.* ;
import src.util.* ;
import org.lwjgl.opengl.GL11 ;



public class ImageSprite extends Sprite {
  
  
  private ImageModel model ;
  private float animProgress ;
  
  
  public ImageSprite(ImageModel model) {
    this.model = model ;
  }
  
  public Model model() { return model ; }
  
  public void setModel(ImageModel m) {
    this.model = m ;
  }
  
  
  public void setAnimation(String animName, float progress) {
    final Model.AnimRange range = rangeFor(animName) ;
    progress %= 1 ;
    animProgress = range.start + ((range.end - range.start) * progress) ;
  }
  
  
  
  final static int GL_DISABLES[] = new int[] { GL11.GL_LIGHTING } ;
  public int[] GL_disables() { return GL_DISABLES ; }
  
  
  public void renderTo(Rendering rendering) {
    //
    //  Since we've disabled normal lighting, we have to 'fake it' using colour
    //  parameters:
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    final Colour c = colour == null ? Colour.WHITE : colour ;
    GL11.glColor4f(c.r * fog, c.g * fog, c.b * fog, c.a) ;
    //
    //  Obtain the correct set of UV coordinates for the current frame-
    model.texture.bindTex() ;
    final float framesUV[][] = model.framesUV() ;
    final float texUV[] = framesUV[(int) (framesUV.length * animProgress)] ;
    //
    //  TODO:  Consider replacing this with an aggregate method within the
    //  MeshBuffer class?  Re-implement RenderPass, in other words.
    GL11.glBegin(GL11.GL_TRIANGLES) ;
    int tI = 0 ; for (Vec3D vert : model.coords) {
      GL11.glTexCoord2f(texUV[tI++], texUV[tI++]) ;
      GL11.glVertex3f(
        position.x + (vert.x * scale),
        position.y + (vert.y * scale),
        position.z + (vert.z * scale)
      ) ;
    }
    GL11.glEnd() ;
  }
}







