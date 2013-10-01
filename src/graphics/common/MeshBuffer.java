/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import java.nio.FloatBuffer ;

import org.lwjgl.BufferUtils ;
import org.lwjgl.opengl.* ;

import src.util.* ;



public class MeshBuffer implements Rendering.Client {
  
  
  final private static int
    VFP = 9 ,  //vertex floats per polygon
    NFP = 9 ,  //normal floats per polygon
    TFP = 6 ;  //texture coordinate floats per polygon
  
  public Colour colour ;
  protected int numFacets ;
  private FloatBuffer vertBuffer, normBuffer, textBuffer ;
  
  //
  //  TODO:  Replace with something more generalised/secure...
  ///public boolean isFog = false ;
  
  
  
  public MeshBuffer(int numFacets) {
    this.numFacets = numFacets ;
    vertBuffer = BufferUtils.createFloatBuffer(numFacets * VFP) ;
    normBuffer = BufferUtils.createFloatBuffer(numFacets * NFP) ;
    textBuffer = BufferUtils.createFloatBuffer(numFacets * TFP) ;
  }
  
  
  protected MeshBuffer(MeshBuffer refers) {
    this.numFacets = refers.numFacets ;
    vertBuffer = refers.vertBuffer ;
    normBuffer = refers.normBuffer ;
    textBuffer = refers.textBuffer ;
  }
  
  
  public void update(float vertA[], float normA[], float textA[]) {
    vertBuffer.clear() ;
    vertBuffer.put(vertA).flip() ;
    normBuffer.clear() ;
    normBuffer.put(normA).flip() ;
    textBuffer.clear() ;
    textBuffer.put(textA).flip() ;
  }
  
  
  /**  Rendering methods-
    */
  /*
  glActiveTextureARB( GL_TEXTURE0_ARB );
  glEnable          ( GL_TEXTURE_2D   );
  glTexEnvi         ( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE ) 
  glBindTexture     ( GL_TEXTURE_2D, texture0 );
 
  glActiveTextureARB( GL_TEXTURE1_ARB );
  glEnable          ( GL_TEXTURE_2D   );
  glTexEnvi         ( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE ) ;
  glBindTexture     ( GL_TEXTURE_2D, texture1 );
  //*/
  
  //
  //  TODO:  You may need to create a dedicated FogMesh class.
  
  public void renderTo(Rendering rendering) {
    if (numFacets == 0) return ;
    if (colour != null) colour.bindColour() ;
    render(1, 0, null, vertBuffer, normBuffer, textBuffer, numFacets) ;
  }

  /*
  //GL11.glDisable(GL11.GL_DEPTH_TEST) ;
  GL11.glTexEnvf(
    GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_SUBTRACT
  ) ;
  GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA) ;
  //*/
  ///GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT) ;

  ///GL14.glBlendEquation(GL14.GL_FUNC_ADD) ;
  /*
  //GL11.glEnable(GL11.GL_DEPTH_TEST) ;
  GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA) ;
  GL11.glTexEnvf(
    GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE
  ) ;
  //*/
  
  
  final static int GL_DISABLES[] = new int[] {} ;
  public int[] GL_disables() { return GL_DISABLES ; }
  

  public static void render(
    float scale,
    float rotation,
    Vec3D offset,
    FloatBuffer vertBuffer,
    FloatBuffer normBuffer,
    FloatBuffer textBuffer,
    int numFacets
  ) {
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    if (numFacets < 1) numFacets = vertBuffer.capacity() / VFP ;
    if (offset != null) GL11.glTranslatef(offset.x, offset.y, offset.z) ;
    else GL11.glTranslatef(0, 0, 0) ;
    GL11.glRotatef(rotation, 0, 0, 1) ;
    GL11.glScalef(scale, scale, scale) ;
    //
    //  Only set the texture buffer if one has been provided:
    if (textBuffer == null) {
      GL11.glDisable(GL11.GL_TEXTURE_2D) ;
      GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY) ;
    }
    else {
      GL11.glEnable(GL11.GL_TEXTURE_2D) ;
      GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY) ;
      GL11.glTexCoordPointer(2, 0, textBuffer) ;
    }
    //
    //  And only set the normal buffer if one has been provided:
    if (normBuffer == null) GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY) ;
    else {
      GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY) ;
      GL11.glNormalPointer(0, normBuffer) ;
    }
    //
    //  Bind the vertex buffer and render-
    GL11.glVertexPointer(3, 0, vertBuffer) ;
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, numFacets * 3) ;
  }
  
  
  
  /**  Assorted helper methods and temp data for compiling geometry records-
    */
  
  final static int PORTION_SIZE = 1000 ;
  private static int vI, nI, tI ;
  private static List <float[]>
    vertR = new List <float[]> (),
    normR = new List <float[]> (),
    textR = new List <float[]> () ;
  
    
  public static void beginRecord() {
    vI = resetRecord(vertR) ;
    nI = resetRecord(normR) ;
    tI = resetRecord(textR) ;
  }
  
  private static int resetRecord(List <float[]> record) {
    record.clear() ;
    record.add(new float[PORTION_SIZE]) ;
    return 0 ;
  }
  
  
  public static void recordGeom(
    float verts[], float norms[], float texts[]
  ) {
    vI = pushFloats(verts, vertR, vI) ;
    nI = pushFloats(norms, normR, nI) ;
    tI = pushFloats(texts, textR, tI) ;
  }
  
  
  private static int pushFloats(
    float pushed[], List <float[]> record, int index
  ) {
    float latest[] = record.last() ;
    for (int i = 0 ; i < pushed.length ; i++) {
      if (index >= PORTION_SIZE) {
        record.add(latest = new float[PORTION_SIZE]) ;
        index = 0 ;
      }
      latest[index] = pushed[i] ;
      index++ ;
    }
    return index ;
  }
  
  
  public static Object[] compileRecord() {
    return new Object[] {
      compile(vertR, vI),
      compile(normR, nI),
      compile(textR, tI)
    } ;
  }
  
  
  private static float[] compile(List <float[]> record, int lastIndex) {
    final int totalSize = ((record.size() - 1) * PORTION_SIZE) + lastIndex ;
    float compiled[] = new float[totalSize] ;
    int cI = 0, i = 0 ; for (float[] portion : record) {
      while (i < PORTION_SIZE && cI < totalSize) {
        compiled[cI++] = portion[i++] ;
      }
      i = 0 ;
    }
    record.clear() ;
    return compiled ;
  }
  
  
  public static void renderCompiled() {
    final Object record[] = compileRecord() ;
    final float
      verts[] = (float[]) record[0],
      norms[] = (float[]) record[1],
      texts[] = (float[]) record[2] ;
    final FloatBuffer
      tempVB = BufferUtils.createFloatBuffer(verts.length),
      tempNB = BufferUtils.createFloatBuffer(norms.length),
      tempTB = BufferUtils.createFloatBuffer(texts.length) ;
    tempVB.put(verts).flip() ;
    tempNB.put(norms).flip() ;
    tempTB.put(texts).flip() ;
    render(1, 0, null, tempVB, tempNB,tempTB, verts.length / VFP) ;
  }
}










