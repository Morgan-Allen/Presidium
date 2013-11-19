/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.wild.* ;
import src.util.* ;


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
  
  private float fertilityLevels[][] ;
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
    }
    for (Coord c : Visit.grid(0, 0, SR, SR, 1)) {
      fertilityLevels[c.x][c.y] /= SS * SS * 10 ;
    }
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
      float rating = fertilityLevels[c.x][c.y], distPenalty = 0 ;
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
    
    return best ;
  }
  
  
  
  /**  Placement of ruins-
    */
  public void populateWithRuins() {
    final int SS = World.SECTOR_SIZE ;
    final float ruined = terGen.baseAmount(Habitat.CURSED_EARTH) ;
    final int
      numMajorRuins = (int) ((ruined * numMajor) + 0.5f),
      numMinorRuins = (int) ((ruined * numMinor) + 0.5f) ;
    I.say("Major/minor ruins: "+numMajorRuins+"/"+numMinorRuins) ;
    
    
    for (int n = numMajorRuins + numMinorRuins ; n-- > 0 ;) {
      Coord pos = findBasePosition(null, -1) ;
      I.say("Ruins site at: "+pos) ;
      final Tile centre = world.tileAt(
        (pos.x + 0.5f) * SS,
        (pos.y + 0.5f) * SS
      ) ;
      final boolean minor = n < numMinorRuins ;
      final Batch <Ruins> ruins = populateRuins(centre, SS / (minor ? 2 : 1)) ;
      populateArtilects(ruins, minor) ;
    }
  }
  

  //
  //  TODO:  Get rid of the terrain-painting?
  private Batch <Ruins> populateRuins(Tile centre, float radius) {
    
    final World world = centre.world ;
    final Box2D area = new Box2D() ;
    final int maxRuins = 1 + (int) ((Rand.avgNums(2) + 0.5f) * radius / 4) ;
    final Batch <Ruins> allRuins = new Batch <Ruins> () ;
    final Batch <Tile> wastes = new Batch <Tile> () ;
    final Batch <Tile> barrens = new Batch <Tile> () ;
    Ruins ruins = null ;
    
    for (int d = 0 ; d < radius ; d++) {
      area.set(centre.x - 0.5f, centre.y - 0.5f, 1, 1).expandBy(d) ;
      final Tile perim[] = Spacing.perimeter(area, world) ;
      final int off = Rand.index(perim.length) ;
      for (int step = perim.length ; step-- > 0 ;) {
        final Tile t = perim[(step + off) % perim.length] ;
        if (t == null) continue ;
        final float distance = Spacing.distance(t, centre) / radius ;
        if (distance > 1) continue ;
        if (Rand.avgNums(2) > distance) {
          if (Rand.yes()) wastes.add(t) ;
          barrens.add(t) ;
        }
        
        if (allRuins.size() < maxRuins) {
          if (ruins == null) ruins = new Ruins() ;
          //final int HS = ruins.size / 2 ;
          final Tile i = t ;// world.tileAt(t.x - HS, t.y - HS) ;
          if (tryInsertion(ruins, i, wastes)) {
            ruins.clearSurrounds() ;
            ruins.structure.setState(
              Structure.STATE_INTACT,
              (Rand.num() + 1) / 2f
            ) ;
            ruins.setAsEstablished(true) ;
            allRuins.add(ruins) ;
            ruins = null ;
            continue ;
          }
        }
        
        if (Rand.num() > distance && Rand.index(3) == 0) {
          final Box2D toFill = new Box2D() ;
          final int size = 1 + Rand.index(4) ;
          toFill.set(t.x - (size / 2), t.y - (size / 2), size, size) ;
          toFill.cropBy(world.area()) ;
          trySlagWithin(toFill) ;
        }
      }
    }
    
    for (Tile t : wastes) for (Tile n : t.allAdjacent(Spacing.tempT8)) {
      if (n == null || n.flaggedWith() != null) continue ;
      n.flagWith(t) ;
      barrens.add(n) ;
    }
    for (Tile t : barrens) {
      world.terrain().setHabitat(t, Habitat.BARRENS) ;
      t.flagWith(null) ;
    }
    for (Tile t : wastes) {
      world.terrain().setHabitat(t, Habitat.CURSED_EARTH) ;
    }
    
    return allRuins ;
  }
  
  private boolean trySlagWithin(Box2D toFill) {
    
    ///I.say("Area to fill: "+toFill) ;
    final Fixture f = new Fixture((int) toFill.xdim(), 1) {
      protected boolean canTouch(Element e) {
        return true ;
      }
    } ;
    f.setPosition(toFill.xpos() + 0.5f, toFill.ypos() + 0.5f, world) ;
    if (f.canPlace() && Spacing.perimeterFits(f)) {
      Wreckage.reduceToSlag(toFill, world) ;
      return true ;
    }
    return false ;
  }
  
  
  private boolean tryInsertion(Fixture f, Tile t, Batch <Tile> under) {
    if (t == null) return false ;
    f.setPosition(t.x, t.y, t.world) ;
    if (f.canPlace() && Spacing.perimeterFits(f)) {
      f.enterWorld() ;
      for (Tile u : t.world.tilesIn(f.area(), false)) {
        under.add(u) ;
      } ;
      return true ;
    }
    return false ;
  }
  
  
  private void populateArtilects(Batch <Ruins> ruins, boolean minor) {
    int lairNum = 0 ; for (Ruins r : ruins) {
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



