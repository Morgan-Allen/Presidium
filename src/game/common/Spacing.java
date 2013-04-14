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
    final Tile perim[] = new Tile[(wide + high - 2) * 2] ;
    int tX, tY, pI = 0 ;
    for (tX = minX ; tX++ < maxX ;) perim[pI++] = world.tileAt(tX, minY) ;
    for (tY = minY ; tY++ < maxY ;) perim[pI++] = world.tileAt(maxX, tY) ;
    for (tX = maxX ; tX-- > minX ;) perim[pI++] = world.tileAt(tX, maxY) ;
    for (tY = maxY ; tY-- > minY ;) perim[pI++] = world.tileAt(minX, tY) ;
    return perim ;
  }
  
  
  //
  //  This method checks whether the placement of the given element in this
  //  location would create secondary 'gaps' along it's perimeter that might
  //  lead to the existence of inaccessible 'pockets' of terrain- that would
  //  cause pathing problems.
  public static boolean perimeterFits(Element element) {
    final Box2D area = element.area() ;
    final Tile perim[] = perimeter(area, element.world()) ;
    //
    //  Here, we check the first perimeter.  First, determine where the first
    //  taken (reserved) tile after a contiguous gap is-
    boolean inClearSpace = false ;
    int index = perim.length - 1 ;
    while (index >= 0) {
      final Tile t = perim[index] ;
      if (t.blocked()) { if (inClearSpace) break ; }
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
      if (t.blocked()) { inClearSpace = false ; }
      else if (! inClearSpace) { inClearSpace = true ; numSpaces++ ; }
      index = (index + 1) % perim.length ;
    }
    if (numSpaces > 1) return false ;
    return true ;
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
  

  //
  //  This checks whether the placement of a given fixture is legal.
  //  TODO:  Use this to replace the canPlace method of the Fixture/Venue
  //  classes?
  //  TODO:  Instead of just giving a true/false value, return a batch of all
  //  elements that clash with the placement of this fixture.  Then you can
  //  remove them!
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
    /*
    if (fixture instanceof Venue) {
      final Tile entrance = ((Venue) fixture).entrance() ;
      if (blockedAt(entrance)) return false ;
    }
    //*/
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
  
  private static boolean checkClustering(Element a, Tile t, boolean checkType) {
    final Element b = t.owner() ;
    if (checkType && (b == null || b.owningType() < a.owningType()))
      return true ;
    final Tile oA = a.origin(), oB = b.origin() ;
    return
      ((oA.x / CLUSTER_SIZE) != (oB.x / CLUSTER_SIZE)) ||
      ((oA.y / CLUSTER_SIZE) != (oB.y / CLUSTER_SIZE)) ;
  }
  
  
  
  
  //  TODO:  I want an automatic method of 'grabbing' an area, and possibly
  //  areas within it.
  
  
  
  //  TODO:  You'll need to replace this with a general 'pickBest' method,
  //  possibly from the Visit class.
  
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
  
  
  //
  //  You may need a different type of check for environmental elements like
  //  rocks and flora than you would for venues and other artificial fixtures,
  //  owing to the different construction methods.  (e.g, one depends on
  //  reservation, the other on blockage.)  In fact, it might be simplest to
  //  just make sure the perimeter is clear, or use other local knowledge.
  

  public static Tile nearestOpenTile(Box2D area, Target client, World world) {
    final Vec3D p = client.position(null) ;
    final Tile o = world.tileAt(p.x, p.y) ;
    float minDist = Float.POSITIVE_INFINITY ;
    Tile nearest = null ;
    int numTries = 0 ;
    while (nearest == null && numTries++ < (CLUSTER_SIZE / 2)) {
      for (Tile t : perimeter(area, world)) {
        if (t == null || t.blocked()) continue ;
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
    return nearestOpenTile(tile.area(null), client, tile.world) ;
  }
  
  
  public static Tile nearestOpenTile(Element element, Target client) {
    if (element.pathType() >= Tile.PATH_HINDERS) {
      return nearestOpenTile(element.area(), client, element.world()) ;
    }
    else {
      final Vec3D p = element.position() ;
      return element.world().tileAt(p.x, p.y) ;
    }
  }
  
  
  public static float distance(Target a, Target b) {
    float dist = a.position(null).distance(b.position(null)) ;
    dist -= a.radius() + b.radius() ;
    return (dist < 0) ? 0 : dist ;
  }
  
  
  public static float distance(Tile a, Tile b) {
    final int xd = a.x - b.x, yd = a.y - b.y ;
    return (float) Math.sqrt((xd * xd) + (yd * yd)) ;
  }
  
  
  public static float axisDist(Tile a, Tile b) {
    final int xd = Math.abs(a.x - b.x), yd = Math.abs(a.y - b.y) ;
    return Math.max(xd, yd) ;
  }
}




