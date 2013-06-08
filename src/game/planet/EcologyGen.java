


package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



public class EcologyGen {
  
  
  /**  Field definitions, constructors and setup methods.
    */
  final Species species[] ;
  
  
  public EcologyGen(Species... species) {
    this.species = species ;
  }
  
  
  
  /**  Methods for populating the world-
    */
  public void populateFlora(World world) {
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
    }
  }
  
  
  public void populateFauna(World world) {
    final int SS = World.DEFAULT_SECTOR_SIZE ;
    for (Coord c : Visit.grid(0, 0, world.size, world.size, SS)) {
      //
      //  Go over each tile in this sector, and average each species'
      //  preference for habitat of this type.
      final Box2D area = new Box2D().set(c.x, c.y, SS, SS) ;
      final Float weights[] = new Float[species.length] ;
      
      float avgMoisture = 0 ;
      for (Tile t : world.tilesIn(area, false)) {
        final Habitat h = t.habitat() ;
        if (! h.pathClear) continue ;
        avgMoisture += h.moisture / 10f ;
      }
      avgMoisture /= SS * SS ;
      
      for (int n = species.length ; n-- > 0 ;) {
        //
        //  Predators need to be more thinly spaces, so we handle them in a
        //  separate pass.  TODO:  That.
        final Species specie = species[n] ;
        if (specie.type == Species.Type.PREDATOR) {
          weights[n] = 0f ;
          continue ;
        }
        float weight = 0 ;
        for (Tile t : world.tilesIn(area, false)) {
          weight += specie.preference(t.habitat()) ;
        }
        weight /= (SS * SS) ;
        weights[n] = weight ;
      }
      //
      //  Then, introduce species based on those preferences, up to the area's
      //  biomass limit.
      int numBrowsers = (int) (Fauna.BROWSER_DENSITY * avgMoisture) ;
      while (numBrowsers-- > 0) {
        Species picked = (Species) Rand.pickFrom(species, weights) ;
        final Fauna creature = new Fauna(picked) ;
        Tile entry = world.tileAt(c.x + Rand.index(SS), c.y + Rand.index(SS)) ;
        entry = Spacing.nearestOpenTile(entry, entry) ;
        if (entry == null) continue ;
        creature.enterWorldAt(entry.x, entry.y, world) ;
      }
    }
  }
}












