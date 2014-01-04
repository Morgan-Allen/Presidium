

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;



//
//  TODO:  Try merging this with the TileSpread class.


public class Placement {
  
  
  private static boolean verbose = false, cacheVerbose = true ;
  
  final private static Coord footprintCache[][][] = new Coord[100][100][] ;
  //
  //  NOTE:  This method is intended to generate a sequence of coordinates to
  //  check that should eliminate unfit placement-sites for buildings faster
  //  than a naive sequential iteration.  It splits the x and y axes into
  //  successively finer quadrants, closing a 'net' on any potential obstacles
  //  within the area:
  //
  //  PASS 1:           PASS 2:           PASS 3:          ETC.
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    |           |     *-----*-----*     *--*--*--*--*
  //    |           |     |     |     |     *--*--*--*--*
  //    *-----------*     *-----*-----*     *--*--*--*--*
  //
  //
  private static Coord[] footprintFor(int sizeX, int sizeY) {
    //
    //  Return the cached version if available, initialise otherwise-
    Coord offsets[] = footprintCache[sizeX][sizeY] ;
    if (offsets != null) return offsets ;
    else offsets = footprintCache[sizeX][sizeY] = new Coord[sizeX * sizeY] ;
    //
    //  Find the initial 'step' size for the grid.
    int stepX = 1, stepY = 1, i = 0 ;
    while (stepX <= sizeX * 2) stepX *= 2 ;
    while (stepY <= sizeY * 2) stepY *= 2 ;
    final int maxX = sizeX - 1, maxY = sizeY - 1 ;
    final boolean mask[][] = new boolean[sizeX][sizeY] ;
    //
    //  Shrink the grid with every step, skipping over any previously-used
    //  coordinates, and return the sequence of coordinates compiled-
    while (stepX > 1 || stepY > 1) {
      if (stepX > 1) stepX /= 2 ;
      if (stepY > 1) stepY /= 2 ;
      for (int x = 0 ;;) {
        for (int y = 0 ;;) {
          if (! mask[x][y]) {
            if (cacheVerbose) I.say("X/Y: "+x+"/"+y) ;
            mask[x][y] = true ;
            offsets[i++] = new Coord(x, y) ;
          }
          if (y == maxY) break ;
          y += stepY ;
          if (y >= sizeY) y = maxY ;
        }
        if (x == maxX) break ;
        x += stepX ;
        if (x >= sizeX) x = maxX ;
      }
    }
    if (cacheVerbose) I.say("SIZE X/Y: "+sizeX+"/"+sizeY) ;
    if (cacheVerbose) I.say("OFFSETS GENERATED: "+i) ;
    return offsets ;
  }
  
  
  public static boolean checkAreaClear(
    Tile origin, int sizeX, int sizeY, int owningPriority
  ) {
    for (Coord c : footprintFor(sizeX, sizeY)) {
      final Tile t = origin.world.tileAt(origin.x + c.x, origin.y + c.y) ;
      if (t == null || t.owningType() >= owningPriority) return false ;
    }
    return true ;
  }
  
  
  //
  //  NOTE:  This method assumes that the fixtures in question will occupy a
  //  contiguous 'strip' or bloc for placement purposes.
  public static boolean checkPlacement(
    Fixture fixtures[], World world
  ) {
    Box2D limits = null ;
    for (Fixture f : fixtures) {
      if (limits == null) f.area(limits = new Box2D()) ;
      else limits.include(f.area()) ;
    }
    if (! checkAreaClear(
      world.tileAt(limits.xpos() + 0.5f, limits.ypos() + 0.5f),
      (int) limits.xdim(),
      (int) limits.ydim(),
      fixtures[0].owningType()
    )) return false ;
    
    for (Fixture f : fixtures) if (! f.canPlace()) return false ;
    return true ;
  }
  
  
  
  //
  //  TODO:  Use an agenda based on a quadtree-descent, much like iteration
  //  within a proximity-map.  You can start out with the most promising-looking
  //  tiles, and eventually exhaust them all if need be.  In principle, it can
  //  even be time-sliced.
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final World world,
    Actor... employed
  ) {
    
    Tile init = world.tileAt(atX, atY) ;
    init = Spacing.nearestOpenTile(init, init) ;
    if (init == null) return null ;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world) ;
        if (! checkAreaClear(t, v.size, v.size, v.owningType())) return false ;
        return v.canPlace() ;
      }
    } ;
    search.doSearch() ;
    
    if (! search.success()) return null ;
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







