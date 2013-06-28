/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.terrain ;
import src.graphics.common.* ;
import src.util.* ;
import org.lwjgl.opengl.GL11 ;



public class TerrainMesh extends MeshBuffer implements TileConstants {
  
  
  /**  Field definitions, setup and constructors-
    */
  final int numTiles ;
  private Texture textures[] ;
  private float animFrame = 0 ;
  
  
  
  protected TerrainMesh(int numTiles, Texture... textures) {
    super(numTiles * 2) ;
    this.numTiles = numTiles ;
    assignTexture(textures) ;
  }
  
  
  public void assignTexture(Texture... textures) {
    this.textures = textures ;
    for (Texture t : textures) {
      if (t == null) I.complain("NULL TEXTURE ASSIGNED!") ;
    }
  }
  
  
  public void setAnimTime(float progress) {
    this.animFrame = (progress % 1) * textures.length ;
  }
  
  
  
  /**  Rendering methods-
    */
  final static int GL_DISABLES[] = new int[] {
    GL11.GL_CULL_FACE,
    GL11.GL_ALPHA_TEST,
    ///GL11.GL_LIGHTING
  } ;
  public int[] GL_disables() { return GL_DISABLES ; }
  
  
  public void renderTo(Rendering rendering) {
    if (textures == null) I.complain("TEXTURE NOT ASSIGNED") ;
    if (numFacets == 0) return ;
    final Texture tex = textures[(int) animFrame] ;
    if (tex == null) return ;
    
    GL11.glDepthMask(false) ;
    textures[(int) animFrame].bindTex() ;
    super.renderTo(rendering) ;
    if (textures.length > 1) {
      Texture nextTex = textures[(int) (animFrame + 1) % textures.length] ;
      float opacity = animFrame - (int) animFrame ;
      GL11.glColor4f(1, 1, 1, opacity) ;
      nextTex.bindTex() ;
      super.renderTo(rendering) ;
    }
    GL11.glDepthMask(true) ;
  }
  
  
  
  /**  Generates a mesh from a mask object-
    */
  public static TerrainMesh genMesh(
    int minX, int minY,
    int maxX, int maxY,
    Texture texture,
    final byte heightMap[][],
    //final byte varsIndex[][],
    TileMask mask
  ) {
    //
    //  We iterate through each tile, recording the stream of geometry
    //  produced-
    MeshBuffer.beginRecord() ;
    int numTiled = 0 ;
    final boolean nullsCount = mask.nullsCount() ;
    for (int x = maxX ; x-- > minX ;) for (int y = maxY ; y-- > minY ;) {
      //
      //  We assume the use of inner fringing here, so only masked tiles are
      //  considered-
      if (! mask.maskAt(x, y)) continue ;
      int numNear = 0 ;
      for (int n : N_INDEX) {
        final int nX = N_X[n] + x ;
        final int nY = N_Y[n] + y ;
        try { nearT[n] = mask.maskAt(nX, nY) ; }
        catch (ArrayIndexOutOfBoundsException e) { nearT[n] = nullsCount ; }
        if (nearT[n]) numNear++ ;
      }
      final float UVslices[][] = (numNear == 8) ?
        TerrainPattern.extraFringeUV(mask.varID(x, y), false) :
        TerrainPattern.innerFringeUV(nearT) ;
      //
      //  Get geometry and normals appropriate to this tile, and push them
      //  for each slice (as multiple adjacent tiles might contribute fringing.)
      final float verts[] = getVertsAt(x, y, heightMap) ;
      final float norms[] = getNormsAt(x, y, heightMap) ;
      for (float[] slice : UVslices) if (slice != null) {
        MeshBuffer.recordGeom(verts, norms, slice) ;
        numTiled++ ;
      }
    }
    return compiledMesh(numTiled, texture) ;
  }
  
  
  
  /**  Convenience method for compiling geometry data-
    */
  private static TerrainMesh compiledMesh(int numTiled, Texture texture) {
    final Object geometry[] = MeshBuffer.compileRecord() ;
    TerrainMesh mesh = new TerrainMesh(numTiled * 2, texture) ;
    mesh.update(
      (float[]) geometry[0],
      (float[]) geometry[1],
      (float[]) geometry[2]
    ) ;
    return mesh ;
  }
  
  
  
  /**  Generates an array of meshes from the given sequence of terrain textures
    *  and a particular source.
    */
  public static TerrainMesh[] genMeshes(
    int minX, int minY,
    int maxX, int maxY,
    Texture textures[],
    final byte heightMap[][],
    final byte typeIndex[][],
    final byte varsIndex[][]
  ) {
    final TerrainMesh meshes[] = new TerrainMesh[textures.length] ;
    for (int index = 0 ; index < textures.length ; index++) {
      meshes[index] = TerrainMesh.genMesh(
        minX, minY, maxX, maxY,
        textures[index], index,
        heightMap, typeIndex, varsIndex
      ) ;
    }
    return meshes ;
  }
  
  
  
  /**  Generates a single terrain mesh with a particular texture from the given
    *  source.
    */
  public static TerrainMesh genMesh(
    int minX, int minY,
    int maxX, int maxY,
    Texture texture, int texID,
    byte heightMap[][],
    byte typeIndex[][],
    byte varsIndex[][]
  ) {
    //
    //  We iterate through each tile, recording the stream of geometry
    //  produced-
    MeshBuffer.beginRecord() ;
    int numTiled = 0 ;
    for (int x = maxX ; x-- > minX ;) for (int y = maxY ; y-- > minY ;) {
      //
      //  Actually matching the tile type may result in a random variation.
      //  Otherwise, get the identity of surrounding tiles, and the UV slices
      //  appropriate.
      float UVslices[][] ;
      if (texID == typeIndex[x][y]) {
        UVslices = TerrainPattern.extraFringeUV(varsIndex[x][y], true) ;
      }
      else {
        if (typeIndex[x][y] > texID) continue ;
        for (int n : N_INDEX) {
          final int nX = N_X[n] + x ;
          final int nY = N_Y[n] + y ;
          try { nearT[n] = typeIndex[nX][nY] == texID ; }
          catch (ArrayIndexOutOfBoundsException e) { nearT[n] = false ; }
        }
        UVslices = TerrainPattern.outerFringeUV(nearT) ;
      }
      //
      //  Get geometry and normals appropriate to this tile, and push them
      //  for each slice (as multiple adjacent tiles might contribute fringing.)
      final float verts[] = getVertsAt(x, y, heightMap) ;
      final float norms[] = getNormsAt(x, y, heightMap) ;
      for (float[] slice : UVslices) if (slice != null) {
        MeshBuffer.recordGeom(verts, norms, slice) ;
        numTiled++ ;
      }
    }
    return compiledMesh(numTiled, texture) ;
  }
  

  /**  Generates a single terrain mesh, intended to allow a particular texture
    *  to be overlaid uniformly on top of the terrain.
    */
  public static TerrainMesh getTexUVMesh(
    int minX, int minY,
    int maxX, int maxY,
    byte heightMap[][],
    int texSize
  ) {
    MeshBuffer.beginRecord() ;
    final int mapSize = heightMap.length - 1 ;
    int numTiled = 0 ;
    for (int x = maxX ; x-- > minX ;) for (int y = maxY ; y-- > minY ;) {
      final float verts[] = getVertsAt(x, y, heightMap) ;
      final float norms[] = getNormsAt(x, y, heightMap) ;
      final float UV[] = genTexUV(verts, mapSize, texSize) ;
      MeshBuffer.recordGeom(verts, norms, UV) ;
      numTiled++ ;
    }
    final Object geometry[] = MeshBuffer.compileRecord() ;
    TerrainMesh mesh = new TerrainMesh(numTiled * 2) ;
    mesh.update(
      (float[]) geometry[0],
      (float[]) geometry[1],
      (float[]) geometry[2]
    ) ;
    return mesh ;
  }
  
  
  /**  Assorted helper methods and temporary data.
    */
  final private static float
    tempV[] = new float[18],
    tempN[] = new float[18],
    tempT[] = new float[12] ;
  final private static Vec3D
    tempNorm = new Vec3D() ;
  final private static boolean
    nearT[] = new boolean[8] ;
  
  
  private static float[] getVertsAt(int x, int y, byte heightMap[][]) {
    tempV[0 ] = tempV[9 ] = x - 0.5f ;
    tempV[1 ] = tempV[4 ] = y - 0.5f ;
    tempV[3 ] = tempV[6 ] = x + 0.5f ;
    tempV[7 ] = tempV[10] = y + 0.5f ;
    tempV[2 ] = heightMap[x    ][y    ] / 4f ;
    tempV[5 ] = heightMap[x + 1][y    ] / 4f ;
    tempV[8 ] = heightMap[x + 1][y + 1] / 4f ;
    tempV[11] = heightMap[x    ][y + 1] / 4f ;
    copyLast2(tempV, 0) ;
    return tempV ;
  }

  
  private static float[] getNormsAt(int x, int y, byte heightMap[][]) {
    packNormAt(x    , y    , heightMap, 0) ;
    packNormAt(x + 1, y    , heightMap, 3) ;
    packNormAt(x + 1, y + 1, heightMap, 6) ;
    packNormAt(x    , y + 1, heightMap, 9) ;
    copyLast2(tempN, 0) ;
    return tempN ;
  }
  
  
  private static float[] genTexUV(float verts[], int mapSize, int texSize) {
    int vI = 0, tI = 0 ;
    final float min = 0.5f / texSize, max = (texSize - 0.5f) / texSize ;
    while (tI < tempT.length) {
      tempT[tI++] = Visit.clamp(verts[vI++] / (mapSize + 1), min, max) ;
      tempT[tI++] = Visit.clamp(verts[vI++] / (mapSize + 1), min, max) ;
      vI++ ;
    }
    return tempT ;
  }
  
  
  private static void packNormAt(int x, int y, byte heightMap[][], int i) {
    //
    //  We have to allow for 'edge cases' against the sides of the map, which
    //  complicates matters slightly.
    final float slopeX ;
    if (x > 0 && x < heightMap.length - 1) slopeX = (
      (heightMap[x][y] - heightMap[x - 1][y]) +
      (heightMap[x + 1][y] - heightMap[x][y])
    ) / 2 ;
    else if (x == 0) slopeX = heightMap[x + 1][y] - heightMap[x][y] ;
    else slopeX = heightMap[x][y] - heightMap[x - 1][y] ;
    //
    //  By getting the average slope at a given corner of a tile, we can get an
    //  apropriate normal value.
    final float slopeY ;
    if (y > 0 && y < heightMap[0].length - 1) slopeY = (
      (heightMap[x][y] - heightMap[x][y - 1]) +
      (heightMap[x][y + 1] - heightMap[x][y])
    ) / 2 ;
    else if (y == 0) slopeY = heightMap[x][y + 1] - heightMap[x][y] ;
    else slopeY = heightMap[x][y] - heightMap[x][y - 1] ;
    //
    //  We then pack said normal into the array at the proper index, and
    //  return-
    tempNorm.set(-slopeX / 4, -slopeY / 4, 1) ;
    tempNorm.normalise() ;
    tempN[0 + i] = tempNorm.x ;
    tempN[1 + i] = tempNorm.y ;
    tempN[2 + i] = tempNorm.z ;
  }
  
  
  private final static void copyLast2(final float a[], final int i) {
    for (int n = 3 ; n-- > 0 ;) {
      a[i + 12 + n] = a[i + 0 + n] ;
      a[i + 15 + n] = a[i + 6 + n] ;
    }
  }
  
}





