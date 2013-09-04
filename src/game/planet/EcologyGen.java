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
          chosen.structure.setState(VenueStructure.STATE_INTACT, 1) ;
          chosen.setAsEstablished(true) ;
        }
      }
    } ;
    scan.doFullScan() ;
  }
}





