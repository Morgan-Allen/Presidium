/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class TownVault extends Venue implements VenueConstants {
  

  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    TownVault.class, "media/Buildings/vendor aura/town_vault.png", 4, 3
  ) ;
  
  List <Holding> holdings = new List <Holding> () ;
  List <Citizen> toHouse = new List <Citizen> () ;
  
  
  
  public TownVault(Base belongs) {
    super(4, 2, ENTRANCE_EAST, belongs) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  public TownVault(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Updates and behaviour-
    */
  //  If there's demand for housing, look for space nearby, and build homes to
  //  accomodate folk.
  
  //  TODO:  You also want to build housing up gradually.
  
  //  Are the technicians responsible for construction?  Yes they are.
  //  So what do the citizens do?  They pick up food and other essentials.
  
  
  public void updateAsScheduled() {
    super.updateAsScheduled() ;
    
    orders.clearDemands() ;
    for (Holding holding : holdings) {
      for (Item i : holding.goodsWanted().raw) {
        orders.incRequired(i.type, i.amount) ;
      }
      orders.incRequired(STARCHES, 10) ;
      orders.incRequired(GREENS  , 10) ;
      orders.incRequired(PROTEIN , 10) ;
    }
    
    for (Object o : base().servicesNear(base(), this, 32)) {
      final Venue v = (Venue) o ;
      for (Citizen c : v.personnel.workers()) {
        if (c.home() == null) toHouse.include(c) ;
      }
    }
    if (toHouse.size() > 0) {
      Citizen citizen = toHouse.first() ;
      I.say("Attempting to find housing for: "+citizen) ;
      Holding holding = findHousingSite(citizen) ;
      if (holding != null) {
        I.say("Housing found!") ;
        final Tile o = holding.origin() ;
        holding.clearSurrounds() ;
        holding.enterWorldAt(o.x, o.y, world) ;
        citizen.setHomeVenue(holding) ;
        toHouse.remove(citizen) ;
        holdings.add(holding) ;
      }
    }
  }
  
  
  /**  Obtaining and rating housing sites-
    *    Consider making this static within the Holding class.
    */
  private Holding findHousingSite(Citizen citizen) {
    
    final int maxRange = Planet.SECTOR_SIZE ;
    final Holding holding = new Holding(base(), this) ;
    
    Vec3D midPos = idealSite(citizen) ;
    final Tile midTile = world.tileAt(midPos.x, midPos.y) ;
    final Tile enterTile = Spacing.nearestOpenTile(midTile, midTile) ;
    final Box2D limit = new Box2D().set(
      midPos.x - maxRange, midPos.y - maxRange,
      maxRange * 2, maxRange * 2
    ) ;
    
    final Spread spread = new Spread(enterTile) {
      
      protected boolean canAccess(Tile t) {
        if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
        return limit.contains(t.x, t.y) ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        holding.setPosition(t.x, t.y, world) ;
        if (holding.canPlace()) {
          return true ;
        }
        return false;
      }
    } ;
    spread.doSearch() ;
    
    if (holding.origin() != null) return holding ;
    return null ;
  }
  
  
  private Vec3D idealSite(Citizen citizen) {
    Vec3D midPos = citizen.work().position() ;
    midPos.add(this.position()).scale(0.5f) ;
    return midPos ;
  }
  
  
  private float rateHolding(Holding holding, Citizen citizen) {
    Vec3D midPos = idealSite(citizen) ;
    float rating = 0 - midPos.distance(holding.position()) ;
    return rating ;
  }
  
  
  
  /**  Implementing construction, upgrades, downgrades and salvage-
    */
  public Behaviour nextStepFor(Actor actor) {
    for (Holding h : holdings) for (Item i : h.goodsNeeded().raw) {
      final Delivery d = deliveryFor(i, h) ;
      if (d != null) return d ;
    }
    for (Holding h : holdings) for (Item i : h.goodsWanted().raw) {
      final Delivery d = deliveryFor(i, h) ;
      if (d != null) return d ;
    }
    return null ;
  }
  
  private Delivery deliveryFor(Item i, Holding h) {
    float needed = i.amount - h.stocks.amountOf(i) ;
    if (needed < 0) return null ;
    needed = (float) Math.ceil(needed / 5) * 5 ;
    final Delivery d = new Delivery(new Item(i, needed), this, h) ;
    if (d.valid()) return d ;
    return null ;
  }
  
  

  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Town Vault" ;
  }
  
  
  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/vault_button.gif") ;
  }
  
  
  public String helpInfo() {
    return "The Town Vault provides an emergency refuge for base personnel, "+
      "thereby allowing construction of civilian housing.";
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.TECHNICIAN } ;
  }
  
  protected Item.Type[] itemsMade() {
    return new Item.Type[0] ;
  }
}






