/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
///import src.game.building.Inventory.Owner ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class SupplyDepot extends Venue implements BuildConstants {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
      SupplyDepot.class, "media/Buildings/merchant/supply_depot.png",
      3, 2
    ) ;
  
  private CargoBarge cargoBarge ;
  

  public SupplyDepot(Base base) {
    super(3, 2, ENTRANCE_NORTH, base) ;
    
    structure.setupStats(100, 2, 200, 0, false) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s) ;
    cargoBarge = (CargoBarge) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(cargoBarge) ;
  }
  
  
  public CargoBarge cargoBarge() {
    return cargoBarge ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor) ;
    
    final Building b = Building.getNextRepairFor(actor) ;
    if (b != null) {
      b.priorityMod = Plan.CASUAL ;
      choice.add(b) ;
    }
    
    //
    //  TODO:  Ensure only one actor at a time performs this kind of long-
    //  range delivery.
    final Batch <Venue> depots = nearbyDepots() ;
    final Delivery d = Delivery.nextDeliveryFrom(
      this, BuildConstants.ALL_CARRIED_ITEMS,
      depots, 50, world
    ) ;
    if (d != null && ! actor.isDoing(Delivery.class, null)) {
      d.priorityMod = Plan.CASUAL ;
      d.driven = cargoBarge ;
      choice.add(d) ;
    }
    //
    //  TODO:  You need to deliver to, and collect from, Dropship freighters.
    
    final Delivery lD = Delivery.nextDeliveryFrom(
      this, actor, BuildConstants.ALL_CARRIED_ITEMS
    ) ;
    if (lD != null) {
      choice.add(lD) ;
    }
    
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  private Batch <Venue> nearbyDepots() {
    final Batch <Venue> depots = new Batch <Venue> () ;
    for (Object o : world.presences.matchesNear(SupplyDepot.class, this, -1)) {
      if (o == this) continue ;
      depots.add((Venue) o) ;
    }
    return depots ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    final Batch <Venue> depots = nearbyDepots() ;
    for (Service type : ALL_CARRIED_ITEMS) {
      stocks.diffuseDemand(type, depots) ;
    }
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
    return ALL_CARRIED_ITEMS ;
  }
  
  
  public void onCompletion() {
    super.onCompletion() ;
    cargoBarge = new CargoBarge() ;
    cargoBarge.assignBase(base()) ;
    final Tile o = origin() ;
    cargoBarge.enterWorldAt(o.x, o.y, world) ;
    cargoBarge.goAboard(this, world) ;
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
    return "Supply Depot" ;
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







