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
  
  
  
  public MeshBuffer(int numFacets) {
    this.numFacets = numFacets ;
    vertBuffer = BufferUtils.createFloatBuffer(numFacets * VFP) ;
    normBuffer = BufferUtils.createFloatBuffer(numFacets * NFP) ;
    textBuffer = BufferUtils.createFloatBuffer(numFacets * TFP) ;
  }
  
  
  public MeshBuffer(float vertA[], float normA[], float textA[]) {
    this(vertA.length / VFP) ;
    update(vertA, normA, textA) ;
  }
  
  
  public MeshBuffer(Object geom[]) {
    this((float[]) geom[0], (float[]) geom[1], (float[]) geom[2]) ;
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
  public void renderTo(Rendering rendering) {
    if (numFacets == 0) return ;
    if (colour != null) colour.bindColour() ;
    render(1, 0, null, vertBuffer, normBuffer, textBuffer, numFacets) ;
  }
  
  
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
  private static Batch <Vec3D> vertB = new Batch <Vec3D> () ;
  private static Batch <Vec3D> normB = new Batch <Vec3D> () ;
  private static Batch <Vec2D> textB = new Batch <Vec2D> () ;
  
    
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
  
  
  public static void recordSimpleQuad(
    float x, float y, float size, float z, float UV[]
  ) {
    recordPoint(
        x, y, z,
        0, 0, 0,
        UV[0], UV[1]
    ) ;
    recordPoint(
        x, y + size, z,
        0, 0, 0,
        UV[2], UV[3]
    ) ;
    recordPoint(
        x + size, y + size, z,
        0, 0, 0,
        UV[4], UV[5]
    ) ;
    recordPoint(
        x + size, y + size, z,
        0, 0, 0,
        UV[6], UV[7]
    ) ;
    recordPoint(
        x + size, y, z,
        0, 0, 0,
        UV[8], UV[9]
    ) ;
    recordPoint(
        x, y, z,
        0, 0, 0,
        UV[10], UV[11]
    ) ;
  }
  
  
  public static void recordPoint(
    float vX, float vY, float vZ,
    float nX, float nY, float nZ,
    float tU, float tV
  ) {
    vertB.add(new Vec3D(vX, vY, vZ)) ;
    normB.add(new Vec3D(nX, nY, nZ)) ;
    textB.add(new Vec2D(tU, tV)) ;
  }
  
  
  public static void recordPoint(Vec3D v, Vec3D n, Vec2D t) {
    vertB.add(v) ;
    normB.add(n) ;
    textB.add(t) ;
  }
  
  
  public static void recordGeom(
    Vec3D verts[], Vec3D norms[], Vec2D texts[]
  ) {
    if (verts.length != norms.length || norms.length != texts.length) {
      I.complain("All geom arrays must of the same length.") ;
    }
    final float
      vertA[] = new float[verts.length * 3],
      normA[] = new float[norms.length * 3],
      textA[] = new float[texts.length * 2] ;
    for (int i = 0, vI = 0, nI = 0, tI = 0 ; i < verts.length ; i++) {
      final Vec3D v = verts[i] ;
      final Vec3D n = norms[i] ;
      final Vec2D t = texts[i] ;
      vertA[vI++] = v.x ;
      vertA[vI++] = v.y ;
      vertA[vI++] = v.z ;
      normA[nI++] = n.x ;
      normA[nI++] = n.y ;
      normA[nI++] = n.z ;
      textA[tI++] = t.x ;
      textA[tI++] = t.y ;
    }
    recordGeom(vertA, normA, textA) ;
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
    if (vertB.size() > 0) {
      final Vec3D verts[] = vertB.toArray(Vec3D.class) ;
      final Vec3D norms[] = normB.toArray(Vec3D.class) ;
      final Vec2D texts[] = textB.toArray(Vec2D.class) ;
      vertB.clear() ;
      normB.clear() ;
      textB.clear() ;
      recordGeom(verts, norms, texts) ;
    }
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










