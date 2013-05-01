


package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.TerrainPattern;
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
    //
    //  TODO:  Permit placement over magline nodes?  Or reserve that for Blast
    //  Doors?
    path = lineVicinityPath(from, to, false, true, ShieldWallSection.class) ;
    return lineVicinity(path, ShieldWallSection.class) ;
  }
  
  //
  //  TODO:  You may have to restrict the path to a single straight line.
  //  Well, for now, just get a tile in the middle.
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = lineVicinityPath(from, to, false, true, ShieldWallSection.class) ;
    if (path == null) return null ;
    final Batch <Element> nodes = new Batch <Element> () ;
    for (Tile t : path) t.flagWith(this) ;
    Box2D doorsArea = null ;
    final ShieldWallBlastDoors doors = doorsFor(path) ;
    if (doors != null) {
      doors.area(doorsArea = new Box2D()) ;
      nodes.add(doors) ;
    }
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
  
  
  private ShieldWallBlastDoors doorsFor(Tile path[]) {
    if (path.length <= 6) return null ;
    final Tile under = path[path.length / 2] ;
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
  
  
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/shield_wall_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Shield Walls are defensive emplacements that improve base security." ;
  }
}




