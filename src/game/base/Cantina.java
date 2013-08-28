


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;




public class Cantina extends Venue {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asIsometricModel(
    Cantina.class, "media/Buildings/merchant/cantina.gif", 4, 3
  ) ;
  final static String VENUE_NAMES[] = {
    "The Hive From Home",
    "The Inverse Square",
    "Uncle Fnargex-3Zs",
    "Feynmann's Fortune",
    "The Heavenly Body",
    "The Plug And Play",
    "The Zeroth Point",
    "Lensmans' Folly",
    "The Purple Haze",
  } ;
  final static String PERFORM_NAMES[] = {
    "Red Planet Blues, by Khal Segin & Tolev Zaller",
    "It's Full Of Stars, by D. B. Unterhaussen",
    "Take The Sky From Me, by Wedon the Elder",
    "Men Are From Asra Novi, by The Ryot Sisters",
    "Ode To A Hrexxen Gorn, by Ultimex 1450",
    "Geodesic Science Rap, by Sarles Matson",
    "Stuck In The Lagrange Point With You, by Eniud Yi",
    "Untranslatable Feelings, by Strain Variant Beta-7J",
    "A Credit For Your Engram, by Tobul Masri Mark IV",
  } ;
  
  
  
  private int nameID = -1, performID = -1 ;
  
  
  public Cantina(Base base) {
    super(4, 3, Venue.ENTRANCE_SOUTH, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Cantina(Session s) throws Exception {
    super(s) ;
    nameID = s.loadInt() ;
    performID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(nameID) ;
    s.saveInt(performID) ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    nameID = Rand.index(VENUE_NAMES.length) ;
  }
  


  /**  Upgrades, services and economic functions-
    */
  protected Vocation[] careers() {
    //return new Vocation[] { Vocation.FRONTMAN, Vocation.PERFORMER } ;
    return null ;
  }
  
  
  protected Service[] services() {
    return null ;
  }
  

  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/cantina_button.gif") ;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "The Cantina" ;
    return VENUE_NAMES[nameID] ;
  }
  
  
  public String helpInfo() {
    return
      "Citizens can seek lodgings or simply rest and relax at the Cantina, "+
      "which serves as both a social focal point and a potential breeding "+
      "ground for criminal activities." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }


  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    //
    //  TODO:  Describe the current performance.  You don't need upgrades or
    //  staffing descriptors, since those are spontaneous.
    
    //  Enable gambling/games of chance/cards.
    //  Enable chance meetings with Runners.
  }
  
}










