/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import src.util.* ;
import java.awt.image.* ;
import java.io.* ;
import java.nio.* ;
import javax.imageio.* ;
import org.lwjgl.opengl.* ;
import org.lwjgl.* ;





/**  Generalised class for loading and application of 2-dimensional image files
  *  to OpenGL engine constructs.
  */
final public class Texture {
  
  
  /**  xdim - size, in pixels, of the original image along x axis.
    *  ydim - size, in pixels, of the original image along y axis.
    *  size - the present power-of-2 size of the texture accomodating the image.
    *  
    *  xRatio - the ratio of image width/texture size.
    *  yRatio - the ratio of image height/texture size.
    */
  protected ByteBuffer buffer ;
  protected Colour averaged ;
  private static int totalBytesUsed = 0 ;
  private static byte tempRGBA[] = new byte[4] ;
  
  protected int
    xdim,
    ydim,
    trueSize ;
  private float
    uRange = 1.0f,
    vRange = 1.0f ;
  
  private int glID = -1 ;
  private boolean cached = false ;
  private String texName = "", alphaName = "" ;
  
  public int xdim() { return xdim ; }
  public int ydim() { return ydim ; }
  public int trueSize() { return trueSize ; }
  public float maxU() { return uRange ; }
  public float maxV() { return vRange ; }
  public String name()  { return texName   ; }
  public String alpha() { return alphaName ; }
  
  
  
  /**  Convenient factory methods-
    */
  public static Texture[] loadTextures(String... names) {
    final Texture loaded[] = new Texture[names.length] ;
    for (int i = 0 ; i < names.length ; i++)
      loaded[i] = loadTexture(names[i], null) ;
    return loaded ;
  }
  
  
  public static Texture loadTexture(String name) {
    return Texture.loadTexture(name, null) ;
  }
  
  
  public static Texture loadTexture(DataInputStream in) throws Exception {
    final String texName   = LoadService.readString(in) ;
    final String alphaName = LoadService.readString(in) ;
    return loadTexture(texName, alphaName) ;
  }
  
  
  public static void saveTexture(
    Texture tex, DataOutputStream out
  ) throws Exception {
    LoadService.writeString(out, tex.texName  ) ;
    LoadService.writeString(out, tex.alphaName) ;
  }
  
  
  
  /**  The main loading method used by most external classes-
    */
  public static Texture loadTexture(String pathName, String alphaName) {
    //
    //  Firstly, check to see if the texture in question has already been
    //  loaded-
    Object cached = LoadService.getResource(pathName) ;
    if (cached != null) return (Texture) cached ;
    //
    //  Otherwise, load up the underlying images, extract the pixel data, and
    //  pass that onto the dedicated setup method-
    final Texture newTex = new Texture() ;
    try {
      pathName = LoadService.safePath(pathName) ;
      BufferedImage image = ImageIO.read(new File(pathName)) ;
      final int
        xdim = newTex.xdim = image.getWidth() ,
        ydim = newTex.ydim = image.getHeight() ;
      final int imageP[] = new int[xdim * ydim] ;
      image.getRGB(0, 0, xdim, ydim, imageP, 0, xdim) ;
      //
      //  Alpha is optional, so we may have to skip it-
      int alphaP[] = null ;
      if (alphaName != null && alphaName.length() > 0) {
        alphaName = LoadService.safePath(alphaName) ;
        BufferedImage mask = ImageIO.read(new File(alphaName)) ;
        alphaP = new int[xdim * ydim] ;
        mask.getRGB(0, 0, xdim, ydim, alphaP, 0, xdim) ;
      }
      newTex.setup(alphaP, imageP, xdim, ydim, 0, xdim) ;
    }
    //
    //  If something goes wrong, report the problem and return a blank texture-
    catch(IOException e) {
      I.say(
        "PROBLEM LOADING TEXTURE. "+pathName+" / "+alphaName
      ) ;
      ///e.printStackTrace() ;
      return WHITE_TEX ;
    }
    //
    //  Cache the texture for future reference, and return-
    LoadService.cacheResource(newTex, pathName) ;
    newTex.texName = pathName ;
    newTex.alphaName = alphaName ;
    return newTex ;
  }
  
  
  private Texture() {}
  
  
  
  /**  A few basic utility textures for general use.
    */
  public final static Texture
    CLEAR_TEX = new Texture(),
    GRAYS_TEX = new Texture(),
    BLACK_TEX = new Texture(),
    WHITE_TEX = Texture.loadTexture("media/Terrain/blank_white.png") ;
  static {
    CLEAR_TEX.setup(1, 1) ;
    CLEAR_TEX.buffer.putInt(0x00000000) ;
    GRAYS_TEX.setup(1, 1) ;
    GRAYS_TEX.buffer.putInt(0x99999999) ;
    BLACK_TEX.setup(1, 1) ;
    BLACK_TEX.buffer.putInt(0xff000000) ;
  }
  
  
  public static Texture createTexture(int w, int h) {
    final Texture tex = new Texture() ;
    tex.setup(w, h) ;
    return tex ;
  }
  
  
  private void setup(int w, int h) {
    xdim = w ; ydim = h ;
    trueSize = 1 ;
    while (trueSize < Math.max(w, h)) trueSize *= 2 ;
    uRange = ((float) xdim) / trueSize ;
    vRange = ((float) ydim) / trueSize ;
    buffer = BufferUtils.createByteBuffer(trueSize * trueSize * 4) ;
  }
  

  public Colour averaged() {
    return this.averaged ;
  }
  
  
  public void putBytes(byte vals[]) {
    buffer.clear() ;
    buffer.put(vals) ;
  }
  
  
  public void putColour(Colour c, int x, int y) {
    c.storeByteValue(tempRGBA, 0) ;
    final int limit = buffer.limit() ;
    final int offset = ((y * trueSize) + x) * 4 ;
    buffer.position(offset) ;
    buffer.put(tempRGBA, 0, 4) ;
    buffer.limit(limit) ;
    buffer.position(limit) ;
    cached = false ;
  }
  
  
  
  /**  Helper methods for accessing RGBA values at relative texture UV or pixel
    *  coordinates.
    */
  public Colour getColour(float tU, float tV) {
    final int
      tX = (int) ((tU * trueSize) * uRange),
      tY = (int) (((1 - tV) * trueSize) * vRange) ;
    
    final int pixVal = getPixelVal(
      Visit.clamp(tX, trueSize),
      Visit.clamp(tY, trueSize)
    ) ;
    final Colour c = new Colour() ;
    c.r = ((pixVal >> 24) & 0xff) / 255f ;
    c.g = ((pixVal >> 16) & 0xff) / 255f ;
    c.b = ((pixVal >> 8 ) & 0xff) / 255f ;
    c.a = ((pixVal >> 0 ) & 0xff) / 255f ;
    ///I.say("colour obtained at "+tX+" "+tY+" is: \n"+c) ;
    return c ;
  }
  
  
  public int getPixelVal(int tX, int tY) {
    //tY = trueSize - (tY + 1) ;
    return buffer.getInt(((tY * trueSize) + tX) * 4) ;
  }
  
  
  
  /**  Internal method used to combine colour and relAlpha channels (which may be
    *  stored seperately,) to create a texture from a portion of a given
    *  composite image.
    *  @param fill  the relAlpha channel data.
    *  @param pix  the colour channel data.
    *  @param wide  the width of the total sample image used.
    *  @param high  the height of the total samples image used.
    *  @param start  the x offset to begin sampling at.
    *  @param len  the width of the sample to use.
    */
  private void setup(
    int mask[],
    int fill[],
    int wide,
    int high,
    int start,
    int len
  ) {
    final int max = ((len > high) ? len : high) ;
    trueSize = 1 ;
    while (trueSize < max) trueSize *= 2 ;
    uRange = ((float) xdim) / trueSize ;
    vRange = ((float) ydim) / trueSize ;
    //
    //  If a separate mask texture isn't provided, use the base texture for
    //  relAlpha values.
    final int shiftA ; if (mask == null) {
      mask = fill ;
      shiftA = 24 ;
    }
    else shiftA = 0 ;
    final byte vals[] = new byte[trueSize * trueSize * 4] ;
    //
    //  We must average rgb values.
    float aR = 0, aB = 0, aG = 0 ;
    float sumWeights = 0 ;
    int yp = 0, xp, ind, alpha, pixel, v = 0, r, g, b, a ;
    //
    //  We also need to convert from ARGB to RGBA-
    for(; yp < high ; yp++) {
      v = yp * trueSize * 4 ;
      ind = (yp * wide) + start ;
      for(xp = 0 ; xp < len ; xp++) {
        alpha = mask[ind]   ;
        pixel = fill[ind++] ;
        r = (pixel >> 16) & 0xff ;
        g = (pixel >> 8 ) & 0xff ;
        b = (pixel >> 0 ) & 0xff ;
        a = (alpha >> shiftA) & 0xff ;
        //
        //  weight the average by relAlpha, and insert the bytes into the array:
        final float w = a / 255f ; if (w > 0) {
          sumWeights += w ;
          aR += r * w ; aG += g * w ; aB += b * w ;
        }
        vals[v++] = (byte) r ;
        vals[v++] = (byte) g ;
        vals[v++] = (byte) b ;
        vals[v++] = (byte) a ;
      }
    }
    //
    //  Finish averaging and store results (diminishing saturation a little.)
    averaged = new Colour().set(
      aR / (sumWeights * 255),
      aG / (sumWeights * 255),
      aB / (sumWeights * 255),
      sumWeights / (high * len)
    ) ;
    final float HSV[] = averaged.getHSV(null) ;
    HSV[1] *= 0.7f ;
    averaged.setHSV(HSV) ;
    //
    //  Create the byte buffer and store contents-
    buffer = BufferUtils.createByteBuffer(vals.length) ;
    totalBytesUsed += buffer.capacity() ;
    //I.say("Total bytes allocated to textures: "+totalBytesUsed) ;
    //I.say("Creating texture of size in KB: "+(buffer.capacity() / 1000)) ;
    buffer.put(vals) ;
  }
  
  
  
  /**  Binds this texture to the current Gl rendering context, caching itself
    *  as neccesary.
    */
  public void bindTex() {
    if (! cached) cacheTex() ;
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, glID) ;
  }
  
  
  public static void setDefaultTexParams() {
    GL11.glTexParameteri(
      GL11.GL_TEXTURE_2D,
      GL11.GL_TEXTURE_MAG_FILTER,
      GL11.GL_LINEAR
    ) ;
    GL11.glTexParameteri(
      GL11.GL_TEXTURE_2D,
      GL11.GL_TEXTURE_MIN_FILTER,
      GL11.GL_LINEAR
    ) ;
    GL11.glTexParameteri(
      GL11.GL_TEXTURE_2D,
      GL11.GL_TEXTURE_WRAP_T,
      GL11.GL_CLAMP
    ) ;
    GL11.glTexParameteri(
      GL11.GL_TEXTURE_2D,
      GL11.GL_TEXTURE_WRAP_S,
      GL11.GL_CLAMP
    ) ;
  }
  
  
  /**  Caches this texture for later binding-
   */
  private static IntBuffer tmpID = BufferUtils.createIntBuffer(1) ;
  private void cacheTex() {
    if (glID == -1) {
      tmpID.rewind() ;
      GL11.glGenTextures(tmpID) ;
      glID = tmpID.get(0) ;
    }
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, glID) ;
    buffer.flip() ;
    setDefaultTexParams() ;
    GL11.glTexImage2D(
      GL11.GL_TEXTURE_2D,
      0, GL11.GL_RGBA8,
      trueSize, trueSize, 0,
      GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
      buffer
    ) ;
    cached = true ;
  }
}
