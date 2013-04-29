


package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.util.* ;



public class ShieldWall extends Installation.Line {
  
  
  
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
    //  Doors?  (How often should those be placed?)
    path = lineVicinityPath(from, to, false, true, ShieldWallSection.class) ;
    return lineVicinity(path, ShieldWallSection.class) ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    path = lineVicinityPath(from, to, false, true, ShieldWallSection.class) ;
    if (path == null) return null ;
    final Batch <ShieldWallSection> nodes = new Batch () ;
    for (Tile t : path) {
      final ShieldWallSection node = new ShieldWallSection() ;
      node.setPosition(t.x, t.y, t.world) ;
      nodes.add(node) ;
    }
    for (Tile t : path) t.flagWith(this) ;
    for (ShieldWallSection node : nodes) node.updateSprite() ;
    for (Tile t : path) t.flagWith(null) ;
    return (Batch <Element>) (Batch) nodes ;
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




