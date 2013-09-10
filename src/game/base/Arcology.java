/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;





public class Arcology extends Segment {
  
  
  final static String IMG_DIR = "media/Buildings/aesthete/" ;
  final static Model
    BEDS_MODELS[][] = ImageModel.fromTextureGrid(
      Arcology.class, Texture.loadTexture(IMG_DIR+"all_arcology.png"),
      4, 4, 2, ImageModel.TYPE_FLAT
    ),

    MODEL_BEDS_WEST  = BEDS_MODELS[0][1],
    MODEL_BEDS_RIGHT = BEDS_MODELS[1][1],  //bottom goes south to north...
    MODEL_BEDS_EAST  = BEDS_MODELS[2][1],
    
    MODEL_BEDS_SOUTH = BEDS_MODELS[0][0],
    MODEL_BEDS_LEFT  = BEDS_MODELS[1][0],  //top goes west to east...
    MODEL_BEDS_NORTH = BEDS_MODELS[2][0],
    
    ART_MODELS[] = {
      BEDS_MODELS[0][2], BEDS_MODELS[1][2], BEDS_MODELS[2][2],
      BEDS_MODELS[0][3], BEDS_MODELS[1][3], BEDS_MODELS[2][3]
    },
    
    PLAZA_MODEL = ImageModel.asPoppedModel(
      Arcology.class, IMG_DIR+"PLAZA.png", 3, 1
    ) ;
  
  
  
  public Arcology(Base base) {
    super(2, 2, base) ;
  }
  
  
  public Arcology(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void configFromAdjacent(boolean[] near, int numNear) {

    final Tile o = origin() ;
    final int varID = (o.world.terrain().varAt(o) + o.x + o.y) % 6 ;
    int capIndex = -1 ;
    
    if (numNear == 2) {
      if (near[N] && near[S]) facing = Y_AXIS ;
      if (near[W] && near[E]) facing = X_AXIS ;
    }
    else if (numNear == 1) {
      if (near[N] || near[S]) {
        facing = Y_AXIS ;
        capIndex = near[N] ? 6 : 2 ;
      }
      if (near[W] || near[E]) {
        facing = X_AXIS ;
        capIndex = near[W] ? 2 : 6 ;
      }
    }
    if (facing == -1) facing = CORNER ;

    if (facing == X_AXIS) {
      final int x = o.x % 8 ;
      if (x == 0 || x == capIndex) attachModel(ART_MODELS[varID]) ;
      else if (x == 6 || capIndex == 2) attachModel(MODEL_BEDS_EAST ) ;
      else if (x == 2 || capIndex == 6) attachModel(MODEL_BEDS_WEST ) ;
      else attachModel(MODEL_BEDS_RIGHT) ;
    }
    if (facing == Y_AXIS) {
      final int y = o.y % 8 ;
      if (y == 0 || y == capIndex) attachModel(ART_MODELS[varID]) ;
      else if (y == 6 || capIndex == 2) attachModel(MODEL_BEDS_NORTH) ;
      else if (y == 2 || capIndex == 6) attachModel(MODEL_BEDS_SOUTH) ;
      else attachModel(MODEL_BEDS_LEFT ) ;
    }
    if (facing == CORNER) {
      attachModel(ART_MODELS[varID]) ;
    }
  }
  
  
  
  /**  Searching for a suitable path between tiles or venues-
    */
  protected Tile[] lineVicinityPath(
    Tile from, Tile to, boolean full
  ) {
    return null ;
  }
  
  
  protected Batch <Tile> toClear(Tile from, Tile to) {
    return null ;
  }
  
  
  protected Batch <Element> toPlace(Tile from, Tile to) {
    return null ;
  }
  
  
  public int buildCost() {
    return 100 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/arcology_button.gif") ;
  }
  
  
  public String fullName() { return "Arcology" ; }
  
  
  public String helpInfo() {
    return
      "Arcology provides beauty and life support to your settlement, helping "+
      "to improve ambience and minimise squalor." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_AESTHETE ;
  }
}











