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


//
//  Treat each corner of the map as, potentially, a kind of base.




public class EcologyGen {
  
  
  /**  Field definitions, constructors and setup methods.
    */
  
  

  
  
  
  /**  Populates a given area of the map with ruins and ruin-associated
    *  species.
    */
  public Batch <Ruins> populateRuins(Tile centre, float radius) {
    
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
          trySlagWithin(world, toFill) ;
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
  
  
  private boolean trySlagWithin(World world, Box2D toFill) {
    
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
  
  
  public void populateArtilects(Batch <Ruins> ruins, World world) {
    int lairNum = 0 ; for (Ruins r : ruins) {
      if (lairNum++ > 0 && Rand.yes()) continue ;
      final Tile e = r.mainEntrance() ;
      int numT = Rand.index(3) == 0 ? 1 : 0, numD = 1 + Rand.index(2) ;
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
      if (lairNum == 1 && Rand.yes()) {
        final Cranial cranial = new Cranial() ;
        cranial.enterWorldAt(e.x, e.y, e.world) ;
        cranial.mind.setHomeVenue(r) ;
      }
    }
  }
  
  
  
  
  
  /**  Methods for populating the world-
    */
  public void populateFlora(World world) {
    //
    //  Migrate the population code for the Flora class over to here?
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
    }
  }
  
  
  
  public void populateFauna(
    final World world, final Species... species
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














