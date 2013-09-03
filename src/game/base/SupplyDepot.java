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



public class SupplyDepot extends Venue implements BuildConstants {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asIsometricModel(
    SupplyDepot.class, "media/Buildings/merchant/supply_depot.png", 4, 2.5f
  ) ;
  

  public SupplyDepot(Base base) {
    super(4, 2, ENTRANCE_NORTH, base) ;
    structure.setupStats(100, 2, 200, 0, false) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.SUPPLY_CORPS } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.SUPPLY_CORPS) return nO + 2 ;
    return 0 ;
  }
  
  
  protected Service[] services() {
    return null ;
    //return CARRIED_ITEM_TYPES ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String[] infoCategories() {
    return super.infoCategories() ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    //
    //  TODO:  You need the ability to specify which goods, in what amounts,
    //  you are willing to accept, and from whom.
  }
  
  
  public String fullName() {
    return "The Supply Depot" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/supply_depot_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Supply Depot mediates long-distance trade, both between remote "+
      "outposts of your own colony and offworld commercial partners." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}









