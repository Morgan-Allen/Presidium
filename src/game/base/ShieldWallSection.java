


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
  
  
  ShieldWallSection() {
    super() ;
  }
  
  
  public ShieldWallSection(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
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
  
  
  void updateSprite() {
    final Model model = getModel() ;
    attachSprite(model.makeSprite()) ;
  }
  
  
  private ImageModel getModel() {
    final Tile o = origin() ;
    o.allAdjacent(around) ;
    int numNear = 0 ;
    for (int n : N_ADJACENT) if (isNode(n)) numNear++ ;
    if (numNear != 2) return SECTION_MODEL_CORNER ;
    if (isNode(N) && isNode(S)) {
      if (o.y % 6 == 0) return TOWER_MODEL_RIGHT ;
      if (o.y % 3 == 0) return SECTION_MODEL_CORNER ;
      return SECTION_MODEL_RIGHT ;
    }
    if (isNode(W) && isNode(E)) {
      if (o.x % 6 == 0) return TOWER_MODEL_LEFT ;
      if (o.x % 3 == 0) return SECTION_MODEL_CORNER ;
      return SECTION_MODEL_LEFT  ;
    }
    return SECTION_MODEL_CORNER ;
  }
  
  
  private boolean isNode(int dir) {
    final Tile t = around[dir] ;
    if (t == null) return false ;
    return t.flaggedWith() != null || t.owner() instanceof MagLineNode ;
  }
}
