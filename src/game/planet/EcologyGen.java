/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.campaign.Scenario ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.wild.* ;
import src.util.* ;


//
//  TODO:  Give this a less specifically-ecological name?  Like Placement?
/*
...I need a system for describing alliances.  During contact missions at least.

I need a system for generating local flora and fauna.  Base this, directly, on
underlying terrain.  Player location goes first.  (For that, you still need
fertility assessments.)  Then fill in ruins and natives (not too many.)  Then
put in animal lairs and flora (not too close to other structures.)

TODO:  Put in mineral outcrops during this phase as well?
//*/



public class EcologyGen {
  
  
  /**  Field definitions, constructors and setup methods.
    */
  final static int
    WORLD_SIZE_CATS[] = { 32, 64, 128, 256 },
    MAJOR_LAIR_COUNTS[] = { 0, 0, 1, 4 },
    MINOR_LAIR_COUNTS[] = { 0, 1, 4, 8 },
    VARIANCE = 4 ;
  //
  //  TODO:  Try creating a dedicated 'LairSite' class.
  
  
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
  public void populateWithNatives() {
    float meadowed = (1 + totalFertility) / 2f ;
    final int
      numMajorHuts = (int) ((meadowed * numMajor) + 0.5f),
      numMinorHuts = (int) ((meadowed * numMinor) + 0.5f) ;
    I.say("Major/minor huts: "+numMajorHuts+"/"+numMinorHuts) ;
    
    final Base base = Base.createFor(world) ;
    
    for (int n = numMajorHuts + numMinorHuts ; n-- > 0 ;) {
      final int SS = World.SECTOR_SIZE ;
      Coord pos = findBasePosition(null, 1) ;
      I.say("Huts site at: "+pos) ;
      final Tile centre = world.tileAt(
        (pos.x + 0.5f) * SS,
        (pos.y + 0.5f) * SS
      ) ;
      final boolean minor = n < numMinorHuts ;
      
      int maxRuins = (minor ? 3 : 1) + Rand.index(3) ;
      final Batch <Venue> huts = new Batch <Venue> () ;
      final Habitat under = terGen.baseHabitat(pos, World.SECTOR_SIZE) ;
      
      while (maxRuins-- > 0) {
        final NativeHut r = new NativeHut(under, base) ;
        Scenario.establishVenue(r, centre.x, centre.y, true, world) ;
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
        lives.mind.setHomeVenue(hut) ;
        lives.mind.setEmployer(hut) ;
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
        Scenario.establishVenue(r, centre.x, centre.y, true, world) ;
        if (r.inWorld()) ruins.add(r) ;
      }
      
      for (Venue r : ruins) for (Tile t : world.tilesIn(r.area(), true)) {
        Habitat h = Rand.yes() ? Habitat.CURSED_EARTH : Habitat.DESERT ;
        world.terrain().setHabitat(t, h) ;
      }
      populateArtilects(ruins, minor) ;
    }
    //
    //  TODO:  The slag/wreckage positioning must be done in a distinct pass.
  }
  
  
  private void populateArtilects(Batch <Venue> ruins, boolean minor) {
    //
    //  TODO:  Generalise this, too?  Using pre-initialised actors?
    int lairNum = 0 ; for (Venue r : ruins) {
      if (lairNum++ > 0 && Rand.yes()) continue ;
      
      final Tile e = r.mainEntrance() ;
      int numT = Rand.index(3) == 0 ? 1 : 0, numD = 1 + Rand.index(2) ;
      if (minor && Rand.yes()) { numT = 0 ; numD-- ; }
      
      while (numT-- > 0) {
        final Tripod tripod = new Tripod() ;
        tripod.enterWorldAt(e.x, e.y, world) ;
        tripod.mind.setHomeVenue(r) ;
      }
      
      while (numD-- > 0) {
        final Drone drone = new Drone() ;
        drone.enterWorldAt(e.x, e.y, world) ;
        drone.mind.setHomeVenue(r) ;
      }
      
      if (lairNum == 1 && Rand.yes() && ! minor) {
        final Cranial cranial = new Cranial() ;
        cranial.enterWorldAt(e.x, e.y, e.world) ;
        cranial.mind.setHomeVenue(r) ;
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
    final Lair typeLairs[] = new Lair[species.length] ;
    
    final RandomScan scan = new RandomScan(world.size) {
      protected void scanAt(int x, int y) {
        int bestIndex = -1 ;
        float bestRating = 0 ;
        
        for (int i = species.length ; i-- > 0 ;) {
          final Species specie = species[i] ;
          if (typeLairs[i] == null) typeLairs[i] = specie.createLair() ;
          final Lair lair = typeLairs[i] ;
          lair.setPosition(x, y, world) ;
          final float rating = lair.rateCurrentSite(world) * Rand.avgNums(2) ;
          if (rating > bestRating) { bestIndex = i ; bestRating = rating ; }
        }
        
        if (bestIndex != -1) {
          final Lair chosen = typeLairs[bestIndex] ;
          typeLairs[bestIndex] = null ;
          chosen.clearSurrounds() ;
          chosen.enterWorld() ;
          chosen.structure.setState(Structure.STATE_INTACT, 1) ;
          chosen.setAsEstablished(true) ;
        }
      }
    } ;
    scan.doFullScan() ;
  }
}





/*
if (Rand.num() > distance && Rand.index(3) == 0) {
  final Box2D toFill = new Box2D() ;
  final int size = 1 + Rand.index(4) ;
  toFill.set(t.x - (size / 2), t.y - (size / 2), size, size) ;
  toFill.cropBy(world.area()) ;
  trySlagWithin(toFill) ;
}
//*/
/*
private boolean trySlagWithin(Box2D toFill) {
  
  ///I.say("Area to fill: "+toFill) ;
  final Fixture f = new Fixture((int) toFill.xdim(), 1) {
    protected boolean canTouch(Element e) {
      return true ;
    }
  } ;
  f.setPosition(toFill.xpos(), toFill.ypos(), world) ;
  if (f.canPlace() && Spacing.perimeterFits(f)) {
    Wreckage.reduceToSlag(toFill, world) ;
    return true ;
  }
  return false ;
}
//*/

/*
//final int maxRuins = 1 + (int) ((Rand.avgNums(2) + 0.5f) * radius / 4) ;
//final Batch <Ruins> allRuins = new Batch <Ruins> () ;
//Ruins ruins = null ;

for (int d = 0 ; d < radius ; d++) {
  area.set(centre.x - 0.5f, centre.y - 0.5f, 1, 1).expandBy(d) ;
  final Tile perim[] = Spacing.perimeter(area, world) ;
  final int off = Rand.index(perim.length) ;
  
  for (int step = perim.length ; step-- > 0 ;) {
    final Tile t = perim[(step + off) % perim.length] ;
    if (t == null) continue ;
    final float distance = Spacing.distance(t, centre) / radius ;
    if (distance > 1) continue ;
    
    if (allRuins.size() < maxRuins) {
      if (ruins == null) ruins = new Ruins() ;
      if (Scenario.establishVenue(ruins, t.x, t.y, true, world) != null) {
        allRuins.add(ruins) ;
        ruins = null ;
        continue ;
      }
    }
  }
}
return allRuins ;
    //*/
//}

