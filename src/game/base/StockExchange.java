

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;





public class StockExchange extends Venue implements BuildConstants {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
      StockExchange.class,
      "media/Buildings/merchant/stock_exchange.png",
      4, 2
    ) ;
  
  
  
  public StockExchange(Base base) {
    super(4, 2, ENTRANCE_SOUTH, base) ;
    structure.setupStats(
      150, 3, 250,
      VenueStructure.SMALL_MAX_UPGRADES, false
    ) ;
    attachSprite(MODEL.makeSprite()) ;
    /*
    int i = 0 ; for (ItemType type : Economy.ALL_COMMODITIES) {
      attachStackFor(type, i / 3, 1 + (i++ % 3)) ;
    }
    //*/
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    StockExchange.class, "stock_exchange_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    HARDWARE_STOCK = new Upgrade(
      "Hardware Stock",
      "Increases space available to parts, plastics and inscription, and "+
      "augments profits from their sale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    RATIONS_STOCK = new Upgrade(
      "Rations Stock",
      "Increases space available to starches, greens and protein, and "+
      "augments profits from their sale.",
      100, null, 1, null, ALL_UPGRADES
    ),
    ARMS_DEALING = new Upgrade(
      "Arms Dealing",
      "Increases space available to medkits, power cells, arms and armour, "+
      "and augments profit from their sale.",
      200, null, 1, null, ALL_UPGRADES
    ),
    VENDOR_QUARTERS = new Upgrade(
      "Vendor Quarters",
      "Vendors are responsible for transport and presentation of essential "+
      "commodities.",
      50, Vocation.STOCK_VENDOR, 1, null, ALL_UPGRADES
    ) ;
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.STOCK_VENDOR } ;
  }
  
  
  protected Service[] services() {
    return new Service[] {
      PARTS, PLASTICS, INSCRIPTION, PRESSFEED,
      STARCHES, PROTEIN, GREENS, SPICE
      //  Medkits and Power Cells.  Plus weapons/armour.
    } ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    //
    //  TODO:  You need to return the list of possible deliveries to this
    //  venue... and arrange for bulk transport.
    return new Supervision(actor, this) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    //
    //  TODO:  You need to translate all demands made at this venue into orders
    //  placed elsewhere...
  }
  
  
  
  /**  Rendering has to be tweaked a little here.  The idea is that the item-
    *  stacks will be sandwiched between the back-model and front-model, and
    *  effectively shown 'inside' the venue.  TODO  ...that?
    */
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "The Stock Exchange" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/stock_exchange_button.gif") ;
  }


  public String helpInfo() {
    return
      "The Stock Exchange facilitates small-scale purchases within the "+
      "neighbourhood, and bulk trade between settlements." ;
  }


  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}





