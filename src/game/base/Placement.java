

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;



public class Placement {
  
  
  /*
  
  ...Use a different method of footprint-checking.  Start with extremities and
  fill in at half-intervals.  Will most likely eliminate unfit positions
  quickly and cheaply in the vast majority of cases.
  
  ...In fact, you could cache this fairly easily for common sizes & offsets.
    ...And, you could use a mip-map for pathing to help narrow down the clearer
    areas beforehand.  (Or just the region map, in fact.)
  
  
  final int sizeX = <argument>, sizeY = <argument>
  final int step = <smallest power of 2 greater than size>  //  FOR BOTH X AND Y
  final int maxX = sizeX - 1, maxY = sizeY - 1
  
  //
  //  TODO:  Record the offsets employed in a Batch, so you can cache them later.
  
  int x = 0, y = 0 ;
  while (true) {
    while (true) {
      final Tile t = world.tileAt(x + origin.x, y + origin.y) ;
      ...<PERFORM THE ACTUAL PLACEMENT CHECK FOR THE TILE HERE>...
      
      if (y == maxY) break ;
      y += stepY ;
      if (y >= sizeY) y = maxY ;
    }
    if (x == maxX) break ;
    y = 0 ;
    x += stepX ;
    if (x >= sizeX) x = maxX ;
  }
  //*/
  
  
  
  
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final World world,
    Actor... employed
  ) {
    
    //
    //  First, you need to find a suitable entry point.  Try points close to
    //  the starting location, and perform tilespreads from there.
    //
    //  ...I think a pathing map might be needed for this purpose.
    
    Tile init = world.tileAt(atX, atY) ;
    init = Spacing.nearestOpenTile(init, init) ;
    if (init == null) return null ;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world) ;
        return v.canPlace() ;
      }
    } ;
    search.doSearch() ;
    
    if (! search.success()) return null ;//I.complain("NO STARTING POSITION FOUND!") ;
    else v.doPlace(v.origin(), null) ;
    
    if (intact) {
      v.structure.setState(Structure.STATE_INTACT, 1.0f) ;
      v.onCompletion() ;
    }
    else {
      v.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
    }
    final Tile e = world.tileAt(v) ;
    for (Actor a : employed) {
      a.mind.setWork(v) ;
      if (! a.inWorld()) {
        a.assignBase(v.base()) ;
        a.enterWorldAt(e.x, e.y, world) ;
        a.goAboard(v, world) ;
      }
    }
    if (GameSettings.hireFree) Personnel.fillVacancies(v) ;
    v.setAsEstablished(true) ;
    return v ;
  }
}







