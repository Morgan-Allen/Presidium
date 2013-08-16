


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Introduce Paving effects!


public class MagLine extends LineInstallation {
  
  
  final Base base ;
  private Tile path[] ;
  final private Tile tempB[] = new Tile[9] ;
  
  
  public MagLine(Base base) {
    super() ;
    this.base = base ;
  }
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  private Tile offsetFor(Tile t) {
    return t ;
    /*
    if (t == null) return null ;
    if (! (t.owner() instanceof Venue)) return t ;
    final Venue v = (Venue) t.owner() ;
    Tile closest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Tile e : v.entrances()) {
      e.edgeAdjacent(tempB) ;
      for (int i = 4 ; i-- > 0 ;) {
        if (tempB[i] != null && tempB[i].owner() == v) {
          final Tile opposite = tempB[(i + 2) % 4] ;
          final float dist = Spacing.distance(opposite, t) ;
          if (dist < minDist) { closest = opposite ; minDist = dist ; }
        }
      }
    }
    return closest ;
    //*/
  }
  
  
  protected Tile[] lineVicinityPath(
    Tile from, Tile to, boolean full
  ) {
    final Tile start = offsetFor(from), end = offsetFor(to) ;
    if (start == null || end == null) return null ;
    return lineVicinityPath(
      start, end, full, false,
      MagLineNode.class, ShieldWallBlastDoors.class
    ) ;
  }
  
  
  protected Batch <Tile> toClear(Tile from, Tile to) {
    path = lineVicinityPath(from, to, false) ;
    return lineVicinity(path, MagLineNode.class, ShieldWallBlastDoors.class) ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = lineVicinityPath(from, to, false) ;
    if (path == null) return null ;
    final Batch <Element> nodes = new Batch <Element> () ;
    for (Tile t : path) t.flagWith(this) ;
    for (Tile t : path) {
      if (t.owner() instanceof ShieldWallBlastDoors) continue ;
      final MagLineNode node = new MagLineNode(base) ;
      node.setPosition(t.x, t.y, t.world) ;
      nodes.add(node) ;
      node.updateFacing() ;
    }
    for (Tile t : path) t.flagWith(null) ;
    return nodes ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Mag Line" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/mag_line_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Mag Lines facilitate transport between distant sectors of your base, "+
      "along with distribution of power and life support." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN ;
  }
}





