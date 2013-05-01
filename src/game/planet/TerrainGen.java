


package src.game.planet ;
import src.util.* ;



public class TerrainGen implements TileConstants {
  
  
  /**  Constructors and field definitions-
    */
  final static int
    DETAIL_RESOLUTION = 8 ;
  
  final int mapSize, sectorGridSize ;
  final float typeNoise ;
  final Habitat habitats[] ;
  final Float habitatAmounts[] ;
  
  static class Sector {
    int coreX, coreY ;
    int gradientID ;
  }

  private Sector sectors[][] ;
  private float blendsX[][], blendsY[][] ;
  private byte sectorVal[][] ;
  private byte typeIndex[][] ;
  private byte heightMap[][] ;
  private byte varsIndex[][] ;
  
  
  
  /**  NOTE:  This constructor expects a gradient argument consisting of paired
    *  habitats and float-specified proportions for each.  Do not fuck with it.
    */
  public TerrainGen(int minSize, float typeNoise, Object... gradient) {
    this.mapSize = checkMapSize(minSize) ;
    this.typeNoise = Visit.clamp(typeNoise, 0, 1) ;
    this.sectorGridSize = mapSize / Planet.SECTOR_SIZE ;
    //
    //  Here, we verify amd compile the gradient of habitat proportions.
    final Batch <Habitat> habB  = new Batch <Habitat> () ;
    final Batch <Float> amountB = new Batch <Float> ()   ;
    boolean habNext = true ;
    for (Object o : gradient) {
      if (habNext) {
        if (! (o instanceof Habitat)) I.complain("Expected habitat...") ;
        habB.add((Habitat) o) ;
        habNext = false ;
      }
      else {
        if (! (o instanceof Float)) I.complain("Expected amount as float...") ;
        amountB.add((Float) o) ;
        habNext = true ;
      }
    }
    if (gradient.length % 2 != 0) I.complain("Missing argument...") ;
    habitats = habB.toArray(Habitat.class) ;
    habitatAmounts = amountB.toArray(Float.class) ;
  }
  
  
  public Terrain generateTerrain() {
    setupSectors() ;
    setupTiles() ;
    return new Terrain(habitats, typeIndex, varsIndex, heightMap) ;
  }
  
  
  
  /**  Generating the overall region layout:
    */
  private int checkMapSize(int minSize) {
    int mapSize = Planet.SECTOR_SIZE * 2 ;
    while (mapSize < minSize) mapSize *= 2 ;
    if (mapSize == minSize) return mapSize ;
    I.complain("MAP SIZE MUST BE A POWER OF 2 MULTIPLE OF SECTOR SIZE.") ;
    return -1 ;
  }
  
  
  private void setupSectors() {
    final int GS = sectorGridSize ;
    initSectorVals(GS) ;
    initSectorBlends(GS) ;
    sectors = new Sector[GS][GS] ;
    for (Coord c : Visit.grid(0, 0, GS, GS, 1)) {
      final Sector s = new Sector() ;
      s.coreX = (int) ((c.x + 0.5f) * Planet.SECTOR_SIZE) ;
      s.coreY = (int) ((c.y + 0.5f) * Planet.SECTOR_SIZE) ;
      s.gradientID = sectorVal[c.x][c.y] ;
      I.say("Type ID: "+s.gradientID+", core: "+s.coreX+"|"+s.coreY) ;
      sectors[c.x][c.y] = s ;
    }
  }
  
  
  private void initSectorVals(int GS) {
    //
    //  Set up the requisite data stores first-
    final Vec3D seedVals[][] = new Vec3D[GS][GS] ;
    final HeightMap heightMap = new HeightMap(GS + 1) ;
    final float heightVals[][] = heightMap.value() ;
    sectorVal = new byte[GS][GS] ;
    //
    //  We then generate seed values for each sector, and sort by height.
    final Sorting <Vec3D> sorting = new Sorting <Vec3D> () {
      public int compare(Vec3D a, Vec3D b) {
        if (a == b) return 0 ;
        return a.z > b.z ? 1 : -1 ;
      }
    } ;
    for (Coord c : Visit.grid(0, 0, GS, GS, 1)) {
      final Vec3D v = seedVals[c.x][c.y] = new Vec3D() ;
      final float val = (Rand.num() < typeNoise) ?
        ((Rand.num() < typeNoise) ? Rand.num() : heightVals[c.x][c.y]) :
        ((Rand.num() * typeNoise) + (heightVals[c.x][c.y] * (1 - typeNoise))) ;
      v.set(c.x, c.y, val) ;
      sorting.add(v) ;
    }
    //
    //  We then determine how many sectors of each habitat are required,
    //  compile the IDs sequentially by height, and assign to their sectors.
    float sumAmounts = 0, sumToNext = 0 ;
    for (int i = habitats.length ; i-- > 0 ;) sumAmounts += habitatAmounts[i] ;
    final byte typeAssigned[] = new byte[sorting.size()] ;
    byte currentTypeID = -1 ;
    for (int i = 0 ; i < typeAssigned.length ; i++) {
      final float indexInSum = i * sumAmounts / typeAssigned.length ;
      if (indexInSum >= sumToNext) {
        currentTypeID++ ;
        sumToNext += habitatAmounts[currentTypeID] ;
      }
      typeAssigned[i] = currentTypeID ;
    }
    int count = 0 ; for (Vec3D v : sorting) {
      sectorVal[(int) v.x][(int) v.y] = typeAssigned[count++] ;
    }
  }
  
  
  private void initSectorBlends(int GS) {
    blendsX = new float[GS - 1][] ;
    blendsY = new float[GS - 1][] ;
    final int SS = Planet.SECTOR_SIZE, DR = DETAIL_RESOLUTION ;
    for (int n = GS - 1 ; n-- > 0 ;) {
      blendsX[n] = staggeredLine(mapSize + 1, DR, SS / 2, true) ;
      blendsY[n] = staggeredLine(mapSize + 1, DR, SS / 2, true) ;
    }
  }
  
  
  
  /**  Generating fine-scale details.
    */
  private void setupTiles() {
    //final int GS = sectorGridSize ;
    
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1 ;
    final HeightMap heightDetail = new HeightMap(
      mapSize + 1, new float[seedSize][seedSize], 1, 0.5f
    ) ;
    final byte detailGrid[][] = heightDetail.asScaledBytes(10) ;
    typeIndex = new byte[mapSize][mapSize] ;
    varsIndex = new byte[mapSize][mapSize] ;
    heightMap = new byte[mapSize + 1][mapSize + 1] ;
    
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      varsIndex[c.x][c.y] = terrainVarsAt(c.x, c.y) ;
      
      final int
        XBI = (int) ((c.x * 1f / mapSize) * blendsX.length),
        YBI = (int) ((c.y * 1f / mapSize) * blendsY.length) ;
      final float
        sampleX = Visit.clamp(c.x + blendsX[XBI][c.y], 0, mapSize - 1),
        sampleY = Visit.clamp(c.y + blendsY[YBI][c.x], 0, mapSize - 1) ;
      float sum = HeightMap.sampleAt(mapSize, sectorVal, sampleX, sampleY) ;
      
      int gradID = Visit.clamp((int) sum, habitats.length) ;
      if (! habitats[gradID].isOcean) {
        float detail = HeightMap.sampleAt(mapSize, detailGrid, c.x, c.y) / 10f ;
        sum += detail * detail * 2 ;
        gradID = Visit.clamp((int) sum, habitats.length) ;
        typeIndex[c.x][c.y] = (byte) habitats[gradID].ID ;
      }
      
      if (habitats[gradID] == Habitat.ESTUARY && Rand.index(4) == 0) {
        typeIndex[c.x][c.y] = (byte) Habitat.MEADOW.ID ;
      }
    }
    //
    //  Finally, pain the interiors of any ocean tiles-
    paintEdge(Habitat.OCEAN.ID, Habitat.SHORELINE.ID) ;
    paintEdge(Habitat.OCEAN.ID, Habitat.SHALLOWS .ID) ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final byte type = typeIndex[c.x][c.y] ;
      //if (! Habitat.ALL_HABITATS[type].isOcean) continue ;
      if (type >= Habitat.SHALLOWS.ID) continue ;
      float detail = HeightMap.sampleAt(mapSize, detailGrid, c.x, c.y) / 10f ;
      detail *= detail * 1.5f ;
      typeIndex[c.x][c.y] = (byte) (((detail * detail) > 0.25f) ?
        Habitat.SHALLOWS.ID : Habitat.OCEAN.ID
      ) ;
    }
  }
  
  
  private void paintEdge(int edgeID, int replaceID) {
    final Batch <Coord> toPaint = new Batch <Coord> () ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final int h = typeIndex[c.x][c.y] ;
      if (h != edgeID) continue ;
      boolean inside = true ;
      for (int i : N_INDEX) {
        try {
          final int n = typeIndex[c.x + N_X[i]][c.y + N_Y[i]] ;
          if (n != edgeID) { inside = false ; break ; }
        }
        catch (Exception e) { continue ; }
      }
      if (! inside) toPaint.add(new Coord(c)) ;
    }
    for (Coord c : toPaint) {
      typeIndex[c.x][c.y] = (byte) replaceID ;
    }
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
  
  
  private void raiseHeight(int x, int y, float val) {
    heightMap[x    ][y    ] = (byte) Math.max(heightMap[x    ][y    ], val) ;
    heightMap[x + 1][y    ] = (byte) Math.max(heightMap[x + 1][y    ], val) ;
    heightMap[x    ][y + 1] = (byte) Math.max(heightMap[x    ][y + 1], val) ;
    heightMap[x + 1][y + 1] = (byte) Math.max(heightMap[x + 1][y + 1], val) ;
  }

  
  /**  Methods for determining boundaries between sectors-
    */
  private float[] staggeredLine(
    int length, int initStep, float variation, boolean sub
  ) {
    //  NOTE:  Length must be an exact power of 2, plus 1.
    float line[] = new float[length] ;
    int step = (initStep > 0) ? initStep : (length - 1) ;
    while (step > 1) {
      for (int i = 0 ; i < length - 1 ; i += step) {
        final float value = (line[i] + line[i + step]) / 2 ;
        final float rand = Rand.num() - (sub ? 0.5f : 0) ;
        line[i + (step / 2)] = value + (rand * variation) ;
      }
      step /= 2 ;
      variation /= 2 ;
    }
    
    return line ;
  }
  
  
  private Vec2D disp = new Vec2D() ;
}




/*
final float transposed[] = new float[length] ;
final int offset = Rand.index(length) ;
for (int n = length ; n-- > 0 ;) {
  transposed[n] = line[(n + offset) % length] ;
}
I.say("  LINE IS: ") ;
for (float f : transposed) I.add(" "+((int) (f * 10))) ;
return transposed ;
//*/

/*
float sum = 0 ;
sum += HeightMap.sampleAt(mapSize, sectorVal, c.x, c.y) ;

float detail = HeightMap.sampleAt(mapSize, detailGrid, c.x, c.y) ;
detail /= 10f ;

sum += detail * detail * 2 ;
//sum /= 2 ;
int gradID ;
//*/
//*

/*

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
      final Habitat h = habitats[c.x][c.y] ;
      byte height = 0 ;
      if (h.ID <= Habitat.SHORELINE.ID) height = -1 ;
      else if (h.ID >= Habitat.BARRENS.ID) height = 1 ;
      else if (h.ID >= Habitat.DESERT.ID) height = 2 ;
      sinkHeight(c.x, c.y, height) ;
      sinkHeight(c.x + 1, c.y, height) ;
      sinkHeight(c.x, c.y + 1, height) ;
      sinkHeight(c.x + 1, c.y + 1, height) ;
    }
    paintEdge(Habitat.OCEAN, Habitat.SHORELINE) ;
    paintEdge(Habitat.OCEAN, Habitat.SHALLOWS) ;
  }


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
//*/








/**  Here are methods related to populating and updating the world-
  */
/*
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
//*/


/**  Returns the average height of the given area of terrain-
  */

/*
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

public float trueMaxHeight() {
  return maxElevation / 4 ;
}

public float seaLevel() {
  return seaLevel ;
}
//*/


