


package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



public class Ecology {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static float
    UPDATE_INC = 0.01f ;
  
  final World world ;
  final int SR, SS ;
  final RandomScan growthMap ;
  
  final float fertilities[][] ;   //Of crops/flora.
  final float squalorMap[][] ;    //For pollution/ambience.
  final float preyMap[][], hunterMap[][], abundances[][][] ;  //Of species.
  
  final float globalAbundance[] ;
  final Batch <float[][]> allMaps = new Batch <float[][]> () ;
  
  private float globalFertility = 0 ;
  
  
  public Ecology(final World world) {
    this.world = world ;
    SR = World.SECTION_RESOLUTION ;
    SS = world.size / SR ;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)) ; }
    } ;
    allMaps.add(fertilities = new float[SS][SS]) ;
    allMaps.add(squalorMap  = new float[SS][SS]) ;
    allMaps.add(preyMap     = new float[SS][SS]) ;
    allMaps.add(hunterMap   = new float[SS][SS]) ;
    abundances = new float[Species.ALL_SPECIES.length][SS][SS] ;
    for (float map[][] : abundances) allMaps.add(map) ;
    globalAbundance = new float[Species.ALL_SPECIES.length] ;
  }
  
  
  public void loadState(Session s) throws Exception {
    growthMap.loadState(s) ;
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      map[c.x][c.y] = s.loadFloat() ;
    }
    for (Species p : Species.ALL_SPECIES) {
      globalAbundance[p.ID] = s.loadFloat() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s) ;
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      s.saveFloat(map[c.x][c.y]) ;
    }
    for (Species p : Species.ALL_SPECIES) {
      s.saveFloat(globalAbundance[p.ID]) ;
    }
  }
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    final int size = world.size ;
    final float time = world.currentTime() ;
    
    float growIndex = (time % World.GROWTH_INTERVAL) ;
    growIndex *= size * size * 1f / World.GROWTH_INTERVAL ;
    growthMap.scanThroughTo((int) growIndex) ;
    globalFertility = 0 ;
    
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      if (map == fertilities) {
        map[c.x][c.y] *= 1 - (UPDATE_INC / World.GROWTH_INTERVAL) ;
        ///I.say("Val is: "+map[c.x][c.y]) ;
        globalFertility += map[c.x][c.y] ;
        continue ;
      }
      map[c.x][c.y] *= 1 - UPDATE_INC ;
    }
    for (Species p : Species.ALL_SPECIES) {
      globalAbundance[p.ID] *= 1 - UPDATE_INC ;
    }
    globalFertility /= (SS * SS) ;
    ///I.say("Global fertility is: "+globalFertility) ;
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t.x, t.y, world, false) ;
    final Element owner = t.owner() ;
    if (owner != null) owner.onGrowth(t) ;
  }
  
  
  public void impingeFertility(Flora f, boolean gradual) {
    final Tile t = f.origin() ;
    final int g = f.growStage() ;
    ///I.say("Impinging growth: "+g) ;
    fertilities[t.x / SR][t.y / SR] += g * (gradual ? UPDATE_INC : 1) ;
  }
  
  
  public void impingeSqualor(float squalorVal, Tile t, boolean gradual) {
    squalorMap[t.x / SR][t.y / SR] += squalorVal * (gradual ? UPDATE_INC : 1) ;
  }
  
  
  public void impingePollution(float squalorVal, Fixture f, boolean gradual) {
    final Tile centre = world.tileAt(f) ;
    impingeSqualor(squalorVal, centre, gradual) ;
  }
  
  
  public void impingeAbundance(Fauna f, boolean gradual) {
    final Tile t = f.origin() ;
    final Species s = f.species ;
    final float inc = (gradual ? UPDATE_INC : 1) * f.health.maxHealth() ;
    final int aX = t.x / SR, aY = t.y / SR ;
    abundances[s.ID][aX][aY] += inc ;
    globalAbundance[s.ID] += inc ;
    if (s.type == Species.Type.BROWSER ) preyMap  [aX][aY] += inc ;
    if (s.type == Species.Type.PREDATOR) hunterMap[aX][aY] += inc ;
  }
  
  
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
  
  
  public float globalFertility() {
    return globalFertility / (SS * SS) ;
  }
  
  
  
  /**  Querying sample values-
    */
  public float fertilityAmount(Tile t) {
    return Visit.sampleMap(world.size, fertilities, t.x, t.y) ;
  }
  
  
  public float fertilityRating(Tile t) {
    return fertilityAmount(t) / (SR * SR) ;
  }
  
  
  public float squalorAmount(Tile t) {
    return Visit.sampleMap(world.size, squalorMap, t.x, t.y) ;
  }
  
  
  public float squalorRating(Tile t) {
    return squalorAmount(t) / (SR * SR) ;
  }
  
  
  public float preyDensityAt(Tile t) {
    return Visit.sampleMap(world.size, preyMap, t.x, t.y) ;
  }
  
  
  public float hunterDensityAt(Tile t) {
    return Visit.sampleMap(world.size, hunterMap, t.x, t.y) ;
  }
  
  
  public float absoluteAbundanceAt(Species s, Tile t) {
    return Visit.sampleMap(world.size, abundances[s.ID], t.x, t.y) ;
  }
  
  
  public float globalAbundance(Species s) {
    return globalAbundance[s.ID] ;
  }
  
  
  public float relativeAbundanceAt(Species species, Tile o, int range) {
    float fertility = 0, numPeers = 0, numPrey = 0, numHunters = 0 ;
    
    for (Coord c : Visit.grid(
      o.x - range, o.y - range,
      range * 2, range * 2, SR
    )) {
      final Tile t = world.tileAt(c.x, c.y) ;
      if (t == null) continue ;
      fertility  += fertilityAmount(t) ;
      fertility  -= squalorAmount(t) ;
      numPeers   += absoluteAbundanceAt(species, t) ;
      numPrey    += preyDensityAt(t) ;
      numHunters += hunterDensityAt(t) ;
    }
    
    final float idealPop, numOfType ;
    if (species.type == Species.Type.BROWSER) {
      idealPop = fertility / Lair.BROWSER_RATIO ;
      numOfType = numPrey ;
    }
    else {
      idealPop = numPrey / Lair.PREDATOR_RATIO ;
      numOfType = numHunters ;
    }
    float rarity = (((numOfType + 1) / (numPeers + 1)) + 1) / 2f ;
    return numOfType / (idealPop * rarity) ;
  }
  
  
  public float relativeAbundance(Object prey) {
    if (! (prey instanceof Fauna)) return 0 ;
    final Fauna f = (Fauna) prey ;
    return relativeAbundanceAt(f.species, f.origin(), f.species.forageRange()) ;
  }
}









