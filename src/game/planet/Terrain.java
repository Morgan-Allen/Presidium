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



public class Terrain implements TileConstants, Session.Saveable {
  
  
  final public static int
    MAX_INSOLATION  = 10,
    MAX_MOISTURE    = 10,
    MAX_RADIATION   = 10 ;
  
  final public static byte
    TYPE_METALS   = 1,
    TYPE_CARBONS  = 2,
    TYPE_ISOTOPES = 3,
    TYPE_NOTHING  = 0,
    
    DEGREE_TRACE  = 1,
    DEGREE_COMMON = 2,
    DEGREE_HEAVY  = 3,
    DEGREE_TAKEN  = 0,
    
    AMOUNT_TRACE    = 1,
    AMOUNT_COMMON   = 3,
    AMOUNT_HEAVY    = 9,
    
    NUM_TYPES = 4,
    NUM_DEGREES = 4,
    MAX_MINERAL_AMOUNT = 10 ;
  
  
  final public int
    mapSize ;
  private byte
    heightVals[][],
    typeIndex[][],
    varsIndex[][] ;
  
  final Habitat
    habitats[][] ;
  private byte
    minerals[][] ;
  private byte
    roadCounter[][] ;
  
  
  final TileMask pavingMask = new TileMask() {
    public boolean maskAt(int x, int y) {
      return roadCounter[x][y] > 0 ;
    }
    public boolean nullsCount() {
      return true ;
    }
    public byte varID(int x, int y) {
      //
      //  TODO:  Base this on no. of adjacent fixtures, spaced intervals, etc.
      return (byte) (x + y) ;
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
    this.roadCounter = new byte[mapSize][mapSize] ;
    this.habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
    this.minerals = new byte[mapSize][mapSize] ;
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
    
    roadCounter = new byte[mapSize][mapSize] ;
    s.loadByteArray(roadCounter) ;
    
    habitats = new Habitat[mapSize][mapSize] ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      habitats[c.x][c.y] = Habitat.ALL_HABITATS[typeIndex[c.x][c.y]] ;
    }
    minerals = new byte[mapSize][mapSize] ;
    s.loadByteArray(minerals) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(mapSize) ;
    s.saveByteArray(heightVals) ;
    s.saveByteArray(typeIndex) ;
    s.saveByteArray(varsIndex) ;
    
    s.saveByteArray(roadCounter) ;
    s.saveByteArray(minerals) ;
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
  
  
  
  /**  Habitats and mineral deposits-
    */
  private static Tile tempV[] = new Tile[9] ;
  
  public void setHabitat(Tile t, Habitat h) {
    habitats[t.x][t.y] = h ;
    typeIndex[t.x][t.y] = (byte) h.ID ;
    t.refreshHabitat() ;
    for (Tile n : t.vicinity(tempV)) if (n != null) {
      final MeshPatch patch = patches[n.x / patchSize][n.y / patchSize] ;
      patch.updateMesh = true ;
    }
  }
  
  
  public Habitat habitatAt(int x, int y) {
    try { return habitats[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  public float mineralsAt(Tile t, byte type) {
    byte m = minerals[t.x][t.y] ;
    if (m == 0) return 0 ;
    if (m / NUM_TYPES != type) return 0 ;
    switch (m % NUM_TYPES) {
      case (DEGREE_TRACE)  : return AMOUNT_TRACE  ;
      case (DEGREE_COMMON) : return AMOUNT_COMMON ;
      case (DEGREE_HEAVY ) : return AMOUNT_HEAVY  ;
    }
    return 0 ;
  }
  
  
  public byte mineralType(Tile t) {
    byte m = minerals[t.x][t.y] ;
    if (m == 0) return 0 ;
    return (byte) (m / NUM_TYPES) ;
  }
  
  
  public float extractMineralAt(Tile t, byte type) {
    final float amount = mineralsAt(t, type) ;
    if (amount <= 0) I.complain("Can't extract that mineral type!") ;
    minerals[t.x][t.y] = (byte) ((type * NUM_DEGREES) + DEGREE_TAKEN) ;
    return amount ;
  }
  
  
  public void setMinerals(Tile t, byte type, byte degree) {
    minerals[t.x][t.y] = (byte) ((type * NUM_DEGREES) + degree) ;
  }
  
  
  public float trueHeight(float x, float y) {
    return Visit.sampleMap(mapSize, heightVals, x, y) / 4 ;
  }
  
  
  public int varAt(Tile t) {
    return varsIndex[t.x][t.y] ;
  }
  
  
  
  /**  Pavements and overlays-
    */
  public boolean isRoad(Tile t) {
    return roadCounter[t.x][t.y] > 0 ;
  }
  
  
  public void maskAsPaved(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return ;
    ///if (! is) I.say("...Roads masking begins.") ;
    Box2D bounds = null ;
    for (Tile t : tiles) if (t != null) {
      if (bounds == null) bounds = new Box2D().set(t.x, t.y, 0, 0) ;
      final byte c = (roadCounter[t.x][t.y] += is ? 1 : -1) ;
      ///if (! is) I.say("  Counter is: "+c+" at "+t.x+"/"+t.y) ;
      if (c < 0) I.complain("CANNOT HAVE NEGATIVE ROAD COUNTER: "+t) ;
      bounds.include(t.x, t.y, 0.5f) ;
    }
    bounds.expandBy(1) ;
    for (MeshPatch patch : patchesUnder(bounds)) patch.updateRoads = true ;
    ///if (! is) I.say("...Roads masking complete.") ;
  }
  
  
  public TerrainMesh createOverlay(
    final World world, Tile tiles[], final boolean nullsCount, Texture tex
  ) {
    if (tiles == null || tiles.length < 1) I.complain("No tiles in overlay!") ;
    final Table <Tile, Tile> pathTable = new Table(tiles.length) ;
    Box2D area = null ;
    for (Tile t : tiles) if (t != null) {
      if (area == null) area = new Box2D().set(t.x, t.y, 0, 0) ;
      pathTable.put(t, t) ;
      area.include(t.x, t.y, 0.5f) ;
    }
    final TerrainMesh overlay = createOverlay(
      area, tex,
      new TerrainMesh.TileMask() {
        public boolean maskAt(int x, int y) {
          final Tile t = world.tileAt(x, y) ;
          return (t == null) ? false : (pathTable.get(t) != null) ;
        }
        public boolean nullsCount() {
          return nullsCount ;
        }
      }
    ) ;
    return overlay ;
  }
  
  
  public TerrainMesh createOverlay(Box2D area, Texture tex, TileMask mask) {
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
  //
  //  TODO:  If there's been a change to the meshes within a given patch, fade
  //  in the new version more gradually!  Then you can introduce fancy terra-
  //  forming effects, smooth changes to the road network, et cetera...
  
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
      patch.fogMesh.colour = Colour.BLACK ;
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



