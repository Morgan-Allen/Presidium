/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



//
//  TODO:  Combine this with the Regions code.  Use it to guarantee the correct
//  structure, as a seed for the height map.



public class WorldSetup {
  
  
  
  /**  Generates large-scale habitat assignments by region.
    */
  /*
  private Habitat[][] genRegionHabitats(int gridSize) {
    final Habitat habs[] = this.habitats() ;
    final float heightVals[][] = new HeightMap(gridSize + 1).value() ;
    final float landVals[][] = new HeightMap(gridSize + 1).value() ;
    
    final Habitat areas[][] = new Habitat[gridSize][gridSize] ;
    final float waterLevel = waterLevel() ;
    for (int x = gridSize ; x-- > 0 ;) for (int y = gridSize ; y-- > 0 ;) {
      if (heightVals[x][y] < waterLevel) areas[x][y] = Habitat.WATER ;
      else areas[x][y] = habs[(int) (landVals[x][y] * habs.length * 0.999f)] ;
    }
    return areas ;
  }
  
  
  /**  Generates small-scale habitat assignments by tile, blending between the
    *  larger region assignments.
    */
  /*
  private Habitat[][] genRegionBlend(int worldSize, Habitat areas[][]) {
    final int seedSize = areas.length ;
    final Habitat tiles[][] = new Habitat[worldSize][worldSize] ;
    final Habitat water = Habitat.WATER ;
    
    final byte seedVals[][] = new byte[seedSize][seedSize] ;
    for (int x = seedSize ; x-- > 0 ;) for (int y = seedSize ; y-- > 0 ;)
      seedVals[x][y] = (byte) areas[x][y].typeID ;
    //
    //  Set all areas of water to pure water.  Otherwise, use the dither-map
    //  values.
    final byte blendKeys[][] = new DitherMap(worldSize, seedVals).values() ;
    for (int x = worldSize ; x-- > 0 ;) for (int y = worldSize ; y-- > 0 ;) {
      final Habitat
        hA = areas[x / DEFAULT_SECTOR_SIZE][y / DEFAULT_SECTOR_SIZE],
        hT = Habitat.TYPES[blendKeys[x][y]] ;
      if (hA == water) tiles[x][y] = water ;
      else tiles[x][y] = (hT == water) ? hA : hT ;
    }
    //
    //  Define the shoreline-
    final RandomMapScan shores = new RandomMapScan(world, 0) {
      protected void scanAt(Tile t) {
        final Habitat tH = tiles[t.x][t.y] ;
        if (tH != water) return ;
        //  Check adjacent tiles to see if we should 'borrow' their terrain
        //  type...
        for (Tile n : t.edgeAdjacent()) if (n != null) {
          final Habitat nH = tiles[n.x][n.y] ;
          if (nH == water) continue ;
          tiles[t.x][t.y] = nH ;
          for (Tile a : t.allAdjacent())
            if (a != null && tiles[a.x][a.y] == water)
              tiles[a.x][a.y] = nH ;
          return ;
        }
      }
    } ;
    shores.doFullScan() ;
    //
    //  Finally, paint the shores of the world, lower the height of the water
    //  areas, and speckle the interior-
    for (int x = worldSize ; x-- > 0 ;) for (int y = worldSize ; y-- > 0 ;) {
      if (tiles[x][y] == water) {
        for (Tile n : world.tileAt(x, y).allAdjacent()) if (n != null) {
          final Habitat nH = tiles[n.x][n.y] ;
          if (nH != Habitat.WATER && nH != Habitat.SHORE) {
            tiles[x][y] = Habitat.SHORE ;
            break ;
          }
        }
      }
      else {
        //  Introduce a little random 'noise' for the sake of variety-
        if (Rand.num() < 0.04f)
          tiles[x][y] = Habitat.TYPES[tiles[x][y].typeID - 1] ;
        for (Tile n : world.tileAt(x, y).allAdjacent()) if (n != null) {
          final Habitat nH = tiles[n.x][n.y] ;
          if (nH == Habitat.WATER || nH == Habitat.SHORE) {
            tiles[x][y] = Habitat.BARRENS ;
            break ;
          }
        }
      }
    }
    return tiles ;
  }
  //*/
}
  


