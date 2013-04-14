/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import java.io.* ;
import src.util.* ;


/**  A mipmap is a quadtree-based averaging system for data over a
  *  two-dimensional area (e.g, a greyscale image or intensity levels.)
  */
public class MipMap {
  
  
  public static void main(String s[]) {
    final int size = 8 ;
    MipMap testMap = new MipMap(size) ;
    final int testArray[][] = {
      {1, 0, 1, 0, 1, 1, 0, 1},
      {0, 1, 0, 0, 1, 1, 0, 1},
      {1, 0, 1, 0, 1, 1, 0, 1},
      {1, 0, 0, 0, 1, 1, 0, 1},
      {1, 0, 1, 0, 1, 1, 0, 1},
      {0, 1, 0, 0, 1, 1, 0, 1},
      {1, 0, 1, 0, 1, 1, 0, 1},
      {1, 0, 0, 0, 1, 1, 0, 1}
    } ;
    for (int x = size ; x-- > 0 ;) for (int y = size ; y-- > 0 ;)
      testMap.accum(testArray[x][y], x, y) ;
    for (int x = 0 ; x < size ; x++) {
      I.say("") ;
      for (int y = 0 ; y < size ; y++)
        I.add(" [] "+testMap.getBlendValAt(x, y)) ;
    }
    mipTest() ;
  }

  static void mipTest() {
    /*
    final int size = 256 ;
    final float heights[][] = new HeightMap(size + 1).value() ;
    final MipMap map = new MipMap(size) ;
    final int rgba[] = new int[size * size] ;
    
    for (int x = size ; x-- > 0 ;) for (int y = size ; y-- > 0 ;)
      map.accum(heights[x][y] > 0.5f ? 1 : 0, x, y) ;
    
    final long init = System.currentTimeMillis() ;
    for (int x = 0 ; x < size ; x++) for (int y = 0 ; y < size ; y++) {
      final float val = map.getBlendValAt(x, y) ;
      final int comp = (int) (0xff * val) ;
      rgba[(y * size) + x] = 0xff000000 | (comp << 16) | (comp << 8) | comp ;
    }
    I.say("   Milliseconds to render: "+(System.currentTimeMillis() - init)) ;
    final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB) ;
    image.setRGB(0, 0, size, size, rgba, 0, size) ;
    
    final Frame frame = new Frame() {
      public void paint(Graphics g) {
        g.drawImage(image, 0, 0, null) ;
      }
    } ;
    frame.setBounds(100, 100, size, size) ;
    frame.setUndecorated(true) ;
    frame.setVisible(true) ;
    //*/
  }
  
  
  
  final int mipLevels[][][] ;
  final int high, size ;
  
  public int high() { return high ; }
  public int size() { return size ; }
  
  
  /**  Creates a basic mipmap of size matching the lowest power of 2 greater
    *  than the minimum size specified.
    */
  public MipMap(final int minSize) {
    int d = 1, s = 1 ;
    while (s < minSize) { d++ ; s *= 2 ; }
    high = d ;
    size = s ;
    mipLevels = new int[high][][] ;
    for (d = 0, s = size ; d < high ; d++, s /= 2) {
      mipLevels[d] = new int[s][s] ;
      //I.say("size is "+mipLevels[d].length) ;
    }
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    for (int d = 0, s = size, x, y ; d < high ; d++, s /= 2)
      for (x = s ; x-- > 0 ;) for (y = s ; y-- > 0 ;)
        mipLevels[d][x][y] = in.readInt() ;
  }
  
  public void saveTo(DataOutputStream out) throws Exception {
    for (int d = 0, s = size, x, y ; d < high ; d++, s /= 2)
      for (x = s ; x-- > 0 ;) for (y = s ; y-- > 0 ;)
        out.writeInt(mipLevels[d][x][y]) ;
  }
  
  public void clear() {
    for (int d = 0, s = size, x, y ; d < high ; d++, s /= 2)
      for (x = s ; x-- > 0 ;) for (y = s ; y-- > 0 ;)
        mipLevels[d][x][y] = 0 ;
  }
  
  
  /**  Accumulates the specified value at the given base array coordinates.
    */
  public void accum(final int val, final int x, final int y) {
    for (int h = 0, dX = x, dY = y ; h < high ; dX /= 2, dY /= 2, h++) {
      mipLevels[h][dX][dY] += val ;
    }
  }
  
  
  /**  Sets the specified value at the given base array coordinates.
    */
  public void set(final int val, final int x, final int y) {
    final int current = mipLevels[0][x][y] ;
    accum(val - current, x, y) ;
  }
  
  
  /**  Gets the total accumulation at the given array coordinates (the higher
   *  the level, the smaller x and y can be.)
   */
  public int getTotalAt(final int mX, final int mY, final int h) {
    return mipLevels[h][mX][mY] ;
  }
  
  
  /**  Gets the average accumulation at the given array coordinates (the higher
    *  the level, the smaller x and y can be.)
    */
  public float getAvgAt(final int mX, final int mY, final int h) {
    final int s = 1 << h ;
    return (mipLevels[h][mX][mY] * 1f) / (s * s) ;
  }
  
  
  /**  Returns the blended and interpolated average of successive mip levels within
    *  the map above the given x/y coordinates.
    */
  public float getBlendValAt(final float x, final float y) {
    int minX, minY, maxX, maxY, range ;
    float pX, pY, aX, aY, sum = 0, avg ;
    for (int h = 0, size = 1 ; h < high ; h++, size *= 2) {
      range = (this.size / size) - 1 ;
      pX = (x / size) - 0.5f ;
      pY = (y / size) - 0.5f ;
      pX = (pX < 0) ? 0 : (pX > range) ? range : pX ;
      pY = (pY < 0) ? 0 : (pY > range) ? range : pY ;
      minX = (int) pX ;
      minY = (int) pY ;
      maxX = (pX == minX) ? minX : minX + 1 ;
      maxY = (pY == minY) ? minY : minY + 1 ;
      aX = pX - minX ;
      aY = pY - minY ;
      avg =
        (mipLevels[h][minX][minY] * (1 - aX) * (1 - aY)) +
        (mipLevels[h][maxX][minY] *      aX  * (1 - aY)) +
        (mipLevels[h][minX][maxY] * (1 - aX) *      aY ) +
        (mipLevels[h][maxX][maxY] *      aX  *      aY ) ;
      sum += avg / (size * size * high) ;
    }
    return sum ;
  }
}






/*
public float getBlendValAt(final float x, final float y) {
  minX = (int) Math.floor(x) ;
  maxX = (int) Math.ceil (x) ;
  minY = (int) Math.floor(y) ;
  maxY = (int) Math.ceil (y) ;
  getBlendValAt(minX, minY) ;
  getBlendValAt(maxX, minY) ;
  getBlendValAt(minX, maxY) ;
  getBlendValAt(maxX, maxY) ;
  return blendAt(x, y, 0) ;
}
//*/
/*

private static int numCaches = 0 ;

public float getBlendValAt(final int x, final int y) {
  ///I.say(" STARTING QUERY... ") ;
  for (int h = high, d = size ; h-- > 0 ; d /= 2) {
    cacheAt(x / d, y / d, h) ;
  }
  return blendCache[0][x][y] ;
}

private float cacheAt(final int mx, final int my, final int h) {
  if (blendCache[h][mx][my] == Float.POSITIVE_INFINITY) {
    numCaches++ ;
    //  Then we need to initialise a cached value for these coordinates-
    ///I.say("Caching at: "+mx+" "+my+" "+h) ;
    if (h < high - 1) {
      //  This is a non-root entry.  Every level in the map gets equal
      //  weight, and the blended val immediately above represents the
      //  blending of all higher levels (i.e, (high - (h + 1)) of them.)
      final int ms = 1 << h ;
      final float
        above = blendAt(mx * ms, my * ms, h + 1),
        below = mipLevels[h][mx][my] * 1f / (ms * ms) ;
      ///I.add(" above/below: "+above+"/"+below) ;
      blendCache[h][mx][my] = (
        (above * ((high - 1) - h)) +
        below
      ) / (high - h) ;  // ...weighted average.
    }
    else {
      //  Initialise root entry (to average of entire map.)
      blendCache[h][mx][my] = mipLevels[h][mx][my] * 1f / (size * size) ;
    }
    if (h > 0) {  //non-leaf- flag underlying entries for similar updates.
      final int dX = mx * 2, dY = my * 2, dH = h - 1 ;
      //for (kx = -1 ; kx <= 1 ; kx++) for (ky = -1 ; ky <= 1 ; ky++) {
      for (int kx = 2 ; kx >= -1 ; kx--) for (int ky = 2 ; ky >= -1 ; ky--) {
        try { blendCache[dH][dX + kx][dY + ky] = Float.POSITIVE_INFINITY ; }
        catch (ArrayIndexOutOfBoundsException e) { continue ; }
        ///I.add(" (FA: "+(dX + kx)+" "+(dY + ky)+" "+dH+")") ;
      }
    }
    ///I.add(" Cached VAL IS: "+blendCache[h][mx][my]) ;
  }
  return blendCache[h][mx][my] ;
}


private float blendAt(final float x, final float y, final int h) {
  final int
    s = 1 << h,
    range = (size / s) - 1 ;
  float pX, pY ;
  int minX, maxX, minY, maxY ;
  pX = (x / s) - 0.5f ;
  pY = (y / s) - 0.5f ;
  pX = (pX < 0) ? 0 : (pX > range) ? range : pX ;
  pY = (pY < 0) ? 0 : (pY > range) ? range : pY ;
  minX = (int) pX ;
  minY = (int) pY ;
  maxX = (pX == minX) ? minX : minX + 1 ;
  maxY = (pY == minY) ? minY : minY + 1 ;
  final float
    aX = pX - minX,
    aY = pY - minY ;
  final float avg =
    (cacheAt(minX, minY, h) * (1 - aX) * (1 - aY)) +
    (cacheAt(maxX, minY, h) *      aX  * (1 - aY)) +
    (cacheAt(minX, maxY, h) * (1 - aX) *      aY ) +
    (cacheAt(maxX, maxY, h) *      aX  *      aY ) ;
  ///I.say(" alpha coords: "+aX+" "+aY+" "+avg) ;
  ///I.add(" Blending at: "+x+" "+y+" "+h+" result- "+avg) ;
  return avg ;
}
//*/