

package src.graphics.space ;
import src.graphics.common.* ;
import src.graphics.cutout.ImageModel;
//import src.graphics.widgets.* ;
import src.util.* ;

import org.lwjgl.opengl.* ;



//
//  So, do you implement rendering.client, or extend UINode?

public class Starfield implements Rendering.Client {
  
  
  
  final static String
    IMG_DIR = "media/GUI/starcharts/" ;
  final static Texture
    //
    //  TODO:  You might also need textures for selection and showing jump
    //  routes, et cetera.
    STAR_TEX = Texture.loadTexture(IMG_DIR+"star_burst.png"),
    AXIS_TEX = Texture.loadTexture(IMG_DIR+"sky_axis.png") ;
  
  final public static ImageModel
    STAR_MODEL = ImageModel.asFlatModel(Starfield.class, STAR_TEX, 1) ;
  

  final public Viewport starPort = new Viewport() ;
  public float fieldElevate, fieldRotate ;
  private Mat3D transform = new Mat3D() ;
  private MeshBuffer geomBuffer ;
  
  private class Star {
    Vec3D coords = new Vec3D() ;
    //Colour hue = Colour.WHITE ;
    float magnitude = 1.0f ;
    private float depth ;
  }
  
  final float maxDist ;
  final List <Star> allStars = new List <Star> () ;
  
  
  public Starfield(int maxStars, float maxDist) {
    geomBuffer = new MeshBuffer(maxStars * 2) ;
    this.maxDist = maxDist ;
  }
  
  
  public void addStar(float x, float y, float z, Colour hue, float mag) {
    final Star s = new Star() ;
    s.coords.set(x, y, z) ;
    s.magnitude = mag ;
    allStars.add(s) ;
  }
  
  
  public int[] GL_disables() {
    return new int[] {
      GL11.GL_LIGHTING, GL11.GL_CULL_FACE, GL11.GL_DEPTH_TEST
    } ;
  }
  
  
  public void renderTo(Rendering rendering) {
    //
    //  Sort by distance from back to front, stuff into the array, and render
    //  as a batch.
    final Sorting <Star> sorting = new Sorting <Star> () {
      public int compare(Star a, Star b) {
        if (a.depth > b.depth) return 1 ;
        if (a.depth < b.depth) return -1 ;
        return 0 ;
      }
    } ;
    
    transform.setIdentity() ;
    transform.rotateX(fieldElevate) ;
    transform.rotateZ(fieldRotate ) ;
    
    final Vec3D tV = new Vec3D() ; for (Star s : allStars) {
      s.depth = transform.trans(tV.setTo(s.coords)).z ;
      sorting.add(s) ;
    }
    //
    //  TODO:  I want to be able to specify colour as well.  Hue and alpha.
    
    MeshBuffer.beginRecord() ;
    for (Star star : sorting) {
      final Vec3D p = transform.trans(tV.setTo(star.coords)) ;
      final float s = star.magnitude, h = s / 2 ;
      MeshBuffer.recordSimpleQuad(p.x - h, p.y - h, s, p.z) ;
    }
    final Object geom[] = MeshBuffer.compileRecord() ;
    
    geomBuffer.update(
      (float[]) geom[0], (float[]) geom[1], (float[]) geom[2]
    ) ;
    
    STAR_TEX.bindTex() ;
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE) ;
    GL11.glColor4f(0.8f, 0.8f, 1, 1.0f) ;
    geomBuffer.renderTo(rendering) ;
    
    AXIS_TEX.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    GL11.glNormal3f(0, 0, 0) ;
    GL11.glColor4f(0.8f, 0.8f, 1, 1.0f) ;
    
    transform.trans(tV.set(-maxDist, -maxDist, 0)).scale(1.1f) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(0, 0) ;
    
    transform.trans(tV.set( maxDist, -maxDist, 0)).scale(1.1f) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(1, 0) ;
    
    transform.trans(tV.set( maxDist,  maxDist, 0)).scale(1.1f) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(1, 1) ;
    
    transform.trans(tV.set(-maxDist,  maxDist, 0)).scale(1.1f) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(0, 1) ;
    
    GL11.glEnd() ;
    GL11.glColor4f(1, 1, 1, 1) ;
  }
  
}







