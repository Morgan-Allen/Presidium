/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class ShieldWallSection extends Element implements TileConstants {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  private Tile around[] = new Tile[9] ;
  private int facing ;
  private boolean isTower ;
  
  
  ShieldWallSection() {
    super() ;
  }
  
  
  public ShieldWallSection(Session s) throws Exception {
    super(s) ;
    this.facing = s.loadInt() ;
    this.isTower = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(facing) ;
    s.saveBool(isTower) ;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS ;
  }
  
  
  public int owningType() {
    return Element.VENUE_OWNS ;
  }
  
  
  
  /**  Life cycle and placement-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.terrain().maskAsPaved(origin().vicinity(around), true) ;
  }
  
  
  public void exitWorld() {
    world.terrain().maskAsPaved(origin().vicinity(around), false) ;
    super.exitWorld() ;
  }
  
  
  public int facing() {
    return facing ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static ImageModel
    SECTION_MODELS[] = ImageModel.loadModels(
      ShieldWallSection.class, 1, 2.0f, "media/Buildings/military aura/",
      "wall_segment_left.png",
      "wall_segment_right.png",
      "wall_corner.png",
      "wall_tower_left.png",
      "wall_tower_right.png"
    ),
    SECTION_MODEL_LEFT   = SECTION_MODELS[0],
    SECTION_MODEL_RIGHT  = SECTION_MODELS[1],
    SECTION_MODEL_CORNER = SECTION_MODELS[2],
    TOWER_MODEL_LEFT     = SECTION_MODELS[3],
    TOWER_MODEL_RIGHT    = SECTION_MODELS[4] ;
  
  
  void updateFacing() {
    final Tile o = origin() ;
    o.allAdjacent(around) ;
    int numNear = 0 ;
    facing = CORNER ;
    for (int n : N_ADJACENT) if (isNode(n)) numNear++ ;
    if (numNear == 2) {
      if (isNode(N) && isNode(S)) facing = Y_AXIS ;
      if (isNode(W) && isNode(E)) facing = X_AXIS ;
    }
    attachSprite(updateModel().makeSprite()) ;
  }
  
  
  void setTower(boolean is) {
    isTower = is ;
    attachSprite(updateModel().makeSprite()) ;
  }
  
  
  private ImageModel updateModel() {
    switch (facing) {
      case (Y_AXIS) :
        return isTower ? TOWER_MODEL_RIGHT : SECTION_MODEL_RIGHT ;
      case (X_AXIS) :
        return isTower ? TOWER_MODEL_LEFT : SECTION_MODEL_LEFT ;
      case (CORNER) : break ;
    }
    return SECTION_MODEL_CORNER ;
  }
  
  
  private boolean isNode(int dir) {
    final Tile t = around[dir] ;
    if (t == null) return false ;
    return
      t.flaggedWith() instanceof ShieldWall ||
      t.owner() instanceof ShieldWallSection ||
      t.owner() instanceof ShieldWallBlastDoors ;
  }
}




