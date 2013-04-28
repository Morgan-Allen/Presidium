


package src.game.base ;
import src.game.common.* ;
import src.graphics.common.Model ;
import src.graphics.common.Texture ;
import src.graphics.cutout.ImageModel ;
import src.util.* ;



//   TODO:  This also needs to implement the Paving interface.



public class MagLineNode extends Element implements TileConstants {
  
  /**  Constants, fields, constructors and save/load functions.
    */
  private Tile around[] = new Tile[9] ;
  
  
  MagLineNode() {
    super() ;
  }
  
  
  public MagLineNode(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
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
    NODE_MODELS[] = ImageModel.loadModels(
      MagLineNode.class, 1, 0.1f, "media/Buildings/vendor aura/",
      "mag_node_left.png",
      "mag_node_right.png",
      "mag_node_centre.png",
      "mag_node_flat.png"
    ),
    NODE_MODEL_LEFT   = NODE_MODELS[0],
    NODE_MODEL_RIGHT  = NODE_MODELS[1],
    NODE_MODEL_CENTRE = NODE_MODELS[2],
    NODE_MODEL_FLAT   = NODE_MODELS[3] ;
  
  
  void updateSprite() {
    final Model model = getModel() ;
    attachSprite(model.makeSprite()) ;
  }
  
  
  private ImageModel getModel() {
    final Tile o = origin() ;
    o.allAdjacent(around) ;
    int numNear = 0 ;
    for (int n : N_ADJACENT) if (isNode(n)) numNear++ ;
    if (numNear != 2) return NODE_MODEL_CENTRE ;
    if (isNode(N) && isNode(S)) {
      if (o.y % 3 == 0) return NODE_MODEL_FLAT ;
      return NODE_MODEL_RIGHT ;
    }
    if (isNode(W) && isNode(E)) {
      if (o.x % 3 == 0) return NODE_MODEL_FLAT ;
      return NODE_MODEL_LEFT  ;
    }
    return NODE_MODEL_CENTRE ;
  }
  
  
  private boolean isNode(int dir) {
    final Tile t = around[dir] ;
    if (t == null) return false ;
    return t.flaggedWith() != null || t.owner() instanceof MagLineNode ;
  }
}  









