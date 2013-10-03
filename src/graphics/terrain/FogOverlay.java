


package src.graphics.terrain ;
import src.graphics.common.* ;
import src.util.* ;
import org.lwjgl.BufferUtils ;
import org.lwjgl.opengl.* ;
import java.nio.* ;



//
//  Creates a 3-dimensional texture to allow smooth fading between old and
//  new fog values.
public class FogOverlay implements Rendering.Client {
  
  
  
  /**  Data fields, construction, setup and updates-
    */
  private boolean cached = false ;
  private int glID = -1 ;
  private ByteBuffer buffer ;
  
  final int size ;
  float fadeVal ;
  private byte oldVals[] = null, newVals[] = null ;
  
  
  
  public FogOverlay(int size) {
    this.size = size ;
    final int BS = size * size * 4 ;
    buffer = BufferUtils.createByteBuffer(BS * 2) ;
    oldVals = new byte[BS] ;
    newVals = new byte[BS] ;
  }
  
  
  public void assignNewVals(float newVals[][]) {
    byte held[] = oldVals ;
    oldVals = this.newVals ;
    this.newVals = held ;
    int i = 0 ;
    
    for (int y = 0 ; y < size ; y++) for (int x = 0 ; x < size ; x++) {
      final byte val = (byte) ((1 - newVals[x][y]) * 255) ;
      held[i++] = 0 ;
      held[i++] = 0 ;
      held[i++] = 0 ;
      held[i++] = val ;
    }
    
    cached = false ;
    assignFadeVal(0) ;
  }
  
  
  public void assignFadeVal(float newVal) {
    this.fadeVal = 0.25f + (newVal / 2) ;
  }
  
  
  
  /**  Actual rendering implementation-
    */
  private static IntBuffer tmpID = BufferUtils.createIntBuffer(1) ;
  private void cacheTex() {
    if (cached) return ;
    if (glID == -1) {
      tmpID.rewind() ;
      GL11.glGenTextures(tmpID) ;
      glID = tmpID.get(0) ;
    }
    
    buffer.rewind() ;
    buffer.put(oldVals) ;
    buffer.put(newVals) ;
    buffer.flip() ;
    
    GL11.glBindTexture(GL12.GL_TEXTURE_3D, glID) ;
    Texture.setDefaultTexParams(GL12.GL_TEXTURE_3D) ;
    GL12.glTexImage3D(
      GL12.GL_TEXTURE_3D, 0, GL11.GL_RGBA,
      size, size, 2,
      0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
      buffer
    ) ;
    cached = true ;
  }
  
  
  public int[] GL_disables() { return new int[] { GL11.GL_CULL_FACE } ; }
  
  
  public void renderTo(Rendering rendering) {
    GL11.glEnable(GL12.GL_TEXTURE_3D) ;
    cacheTex() ;
    GL11.glBindTexture(GL12.GL_TEXTURE_3D, glID) ;
    
    final float
      lT = 0.5f / size,
      hT = 1 - lT,
      lV = -0.5f,
      hV = lV + size ;
    
    GL11.glBegin(GL11.GL_QUADS) ;
    
    GL11.glTexCoord3f(lT, lT, fadeVal) ;
    GL11.glVertex3f(lV, lV, 0) ;
    
    GL11.glTexCoord3f(lT, hT, fadeVal) ;
    GL11.glVertex3f(lV, hV, 0) ;
    
    GL11.glTexCoord3f(hT, hT, fadeVal) ;
    GL11.glVertex3f(hV, hV, 0) ;
    
    GL11.glTexCoord3f(hT, lT, fadeVal) ;
    GL11.glVertex3f(hV, lV, 0) ;
    
    GL11.glEnd() ;
    GL11.glDisable(GL12.GL_TEXTURE_3D) ;
  }
  
  
  
  /**  Access methods for minimap rendering and sprite shading-
    */
  public void bindAsTex() {
    cacheTex() ;
    GL11.glBindTexture(GL12.GL_TEXTURE_3D, glID) ;
  }
  
  
  public float trueFadeVal() {
    return fadeVal ;
  }
  
  
  public float valAt(int x, int y) {
    final int off = (((y * size) + x) * 4) + 3 ;
    final float
      oldVal = (oldVals[off] & 0xff) / 255f,
      newVal = (newVals[off] & 0xff) / 255f ;
    final float time = (fadeVal - 0.25f) * 2 ;
    return 1 - ((oldVal * (1 - time)) + (time * newVal)) ;
  }
}







