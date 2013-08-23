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
import src.graphics.widgets.HUD;
import src.user.InstallTab;
import src.user.Composite;
import src.util.Rand;
import src.util.Visit;



public class Holding extends Venue implements BuildConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MAX_SIZE   = 2,
    MAX_HEIGHT = 4,
    NUM_LEVELS = 4,
    NUM_VARS   = 3 ;
  final static String IMG_DIR = "media/Buildings/merchant/" ;
  final static Model MODELS[][] = ImageModel.fromTextureGrid(
    Holding.class, Texture.loadTexture(IMG_DIR+"all_housing.gif"),
    4, 4, 2, ImageModel.TYPE_POPPED_BOX
  ) ;
  
  final static Conversion construction[] = {
    new Conversion(
      5, PARTS, SIMPLE_DC, ASSEMBLY
    ),
    new Conversion(
      10, PARTS, 5, PLASTICS, ROUTINE_DC, ASSEMBLY
    ),
    new Conversion(
      15, PARTS, 10, PLASTICS, TRICKY_DC, ASSEMBLY
    ),
    new Conversion(
      20, PARTS, 15, PLASTICS, 5, INSCRIPTION, DIFFICULT_DC, ASSEMBLY
    ),
  } ;
  final static int occupancies[] = { 5, 5, 5, 5 } ;
  
  
  
  TownVault shelter ;
  int upgradeLevel, varID ;
  
  
  
  public Holding(Base belongs, TownVault shelter) {
    super(2, 2, ENTRANCE_EAST, belongs) ;
    this.shelter = shelter ;
    this.upgradeLevel = 0 ;
    this.varID = Rand.index(NUM_VARS) ;
    attachSprite(MODELS[varID][upgradeLevel].makeSprite()) ;
  }
  
  
  public Holding(Session s) throws Exception {
    super(s) ;
    shelter = (TownVault) s.loadObject() ;
    upgradeLevel = s.loadInt() ;
    varID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(shelter) ;
    s.saveInt(upgradeLevel) ;
    s.saveInt(varID) ;
  }
  
  
  public int owningType() {
    return Element.FIXTURE_OWNS ;
  }
  
  
  /**  Moderating upgrades-
    */
  public Conversion goodsNeeded() {
    return construction[Visit.clamp(upgradeLevel, NUM_LEVELS)] ;
  }
  
  public Conversion goodsWanted() {
    return construction[Visit.clamp(upgradeLevel + 1, NUM_LEVELS)] ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    
    //  You'll need to have a dedicated class for handling construction
    //  materials.
    
    //  Requirements come under three headings-
    //  Building materials (parts, plastics, inscriptions, decor.)
    //  Health and Entertainment (averaged over all occupants.)
    //  Safety and Ambience (by location.)
    
    //  Food types help ensure health.
    //  Life Support and Water requirements stay constant.
    
    final int oldLevel = upgradeLevel ;
    boolean devolve = false, upgrade = true ;
    
    for (Item i : goodsNeeded().raw) if (! stocks.hasItem(i)) devolve = true  ;
    for (Item i : goodsWanted().raw) if (! stocks.hasItem(i)) upgrade = false ;
    if (devolve) upgradeLevel-- ;
    if (upgrade  ) upgradeLevel++ ;
    upgradeLevel = Visit.clamp(upgradeLevel, NUM_LEVELS) ;
    
    /*
    if (upgradeLevel == 0) {
    }
    if (upgradeLevel == 1) {
    }
    if (upgradeLevel == 2) {
      //  At this point, needs 5 Power.
    }
    if (upgradeLevel == 3) {
      //  At this point, needs 10 Power.
    }
    //*/
    
    if (upgradeLevel != oldLevel) {
      attachSprite(MODELS[varID][upgradeLevel].makeSprite()) ;
    }
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
    shelter.holdings.remove(this) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[0] ;
  }
  
  
  protected Service[] services() {
    return new Service[0] ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Home of "+personnel.residents().first().fullName() ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;//Texture.loadTexture("media/GUI/Buttons/holding.gif") ;
  }
  
  
  public String helpInfo() {
    return "Holdings provide shelter and privacy for your subjects." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MERCHANT ;
  }
}




