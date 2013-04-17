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
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][] ;
  final Habitat
    habitats[][] ;
  
  
  private static class MeshPatch {
    TerrainMesh meshes[], roadsMesh, earthMesh, fogMesh ;
    boolean updateMesh, updateRoads ;
  }
  
  private int patchGridSize, patchSize ;
  private MeshPatch patches[][] ;
  private Batch <MeshPatch> needUpdates = new Batch <MeshPatch> () ;
  
  
  
  Terrain(
    Habitat[] gradient,
    byte typeIndex[][],
    byte varsIndex[][],
    byte heightVals[][]
  ) {
    this.mapSize = typeIndex.length ;
    this.typeIndex = typeIndex ;
    this.varsIndex = varsIndex ;
    this.heightVals = heightVals ;
    this.habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
  }
  
  
  
  public Terrain(Session s) throws Exception {
    s.cacheInstance(this) ;
    mapSize = s.loadInt() ;
    
    heightVals = new byte[mapSize + 1][mapSize + 1] ;
    typeIndex = new byte[mapSize][mapSize] ;
    varsIndex = new byte[mapSize][mapSize] ;
    s.loadByteArray(heightVals) ;
    s.loadByteArray(typeIndex) ;
    s.loadByteArray(varsIndex) ;
    
    habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize) ;
    s.saveByteArray(heightVals) ;
    s.saveByteArray(typeIndex) ;
    s.saveByteArray(varsIndex) ;
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  public float trueHeight(int x, int y) {
    return HeightMap.sampleAt(mapSize, heightVals, x, y) / 4 ;
  }
  
  
  
  /**  Rendering and interface methods-
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



