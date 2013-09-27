/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet;
//import src.game.planet.Planet ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class VaultSystem extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    VaultSystem.class, "media/Buildings/merchant/town_vault.png", 4, 2
  ) ;
  
  final static int
    NUM_PREFS = 4,
    STOCK_LIMIT = 250 ;
  final static String PREF_TITLES[] = {
    "Life Support",
    "Basic rations",
    "Building materials",
    "Medical supplies",
  } ;
  final static int PREF_LEVELS[] = {
    0, 20, 50, 100
  }, NUM_LEVELS = 4 ;
  final static String LEVEL_TITLES[] = {
    "No Reserve", "Light Reserve", "Medium Reserve", "Heavy Reserve",
  } ;
  final private static Colour PREF_COLOURS[] = {
    //  TODO:  Blend the colours a bit more.
    Colour.LIGHT_GREY, Colour.BLUE, Colour.BLUE, Colour.BLUE,
  } ;
  
  
  final int stockLevels[] = new int[NUM_PREFS] ;
  
  
  
  public VaultSystem(Base belongs) {
    super(4, 2, ENTRANCE_EAST, belongs) ;
    structure.setupStats(500, 20, 350, 0, Structure.TYPE_FIXTURE) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public VaultSystem(Session s) throws Exception {
    super(s) ;
    for (int n = NUM_PREFS ; n-- > 0 ;) stockLevels[n] = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    for (int n = NUM_PREFS ; n-- > 0 ;) s.saveInt(stockLevels[n]) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    final Building b = Building.getNextRepairFor(actor, Plan.CASUAL) ;
    if (b != null) {
      final float priority = b.priorityFor(actor) ;
      if (priority * Planet.dayValue(world) >= Plan.ROUTINE) {
        return b ;
      }
    }
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    if (b != null) choice.add(b) ;
    
    final Service services[] = services() ;
    final Delivery d = Deliveries.nextCollectionFor(
      actor, this, services, 10, null, world
    ) ;
    choice.add(d) ;
    
    return choice.weightedPick(0) ;
  }
  
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b) ;
    if (b == Background.RESERVIST) return nO + 2 ;
    return 0 ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.RESERVIST } ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      POWER, LIFE_SUPPORT, FUEL_CORES,
      CARBS, PROTEIN, GREENS,
      PARTS, PLASTICS, CIRCUITRY,
      GENE_SEED, STIM_KITS, MEDICINE
    } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    //
    //  TODO:  Impose cumulative stocking limits.
    super.updateAsScheduled(numUpdates) ;
    final float condition = (structure.repairLevel() + 1f) / 2 ;
    final float stockBonus = 1 + (stockLevels[0] / 10f) ;
    int powerGen = 5, lifeSGen = 10 ;
    powerGen *= condition ;
    lifeSGen *= condition ;
    
    if (stocks.amountOf(POWER) < powerGen * stockBonus) {
      stocks.bumpItem(POWER, powerGen * 0.2f) ;
    }
    if (stocks.amountOf(LIFE_SUPPORT) < lifeSGen * stockBonus) {
      stocks.bumpItem(LIFE_SUPPORT, lifeSGen * 0.2f) ;
    }
    
    final Service services[] = services() ;
    for (int i = 0 ; i < services.length ; i++) {
      final Service s = services[i] ;
      if (s.form != FORM_COMMODITY) continue ;
      int level = stockLevels[i / 3] ;
      stocks.forceDemand(s, level, 1) ;
    }
  }
  
  

  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Vault System" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/vault_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Vault System provides an emergency refuge for base personnel while "+
      "allowing goods to be stockpiled and providing a baseline degree of "+
      "power and life support." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
  

  
  protected Service[] goodsToShow() {
    return new Service[0] ;
  }
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "STAFF", "STOCK", "ORDERS" } ;
  }
  

  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 3) {
      //d.append("Stockpile goods (Click to change)\n") ;
      d.append("Stockpiling "+totalStockLimit()+"/"+STOCK_LIMIT+" goods\n") ;
      for (int n = 0 ; n < NUM_PREFS ; n++) {
        descPref(n, PREF_TITLES[n], d) ;
      }
    }
    else super.writeInformation(d, categoryID, UI) ;
  }
  
  
  private int totalStockLimit() {
    int sum = 0 ;
    for (int s : stockLevels) sum += s ;
    return sum ;
  }
  
  
  private void descPref(final int index, String title, Description d) {
    int level = stockLevels[index] ;
    d.append("\n"+title) ;
    for (int n = 0 ; n < NUM_LEVELS ; n++) {
      if (level <= PREF_LEVELS[n]) {
        final int setting = n ;
        d.append("\n  ") ;
        d.append(new Description.Link(LEVEL_TITLES[n]) {
          public void whenClicked() {
            stockLevels[index] = PREF_LEVELS[(setting + 1) % NUM_LEVELS] ;
            if (totalStockLimit() > STOCK_LIMIT) stockLevels[index] = 0 ;
          }
        }, PREF_COLOURS[setting]) ;
        break ;
      }
    }
  }
}





