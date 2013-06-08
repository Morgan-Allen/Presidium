/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;




public final class Spacing implements TileConstants {
  
  
  private Spacing() {}
  
  final static int
    CLUSTER_SIZE = 8 ;

  private final static Vec3D pA = new Vec3D(), pB = new Vec3D() ;
  private final static Box2D tA = new Box2D(), tB = new Box2D() ;
  
  final static Tile PERIM_ARRAYS[][] = {
    new Tile[8 ],
    new Tile[12],
    new Tile[16],
    new Tile[20]
  } ;
  final static Element NEAR_ARRAYS[][] = {
    new Element[8 ],
    new Element[12],
    new Element[16],
    new Element[20]
  } ;
  
  
  //
  //  Method for getting all tiles around the perimeter of a venue/area.
  public static Tile[] perimeter(Box2D area, World world) {
    final int
      minX = (int) Math.floor(area.xpos()),
      minY = (int) Math.floor(area.ypos()),
      maxX = (int) (minX + area.xdim() + 1),
      maxY = (int) (minY + area.ydim() + 1),
      wide = 1 + maxX - minX,
      high = 1 + maxY - minY ;
    final Tile perim[] ;
    if (wide == high && wide <= 6) perim = PERIM_ARRAYS[wide - 3] ;
    else perim = new Tile[(wide + high - 2) * 2] ;
    int tX, tY, pI = 0 ;
    for (tX = minX ; tX++ < maxX ;) perim[pI++] = world.tileAt(tX, minY) ;
    for (tY = minY ; tY++ < maxY ;) perim[pI++] = world.tileAt(maxX, tY) ;
    for (tX = maxX ; tX-- > minX ;) perim[pI++] = world.tileAt(tX, maxY) ;
    for (tY = maxY ; tY-- > minY ;) perim[pI++] = world.tileAt(minX, tY) ;
    return perim ;
  }
  

  public static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == Venue.ENTRANCE_NONE) return new int[] { 0, 0 } ;
    face = (face + 0.5f) % Venue.NUM_SIDES ;
    float edgeVal = face % 1 ;
    int enterX = 1, enterY = -1 ;
    if (face < Venue.ENTRANCE_EAST) {
      //  This is the north edge.
      enterX = (int) (xdim * edgeVal) ;
      enterY = ydim ;
    }
    else if (face < Venue.ENTRANCE_SOUTH) {
      //  This is the east edge.
      enterX = xdim ;
      enterY = (int) (ydim * (1 - edgeVal)) ;
    }
    else if (face < Venue.ENTRANCE_WEST) {
      //  This is the south edge.
      enterX = (int) (xdim * (1 - edgeVal)) ;
      enterY = -1 ;
    }
    else {
      //  This is the west edge.
      enterX = -1 ;
      enterY = (int) (ydim * edgeVal) ;
    }
    return new int[] { enterX, enterY } ;
  }

  
  
  /**  Methods for assisting placement-viability checks:
    */
  //
  //  This method checks whether the placement of the given element in this
  //  location would create secondary 'gaps' along it's perimeter that might
  //  lead to the existence of inaccessible 'pockets' of terrain- that would
  //  cause pathing problems.
  public static boolean perimeterFits(Element element) {
    final Box2D area = element.area(tA) ;
    final Tile perim[] = perimeter(area, element.origin().world) ;
    //
    //  Here, we check the first perimeter.  First, determine where the first
    //  taken (reserved) tile after a contiguous gap is-
    boolean inClearSpace = false ;
    int index = perim.length - 1 ;
    while (index >= 0) {
      final Tile t = perim[index] ;
      if (t == null || t.owningType() >= element.owningType()) {
        if (inClearSpace) break ;
      }
      else { inClearSpace = true ; }
      index-- ;
    }
    //
    //  Then, starting from that point, and scanning in the other direction,
    //  ensure there's no more than a single contiguous clear space-
    final int
      firstTaken = (index + perim.length) % perim.length,
      firstClear = (firstTaken + 1) % perim.length ;
    inClearSpace = false ;
    int numSpaces = 0 ;
    for (index = firstClear ; index != firstTaken ;) {
      final Tile t = perim[index] ;
      if (t == null || t.owningType() >= element.owningType()) {
        inClearSpace = false ;
      }
      else if (! inClearSpace) { inClearSpace = true ; numSpaces++ ; }
      index = (index + 1) % perim.length ;
    }
    if (numSpaces > 1) return false ;
    return true ;
  }
  
  
  
  public static int numNeighbours(Element element, World world) {
    if (element.xdim() > 4 || element.xdim() != element.ydim()) {
      I.complain("This method is intended only for small, regular elements.") ;
    }
    final int size = element.xdim() - 1 ;
    int numNeighbours = 0 ;
    final Element near[] = NEAR_ARRAYS[size] ;
    final Tile perim[] = (size == 0) ?
      element.origin().allAdjacent(PERIM_ARRAYS[0]) :
      Spacing.perimeter(element.area(tA), world) ;
    
    for (Tile t : perim) if (t != null) {
      final Element o = t.owner() ;
      if (o != null && o.flaggedWith() == null) {
        near[numNeighbours++] = o ;
        o.flagWith(element) ;
      }
    }
    
    for (int i = numNeighbours ; i-- > 0 ;) near[i].flagWith(null) ;
    return numNeighbours ;
  }
  
  
  
  
  
  //
  //  TODO:  You'll need to replace this with a general 'pickBest' method,
  //  possibly from the Visit class.
  
  /**  Promity methods-
    */
  public static Target nearest(
    Series <? extends Target> targets, Target client
  ) {
    Target nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Target t : targets) {
      final float dist = distance(t, client) ;
      if (dist < minDist) { nearest = t ; minDist = dist ; }
    }
    return nearest ;
  }
  

  public static Tile nearestOpenTile(
    Box2D area, Target client, World world
  ) {
    final Vec3D p = client.position(pA) ;
    final Tile o = world.tileAt(p.x, p.y) ;
    float minDist = Float.POSITIVE_INFINITY ;
    Tile nearest = null ;
    int numTries = 0 ;
    while (nearest == null && numTries++ < (CLUSTER_SIZE / 2)) {
      for (Tile t : perimeter(area, world)) {
        //I.say("  Trying tile: "+t) ;
        if (t == null || t.blocked() || t.inside().size() > 0) continue ;
        final float dist = distance(o, t) ;
        if (dist < minDist) { minDist = dist ; nearest = t ; }
      }
      area.expandBy(1) ;
    }
    return nearest ;
  }
  
  
  public static Tile nearestOpenTile(Tile tile, Target client) {
    if (tile == null) return null ;
    if (! tile.blocked()) return tile ;
    //I.say("Looking for open tile around "+tile) ;
    return nearestOpenTile(tile.area(tB), client, tile.world) ;
  }
  
  
  public static Tile nearestOpenTile(
    Element element, Target client, World world
  ) {
    if (element.pathType() >= Tile.PATH_HINDERS) {
      return nearestOpenTile(element.area(tA), client, world) ;
    }
    else {
      final Vec3D p = element.position(pA) ;
      return element.world().tileAt(p.x, p.y) ;
    }
  }
  
  
  /**  Distance calculation methods-
    */
  public static float distance(Target a, Target b) {
    float dist = a.position(pA).distance(b.position(pB)) ;
    dist -= a.radius() + b.radius() ;
    return (dist < 0) ? 0 : dist ;
  }
  

  public static int outerDistance(Target a, Target b) {
    float dist = a.position(pA).distance(b.position(pB)) ;
    return (int) Math.ceil(dist + a.radius() + b.radius()) ;
  }
  
  
  public static float distance(Tile a, Tile b) {
    final int xd = a.x - b.x, yd = a.y - b.y ;
    return (float) Math.sqrt((xd * xd) + (yd * yd)) ;
  }
  
  
  public static int axisDist(Tile a, Tile b) {
    final int xd = Math.abs(a.x - b.x), yd = Math.abs(a.y - b.y) ;
    return Math.max(xd, yd) ;
  }
  
  
  public static boolean adjacent(Tile t, Element e) {
    if (t == null || e == null) return false ;
    e.area(tA) ;
    tA.expandBy(1) ;
    return tA.contains(t.x, t.y) ;
  }
  
  
  public static boolean edgeAdjacent(Tile a, Tile b) {
    if (a.x == b.x) return a.y == b.y + 1 || a.y == b.y - 1 ;
    if (a.y == b.y) return a.x == b.x + 1 || a.x == b.x - 1 ;
    return false ;
  }
  
  
  public static boolean adjacent(Element a, Element b) {
    if (a == null || b == null) return false ;
    a.area(tA) ;
    b.area(tB) ;
    return tA.intersects(tB) ;
  }
}







/*
public static Tile[] traceSurrounding(Element element, int maxLen) {
  final Tile perim[] = perimeter(element.area(), element.world()) ;
  Tile temp[] = new Tile[8] ;
  Tile lastClear = null, lastBlock = null ;
  
  for (Tile t : perim) if (! t.blocked()) { lastClear = t ; break ; }
  if (lastClear != null) for (Tile t : lastClear.edgeAdjacent(temp)) {
    if (t.blocked()) { lastBlock = t ; break ; }
  }
  if (lastClear == null || lastBlock == null) return new Tile[0] ;
  
  
  final Batch <Tile> clear = new Batch <Tile> () ;
  Tile nextClear, nextBlock ;
  
  //  TODO:  Figure out how this works.
  
  return (Tile[]) clear.toArray(Tile.class) ;
}
//*/

/*
public static boolean canPlace(Element fixture, World world) {
  //
  //  Firstly, we obtain the basic measure of the areas to check-
  final Box2D
    area = fixture.area(),
    outerArea = new Box2D().setTo(area).expandBy(1) ;
  final Tile
    perim[] = perimeter(area, world),
    outerPerim[] = perimeter(outerArea, world) ;
  //
  //  Check that all underlying tiles are clear, and that you're not on the
  //  edge of the map.
  for (Coord c : Visit.grid(area)) {
    final Tile t = world.tileAt(c.x, c.y) ;
    if (t == null) return false ;
    if (! t.habitat().pathClear) return false ;
    if (t.owner() == null) continue ;
    if (t.owner().owningType() >= fixture.owningType()) return false ;
  }
  for (Tile t : perim) if (t == null) return false ;
  //
  //  Check that placement won't cause pathing problems-
  if (! perimeterFits(fixture)) return false ;
  //
  //  Then, ensure no element belonging to a different cluster is within two
  //  tiles of this fixture-
  for (Tile t : perim) {
    if (! checkClustering(fixture, t, true)) return false ;
  }
  for (Tile t : outerPerim) if (t != null) {
    if (! checkClustering(fixture, t, true)) return false ;
  }
  return true ;
}
//*/

/*
private static boolean checkClustering(Element a, Tile t, boolean checkType) {
  final Element b = t.owner() ;
  if (checkType && (b == null || b.owningType() < a.owningType()))
    return true ;
  final Tile oA = a.origin(), oB = b.origin() ;
  return
    ((oA.x / CLUSTER_SIZE) != (oB.x / CLUSTER_SIZE)) ||
    ((oA.y / CLUSTER_SIZE) != (oB.y / CLUSTER_SIZE)) ;
}
//*/