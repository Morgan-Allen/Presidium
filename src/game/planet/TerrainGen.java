


package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



public class TerrainGen implements TileConstants {
  
  
  /**  Constructors and field definitions-
    */
  final static int
    DETAIL_RESOLUTION = 8 ;
  final static float
    MAX_MINERAL_DENSITY = 1.0f ;
  
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
  
  private float
    amountCarbons = 0.5f,
    amountMetals = 1,
    amountIsotopes = 0.5f ;
  
  
  
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
    setupTileHabitats() ;
    final Terrain t = new Terrain(habitats, typeIndex, varsIndex, heightMap) ;
    return t ;
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
  
  
  private byte[][] genSectorMap(int scale) {
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1 ;
    final HeightMap sectorMap = new HeightMap(seedSize) ;
    return sectorMap.asScaledBytes(scale) ;
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
      ///I.say("Type ID: "+s.gradientID+", core: "+s.coreX+"|"+s.coreY) ;
      sectors[c.x][c.y] = s ;
    }
  }
  
  
  private void initSectorVals(int GS) {
    //
    //  Set up the requisite data stores first-
    final Vec3D seedVals[][] = new Vec3D[GS][GS] ;
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1 ;
    final HeightMap sectorMap = new HeightMap(seedSize) ;
    final float heightVals[][] = sectorMap.value() ;
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
  private void setupTileHabitats() {
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
  

  /**  Generating mineral deposits-
    */
  public void setupMinerals(final World world) {
    final Terrain terrain = world.terrain() ;
    if (terrain == null) I.complain("No terrain assigned to world!") ;
    final byte
      carbonsMap [][] = genSectorMap(10),
      metalsMap  [][] = genSectorMap(10),
      isotopesMap[][] = genSectorMap(10),
      allMaps[][][] = { null, carbonsMap, metalsMap, isotopesMap } ;
    final float
      abundances[] = { 0, amountCarbons, amountMetals, amountIsotopes } ;
    //
    //  
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      
      int var = varsIndex[c.x][c.y] % 4 ;
      final byte mapVals[][] = allMaps[var] ;
      float chance = 0 ;
      if (mapVals != null) chance += HeightMap.sampleAt(
        mapSize, mapVals, c.x, c.y
      ) / 10f ;
      chance *= terrain.habitatAt(c.x, c.y).minerals / 10f ;
      chance *= abundances[var] ;
      
      final Tile location = world.tileAt(c.x, c.y) ;
      byte degree = (byte) Math.ceil(chance * Terrain.NUM_DEGREES) ;
      if (Rand.index(10) < chance * 10) degree++ ;
      degree = (byte) Visit.clamp(degree, Terrain.NUM_DEGREES) ;
      
      if (degree == 0) var = 0 ;
      terrain.setMinerals(location, (byte) var, degree) ;
    }
    ///presentMineralMap(world, terrain) ;
  }
  
  
  private void presentMineralMap(World world, Terrain terrain) {
    final int colourKey[][] = new int[mapSize][mapSize] ;
    final int typeColours[] = {
      0xff000000,
      0xff0000ff,
      0xffff0000,
      0xff00ff00
    } ;
    final int degreeMasks[] = {
      0xff000000,
      0xff3f3f3f,
      0xff7f7f7f,
      0xffbfbfbf,
      0xffffffff
    } ;
    for (Coord c : Visit.grid(0, 0, mapSize, mapSize, 1)) {
      final Tile t = world.tileAt(c.x, c.y) ;
      final byte type = terrain.mineralType(t) ;
      final float amount = terrain.mineralsAt(t, type) ;
      final byte degree = (byte) Math.ceil(amount / 3) ;
      colourKey[c.x][c.y] = typeColours[type] & degreeMasks[degree] ;
    }
    I.present(colourKey, "minerals map", 256, 256) ;
  }
  
  
  
  //  TODO:  CLEAN THIS UP
  
  //
  //  ...You might want to move some of this to the Outcrop class itself.
  //  ...You'll also want to *average* mineral content over a broader area to
  //  determine the Lode content of an outcrop.  The current system is way too
  //  random.
  
  //  Put the various tiles for processing in different batches and treat 'em
  //  that way?
  public void setupOutcrops(final World world) {
    final Terrain terrain = world.terrain() ;
    final int seedSize = (mapSize / DETAIL_RESOLUTION) + 1 ;
    final HeightMap heightDetail = new HeightMap(
      mapSize + 1, new float[seedSize][seedSize], 1, 0.5f
    ) ;
    final byte detailGrid[][] = heightDetail.asScaledBytes(10) ;
    final Batch <Tile> desertTiles = new Batch <Tile> () ;
    
    final RandomScan scan = new RandomScan(mapSize) {
      protected void scanAt(int x, int y) {
        //
        //  First, determine the outcrop type.  (In the case of desert tiles,
        //  we insert dunes wherever possible.)
        final Habitat habitat = terrain.habitatAt(x, y) ;
        final Tile location = world.tileAt(x, y) ;
        float rockAmount = detailGrid[x][y] / 10f ;
        rockAmount *= rockAmount ;
        
        if ((rockAmount * 11) > (10 - habitat.minerals)) {
          byte mineral = terrain.mineralType(location) ;
          if (Rand.index(10) >= terrain.mineralsAt(location, mineral)) {
            mineral = 0 ;
          }
          int maxSize = Visit.clamp((int) (habitat.minerals * 1.5f / 3), 4) ;
          int rockType = Outcrop.TYPE_MESA ;
          if (maxSize < 2) {
            if (mineral == 0) return ;
            rockType = Outcrop.TYPE_DEPOSIT ;
            maxSize = 3 ;// Rand.yes() ? 3 : 2 ;
          }
          //
          //  If placement was successful, 'paint' the perimeter with suitable
          //  habitat types-
          final Outcrop o = tryOutcrop(
            rockType, mineral, location, maxSize, 1
          ) ;
          //*
          if (o != null) {
            //final boolean mesa = rockType == Outcrop.TYPE_MESA ;
            //*
            for (Tile t : Spacing.perimeter(o.area(), world)) {
              if (t == null || t.habitat().ID > Habitat.BARRENS.ID) continue ;
              if (Rand.index(4) == 0) terrain.setHabitat(t, Habitat.BARRENS) ;
            }
            //*/
            //*
            for (Tile t : world.tilesIn(o.area(), false)) {
              if (Rand.index(4) > 0) terrain.setHabitat(t, Habitat.BARRENS) ;
              else terrain.setHabitat(t, Habitat.MESA) ;
            }
            //*/
          }
          //*/
        }
        
        if (habitat == Habitat.DESERT) {
          desertTiles.add(location) ;
        }
        else if (habitat == Habitat.BARRENS && Rand.index(10) == 0) {
          tryOutcrop(Outcrop.TYPE_DUNE, 0, location, 1, 1) ;
        }
      }
    } ;
    scan.doFullScan() ;
    //
    //  Desert tiles get special treatment-
    for (Tile t : desertTiles) if (Rand.num() < 0.1f) {
      tryOutcrop(Outcrop.TYPE_DUNE, 0, t, 3, 3) ;
    }
    for (Tile t : desertTiles) tryOutcrop(Outcrop.TYPE_DUNE, 0, t, 2, 2) ;
    for (Tile t : desertTiles) tryOutcrop(Outcrop.TYPE_DUNE, 0, t, 1, 1) ;
  }
  
  
  private Outcrop tryOutcrop(
    int type, int mineral, Tile t, int maxSize, int minSize
  ) {
    for (int size = maxSize ; size >= minSize ; size--) {
      final Outcrop o = new Outcrop(size, 1, type, mineral) ;
      o.setPosition(t.x, t.y, t.world) ;
      if (Spacing.perimeterFits(o) && o.canPlace()) {
        o.enterWorld() ;
        return o ;
      }
    }
    return null ;
  }
}

