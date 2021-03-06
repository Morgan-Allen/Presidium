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
  
  
  
  /**  Data fields, constructors, setup and save/load methods-
    */
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
    } ;
  
  
  private float plantsHealth = 0.5f ;
  
  
  public Arcology(Base base) {
    super(2, 2, base) ;
    structure.setupStats(15, 1, 100, 0, Structure.TYPE_FIXTURE) ;
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
  
  
  
  /**  Behaviour and economic functions-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    /*
    //
    //  Demand water in proportion to dryness of the surrounding terrain.
    //  TODO:  You'll also need input of greens or saplings, in all likelihood.
    float needWater = 1 - (origin().habitat().moisture() / 10f) ;
    needWater *= needWater ;
    stocks.incDemand(WATER, needWater, VenueStocks.TIER_CONSUMER, 1) ;
    stocks.bumpItem(WATER, needWater / -10f, 1) ;
    final float shortWater = stocks.shortagePenalty(WATER) ;
    //
    //  Kill off the plants if you don't have enough.  Grow 'em otherwise.
    if (shortWater > 0) {
      plantsHealth -= shortWater * needWater / World.STANDARD_DAY_LENGTH ;
    }
    else {
      plantsHealth += 1f / World.STANDARD_DAY_LENGTH ;
    }
    plantsHealth = Visit.clamp(plantsHealth, 0, 1) ;
    //
    //  TODO:  UPDATE SPRITE TO REFLECT THIS.
    //*/
    plantsHealth = 1 ;
    //
    //  Combat pollution and improve global biomass based on health.
    world.ecology().impingeBiomass(this, 5 * plantsHealth, true) ;
    structure.setAmbienceVal(10 * plantsHealth) ;
  }
  
  //
  //  TODO:  Have samples of various different indigenous or foreign flora,
  //  suited to the local climate.
  private float numSaplings() {
    float num = 0 ;
    for (Item i : stocks.matches(SAMPLES)) {
      final Crop crop = (Crop) i.refers ;
      num += i.amount ;
    }
    return num ;
  }
  
  
  private void updateSprite() {
    
  }
  
  
  protected void updatePaving(boolean inWorld) {
    base().paving.updatePerimeter(this, inWorld) ;
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











