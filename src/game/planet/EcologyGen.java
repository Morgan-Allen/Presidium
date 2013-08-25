/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.building.VenueStructure;
import src.game.common.* ;
import src.util.* ;


//
//  Treat each corner of the map as, potentially, a kind of base.




public class EcologyGen {
  
  
  /**  Field definitions, constructors and setup methods.
    */
  
  
  
  /**  Methods for populating the world-
    */
  public void populateFlora(World world) {
    //
    //  Migrate the population code for the Flora class over to here?
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
    }
  }
  
  
  //
  //  TODO:  This method needs to insert lairs, instead of inserting animals
  //  directly.  (Then top up the population.)
  
  public void populateFauna(
    final World world, final int rangeSize, final Species... species
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
          final float rating = lair.rateCrowding(world) ;
          if (rating > bestRating) { bestIndex = i ; bestRating = rating ; }
        }
        
        if (bestIndex != -1) {
          final Lair chosen = typeLairs[bestIndex] ;
          typeLairs[bestIndex] = null ;
          chosen.enterWorld() ;
          chosen.structure.setState(VenueStructure.STATE_INTACT, 1) ;
          chosen.setAsEstablished(true) ;
        }
      }
    } ;
    scan.doFullScan() ;
  }
}




/*
final int RS = rangeSize ;

for (Coord c : Visit.grid(0, 0, world.size, world.size, RS)) {
  final Tile midTile = world.tileAt(c.x + (RS / 2), c.y + (RS / 2)) ;
  
  while (true) {
    Fauna picked = null ;
    float minCrowding = 1.0f ;
    
    for (int n = species.length ; n-- > 0 ;) {
      final Fauna specimen = species[n].newSpecimen() ;
      float crowding = sampleCrowding(specimen, world, midTile, RS / 2) ;
      
      if (specimen.origin() != null && crowding < minCrowding) {
        minCrowding = crowding ;
        picked = specimen ;
      }
    }
    
    if (picked != null) {
      picked.health.setupHealth(
        Rand.num(),  //current age
        (Rand.num() + 1) / 2,  //overall health
        0.1f  //accident chance
      ) ;
      picked.enterWorld() ;
    }
    else break ;
  }
}
//*/

  
  /*
  protected float sampleCrowding(
    Fauna specimen, World world, Tile around, float range
  ) {
    float sumSamples = 0 ;
    final int numSamples = 1 ;
    Tile bestEntry = null ;
    float minSample = Float.POSITIVE_INFINITY ;
    ///specimen.setPosition(around.x, around.y, world) ;
    //
    //  Sample various points within the sector to get a reading of how crowded
    //  the place is.
    for (int n = numSamples ; n-- > 0 ;) {
      final Tile sampled = world.tileAt(
        Visit.clamp(around.x + Rand.range(-range, range), 0, world.size - 1),
        Visit.clamp(around.y + Rand.range(-range, range), 0, world.size - 1)
      ) ;
      final float sample = specimen.guessCrowding(sampled) ;
      sumSamples += sample ;
      if (sample < minSample) { minSample = sample ; bestEntry = sampled ; }
    }
    if (bestEntry != null) {
      final Tile free = Spacing.nearestOpenTile(bestEntry, bestEntry) ;
      if (free != null) specimen.setPosition(free.x, free.y, world) ;
    }
    return sumSamples / numSamples ;
  }
  //*/




/*
public void populateFauna(World world) {
  final int SS = World.DEFAULT_SECTOR_SIZE ;
  for (Coord c : Visit.grid(0, 0, world.size, world.size, SS)) {
    //
    //  Go over each tile in this sector, and average each species'
    //  preference for habitat of this type.
    final Box2D area = new Box2D().set(c.x, c.y, SS, SS) ;
    final Float weights[] = new Float[ecology.length] ;
    
    float avgMoisture = 0 ;
    for (Tile t : world.tilesIn(area, false)) {
      final Habitat h = t.habitat() ;
      if (! h.pathClear) continue ;
      avgMoisture += h.moisture / 10f ;
    }
    avgMoisture /= SS * SS ;
    
    for (int n = ecology.length ; n-- > 0 ;) {
      //
      //  Predators need to be more thinly spaced, so we handle them in a
      //  separate pass.  TODO:  That.
      final Species species = ecology[n] ;
      if (species.type == Species.Type.PREDATOR) {
        weights[n] = 0f ;
        continue ;
      }
      float weight = 0 ;
      for (Tile t : world.tilesIn(area, false)) {
        weight += species.preference(t.habitat()) ;
      }
      weight /= (SS * SS) ;
      weights[n] = weight ;
    }
    //
    //  Then, introduce species based on those preferences, up to the area's
    //  biomass limit.
    int numBrowsers = (int) (Fauna.BROWSER_DENSITY * avgMoisture) ;
    while (numBrowsers-- > 0) {
      final Species picked = (Species) Rand.pickFrom(ecology, weights) ;
      if (picked.type != Species.Type.BROWSER) continue ;
      
      final Fauna creature = picked.newSpecimen() ;
      Tile entry = world.tileAt(c.x + Rand.index(SS), c.y + Rand.index(SS)) ;
      entry = Spacing.nearestOpenTile(entry, entry) ;
      if (entry == null) continue ;
      
      creature.health.setupHealth(
        Rand.num(),  //current age
        (Rand.num() + 1) / 2,  //overall health
        0.1f  //accident chance
      ) ;
      creature.enterWorldAt(entry.x, entry.y, world) ;
    }
  }
}
//*/







