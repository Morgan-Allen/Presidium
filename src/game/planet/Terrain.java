/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.util.* ;



//
//  TODO:  You'll have to replace this with a different system.  More similar
//  to the old one, based on dither values and pre-gen regions.

//  I want an underground level, storing mineral deposits.


public class Terrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    SECTOR_SIZE     = 16,
    SMOOTH_MARGIN   = 2,
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10,
    GROWTH_INTERVAL = Planet.DAY_LENGTH ;
  
  final public int
    mapSize ;
  private float
    maxElevation,
    seaLevel ;
  private float
    insolation,
    moisture,
    radiation ;
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][] ;
  final Habitat
    habitats[][] ;
  
  
  private static class MeshPatch {
    //  TODO:  You may also want specialised meshes for water and roads.
    TerrainMesh meshes[], roadsMesh, fogMesh ;
    boolean needsUpdate ;
  }
  
  private int patchGridSize, patchSize ;
  private MeshPatch patches[][] ;
  private Batch <MeshPatch> needUpdates = new Batch <MeshPatch> () ;
  
  
  
  //  TODO:  You must implement save/load functions!
  /**  Default constructor.
    */
  public Terrain(
    int minSize,
    float relativeElevation,
    float landAmount,
    float insolation,
    float moisture,
    float radiation
  ) {
    this.mapSize = checkMapSize(minSize) ;
    this.habitats = new Habitat[mapSize][mapSize] ;
    this.maxElevation = relativeElevation * mapSize ;
    this.seaLevel = HeightMap.heightCoveringArea(1 - landAmount) ;
    I.say("SEA LEVEL IS: "+seaLevel+" MAX ELEVATION: "+maxElevation) ;
    final HeightMap elevationMap = new HeightMap(mapSize) ;
    this.heightVals = elevationMap.asScaledBytes(maxElevation) ;
    this.moisture = moisture ;
    this.insolation = insolation ;
    this.radiation = radiation ;
    //
    //  TODO:  Create a small height map for radiation.
    //
    //  With that done, you simply(tm) visit every tile on the map, and
    //  determine the terrain type appropriate.
    this.typeIndex = new byte[mapSize][mapSize] ;
    this.varsIndex = new byte[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final Habitat habitat = genHabitat(c.x, c.y) ;
      habitats[c.x][c.y] = habitat ;
      typeIndex[c.x][c.y] = (byte) habitat.ID ;
      varsIndex[c.x][c.y] = terrainVarsAt(c.x, c.y) ;
    }
    //
    //  Flatten the whole thing.
    //  TODO:  You'll need to store these values separately, in that case.
    for (Coord c : Visit.grid(0, 0, mapSize + 1, mapSize + 1, 1)) {
      heightVals[c.x][c.y] = 0 ;
      /*
      final Habitat h = habitats[c.x][c.y] ;
      byte height = 0 ;
      if (h.ID <= Habitat.SHORELINE.ID) height = -1 ;
      else if (h.ID >= Habitat.BARRENS.ID) height = 1 ;
      else if (h.ID >= Habitat.DESERT.ID) height = 2 ;
      sinkHeight(c.x, c.y, height) ;
      sinkHeight(c.x + 1, c.y, height) ;
      sinkHeight(c.x, c.y + 1, height) ;
      sinkHeight(c.x + 1, c.y + 1, height) ;
      //*/
    }
    paintEdge(Habitat.OCEAN, Habitat.SHORELINE) ;
    paintEdge(Habitat.OCEAN, Habitat.SHALLOWS) ;
  }
  
  
  public Terrain(Session s) throws Exception {
    s.cacheInstance(this) ;
    mapSize = s.loadInt() ;
    maxElevation = s.loadFloat() ;
    seaLevel = s.loadFloat() ;
    moisture = s.loadFloat() ;
    insolation = s.loadFloat() ;
    radiation = s.loadFloat() ;
    
    heightVals = new byte[mapSize + 1][mapSize + 1] ;
    typeIndex = new byte[mapSize][mapSize] ;
    varsIndex = new byte[mapSize][mapSize] ;
    s.loadByteArray(heightVals) ;
    s.loadByteArray(typeIndex) ;
    s.loadByteArray(varsIndex) ;
    
    //
    //  TODO:  True habitats may have to be stored differently.
    habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize) ;
    s.saveFloat(maxElevation) ;
    s.saveFloat(seaLevel) ;
    s.saveFloat(moisture) ;
    s.saveFloat(insolation) ;
    s.saveFloat(radiation) ;
    s.saveByteArray(heightVals) ;
    s.saveByteArray(typeIndex) ;
    s.saveByteArray(varsIndex) ;
  }
  
  
  
  
  
  
  /**  Various helper methods invoked during setup-
    */
  private int checkMapSize(int minSize) {
    int mapSize = SECTOR_SIZE ;
    while (mapSize < minSize) mapSize *= 2 ;
    if (mapSize == minSize) return mapSize ;
    I.complain("MAP SIZE MUST BE A POWER OF 2 MULTIPLE OF SECTOR SIZE.") ;
    return -1  ;
  }
  
  
  private void sinkHeight(int x, int y, int val) {
    heightVals[x][y] = (byte) Math.min(heightVals[x][y], val) ;
  }
  
  
  private void paintEdge(Habitat edge, Habitat replace) {
    final Batch <Coord> toPaint = new Batch <Coord> () ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final Habitat h = habitats[c.x][c.y] ;
      if (h != edge) continue ;
      boolean inside = true ;
      for (int i : N_INDEX) {
        final Habitat n = habitatAt(c.x + N_X[i], c.y + N_Y[i]) ;
        if (n != null && n != edge) { inside = false ; break ; }
      }
      if (! inside) toPaint.add(new Coord(c)) ;
    }
    for (Coord c : toPaint) {
      habitats[c.x][c.y] = replace ;
      typeIndex[c.x][c.y] = (byte) replace.ID ;
    }
  }
  
  
  private float sampleAt(byte vals[][], float mX, float mY) {
    mX *= (vals.length - 1) * 1f / mapSize ;
    mY *= (vals.length - 1) * 1f / mapSize ;
    final int vX = (int) mX, vY = (int) mY ;
    final float rX = mX % 1, rY = mY % 1 ;
    return
      (vals[vX    ][vY    ] * (1 - rX) * (1 - rY)) +
      (vals[vX + 1][vY    ] * rX       * (1 - rY)) +
      (vals[vX    ][vY + 1] * (1 - rX) * rY      ) +
      (vals[vX + 1][vY + 1] * rX       * rY      ) ;
  }
  
  
  
  /**  Here are methods related to populating and updating the world-
    */
  
  final private static float RELIEF_HEIGHTS[] = new float[100] ;
  static { for (int i = 100 ; i-- > 0 ;) {
    RELIEF_HEIGHTS[i] = HeightMap.areaUnderHeight(i / 100f) ;
  }}
  
  final Habitat GRADIENT[] = {
    Habitat.MEADOW,
    Habitat.BARRENS,
    Habitat.DESERT
  } ;
  
  
  private Habitat genHabitat(int x, int y) {
    float high = sampleAt(heightVals, x, y) / maxElevation ;
    if (high < seaLevel) {
      return Habitat.OCEAN ;
    }
    high = RELIEF_HEIGHTS[(int) (high * 99.99f)] ;
    final float seaRelief = RELIEF_HEIGHTS[(int) (seaLevel * 99.99f)] ;
    high = (high - seaRelief) / (1 - seaRelief) ;
    high += 0.5f - (moisture / 10) ;
    high = Visit.clamp(high, 0, 0.99f) ;
    int index = (int) (high * GRADIENT.length) ;
    return GRADIENT[Visit.clamp(index, GRADIENT.length)] ;
  }
  
  
  private byte terrainVarsAt(int x, int y) {
    final int dir = Rand.index(N_INDEX.length) ;
    byte sampleVar ;
    try { sampleVar = varsIndex[x + N_X[dir]][y + N_Y[dir]] ; }
    catch (ArrayIndexOutOfBoundsException e) { sampleVar = 0 ; }
    if (sampleVar == 0) sampleVar = (byte) (Rand.index(5) + 1) ;
    varsIndex[x][y] = sampleVar ;
    return sampleVar ;
  }
  
  
  
  /**  Returns the average height of the given area of terrain-
    */
  public byte heightAverage(int x, int y, int xD, int yD) {
    float sum = 0, numTiles = 0 ;
    for (Coord c : Visit.grid(x, y, xD, yD, 1)) {
      sum += heightVals[c.x][c.y] ;
      numTiles++ ;
    }
    return (byte) ((sum / numTiles) + 0.5f) ;
  }
  
  public float insolation() {
    return insolation ;
  }
  
  public float moisture() {
    return moisture ;
  }
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  public float trueHeight(int x, int y) {
    return sampleAt(heightVals, x, y) / 4 ;
  }
  
  public float trueMaxHeight() {
    return maxElevation / 4 ;
  }
  
  public float seaLevel() {
    return seaLevel ;
  }
  
  
  
  
  
  
  /**  Here are methods related to rendering-
    */
  public void initPatchGrid(int patchSize) {
    this.patchSize = patchSize ;
    this.patchGridSize = mapSize / patchSize ;
    patches = new MeshPatch[patchGridSize][patchGridSize] ;
    for (Coord c : Visit.grid(0, 0, patchGridSize, patchGridSize, 1)) {
      patches[c.x][c.y] = new MeshPatch() ;
      updatePatchAt(c.x * patchSize, c.y * patchSize) ;
    }
  }
  
  
  private void updatePatchAt(int x, int y) {
    final MeshPatch patch = patches[x / patchSize][y / patchSize] ;
    patch.meshes = TerrainMesh.genMeshes(
      x, y, x + patchSize, y + patchSize,
      Habitat.BASE_TEXTURES,
      heightVals, typeIndex, varsIndex
    ) ;
    animate(patch.meshes, Habitat.SHALLOWS) ;
    animate(patch.meshes, Habitat.OCEAN   ) ;
    //
    //
    patch.fogMesh = TerrainMesh.getTexUVMesh(
      x, y, x + patchSize, y + patchSize,
      heightVals, mapSize
    ) ;
  }
  
  
  public void updatePatchRoads(int x, int y, TerrainMesh.Mask mask) {
    final MeshPatch patch = patches[x / patchSize][y / patchSize] ;
    patch.roadsMesh = TerrainMesh.genMesh(
      x, y, x + patchSize, y + patchSize,
      Habitat.ROAD_TEXTURE, heightVals, mask
    ) ;
  }
  
  
  private void animate(TerrainMesh meshes[], Habitat h) {
    meshes[h.ID].assignTexture(h.animTex) ;
  }
  
  /*
  public void setTerrainAt(int x, int y, int type) {
  }
  
  public void setTerrainHeight(int x, int y, byte level) {
  }
  
  private void flagUpdateAt(int x, int y) {
    final MeshPatch patch = patches[x / patchSize][x / patchSize] ;
    if (patch.needsUpdate) return ;
    patch.needsUpdate = true ;
    needUpdates.add(patch) ;
  }
  //*/
  
  
  public void renderFor(int x, int y, Rendering rendering, float time) {
    if (patches == null) I.complain("PATCHES MUST BE INITIALISED FIRST!") ;
    final MeshPatch patch = patches[x / patchSize][y / patchSize] ;
    //
    //  TODO:  You also need to render the roads.
    for (TerrainMesh mesh : patch.meshes) {
      mesh.setAnimTime(time) ;
      rendering.addClient(mesh) ;
    }
    if (patch.roadsMesh != null) rendering.addClient(patch.roadsMesh) ;
  }
  
  
  public void renderFogFor(int x, int y, Texture fog, Rendering rendering) {
    if (fog == null )I.say("FOG IS NULL!") ;
    if (patches == null) I.complain("PATCHES MUST BE INITIALISED FIRST!") ;
    final MeshPatch patch = patches[x / patchSize][y / patchSize] ;
    patch.fogMesh.assignTexture(fog) ;
    rendering.addClient(patch.fogMesh) ;
  }
}






/**  Generates an initial texture for the whole map.
  *  TODO:  Allow for incremental updates to said texture, whenever the
  *  terrain in a given sector is disturbed.
  */
/*
//
//  TODO:  Get rid of these features?  They're not seeing use.
public Texture wholeMapTexture(int resolution, float opacity) {
  int sizeW = mapSize / resolution ;
  final Texture wholeMap = Texture.createTexture(sizeW, sizeW) ;
  final byte vals[] = new byte[sizeW * sizeW * 4] ;
  int mark = 0 ;
  for (int y = 0 ; y < mapSize ; y += resolution)
    for (int x = 0 ; x < mapSize ; x += resolution) {
      final Colour avg = avgSectorColour(x, y, resolution) ;
      avg.a = opacity ;
      avg.storeByteValue(vals, mark++ * 4) ;
    }
  wholeMap.putBytes(vals) ;
  return wholeMap ;
}


private Colour avgSectorColour(int sX, int sY, int resolution) {
  final Colour avg = new Colour() ;
  int numTiles = 0 ;
  for (int x = sX ; x < sX + resolution ; x++)
    for (int y = sY ; y < sY + resolution ; y++) {
      final Texture tileTex = ALL_TERRAIN_TEX[typeIndex[x][y]] ;
      final Colour atTile = tileTex.averaged() ;
      avg.r += atTile.r ;
      avg.g += atTile.g ;
      avg.b += atTile.b ;
      numTiles++ ;
    }
  avg.r /= numTiles ;
  avg.g /= numTiles ;
  avg.b /= numTiles ;
  return avg ;
}


  
  private void smoothSectorArea(int sX, int sY) {
    byte level = heightAverage(sX, sY, SECTOR_SIZE, SECTOR_SIZE), diff ;
    final int m = SMOOTH_MARGIN, span = SECTOR_SIZE - (m * 2) ;
    for (Coord c : Visit.grid(sX + m, sY + m, span, span, 1)) {
      byte high = heightVals[c.x][c.y] ;
      if (high / maxElevation < seaLevel) continue ;
      diff = (byte) (high - level) ;
      heightVals[c.x][c.y] = (Math.abs(diff) < 2) ?
        level :
        (byte) (level + (diff / 2)) ;
    }
  }
//*/



