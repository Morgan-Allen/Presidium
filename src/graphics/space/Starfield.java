/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.space ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;
import org.lwjgl.opengl.* ;



public class Starfield implements Rendering.Client {
  
  
  
  final static String
    IMG_DIR = "media/GUI/starcharts/" ;
  final static Texture
    //
    //  TODO:  You might also need textures for selection and showing jump
    //  routes, et cetera.
    STAR_TEX    = Texture.loadTexture(IMG_DIR+"star_burst.png"   ),
    AXIS_TEX    = Texture.loadTexture(IMG_DIR+"sky_axis.png"     ),
    
    ALL_STAR_TEX = Texture.loadTexture(IMG_DIR+"stellar_objects.png"),
    SECTORS_TEX  = Texture.loadTexture(IMG_DIR+"chart_sectors.png") ;
  
  final public static int
    TYPE_GIANT  = 0,
    TYPE_MAIN   = 1,
    TYPE_DWARF  = 2,
    TYPE_EXOTIC = 3,
    TYPE_NEBULA = 4 ;
  
  final static ImageModel
    STELLAR_BODIES[][] = ImageModel.fromTextureGrid(
        Starfield.class, ALL_STAR_TEX,
        5, 1
    ) ;
  

  final public Viewport starPort = new Viewport() ;
  public float fieldElevate, fieldRotate ;
  private Mat3D transform = new Mat3D() ;
  private MeshBuffer geomBuffer ;
  
  private class Star {
    Vec3D coords = new Vec3D() ;
    float magnitude = 1.0f ;
    private float depth ;
    private ImageModel model ;
  }
  
  final float maxDist ;
  final List <Star> allStars = new List <Star> () ;
  
  
  public Starfield(int maxStars, float maxDist) {
    geomBuffer = new MeshBuffer(maxStars * 2) ;
    this.maxDist = maxDist ;
  }
  
  
  public void addStar(
    float x, float y, float z, float size, float age
  ) {
    final Star s = new Star() ;
    s.coords.set(x, y, z) ;
    s.magnitude = (Rand.avgNums(2) + 0.5f) / 2.0f ;
    final int type = (int) (size * 5) ;
    
    s.model = STELLAR_BODIES[type][(int) (age * 4.99f)] ;
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
    
    MeshBuffer.beginRecord() ;
    
    for (Star star : sorting) {
      final Vec3D p = transform.trans(tV.setTo(star.coords)) ;
      final float s = star.magnitude, h = s / 2 ;
      final float UV[] = star.model.framesUV()[0] ;
      
      MeshBuffer.recordSimpleQuad(p.x - h, p.y - h, s, p.z, UV) ;
    }
    final Object geom[] = MeshBuffer.compileRecord() ;
    
    geomBuffer.update(
      (float[]) geom[0], (float[]) geom[1], (float[]) geom[2]
    ) ;
    
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE) ;
    
    GL11.glColor4f(1.0f, 1.0f, 1, 0.5f) ;
    STAR_TEX.bindTex() ;
    geomBuffer.renderTo(rendering) ;
    
    GL11.glColor4f(1.0f, 1.0f, 1, 1.0f) ;
    ALL_STAR_TEX.bindTex() ;
    geomBuffer.renderTo(rendering) ;
    
    GL11.glColor4f(0.8f, 0.8f, 1, 0.25f) ;
    final Mat3D oppZ = new Mat3D().setTo(transform) ;
    axisWithTransform(oppZ, SECTORS_TEX, maxDist) ;
    
    GL11.glColor4f(0.8f, 1, 1, 0.25f) ;
    final Mat3D oppX = new Mat3D().setTo(transform) ;
    oppX.rotateY((float) Math.PI / 2) ;
    axisWithTransform(oppX, AXIS_TEX, maxDist * 0.5f) ;

    GL11.glColor4f(1, 0.8f, 1, 0.25f) ;
    final Mat3D oppY = new Mat3D().setTo(transform) ;
    oppY.rotateX((float) Math.PI / 2) ;
    axisWithTransform(oppY, AXIS_TEX, maxDist * 0.5f) ;
    
    GL11.glColor4f(1, 1, 1, 1) ;
  }
  
  
  private void axisWithTransform(Mat3D transform, Texture tex, float radius) {
    final Vec3D tV = new Vec3D() ;
    tex.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    GL11.glNormal3f(0, 0, 0) ;
    
    transform.trans(tV.set(-1, -1, 0)).scale(1.1f * radius) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(0, 0) ;
    
    transform.trans(tV.set( 1, -1, 0)).scale(1.1f * radius) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(1, 0) ;
    
    transform.trans(tV.set( 1,  1, 0)).scale(1.1f * radius) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(1, 1) ;
    
    transform.trans(tV.set(-1,  1, 0)).scale(1.1f * radius) ;
    GL11.glVertex3f(tV.x, tV.y, tV.z) ;
    GL11.glTexCoord2f(0, 1) ;
    
    GL11.glEnd() ;
  }
  
}

