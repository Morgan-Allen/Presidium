/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.planet ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.wild.* ;
import src.util.* ;





public class EcologyGen {
  
  
  /**  Field definitions, constructors and setup methods.
    */
  final static int
    WORLD_SIZE_CATS[] = { 32, 64, 128, 256 },
    MAJOR_LAIR_COUNTS[] = { 0, 0, 1, 4 },
    MINOR_LAIR_COUNTS[] = { 0, 1, 4, 8 },
    VARIANCE = 4 ;
  
  
  final World world ;
  final TerrainGen terGen ;
  
  private float fertilityLevels[][], totalFertility ;
  private int numMinor, numMajor ;
  private List <Vec2D> allSites = new List <Vec2D> () ;
  
  
  public EcologyGen(World world, TerrainGen terGen) {
    this.world = world ;
    this.terGen = terGen ;
    summariseFertilities() ;
    calcNumSites() ;
  }
  
  
  
  
  /**  General helper methods-
    */
  private void summariseFertilities() {
    final int SS = World.SECTOR_SIZE, SR = world.size / World.SECTOR_SIZE ;
    fertilityLevels = new float[SR][SR] ;
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y) ;
      final Habitat h = t.habitat() ;
      float f = h.moisture() ;
      if (! h.pathClear) f = -5 ;
      if (h == Habitat.CURSED_EARTH) f = -10 ;
      fertilityLevels[c.x / SS][c.y / SS] += f ;
      totalFertility += f ;
    }
    for (Coord c : Visit.grid(0, 0, SR, SR, 1)) {
      fertilityLevels[c.x][c.y] /= SS * SS * 10 ;
    }
    totalFertility /= world.size * world.size * 10 ;
  }
  
  
  private void calcNumSites() {
    int i = WORLD_SIZE_CATS.length ;
    for (; i-- > 0 ;) {
      if (WORLD_SIZE_CATS[i] == world.size) break ;
    }
    numMajor = MAJOR_LAIR_COUNTS[i] ;
    numMinor = MINOR_LAIR_COUNTS[i] ;
    final int majorVar = numMajor / VARIANCE, minorVar = numMinor / VARIANCE ;
    numMajor += Rand.index((majorVar * 2) + 1) - majorVar ;
    numMinor += Rand.index((minorVar * 2) + 1) - minorVar ;
  }
  
  
  private Coord findBasePosition(
    Vec2D preferred, float fertilityMult
  ) {
    final int SR = fertilityLevels.length ;
    Coord best = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    
    for (Coord c : Visit.grid(0, 0, SR, SR, 1)) {
      final Habitat base = terGen.baseHabitat(c, World.SECTOR_SIZE) ;
      if (! base.pathClear) continue ;
      
      float rating = fertilityLevels[c.x][c.y], distPenalty = 0 ;
      if (fertilityMult > 0 && rating < (1 - fertilityMult)) continue ;
      if (fertilityMult < 0 && base.moisture() > 5) continue ;
      
      for (Vec2D pos : allSites) {
        final float dist = pos.pointDist(c.x, c.y) ;
        distPenalty += 1f / (1 + dist) ;
      }
      if (allSites.size() > 1) distPenalty /= allSites.size() ;
      
      if (preferred != null) {
        final float dist = preferred.pointDist(c.x, c.y) ;
        distPenalty += dist / SR ;
      }
      rating = (rating * fertilityMult) - distPenalty ;
      if (rating > bestRating) { best = new Coord(c) ; bestRating = rating ; }
    }
    
    if (best == null) return null ;
    allSites.add(new Vec2D(best.x, best.y)) ;
    return best ;
  }
  
  
  
  /**  Placement of natives-
    */
  public void populateWithNatives(int tribeID) {
    float meadowed = (1 + totalFertility) / 2f ;
    final int
      numMajorHuts = (int) ((meadowed * numMajor) + 0.5f),
      numMinorHuts = (int) ((meadowed * numMinor) + 0.5f) ;
    I.say("Major/minor huts: "+numMajorHuts+"/"+numMinorHuts) ;
    
    final Base base = world.baseWithName("Natives", true, true) ;
    
    for (int n = numMajorHuts + numMinorHuts ; n-- > 0 ;) {
      final int SS = World.SECTOR_SIZE ;
      Coord pos = findBasePosition(null, 1) ;
      I.say("Huts site at: "+pos) ;
      final Tile centre = world.tileAt(
        (pos.x + 0.5f) * SS,
        (pos.y + 0.5f) * SS
      ) ;
      final boolean minor = n < numMinorHuts ;
      int maxHuts = (minor ? 4 : 2) + Rand.index(3) ;
      final Batch <Venue> huts = new Batch <Venue> () ;
      
      final NativeHall hall = NativeHut.newHall(tribeID, base) ;
      Placement.establishVenue(hall, centre.x, centre.y, true, world) ;
      if (hall.inWorld()) huts.add(hall) ;
      
      while (maxHuts-- > 0) {
        final NativeHut r = NativeHut.newHut(hall) ;
        Placement.establishVenue(r, centre.x, centre.y, true, world) ;
        if (r.inWorld()) huts.add(r) ;
      }
      
      populateNatives(huts, minor) ;
    }
  }
  
  
  private void populateNatives(Batch <Venue> huts, boolean minor) {
    final Background
      NB   = Background.NATIVE_BIRTH,
      NH   = Background.PLANET_DIAPSOR,
      NC[] = Background.NATIVE_CIRCLES ;
    
    for (Venue hut : huts) {
      int numHab = 1 + Rand.index(3) ;
      while (numHab-- > 0) {
        float roll = Math.abs(Rand.avgNums(2) - 0.5f) * 2 ;
        final Background b = NC[(int) (roll * NC.length)] ;
        boolean male = Rand.yes() ;
        
        if (Rand.index(5) != 0) {
          if (Visit.arrayIncludes(Background.NATIVE_MALE_JOBS  , b)) {
            male = true  ;
          }
          if (Visit.arrayIncludes(Background.NATIVE_FEMALE_JOBS, b)) {
            male = false ;
          }
        }
        
        final Career c = new Career(male, b, NB, NH) ;
        final Human lives = new Human(c, hut.base()) ;
        lives.mind.setHome(hut) ;
        lives.mind.setWork(hut) ;
        lives.enterWorldAt(hut, world) ;
        lives.goAboard(hut, world) ;
      }
    }
  }
  
  
  
  /**  Placement of ruins-
    */
  //
  //  TODO:  You might want to pass in a constructor.
  
  public void populateWithRuins() {
    float ruined = terGen.baseAmount(Habitat.CURSED_EARTH) ;
    ruined = (ruined + (1 - totalFertility)) / 2f ;
    
    final int
      numMajorRuins = (int) ((ruined * numMajor) + 0.5f),
      numMinorRuins = (int) ((ruined * numMinor) + 0.5f) ;
    I.say("Major/minor ruins: "+numMajorRuins+"/"+numMinorRuins) ;
    
    for (int n = numMajorRuins + numMinorRuins ; n-- > 0 ;) {
      final int SS = World.SECTOR_SIZE ;
      Coord pos = findBasePosition(null, -1) ;
      I.say("Ruins site at: "+pos) ;
      final Tile centre = world.tileAt(
        (pos.x + 0.5f) * SS,
        (pos.y + 0.5f) * SS
      ) ;
      final boolean minor = n < numMinorRuins ;
      
      int maxRuins = (minor ? 3 : 1) + Rand.index(3) ;
      final Batch <Venue> ruins = new Batch <Venue> () ;
      while (maxRuins-- > 0) {
        final Ruins r = new Ruins() ;
        Placement.establishVenue(r, centre.x, centre.y, true, world) ;
        if (r.inWorld()) ruins.add(r) ;
      }
      
      for (Venue r : ruins) for (Tile t : world.tilesIn(r.area(), true)) {
        Habitat h = Rand.yes() ? Habitat.CURSED_EARTH : Habitat.DUNE ;
        world.terrain().setHabitat(t, h) ;
      }
      populateArtilects(ruins, minor) ;
    }
    //
    //  TODO:  The slag/wreckage positioning must be done in a distinct pass.
  }
  
  
  private void populateArtilects(Batch <Venue> ruins, boolean minor) {
    final Base artilects = world.baseWithName(Base.KEY_ARTILECTS, true, true) ;
    //
    //  TODO:  Generalise this, too?  Using pre-initialised actors?
    int lairNum = 0 ; for (Venue r : ruins) {
      r.assignBase(artilects) ;
      if (lairNum++ > 0 && Rand.yes()) continue ;
      
      final Tile e = r.mainEntrance() ;
      int numT = Rand.index(3) == 0 ? 1 : 0, numD = 1 + Rand.index(2) ;
      if (minor && Rand.yes()) { numT = 0 ; numD-- ; }
      
      while (numT-- > 0) {
        final Tripod tripod = new Tripod() ;
        tripod.assignBase(artilects) ;
        tripod.enterWorldAt(e.x, e.y, world) ;
        tripod.mind.setHome(r) ;
      }
      
      while (numD-- > 0) {
        final Drone drone = new Drone() ;
        drone.assignBase(artilects) ;
        drone.enterWorldAt(e.x, e.y, world) ;
        drone.mind.setHome(r) ;
      }
      
      if (lairNum == 1 && Rand.yes() && ! minor) {
        final Cranial cranial = new Cranial() ;
        cranial.assignBase(artilects) ;
        cranial.enterWorldAt(e.x, e.y, e.world) ;
        cranial.mind.setHome(r) ;
      }
    }
  }
  
  
  
  /**  Placement of natural flora and animal dens/populations-
    */
  public void populateFlora() {
    //
    //  Migrate the population code for the Flora class over to here?
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
    }
  }
  
  
  public void populateFauna(
    final Species... species
  ) {
    //
    //  Okay.  First of all, there ought to be a minimum distance between lairs.
    //  Secondly, it might be simplest to just sample the dozen closest lairs
    //  and their occupants to get an idea of predator quotients.
    //
    //  Thirdly... well, terrain sampling can proceed as before.  But
    //  reproduction should probably be limited to once every couple of days-
    //  and longer, for predators.
    //
    //  Fourthly, for animals, maturation should probably be spurred primarily
    //  by food ingestion.  (Lifespan is a maximum only.)  And lairs have no
    //  maximum occupancy- crowding is rated based on abundance of nearby
    //  food sources.
    //
    //  During setup, whichever species is considered least abundant at a given
    //  site will be installed first.
    
    final int SS = World.SECTOR_SIZE ;
    int numAttempts = (world.size * world.size * 4) / (SS * SS) ;
    I.say("No. of attempts: "+numAttempts) ;
    final Base wildlife = world.baseWithName(Base.KEY_WILDLIFE, true, true) ;
    
    while (numAttempts-- > 0) {
      Tile tried = world.tileAt(
        Rand.index(world.size),
        Rand.index(world.size)
      ) ;
      tried = Spacing.nearestOpenTile(tried, tried) ;
      if (tried == null) continue ;
      Nest toPlace = null ;
      float bestRating = 0 ;
      
      for (Species s : species) {
        final Nest nest = Nest.siteNewLair(s, tried, world) ;
        if (nest == null) continue ;
        final float
          idealPop = Nest.idealNestPop(s, nest, world, false),
          adultMass = s.baseBulk * s.baseSpeed,
          rating = (idealPop * adultMass) + 0.5f ;
        if (rating > bestRating) { toPlace = nest ; bestRating = rating ; }
      }
      
      if (toPlace != null) {
        I.say("New lair for "+toPlace.species+" at "+toPlace.origin()) ;
        toPlace.doPlace(toPlace.origin(), null) ;
        toPlace.assignBase(wildlife) ;
        toPlace.structure.setState(Structure.STATE_INTACT, 1) ;
        final Species s = toPlace.species ;
        final float adultMass = s.baseBulk * s.baseSpeed ;
        float bestPop = bestRating / adultMass ;
        
        while (bestPop-- > 0) {
          final Fauna f = toPlace.species.newSpecimen() ;
          f.health.setupHealth(Rand.num(), 0.9f, 0.1f) ;
          f.mind.setHome(toPlace) ;
          f.assignBase(wildlife) ;
          f.enterWorldAt(toPlace, world) ;
          f.goAboard(toPlace, world) ;
        }
      }
    }
  }
}





