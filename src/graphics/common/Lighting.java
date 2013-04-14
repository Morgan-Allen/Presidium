/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.common ;
import src.util.*;

import java.nio.* ;
import org.lwjgl.opengl.* ;
import org.lwjgl.BufferUtils ;


/**  A particular lighting state specified in terms of type, direction,
  *  intensity, specularity, etc.  Applied cumulatively to objects in a given
  *  tile.
 */
public class Lighting {
  
  final public static Vec3D
    DEFAULT_ANGLE = new Vec3D(0.75f, 0.25f, 1.0f) ;
  
  
  protected float[]
    ambience = new float[4],
    diffused = new float[4],
    position = new float[4] ;
  final private FloatBuffer
    ambBuffer = BufferUtils.createFloatBuffer(4),
    difBuffer = BufferUtils.createFloatBuffer(4),
    posBuffer = BufferUtils.createFloatBuffer(4) ;
  private float r, g, b ;
  
  
  /**  Initialises this light based on expected rgb values, ambience ratio,
    *  and whether ambient light should complement diffuse shading (to create
    *  the appearance of naturalistic shadows.)
    */
  //TODO:  Create variant method that deals with Colours directly.
  public void setup(
    float r,
    float g, 
    float b,
    //float brightness,
    //float ambience,
    boolean shadow,
    boolean global
  ) {
    float weigh = 0.8f ;//brightness * (1 - ambience) ;
    diffused[0] = this.r = r * weigh ;
    diffused[1] = this.g = g * weigh ;
    diffused[2] = this.b = b * weigh ;
    weigh = 0.1f ;//brightness * ambience ;
    if (shadow) {
      ambience[0] = weigh * (g + b) / 2 ;
      ambience[1] = weigh * (r + b) / 2 ;
      ambience[2] = weigh * (r + g) / 2 ;  //set to complementary colour.
    }
    else {
      ambience[0] = r * weigh ;
      ambience[1] = g * weigh ;
      ambience[2] = b * weigh ;
    }
    ambience[3] = diffused[3] = (global) ? 0 : 1 ;
  }
  
  public float r() { return r ; }
  public float g() { return g ; }
  public float b() { return b ; }
  
  
  final static FloatBuffer
    DARK = BufferUtils.createFloatBuffer(4) ;
  
  /**  Binds this light to the GL11 light register specified (between 0 to 8).
    */
  public void bindLight(int lI) {
    lI += GL11.GL_LIGHT0 ;
    GL11.glEnable(lI) ;
    ambBuffer.clear() ; ambBuffer.put(ambience).flip() ;
    difBuffer.clear() ; difBuffer.put(diffused).flip() ;
    posBuffer.clear() ; posBuffer.put(position).flip() ;
    GL11.glLight(lI, GL11.GL_AMBIENT, ambBuffer) ;
    GL11.glLight(lI, GL11.GL_DIFFUSE, difBuffer) ;
    GL11.glLight(lI, GL11.GL_POSITION, posBuffer) ;
    DARK.clear() ; DARK.put(new float[4]).flip() ;
    GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, DARK) ;
  }
  
  /**  Sets this Lighting to function as a directional, rather than local,
    *  light source, with the argument vector to orient itself.
    */
  public void direct(Vec3D dirVec) {
    dirVec.normalise() ;
    place(dirVec) ;
    position[3] = 0 ;  //zero indicates a directional light.
  }
  
  /**  Sets the lighting to act as a positional light of given placement, as
    *  supplied by the argument vector.
    */
  public void place(Vec3D posVec) {
    position[0] = posVec.x ;
    position[1] = posVec.y ;
    position[2] = posVec.z ;
    position[3] = 1 ;
  }
}