/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
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
  
  List <Holding> holdings = new List <Holding> () ;
  List <Actor> toHouse  = new List <Actor> () ;
  
  
  
  public VaultSystem(Base belongs) {
    super(4, 2, ENTRANCE_EAST, belongs) ;
    structure.setupStats(500, 20, 350, 0, false) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public VaultSystem(Session s) throws Exception {
    super(s) ;
    s.loadObjects(holdings) ;
    s.loadObjects(toHouse ) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(holdings) ;
    s.saveObjects(toHouse ) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    Building b = Building.getNextRepairFor(actor, Plan.CASUAL) ;
    if (b != null) {
      return b ;
    }
    return null ;
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
    return new Service[] { POWER, LIFE_SUPPORT } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    final float condition = (structure.repairLevel() + 1f) / 2 ;
    int powerLimit = 5, lifeSLimit = 10 ;
    powerLimit *= condition ;
    lifeSLimit *= condition ;
    if (stocks.amountOf(POWER) < powerLimit) {
      stocks.addItem(Item.withAmount(POWER, 1)) ;
    }
    if (stocks.amountOf(LIFE_SUPPORT) < lifeSLimit) {
      stocks.addItem(Item.withAmount(LIFE_SUPPORT, 1)) ;
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
      "The Vault System provides an emergency refuge for base personnel, "+
      "allowing goods to be stockpiled and providing a baseline degree of "+
      "power and life support." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
}




/*
///I.say("Updating demands...") ;
stocks.clearDemands() ;
for (Holding holding : holdings) {
  for (Item i : holding.goodsWanted().raw) {
    stocks.incRequired(i.type, i.amount) ;
  }
  stocks.incRequired(STARCHES, 10) ;
  stocks.incRequired(GREENS  , 10) ;
  stocks.incRequired(PROTEIN , 10) ;
  ///I.say("Adding demand for: "+holding.fullName()) ;
}

for (Object t : world.presences.matchesNear(base(), this, 32)) {
  final Venue v = (Venue) t ;
  for (Actor citizen : v.personnel.workers()) {
    if (citizen.AI.home() != null) continue ;
    I.say("Attempting to find housing for: "+citizen) ;
    Holding holding = findHousingSite(citizen, v) ;
    if (holding != null) {
      I.say("Housing found!") ;
      final Tile o = holding.origin() ;
      holding.clearSurrounds() ;
      holding.enterWorldAt(o.x, o.y, world) ;
      citizen.AI.setHomeVenue(holding) ;
      toHouse.remove(citizen) ;
      holdings.add(holding) ;
    }
  }
}
//*/
/*
for (Holding h : holdings) for (Item i : h.goodsNeeded().raw) {
  final Delivery d = deliveryFor(i, h) ;
  if (d != null) return d ;
}
for (Holding h : holdings) for (Item i : h.goodsWanted().raw) {
  final Delivery d = deliveryFor(i, h) ;
  if (d != null) return d ;
}
//*/

/**  Obtaining and rating housing sites-
  *    Consider making this static within the Holding class.
  */
/*
private Holding findHousingSite(Actor citizen, Venue works) {
  
  final int maxRange = World.DEFAULT_SECTOR_SIZE ;
  //final Holding holding = new Holding(base(), this) ;
  final Holding holding = new Holding(base()) ;
  
  Vec3D midPos = idealSite(citizen, works) ;
  final Tile midTile = world.tileAt(midPos.x, midPos.y) ;
  final Tile enterTile = Spacing.nearestOpenTile(midTile, midTile) ;
  final Box2D limit = new Box2D().set(
    midPos.x - maxRange, midPos.y - maxRange,
    maxRange * 2, maxRange * 2
  ) ;
  
  I.say("  Searching from "+enterTile+"... ") ;
  final TileSpread spread = new TileSpread(enterTile) {
    
    protected boolean canAccess(Tile t) {
      if (t.blocked()) return false ;
      //if (t.pathType() >= Tile.PATH_HINDERS) return false ;
      //if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
      return limit.contains(t.x, t.y) ;
    }
    
    protected boolean canPlaceAt(Tile t) {
      I.add("|") ;
      holding.setPosition(t.x, t.y, world) ;
      if (holding.canPlace()) {
        I.say("Found location!") ;
        return true ;
      }
      return false ;
    }
  } ;
  spread.doSearch() ;
  I.say("  Total tiles searched: "+spread.allSearched(Tile.class).length) ;
  
  if (holding.origin() != null) return holding ;
  return null ;
}


private Vec3D idealSite(Actor citizen, Venue works) {
  Vec3D midPos = works.position(null) ;
  midPos.add(this.position(null)).scale(0.5f) ;
  return midPos ;
}


/*
private float rateHolding(Holding holding, Citizen citizen) {
  Vec3D midPos = idealSite(citizen) ;
  float rating = 0 - midPos.distance(holding.position()) ;
  return rating ;
}
//*/

//  TODO- debug this.

/*
private Delivery deliveryFor(Item i, Holding h) {
  float needed = i.amount - h.stocks.amountOf(i) ;
  if (needed <= 0) return null ;
  needed = (float) Math.ceil(needed / 5) * 5 ;
  final Delivery d = new Delivery(Item.withAmount(i, needed), this, h) ;
  if (d.valid()) return d ;
  return null ;
}
//*/


