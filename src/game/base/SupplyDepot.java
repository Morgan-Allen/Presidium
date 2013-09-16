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





//
//  TODO:  Ensure only one actor at a time performs this kind of long-
//  range delivery.  (In fact, long-range cargo-deposits need work in
//  general.)
//
//  TODO:  Include a Landing Site right nextdoor.
//  TODO:  This has to register for services, even if you are forcing demand.
//         Do something similar for the Stock Exchange.

//
//  TODO:  What you want to do here is assign general priorities for the base
//         as a whole, while using the depot as the interface.
//
//  Light/Medium/Heavy imports/exports- or leave to discretion, or none.



public class SupplyDepot extends Venue implements
  BuildConstants, Service.Trade
{
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
      SupplyDepot.class, "media/Buildings/merchant/supply_depot.png",
      3, 2
    ) ;
  final static Service DEPOT_GOODS[] = {
    CARBS, PROTEIN, PLASTICS, MEDICINE,
    PARTS, ORES, P_CARBONS, FUEL_CORES
  } ;
  
  
  private Table <Service, Integer>
    exportLevels = new Table <Service, Integer> () ;
  private CargoBarge cargoBarge ;
  

  public SupplyDepot(Base base) {
    super(3, 2, ENTRANCE_NORTH, base) ;
    
    structure.setupStats(100, 2, 200, 0, false) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Service type = ALL_ITEM_TYPES[s.loadInt()] ;
      exportLevels.put(type, s.loadInt()) ;
    }
    
    cargoBarge = (CargoBarge) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveInt(exportLevels.size()) ;
    for (Service type : exportLevels.keySet()) {
      s.saveInt(type.typeID) ;
      s.saveInt(exportLevels.get(type)) ;
    }
    
    s.saveObject(cargoBarge) ;
  }
  
  
  public CargoBarge cargoBarge() {
    return cargoBarge ;
  }
  
  
  public float priceFor(Service service) {
    final Integer level = exportLevels.get(service) ;
    if (level == null || level == 0) return super.priceFor(service) ;
    if (level < 0) return base().commerce.importPrice(service) ;
    if (level > 0) return base().commerce.exportPrice(service) ;
    else return service.basePrice ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor) ;
    
    /*
    final Building b = Building.getNextRepairFor(actor) ;
    if (b != null) {
      b.priorityMod = Plan.CASUAL ;
      choice.add(b) ;
    }
    
    final Batch <Venue> depots = nearbyDepots() ;
    final Delivery d = Delivery.nextDeliveryFrom(
      this, DEPOT_GOODS,
      depots, 50, world
    ) ;
    if (d != null && ! actor.isDoing(Delivery.class, null)) {
      d.priorityMod = Plan.CASUAL ;
      d.driven = cargoBarge ;
      choice.add(d) ;
    }
    //*/
    
    final Delivery lD = Delivery.nextDeliveryFrom(
      this, actor, DEPOT_GOODS
    ) ;
    choice.add(lD) ;
    
    Item[] shortages = stocks.shortages().toArray(Item.class) ;
    final Venue CV = Delivery.findBestVendor(this, shortages) ;
    if (CV != null) {
      shortages = Delivery.compressOrder(shortages, 5) ;
      ///I.say("Should be collecting from: "+CV+" "+shortages[0]) ;
      choice.add(new Delivery(shortages, CV, this)) ;
    }
    
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  /*
  private Batch <Venue> nearbyDepots() {
    final Batch <Venue> depots = new Batch <Venue> () ;
    for (Object o : world.presences.matchesNear(SupplyDepot.class, this, -1)) {
      if (o == this) continue ;
      depots.add((Venue) o) ;
    }
    return depots ;
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    //final Batch <Venue> depots = nearbyDepots() ;
    
    for (Service type : DEPOT_GOODS) {
      final Integer level = exportLevels.get(type) ;
      if (level != null && level > 0) stocks.forceDemand(type, level) ;
      else stocks.forceDemand(type, 0) ;
      ///stocks.diffuseDemand(type, depots) ;
    }
  }
  
  
  public float importDemand(Service type) {
    final Integer level = exportLevels.get(type) ;
    if (level == null || level >= 0) return 0 ;
    return 0 - level ;
  }
  
  
  public float exportDemand(Service type) {
    final Integer level = exportLevels.get(type) ;
    if (level == null || level <= 0) return 0 ;
    return level ;
  }
  
  
  public float importShortage(Service type) {
    return Math.max(0, importDemand(type) - stocks.amountOf(type)) ;
  }
  
  
  public float exportSurplus(Service type) {
    if (exportDemand(type) == 0) return 0 ;
    return stocks.amountOf(type) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.SUPPLY_CORPS } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.SUPPLY_CORPS) return nO + 2 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return DEPOT_GOODS ;
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
    return new String[] { "STATUS", "STAFF", "ORDERS" } ;
  }
  

  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 2) {
      d.append("Trade Quotas (Per Day)") ;
      descStock(d, exportLevels) ;
    }
    else super.writeInformation(d, categoryID, UI) ;
  }
  
  
  final private static Colour PREF_COLOURS[] = {
    Colour.YELLOW, Colour.RED, Colour.MAGENTA,
    Colour.BLUE, Colour.CYAN, Colour.GREEN,
  } ;
  
  
  private void descStock(Description d, final Table <Service, Integer> prefs) {
    
    for (final Service type : DEPOT_GOODS) {
      
      Integer level = prefs.get(type) ;
      if (level == null) level = 0 ;
      final boolean imports = level < 0 ;
      final int oldPref = imports ? (0 - level) : level ;
      final Colour tone = PREF_COLOURS[(oldPref + 5) / 10] ;
      
      final int amount = (int) Math.ceil(stocks.amountOf(type)) ;
      if (oldPref == 0) d.append("\n  Not trading "+type.name, tone) ;
      else {
        d.append("\n  "+(imports ? "Import " : "Export "), tone) ;
        d.append(oldPref+" "+type.name+" (have "+amount+")", tone) ;
      }
      
      d.append("\n    ") ;
      d.append(new Description.Link("Export ") {
        public void whenClicked() {
          if (imports) prefs.put(type, 5) ;
          else {
            if (oldPref >= 50) return ;
            prefs.put(type, oldPref + 5) ;
          }
        }
      }) ;
      d.append(new Description.Link("Import ") {
        public void whenClicked() {
          if (imports) {
            if (oldPref >= 50) return ;
            prefs.put(type, 0 - (oldPref + 5)) ;
          }
          else prefs.put(type, -5) ;
        }
      }) ;
    }
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









