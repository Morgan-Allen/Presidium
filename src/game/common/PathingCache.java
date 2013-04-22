/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.WorldSections.Section ;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;


//
//  TODO:  Next, ideally, you'll want to build up a recursive tree-structure
//  out of Regions so that the viability of pathing attempts can be determined
//  as quickly as possible (when querying nearby venues, etc.)


public class PathingCache {
  
  
  final static float
    UPDATE_INTERVAL = 10 ;
  
  final World world ;
  final Base base ;
  
  Region sectionRegions[][][] ;
  Region tileRegions[][] ;
  float lastSectionUpdates[][] ;
  
  
  PathingCache(World world, Base base) {
    this.world = world ;
    this.base = base ;
    final int SGS = world.size / World.SECTION_RESOLUTION ;
    sectionRegions = new Region[SGS][SGS][] ;
    tileRegions = new Region[world.size][world.size] ;
    lastSectionUpdates = new float[SGS][SGS] ;
  }
  
  //  Are these necessary?
  void loadState(Session s) throws Exception {
  }
  
  void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Searching for- and caching- paths between discrete regions-
    */
  //
  //  TODO:  Consider using a Table to increase lookup efficiency.
  private static class Path {
    
    Region from, to ;
    float cost ;
  }
  
  
  private Tile tilePos(Boardable b) {
    if (b == null) return null ;
    if (b instanceof Venue) return ((Venue) b).entrance() ;
    final Vec3D p = b.position(null) ;
    return Spacing.nearestOpenTile(world.tileAt(p.x, p.y), b) ;
  }
  
  
  public Boardable[] getLocalPath(Boardable initB, Boardable destB) {
    final Tile
      initT = tilePos(initB),
      destT = tilePos(destB) ;
    if (initT == null || destT == null) return null ;
    
    final Region regionPath[] = getRegionPath(initT, destT) ;
    final PathingSearch search ;
    if (regionPath == null || regionPath.length < 2) {
      if (Spacing.axisDist(initT, destT) < World.SECTION_RESOLUTION * 2) {
        search = new PathingSearch(initB, destB) ;
      }
      else return null ;
    }
    else {
      search = new PathingSearch(initB, regionPath[1].core) ;
    }
    search.doSearch() ;
    return search.getPath(Boardable.class) ;
  }
  
  
  Path currentPath(final Region from, final Region to) {
    if (from.isDead || to.isDead) I.complain("DEAD REGIONS!") ;
    for (Path p : from.paths) {
      if ((p.from == from && p.to == to) || (p.to == from && p.from == to)) {
        return p ;
      }
    }
    final Path path = new Path() ;
    path.from = from ;
    path.to = to ;
    final PathingSearch search = new PathingSearch(from.core, to.core) {
      protected boolean canEnter(Boardable spot) {
        if (spot instanceof Tile) {
          final Tile t = (Tile) spot ;
          return
            from.section.area.contains(t.x, t.y) ||
            to.section.area.contains(t.x, t.y) ;
        }
        return true ;
      }
    } ;
    search.doSearch() ;
    path.cost = search.totalCost() ;
    I.say("Cost between "+from.core+" and "+to.core+" was: "+path.cost) ;
    from.paths.include(path) ;
    to.paths.include(path) ;
    return path ;
  }
  
  
  /**  Acquiring a path through the various region-divisions-
    */
  protected Region[] getRegionPath(Tile init, Tile dest) {
    if (init == null || dest == null) return null ;
    updateRegions(world.sections.sectionAt(init.x, init.y)) ;
    updateRegions(world.sections.sectionAt(dest.x, dest.y)) ;
    final Region
      initRegion = tileRegions[init.x][init.y],
      destRegion = tileRegions[dest.x][dest.y] ;
    if (initRegion == null || destRegion == null) return null ;
    
    ///I.say("BEGINNING REGION PATH SEARCH") ;
    final AgendaSearch search = new AgendaSearch <Region> (initRegion) {
      
      protected Region[] adjacent(Region spot) {
        return regionsNear(spot) ;
      }
      
      protected boolean endSearch(Region best) {
        return best == destRegion ;
      }
      
      protected float cost(Region prior, Region spot) {
        return currentPath(prior, spot).cost ;
      }
      
      protected float estimate(Region spot) {
        return Spacing.distance(spot, destRegion) ;
      }
    } ;
    search.doSearch() ;
    ///I.say("FINISHED REGION PATH SEARCH") ;
    return (Region[]) search.getPath(Region.class) ;
  }
  

  Region[] regionsNear(Region region) {
    if (region.nearCache == null) {
      I.say("Obtaining regions near: "+region.core) ;
      final Batch <Region> near = new Batch <Region> () ;
      //
      //  We add any others regions in the same section, along with regions in
      //  directly adjoining sections.
      final Section c = region.section ;
      for (Region r : sectionRegions[c.x][c.y]) {
        if (r != region) near.add(r) ;
      }
      for (Section s : world.sections.neighbours(region.section, null)) {
        if (s == null) continue ;
        updateRegions(s) ;
        for (Region r : sectionRegions[s.x][s.y]) near.add(r) ;
      }
      region.nearCache = (Region[]) near.toArray(Region.class) ;
    }
    return region.nearCache ;
  }
  
  
  /**  Identifying- and caching- discrete regions of the map.
    */
  static class Region implements Target {
    
    Tile tiles[], core ;
    Section section ;
    List <Path> paths = new List <Path> () ;
    
    private Region nearCache[] ;
    private Object flagged ;
    private boolean isDead = false ;
    
    public Vec3D position(Vec3D v) { return core.position(v) ; }
    public float height() { return 0 ; }
    public float radius() { return 0 ; }
    
    public boolean inWorld() { return true ; }
    public void flagWith(Object f) { this.flagged = f ; }
    public Object flaggedWith() { return flagged ; }
    
    public String toString() {
      return "Region with core at "+core.x+" "+core.y ;
    }
  }
  
  
  void updateRegions(final Section section) {
    //
    //  We only need to update regions for this section if they haven't been
    //  initialised, or if they've expired-
    final int sX = section.x, sY = section.y ;
    final float time = world.currentTime() ;
    if (lastSectionUpdates[sX][sY] < (time - UPDATE_INTERVAL)) {
      final Region regions[] = sectionRegions[sX][sY] ;
      if (regions != null) for (Region r : regions) deleteRegion(r) ;
      sectionRegions[sX][sY] = null ;
    }
    if (sectionRegions[sX][sY] != null) return ;
    //
    //  We need to find every contiguous area of unblocked tiles within this
    //  section, and the tiles outside the section which they border on-
    final Box2D limit = section.area ;
    final Batch <Region> allRegions = new Batch <Region> () ;
    //
    //  TODO:  Ideally, you might try to capture different terrain types, fog
    //  levels, walled areas, etc.
    //  TODO:  Test this based on terrain types?
    for (Tile t : world.tilesIn(limit, false)) {
      if (t.blocked() || tileRegions[t.x][t.y] != null) continue ;
      final Spread spread = new Spread(t) {
        
        protected boolean canAccess(Tile t) {
          if (t.blocked() || ! section.area.contains(t.x, t.y)) return false ;
          return true ;
        }
        
        protected boolean canPlaceAt(Tile t) {
          return false ;
        }
      } ;
      spread.doSearch() ;
      final Tile grabbed[] = spread.allSearched(Tile.class) ;
      if (grabbed.length > 0) {
        allRegions.add(createRegion(section, grabbed)) ;
      }
    }
    sectionRegions[sX][sY] = (Region[]) allRegions.toArray(Region.class) ;
    lastSectionUpdates[sX][sY] = time ;
  }
  
  
  Region createRegion(Section section, Tile grabbed[]) {
    //
    //  We base the region's core position on the average of all member
    //  tiles.
    final Region region = new Region() ;
    I.say("Creating region of size: "+grabbed.length) ;
    region.section = section ;
    region.tiles = grabbed ;
    Vec2D corePos = new Vec2D(), tilePos = new Vec2D() ;
    for (Tile t : grabbed) {
      tileRegions[t.x][t.y] = region ;
      corePos.x += t.x ;
      corePos.y += t.y ;
    }
    corePos.scale(1f / grabbed.length) ;
    //I.say("Core position: "+corePos) ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Tile t : grabbed) {
      tilePos.set(t.x, t.y) ;
      final float dist = corePos.pointDist(tilePos) ;
      //I.say("Distance is: "+dist) ;
      if (dist < minDist) { minDist = dist ; region.core = t ; }
    }
    I.say("Core: "+region.core) ; 
    return region ;
  }
  
  
  void deleteRegion(Region region) {
    I.say("Deleting region with core: "+region.core) ;
    for (Path path : region.paths) {
      path.from.paths.remove(path) ;
      path.to.paths.remove(path) ;
      path.from.nearCache = path.to.nearCache = null ;
    }
    for (Tile t : region.tiles) tileRegions[t.x][t.y] = null ;
    region.isDead = true ;
  }
}


