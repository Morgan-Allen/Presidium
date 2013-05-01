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
import src.util.* ;



//  Consider having crops extend flora?


public class Nursery extends Venue implements VenueConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist aura/" ;
  final static Model
    MODEL = ImageModel.asIsometricModel(
      Nursery.class, IMG_DIR+"nursery.png", 2, 2
    ),
    CROP_MODELS[][] = ImageModel.fromTextureGrid(
      Nursery.class, Texture.loadTexture(IMG_DIR+"all_crops.png"), 4, 4, 1
    ) ;
  
  
  final static int
    MAX_PLANT_RANGE = 3 ;
  
  final List <Tile> toPlant = new List <Tile> () ;
  final List <Crop> planted = new List <Crop> () ;
  private int onceGrabbed = 0 ;
  
  
  public Nursery(Base belongs) {
    super(2, 2, Venue.ENTRANCE_SOUTH, belongs) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Nursery(Session s) throws Exception {
    super(s) ;
    s.loadTargets((Series) toPlant) ;
    s.loadObjects(planted) ;
    onceGrabbed = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTargets((Series) toPlant) ;
    s.saveObjects(planted) ;
    s.saveInt(onceGrabbed) ;
  }
  
  
  public boolean usesRoads() {
    return false ;
  }
  
  
  
  /**  Grabbing areas suitable for plantation-
    */
  protected void grabPlantArea() {
    final Tile o = origin() ;
    final int r = MAX_PLANT_RANGE, span = r + size + r ;
    final Box2D area = new Box2D().set(
      (o.x - r) - 0.5f, (o.y - r) - 0.5f,
      span, span
    ) ;
    toPlant.clear() ;
    
    final Nursery nursery = this ;
    final TileSpread spread = new TileSpread(o) {
      protected boolean canAccess(Tile t) {
        return nursery.canAccess(t, area) ;
      }
      protected boolean canPlaceAt(Tile t) {
        if (nursery.canPlant(t)) toPlant.add(t) ;
        return false ;
      }
    } ;
    spread.doSearch() ;
    onceGrabbed = toPlant.size() + planted.size() ;
  }
  

  protected boolean canAccess(Tile t, Box2D area) {
    if (! t.habitat().pathClear) return false ;
    if (! area.contains(t.x, t.y)) return false ;
    if (t.owner() == this) return true ;
    if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
    return true ;
  }
  
  
  protected boolean canPlant(Tile t) {
    if (t.pathType() == Tile.PATH_ROAD) return false ;
    if (t.owner() == this) return false ;
    if (t.owner() instanceof Crop) return false ;
    if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
    return true ;
  }
  
  protected int onceGrabbed() {
    return onceGrabbed ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  protected Item.Type[] itemsMade() {
    return new Item.Type[] { STARCHES, GREENS, PROTEIN } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.FIELD_HAND } ;
  }
  

  public Behaviour nextStepFor(Actor actor) {
    final Delivery d = orders.nextDelivery(actor, itemsMade()) ;
    if (d != null) return d ;
    
    final Farming f = new Farming(actor, this) ;
    if (f.nextStep() != null) return f ;
    
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/ecologist_button.gif") ;
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry.";
  }
}











