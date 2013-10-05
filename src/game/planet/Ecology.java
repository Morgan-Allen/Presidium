

package src.game.planet ;
import src.game.common.* ;
import src.game.common.WorldSections.Section ;
import src.util.* ;



public class Ecology {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static float
    UPDATE_INC = 0.01f ;
  final public static String SQUALOR_DESC[] = {
    "Mild",
    "Moderate",
    "Serious",
    "Terrible",
    "Toxic"
  } ;
  final public static String HAZARD_DESC[] = {
    "Secure",
    "Minimal",
    "Elevated",
    "Hostile",
    "Mortal"
  } ;
  final public static String AMBIENCE_DESC[] = {
    "Fair",
    "Good",
    "Excellent",
    "Beautiful",
    "Paradise"
  } ;
  
  private static boolean verbose = true ;
  
  
  final World world ;
  final int SR, SS ;
  final RandomScan growthMap ;
  
  final float biomass[][] ;   //Of crops/flora.
  final float squalorMap[][] ;    //For pollution/ambience.
  final float preyMap[][], hunterMap[][], abundances[][][] ;  //Of species.
  
  final float globalAbundance[] ;
  final Batch <float[][]> allMaps = new Batch <float[][]> () ;
  
  private float globalBiomass = 0 ;
  
  
  public Ecology(final World world) {
    this.world = world ;
    SR = World.SECTION_RESOLUTION ;
    SS = world.size / SR ;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)) ; }
    } ;
    allMaps.add(biomass = new float[SS][SS]) ;
    allMaps.add(squalorMap  = new float[SS][SS]) ;
    allMaps.add(preyMap     = new float[SS][SS]) ;
    allMaps.add(hunterMap   = new float[SS][SS]) ;
    abundances = new float[Species.ANIMAL_SPECIES.length][SS][SS] ;
    for (float map[][] : abundances) allMaps.add(map) ;
    globalAbundance = new float[Species.ANIMAL_SPECIES.length] ;
  }
  
  
  public void loadState(Session s) throws Exception {
    I.say("Loading ecology state...") ;
    growthMap.loadState(s) ;
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      map[c.x][c.y] = s.loadFloat() ;
    }
    for (Species p : Species.ANIMAL_SPECIES) {
      globalAbundance[p.ID] = s.loadFloat() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s) ;
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      s.saveFloat(map[c.x][c.y]) ;
    }
    for (Species p : Species.ANIMAL_SPECIES) {
      s.saveFloat(globalAbundance[p.ID]) ;
    }
  }
  
  
  
  /**  UI assistance-
    */
  private static String descFrom(String s[], float level) {
    return s[Visit.clamp((int) (level * s.length), s.length)] ;
  }
  
  public static String squalorDesc(float rating) {
    if (rating <= 0) return descFrom(AMBIENCE_DESC, 0 - rating) ;
    return descFrom(SQUALOR_DESC, rating / 2) ;
  }
  
  public static String dangerDesc(float rating) {
    return descFrom(HAZARD_DESC, rating / 10f) ;
  }
  
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    final int size = world.size ;
    final float time = world.currentTime() ;
    
    float growIndex = (time % World.GROWTH_INTERVAL) ;
    growIndex *= size * size * 1f / World.GROWTH_INTERVAL ;
    growthMap.scanThroughTo((int) growIndex) ;
    globalBiomass = 0 ;
    
    for (float map[][] : allMaps) for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      if (map == biomass) {
        map[c.x][c.y] *= 1 - (UPDATE_INC / World.GROWTH_INTERVAL) ;
        ///I.say("Val is: "+map[c.x][c.y]) ;
        globalBiomass += map[c.x][c.y] ;
        continue ;
      }
      map[c.x][c.y] *= 1 - UPDATE_INC ;
    }
    for (Species p : Species.ANIMAL_SPECIES) {
      globalAbundance[p.ID] *= 1 - UPDATE_INC ;
    }
    
    globalBiomass /= (SS * SS) ;
    
    if (verbose) {
      final float squareA = SR * SR * 1 ;
      I.present(squalorMap, "Squalor", 256, 256, squareA, -squareA) ;
    }
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t.x, t.y, world, false) ;
    final Element owner = t.owner() ;
    if (owner != null) owner.onGrowth(t) ;
  }
  
  
  public void impingeBiomass(Element e, float amount, boolean gradual) {
    final Tile t = e.origin() ;
    ///I.say("Impinging growth: "+g) ;
    biomass[t.x / SR][t.y / SR] += amount * (gradual ? UPDATE_INC : 1) ;
  }
  
  
  public void impingeSqualor(float squalorVal, Fixture f, boolean gradual) {
    impingeOnMap(squalorVal, f.area(), squalorMap, gradual) ;
  }
  
  
  public void impingeAbundance(Fauna f, boolean gradual) {
    //
    //  TODO:  Impinge over an area?  Like with squalor?
    final Tile t = f.origin() ;
    final Species s = f.species ;
    final float inc = (gradual ? UPDATE_INC : 1) * f.health.maxHealth() ;
    final int aX = t.x / SR, aY = t.y / SR ;
    abundances[s.ID][aX][aY] += inc ;
    globalAbundance[s.ID] += inc ;
    if (s.type == Species.Type.BROWSER ) preyMap  [aX][aY] += inc ;
    if (s.type == Species.Type.PREDATOR) hunterMap[aX][aY] += inc ;
  }
  
  
  
  
  /**  Terraforming methods-
    */
  
  
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
  
  
  
  
  /**  Diffusing effects over a wider area-
    */
  private Section batchS[] = new Section[8] ;
  private Box2D overlap = new Box2D() ;
  
  
  private void impingeOnMap(
    float val, Box2D bound, float map[][], boolean gradual
  ) {
    final float fullVal = val * bound.area() * (gradual ? UPDATE_INC : 1) / 2 ;
    final Box2D aura = new Box2D().setTo(bound).expandBy(SR / 2) ;
    final Vec2D c = bound.centre() ;
    
    final Section centre = world.sections.sectionAt((int) c.x, (int) c.y) ;
    impingeOnSection(bound, fullVal, centre, map) ;
    impingeOnSection(aura , fullVal, centre, map) ;
    
    for (Section s : world.sections.neighbours(centre, batchS)) {
      if (s == null) continue ;
      impingeOnSection(bound, fullVal, s, map) ;
      impingeOnSection(aura , fullVal, s, map) ;
    }
  }
  
  
  private void impingeOnSection(
    Box2D bound, float fullVal, Section s, float map[][]
  ) {
    overlap.setTo(s.area) ;
    overlap.cropBy(bound) ;
    final float inc = fullVal * overlap.area() / bound.area() ;
    map[s.x][s.y] += inc ;
    if (verbose && (Math.abs(inc) > 1) && (inc * fullVal < 0)) {
      I.say("Value increment: "+inc+" "+overlap.area()) ;
    }
  }
  
  
  
  
  /**  Querying sample values-
    */
  public float biomassAmount(Tile t) {
    return Visit.sampleMap(world.size, biomass, t.x, t.y) ;
  }
  
  
  public float biomassRating(Tile t) {
    return biomassAmount(t) / (SR * SR) ;
  }
  
  
  public float squalorAmount(Tile t) {
    return Visit.sampleMap(world.size, squalorMap, t.x, t.y) ;
  }
  
  
  public float squalorRating(Tile t) {
    return squalorAmount(t) / (SR * SR) ;
  }
  
  
  public float squalorRating(Fixture f) {
    float sum = 0, count = 0 ;
    for (Tile t : world.tilesIn(f.area(), true)) {
      sum += squalorMap[t.x / SR][t.y / SR] ;
      count++ ;
    }
    return sum / (count * SR * SR) ;
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
      fertility  += biomassAmount(t) ;
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
  
  
  public float globalBiomass() {
    return globalBiomass / (SS * SS) ;
  }
}









