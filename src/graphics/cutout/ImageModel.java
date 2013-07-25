/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.cutout ;
import src.graphics.common.* ;
import src.util.* ;



public class ImageModel extends Model {
  
  
  final public static int
    TYPE_FLAT = 0,
    TYPE_BOX = 1,
    TYPE_POPPED_BOX = 2 ;
  final public static ImageModel
    DEFAULT_FLAT_MODEL = new ImageModel(
      "default_flat_model", ImageModel.class,
      Texture.WHITE_TEX, 1, 1, TYPE_FLAT
    ) ;
  
  final public int type ;
  final Texture texture ;
  final Vec3D coords[] ;
  private float framesUV[][] ;
  
  
  public Texture texture() { return texture ; }
  public float[][] framesUV() { return framesUV ; }
  
  
  
  /**  Basic constructors from which all factory methods derive their results-
    */
  private ImageModel(
    String modelName, Class modelClass,
    Texture texture,
    float tileSize, float height, int type
  ) {
    super(modelName, modelClass) ;
    this.bound = Math.max(tileSize, height) ;
    this.type = type ;
    this.texture = texture ;
    if (type == TYPE_FLAT) {
      this.coords = genFlatCoordinates(tileSize, height) ;
      this.framesUV = new float[][] { new float[] {
        0, 1,   1, 1,   1, 0,
        1, 0,   0, 0,   0, 1
      }} ;
    }
    else {
      final Box3D box = new Box3D().set(0, 0, 0, tileSize, tileSize, height) ;
      this.coords = genBoxCoords(box, type == TYPE_POPPED_BOX) ;
      this.framesUV = new float[][] { this.getUVForBox(this.coords) } ;
    }
  }
  
  
  
  /**  Public factory methods for different model types-
    */
  
  public static ImageModel asIsometricModel(
    Class modelClass,
    String texFile, float tileSize, float height
  ) {
    final String modelName = "IMAGE-MODEL-"+texFile ;
    Object cached = LoadService.getResource(modelName) ;
    if (cached != null) return (ImageModel) cached ;
    return new ImageModel(
      modelName, modelClass,
      Texture.loadTexture(texFile),
      tileSize, height, TYPE_POPPED_BOX
    ) ;
  }
  
  
  public static ImageModel asAnimatedModel(
    Class modelClass,
    Texture tex, int gridX, int gridY,
    float spriteSize, int numFrames, float duration
  ) {
    final String modelName = "IMAGE-MODEL-"+tex.name() ;
    Object cached = LoadService.getResource(modelName) ;
    if (cached != null) return (ImageModel) cached ;
    final ImageModel model = new ImageModel(
      modelName, modelClass,
      tex, spriteSize, spriteSize * gridX * 1f / gridY, TYPE_FLAT
    ) ;
    model.animateUV(gridX, gridY, numFrames) ;
    model.animRanges.add(new Model.AnimRange(
      "animation", 0, numFrames, duration
    )) ;
    return model ;
  }
  
  
  public static ImageModel[] loadModels(
    Class modelClass, int tileSize, float height,
    String path, String... filenames
  ) {
    final ImageModel models[] = new ImageModel[filenames.length] ;
    int i = 0 ; for (String s : filenames) {
      final ImageModel model = ImageModel.asIsometricModel(
        modelClass, path+s, tileSize, height
      ) ;
      models[i++] = model ;
    }
    return models ;
  }
  
  
  public static ImageModel[][] fromTextureGrid(
    Class modelClass,
    Texture tex, int gridSize, float tileSize
  ) {
    return fromTextureGrid(
      modelClass, tex, gridSize, gridSize, tileSize, TYPE_FLAT
    ) ;
  }
  
  
  public static ImageModel[][] fromTextureGrid(
    Class modelClass,
    Texture tex, int gridX, int gridY, float tileSize, int type
  ) {
    final String gridName = "IMAGE-MODEL-GRID-"+tex.name() ;
    Object cached = LoadService.getResource(gridName) ;
    if (cached != null) return (ImageModel[][]) cached ;
    final ImageModel cols[][] = new ImageModel[gridX][gridY] ;
    for (int x = gridX ; x-- > 0 ;) {
      cols[x] = new ImageModel[gridY] ;
      for (int y = gridY ; y-- > 0 ;) {
        final String modelName = "IMAGE-MODEL-"+tex.name()+"-GRID-"+x+"|"+y ;
        ImageModel model = new ImageModel(
          modelName, modelClass, tex,
          tileSize, tileSize * gridX * 1f / gridY, type
        ) ;
        model.translateUV(x, y, gridX, gridY) ;
        cols[x][gridY - (y + 1)] = model ;
      }
    }
    return cols ;
  }
  
  
  public static ImageModel asFlatModel(
    Class modelClass, Texture texture, float sSize, float sHigh
  ) {
    final String modelName = "IMAGE-MODEL-"+texture.name() ;
    Object cached = LoadService.getResource(modelName) ;
    if (cached != null) return (ImageModel) cached ;
    final ImageModel model = new ImageModel(
      modelName, modelClass,
      texture, sSize, sHigh, TYPE_FLAT
    ) ;
    model.translateUV(0, 0, 1, 1) ;
    return model ;
  }
  
  
  
  /**  Generation and translation of coordinates-
    */
  
  private Vec3D[] genFlatCoordinates(float s, float h) {
    final float
      w = s * (float) Math.sqrt(2),
      a = w * 0.05f,
      centreX = w * 0.5f,
      centreY = (float) Math.cos(Viewport.DEFAULT_ELEVATE) * centreX ;
    final Batch <Vec3D> points = new Batch <Vec3D> () ;
    points.add(new Vec3D(0, 0, a)) ;
    points.add(new Vec3D(w, 0, 0)) ;
    points.add(new Vec3D(w, h, 0)) ;
    points.add(new Vec3D(w, h, 0)) ;
    points.add(new Vec3D(0, h, 0)) ;
    points.add(new Vec3D(0, 0, a)) ;
    for (Vec3D p : points) {
      p.x -= centreX ;
      p.y = (p.y * w / s) - centreY ;
      Viewport.DEFAULT_VIEW.viewInvert(p) ;
    }
    return (Vec3D[]) points.toArray(Vec3D.class) ;
  }
  
  
  private void animateUV(int gridX, int gridY, int frames) {
    int frame = 0 ;
    float U, V ;
    final float oldUV[] = framesUV[0] ;
    final float newUV[][] = new float[frames][oldUV.length] ;
    loop: for (int y = 0 ; y < gridY ; y++) for (int x = 0 ; x < gridX ; x++) {
      for (int t = 0 ; t < oldUV.length ;) {
        U = texture.maxU() * (oldUV[t] + x) * 1f / gridX ;
        newUV[frame][t++] = U ;
        V = texture.maxV() * (oldUV[t] + y) * 1f / gridY ;
        newUV[frame][t++] = V ;
      }
      if (++frame == frames) break loop ;
    }
    framesUV = newUV ;
  }
  
  
  private void translateUV(int offU, int offV, int dimU, int dimV) {
    //I.say("Translating UV, x/y- "+offU+" "+offV) ;
    float U, V ;
    final float UV[] = framesUV[0], newUV[] = new float[UV.length] ;
    for (int t = 0 ; t < UV.length ;) {
      U = UV[t] * texture.maxU() ;
      newUV[t++] = (U + offU) * 1f / dimU ;
      V = UV[t] * texture.maxV() ;
      newUV[t++] = (V + offV) * 1f / dimV ;
      //I.say("  UV is: "+U+" "+V) ;
    }
    framesUV = new float[][] { newUV } ;
  }
  
  
  private Vec3D[] genBoxCoords(Box3D box, boolean popped) {
    final Batch <Vec3D> points = new Batch <Vec3D> () ;
    final float x = box.xdim(), y = box.ydim(), z = box.zdim() ;
    //  a, w and t are used to 'pucker' the corners of the box slightly, so
    //  preventing problems with depth-write overlap-flicker.
    //final float a = 0.1f, w = y * (1 - a), t = popped ? 0 : (z * a) ;
    //  First plane: y == 1.
    points.add(new Vec3D(0, 1, 0)) ;
    points.add(new Vec3D(1, 1, 0)) ;
    points.add(new Vec3D(1, 1, 1)) ;
    points.add(new Vec3D(1, 1, 1)) ;
    points.add(new Vec3D(0, 1, 1)) ;
    points.add(new Vec3D(0, 1, 0)) ;
    //  Second plane: x == 0.
    points.add(new Vec3D(0, 0, 0)) ;
    points.add(new Vec3D(0, 1, 0)) ;
    points.add(new Vec3D(0, 1, 1)) ;
    points.add(new Vec3D(0, 1, 1)) ;
    points.add(new Vec3D(0, 0, 1)) ;
    points.add(new Vec3D(0, 0, 0)) ;
    //  Third plane: z == 0.
    points.add(new Vec3D(0, 0, 0)) ;
    points.add(new Vec3D(1, 0, 0)) ;
    points.add(new Vec3D(0, 1, 0)) ;
    points.add(new Vec3D(1, 0, 0)) ;
    points.add(new Vec3D(1, 1, 0)) ;
    points.add(new Vec3D(0, 1, 0)) ;
    //  Invert if needed-
    if (popped) for (Vec3D p : points) {
      p.x = 1 - p.x ;
      p.y = 1 - p.y ;
      p.z = 1 - p.z ;
    }
    for (Vec3D p : points) {
      if (p.x == 1 && p.y == 0 && p.z == 1) {
        p.x = 0.9f ;
        p.y = 0.1f ;
        p.z = 0.9f ;
      }
      else if (p.x != 0 || p.y != 1) p.z *= 0.9f ;
    }
    for (Vec3D p : points) {
      p.x *= x ;
      p.y *= y ;
      p.z *= z ;
    }
    //  Offset by half dimensions in x/y, and return-
    for (Vec3D p : points) { p.x -= x / 2 ; p.y -= y / 2 ; }
    return (Vec3D[]) points.toArray(Vec3D.class) ;
  }
  

  private float[] getUVForBox(Vec3D[] boxCoords) {
    //
    //  First, translate all the coords from 3D to flat-screen coordinates.
    final float UV[] = new float[boxCoords.length * 2] ;
    int i = 0 ;
    Vec3D p = new Vec3D() ;
    float
      minU = Float.POSITIVE_INFINITY,
      maxU = Float.NEGATIVE_INFINITY,
      minV = Float.POSITIVE_INFINITY,
      maxV = Float.NEGATIVE_INFINITY,
      U, V ;
    for (Vec3D coord : boxCoords) {
      p.setTo(coord) ;
      Viewport.DEFAULT_VIEW.viewMatrix(p) ;
      UV[i++] = p.x ;
      UV[i++] = p.y ;
      minU = Math.min(minU, p.x) ;
      maxU = Math.max(maxU, p.x) ;
      minV = Math.min(minV, p.y) ;
      maxV = Math.max(maxV, p.y) ;
    }
    //
    //  We scale the sprite so that the width of the UV coordinates exactly
    //  fits the width of the available texture.
    final float scale = texture.maxU() / (maxU - minU) ;
    for (int t = 0 ; t < UV.length ;) {
      U = UV[t] ;
      U = (U - minU) * scale ;
      UV[t++] = U ;
      V = UV[t] ;
      V = (V - minV) * scale ;
      V = texture.maxV() - V ;
      UV[t++] = V ;
    }
    return UV ;
  }
  
  
  /**  Actual sprite production-
    */
  public ImageSprite makeSprite() {
    return new ImageSprite(this) ;
  }
  
  public Colour averageHue() {
    return texture.averaged() ;
  }
}



