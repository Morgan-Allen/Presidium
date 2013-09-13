


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Reactor extends Venue implements BuildConstants {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Reactor.class, "media/Buildings/artificer/reactor.png", 4, 2
  ) ;
  

  public Reactor(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      300, 10, 300,
      VenueStructure.NORMAL_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Reactor(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Reactor.class, "reactor_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  
  
  
  public Behaviour jobFor(Actor actor) {
    //
    //  Manufacture atomics/fuel rods, or check to prevent meltdown.
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.CORE_TECHNICIAN } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.CORE_TECHNICIAN) {
      return nO + 2 ;
    }
    return 0 ;
  }
  
  
  protected Service[] services() {
    return new Service[] { POWER } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    //
    //  TODO:  This should require frequent supervision.
    if (stocks.amountOf(POWER) < 100) {
      //
      //  TODO:  UPGRADE THIS!  AND CONSUME ISOTOPES IN THE PROCESS!
      stocks.addItem(Item.withAmount(POWER, 5)) ;
    }
  }


  /**  Rendering and interface-
    */
  public String fullName() {
    return "Reactor" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/reactor_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Reactor provides a copious supply of power to your settlement and "+
      "is essential to manufacturing fuel rods and atomics, but can produce "+
      "dangerous levels of pollution." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ARTIFICER ;
  }
}







