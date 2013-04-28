/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.graphics.terrain.TerrainMesh.Mask ;
import src.util.* ;



//  TODO:  An underground level, showing mineral deposits?
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
  private byte
    roadMask[][] ;
  final Habitat
    habitats[][] ;
  
  final Mask pavingMask = new Mask() {
    protected boolean maskAt(int x, int y) {
      return roadMask[x][y] > 0 ;
    }
  } ;
  
  private static class MeshPatch {
    int x, y ;
    TerrainMesh meshes[], roadsMesh, earthMesh, fogMesh ;
    boolean updateMesh, updateRoads ;
  }
  
  
  private int patchGridSize, patchSize ;
  private MeshPatch patches[][] ;
  
  
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
    this.roadMask = new byte[mapSize][mapSize] ;
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
    
    roadMask = new byte[mapSize][mapSize] ;
    s.loadByteArray(roadMask) ;
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
    
    s.saveByteArray(roadMask) ;
  }
  
  
  
  /**  Helper methods
    */
  private MeshPatch[] patchesUnder(Box2D area) {
    final int
      minX = (int) (area.xpos() / patchSize),
      minY = (int) (area.ypos() / patchSize),
      dimX = 1 + (int) ((area.xmax() - 1) / patchSize) - minX,
      dimY = 1 + (int) ((area.ymax() - 1) / patchSize) - minY ;
    final MeshPatch under[] = new MeshPatch[dimX * dimY] ;
    int i = 0 ; for (Coord c : Visit.grid(minX, minY, dimX, dimY, 1)) {
      under[i++] = patches[c.x][c.y] ;
    }
    return under ;
  }
  
  
  /**  Modifying and querying terrain contents from within the world-
    */
  public void maskAsPaved(Tile tiles[], boolean is) {
    if (tiles == null) return ;
    final Tile o = tiles[0] ;
    final Box2D bounds = new Box2D().set(o.x, o.y, 0, 0) ;
    for (Tile t : tiles) if (t != null) {
      roadMask[t.x][t.y] += is ? 1 : -1 ;
      bounds.include(t.x, t.y, 0.5f) ;
    }
    bounds.expandBy(1) ;
    for (MeshPatch patch : patchesUnder(bounds)) patch.updateRoads = true ;
  }
  
  
  public void setHabitat(Tile t, Habitat h) {
    habitats[t.x][t.y] = h ;
    typeIndex[t.x][t.y] = (byte) h.ID ;
    final MeshPatch patch = patches[t.x / patchSize][t.y / patchSize] ;
    patch.updateMesh = true ;
  }
  
  
  public boolean isRoad(Tile t) {
    return roadMask[t.x][t.y] > 0 ;
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  public float trueHeight(int x, int y) {
    return HeightMap.sampleAt(mapSize, heightVals, x, y) / 4 ;
  }
  
  
  public TerrainMesh createOverlay(Tile tiles[], Texture tex) {
    if (tiles == null || tiles.length < 1) I.complain("No tiles in overlay!") ;
    final Table <Tile, Tile> pathTable = new Table(tiles.length) ;
    Box2D area = null ;
    for (Tile t : tiles) {
      if (area == null) area = new Box2D().set(t.x, t.y, 0, 0) ;
      pathTable.put(t, t) ;
      area.include(t.x, t.y, 0.5f) ;
    }
    final World world = tiles[0].world ;
    final TerrainMesh overlay = createOverlay(
      area, tex,
      new TerrainMesh.Mask() { protected boolean maskAt(int x, int y) {
        final Tile t = world.tileAt(x, y) ;
        return (t == null) ? false : (pathTable.get(t) != null) ;
      } }
    ) ;
    return overlay ;
  }
  
  
  public TerrainMesh createOverlay(Box2D area, Texture tex, Mask mask) {
    final int
      minX = (int) Math.ceil(area.xpos()),
      minY = (int) Math.ceil(area.ypos()),
      dimX = (int) area.xdim(),
      dimY = (int) area.ydim() ;
    final TerrainMesh overlay = TerrainMesh.genMesh(
      minX, minY, minX + dimX, minY + dimY,
      tex, heightVals, mask
    ) ;
    return overlay ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void initPatchGrid(int patchSize) {
    this.patchSize = patchSize ;
    this.patchGridSize = mapSize / patchSize ;
    patches = new MeshPatch[patchGridSize][patchGridSize] ;
    for (Coord c : Visit.grid(0, 0, patchGridSize, patchGridSize, 1)) {
      final MeshPatch p = patches[c.x][c.y] = new MeshPatch() ;
      p.x = c.x * patchSize ;
      p.y = c.y * patchSize ;
      updatePatchMesh(p) ;
      updatePatchRoads(p) ;
    }
  }
  
  
  private void updatePatchMesh(MeshPatch patch) {
    final int x = patch.x, y = patch.y ;
    patch.meshes = TerrainMesh.genMeshes(
      x, y, x + patchSize, y + patchSize,
      Habitat.BASE_TEXTURES,
      heightVals, typeIndex, varsIndex
    ) ;
    animate(patch.meshes, Habitat.SHALLOWS) ;
    animate(patch.meshes, Habitat.OCEAN   ) ;
    patch.fogMesh = TerrainMesh.getTexUVMesh(
      x, y, x + patchSize, y + patchSize,
      heightVals, mapSize
    ) ;
    patch.updateMesh = false ;
  }
  
  
  private void updatePatchRoads(MeshPatch patch) {
    final int x = patch.x, y = patch.y ;
    patch.roadsMesh = TerrainMesh.genMesh(
      x, y, x + patchSize, y + patchSize,
      Habitat.ROAD_TEXTURE, heightVals, pavingMask
    ) ;
    patch.updateRoads = false ;
  }
  
  
  private void animate(TerrainMesh meshes[], Habitat h) {
    meshes[h.ID].assignTexture(h.animTex) ;
  }
  
  
  public void renderFor(Box2D area, Rendering rendering, float time) {
    if (patches == null) I.complain("PATCHES MUST BE INITIALISED FIRST!") ;
    for (MeshPatch patch : patchesUnder(area)) {
      if (patch.updateMesh ) updatePatchMesh (patch) ;
      if (patch.updateRoads) updatePatchRoads(patch) ;
      for (TerrainMesh mesh : patch.meshes) {
        mesh.setAnimTime(time) ;
        rendering.addClient(mesh) ;
      }
      if (patch.roadsMesh != null) rendering.addClient(patch.roadsMesh) ;
    }
  }
  
  
  public void renderFogFor(Box2D area, Texture fog, Rendering rendering) {
    if (fog == null )I.say("FOG IS NULL!") ;
    if (patches == null) I.complain("PATCHES MUST BE INITIALISED FIRST!") ;
    for (MeshPatch patch : patchesUnder(area)) {
      patch.fogMesh.assignTexture(fog) ;
      rendering.addClient(patch.fogMesh) ;
    }
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



