/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.BaseUI;
import src.user.InstallTab;
import src.user.Composite;
import src.util.* ;



public class ShieldWall extends LineInstallation {
  
  
  final Base base ;
  private Tile path[] ;
  final private Tile tempB[] = new Tile[8] ;
  
  
  public ShieldWall(Base base) {
    super(base.world) ;
    this.base = base ;
  }
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  protected Batch <Tile> toClear(Tile from, Tile to) {
    path = lineVicinityPath(
      from, to, false, true, ShieldWallSection.class, MagLineNode.class
    ) ;
    return lineVicinity(path, ShieldWallSection.class, MagLineNode.class) ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = lineVicinityPath(
      from, to, false, true, ShieldWallSection.class, MagLineNode.class
    ) ;
    if (path == null) return null ;
    //
    //  Firstly, we need to ascertain if we crossed over any mag lines.  If so,
    //  we'll have to place blast doors over 'em.
    int lineIndex = -1 ;
    for (int i = path.length ; i-- > 0 ;) {
      if (path[i].owner() instanceof MagLineNode) {
        if (lineIndex != -1) return null ;
        lineIndex = i ;
      }
    }
    final Batch <Element> nodes = new Batch <Element> () ;
    for (Tile t : path) t.flagWith(this) ;
    //
    //  Then, see if we have a valid location for blast doors-
    Box2D doorsArea = null ;
    final ShieldWallBlastDoors doors = doorsFor(path, lineIndex) ;
    if (doors != null) {
      doors.area(doorsArea = new Box2D()) ;
      nodes.add(doors) ;
    }
    //
    //  And regardless, get the set of wall sections for the path-
    setupSections(path, doorsArea, nodes) ;
    for (Tile t : path) t.flagWith(null) ;
    return nodes ;
  }
  
  
  private ShieldWallSection sectionFor(Tile t) {
    final ShieldWallSection node = new ShieldWallSection() ;
    node.setPosition(t.x, t.y, t.world) ;
    node.updateFacing() ;
    return node ;
  }
  
  
  private ShieldWallBlastDoors doorsFor(Tile path[], int lineIndex) {
    if (lineIndex == -1 && path.length <= 6) return null ;
    final Tile under = path[lineIndex == -1 ? (path.length / 2) : lineIndex] ;
    ShieldWallSection sample = sectionFor(under) ;
    if (sample.facing() == CORNER) return null ;
    final int facing = sample.facing() ;
    ShieldWallBlastDoors doors = new ShieldWallBlastDoors(base, facing) ;
    doors.setPosition(under.x - 1, under.y - 1, base.world) ;
    boolean canPlace = true ;
    for (Tile t : path) if (doors.area().contains(t.x, t.y)) {
      if (sectionFor(t).facing() != facing) canPlace = false ;
    }
    return canPlace ? doors : null ;
  }
  
  
  private void setupSections(
    Tile path[], Box2D doorsArea, Batch <Element> nodes
  ) {
    for (int i = 0, n = 0 ; i < path.length ; i++) {
      final Tile t = path[i] ;
      if (doorsArea != null && doorsArea.contains(t.x, t.y)) continue ;
      final ShieldWallSection node = sectionFor(t) ;
      boolean isTower = false ;
      if (node.facing() == X_AXIS && t.x % 3 == 0) isTower = true ;
      if (node.facing() == Y_AXIS && t.y % 3 == 0) isTower = true ;
      node.setTower(isTower) ;
      nodes.add(node) ;
    }
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Shield Wall" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/shield_wall_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Shield Walls are defensive emplacements that improve base security." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN ;
  }
}




