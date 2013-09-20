

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



//
//  The Stock Exchange also creates a small amount of money 'ex nihilo' with
//  every sales transaction.
//  
//  TODO:  Have this be responsible for long-range cargo transport(?)  Or do
//  I just make the supply depot a simpler version of the same basic premise?


public class StockExchange extends Venue implements BuildConstants {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
      StockExchange.class,
      "media/Buildings/merchant/stock_exchange.png",
      4, 2
    ) ;
  private CargoBarge cargoBarge ;
  
  
  
  public StockExchange(Base base) {
    super(4, 2, ENTRANCE_SOUTH, base) ;
    structure.setupStats(
      150, 3, 250,
      VenueStructure.SMALL_MAX_UPGRADES, false
    ) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s) ;
    cargoBarge = (CargoBarge) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(cargoBarge) ;
  }
  
  
  public void onCompletion() {
    super.onCompletion() ;
    cargoBarge = new CargoBarge() ;
    cargoBarge.assignBase(base()) ;
    final Tile o = origin() ;
    cargoBarge.enterWorldAt(o.x, o.y, world) ;
    cargoBarge.goAboard(this, world) ;
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
      "Increases space available to carbs, greens and protein, and augments"+
      "profits from their sale.",
      100, null, 1, null, ALL_UPGRADES
    ),
    ARMS_DEALING = new Upgrade(
      "Arms Dealing",
      "Increases space available to stim kits, power cells, arms and armour, "+
      "and augments profit from their sale.",
      200, null, 1, null, ALL_UPGRADES
    ),
    VENDOR_QUARTERS = new Upgrade(
      "Vendor Quarters",
      "Vendors are responsible for transport and presentation of essential "+
      "commodities.",
      50, Background.STOCK_VENDOR, 1, null, ALL_UPGRADES
    ) ;
  
  
  protected Background[] careers() {
    return new Background[] { Background.STOCK_VENDOR } ;
  }
  
  
  public Service[] services() {
    return ALL_COMMODITIES ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    
    final Batch <Venue> depots = Deliveries.nearbyDepots(this, world) ;
    final Delivery d = Deliveries.nextDeliveryFrom(
      this, ALL_COMMODITIES, depots, 50, world
    ) ;
    
    if (d != null && personnel.assignedTo(d) < 1) {
      d.priorityMod = Plan.CASUAL ;
      d.driven = cargoBarge ;
      return d ;
    }
    
    return new Supervision(actor, this) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    final Batch <Venue> depots = Deliveries.nearbyDepots(this, world) ;
    for (Service type : ALL_COMMODITIES) {
      stocks.diffuseDemand(type, depots) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Stock Exchange" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/stock_exchange_button.gif") ;
  }


  public String helpInfo() {
    return
      "The Stock Exchange facilitates small-scale purchases within the "+
      "neighbourhood, and bulk transactions between local merchants." ;
  }


  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}





