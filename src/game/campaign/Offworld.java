


package src.game.campaign ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;



public class Offworld {
  
  
  /**  Fields definitions, constructor, save/load methods-
    */
  //
  //  List of migrants.
  //  List of goods to buy/sell.
  //  Supply freighter.  Smuggler freighter.  Spacer freighter.
  //  Planetary trade/migration (same as with any other planet.)
  final static float
    SUPPLY_INTERVAL = 10,
    SUPPLY_DURATION = 40 ;
  
  final Base base ;
  
  List <Actor>
    candidates = new List <Actor> (),
    migrantsIn = new List <Actor> () ;
  
  static class Commerce {
    Dropship ship ;
    float nextVisitTime ;
    //  Planet in question, visit intervals, diplomatic relations, etc.
  }
  
  Commerce homeSupply ;
  
  System
    thisPlanet,
    homePlanet,
    housePlanets[] ;
  
  
  public Offworld(Base base) {
    this.base = base ;
    homeSupply = new Commerce() ;
    homeSupply.ship = new Dropship() ;
    homeSupply.nextVisitTime = Rand.num() * SUPPLY_INTERVAL ;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(candidates) ;
    s.loadObjects(migrantsIn) ;
    homeSupply = loadCommerce(s) ;
    //supplyFreighter = (Freighter) s.loadObject() ;
  }
  
  
  private Commerce loadCommerce(Session s) throws Exception {
    final Commerce c = new Commerce() ;
    c.ship = (Dropship) s.loadObject() ;
    c.nextVisitTime = s.loadFloat() ;
    return c ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(candidates) ;
    s.saveObjects(migrantsIn) ;
    //s.saveObject(supplyFreighter) ;
    saveCommerce(homeSupply, s) ;
  }
  
  
  private void saveCommerce(Commerce c, Session s) throws Exception {
    s.saveObject(c.ship) ;
    s.saveFloat(c.nextVisitTime) ;
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  public void addImmigrant(Actor actor) {
    migrantsIn.add(actor) ;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateEvents() {
    final Inventory shortages = summariseDemand(base) ;
    updateCommerce(homeSupply, shortages, SUPPLY_INTERVAL, SUPPLY_DURATION) ;
  }
  
  //
  //  TODO:  Consider moving the code for finding landing sites to here?
  
  
  Inventory summariseDemand(Base base) {
    final Inventory summary = new Inventory(null) ;
    for (Object o : base.servicesNear(base, base.world.tileAt(0, 0), -1)) {
      if (! (o instanceof Venue)) continue ;
      final Venue venue = (Venue) o ;
      for (Item item : venue.orders.shortages(true)) {
        summary.addItem(item) ;
      }
    }
    for (Item item : summary.allItems()) {
      final float bump = (float) Math.ceil(item.amount / 5f) * 5 ;
      I.say(bump+" of "+item.type+" demanded...") ;
      summary.removeItem(item) ;
      summary.addItem(new Item(item, bump)) ;
    }
    return summary ;
  }
  
  
  //
  //  A good deal of this code should probably be moved to the Dropship class.
  private void updateCommerce(
    Commerce commerce, Inventory shortages,
    float visitInterval, float stayDuration
  ) {
    if (true) return ;
    final Dropship ship = commerce.ship ;
    /*
    I.say(
      "  Updating events for: "+ship+
      "\n  Next visit time: "+commerce.nextVisitTime+
      "\n  Current time: "+base.world.currentTime()
    ) ;
    //*/
    if (ship.landed()) {
      final float sinceDescent = ship.timeSinceDescent(base.world) ;
      I.say("All aboard? "+ship.allBoarded()) ;
      if (sinceDescent > stayDuration) {
        if (ship.flightStage() == Dropship.STAGE_LANDED) ship.beginBoarding() ;
        if (ship.allBoarded() || sinceDescent > stayDuration * 2) {
          loadCargo(ship, ship.landingSite().stocks, false) ;
          ship.beginAscent() ;
          commerce.nextVisitTime = base.world.currentTime() + visitInterval ;
        }
      }
    }
    if (! ship.inWorld()) {
      boolean shouldVisit = migrantsIn.size() > 0 || ! shortages.empty() ;
      shouldVisit &= base.world.currentTime() > commerce.nextVisitTime ;
      //
      //  You'll want to replace this with something more nuanced later.
      //  TODO:  Just replace with a different freighter?  Maybe have a certain
      //  chance of crew coming back to visit?
      if (ship.flightStage() == Dropship.STAGE_AWAY) {
        ship.cargo.removeAllItems() ;
        ship.inside().clear() ;
        ship.crew().clear() ;
      }
      if (shouldVisit) {
        //
        //  See if you can find a suitable site to land, and set up the crew-
        final DropZone zone = DropZone.findLandingSite(ship, base) ;
        if (zone == null) return ;
        I.say("Sending ship to land at: "+zone.area()) ;
        addCrew(ship, Vocation.SUPPLY_CORPS) ;  //TODO:  Include a pilot?
        //
        //  Board as many passengers as possible-
        //*
        while (ship.inside().size() < Dropship.MAX_PASSENGERS) {
          if (migrantsIn.size() == 0) break ;
          final Actor migrant = migrantsIn.removeFirst() ;
          ship.setInside(migrant, true) ;
        }
        //*/
        //
        //  Load up as much cargo as you can, and then enter the world-
        loadCargo(ship, shortages, true) ;
        zone.clearSurrounds() ;
        zone.enterWorld() ;
        ship.beginDescent(zone) ;
      }
    }
  }
  
  
  private void addCrew(Dropship ship, Vocation... positions) {
    for (Vocation v : positions) {
      final Human staff = new Human(new Career(v), base) ;
      staff.psyche.setEmployer(ship) ;
      ship.setInside(staff, true) ;
    }
  }
  
  
  private void loadCargo(
    Dropship ship, Inventory available, final boolean imports
  ) {
    //
    //  We prioritise items based on the amount of demand and the price of the
    //  goods in question-
    final Sorting <Item> sorting = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a == b) return 0 ;
        final float
          pA = a.amount / a.type.basePrice,
          pB = b.amount / b.type.basePrice ;
        return (imports ? 1 : -1) * (pA > pB ? 1 : -1) ;
      }
    } ;
    for (Item item : available.allItems()) sorting.add(item) ;
    float totalAmount = 0 ;
    for (Item item : sorting) {
      if (totalAmount + item.amount > ship.MAX_CAPACITY) break ;
      available.removeItem(item) ;
      ship.cargo.addItem(item) ;
      totalAmount += item.amount ;
    }
  }
}














