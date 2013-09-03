


package src.game.campaign ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;



/*
System
  thisPlanet,
  homePlanet,
  housePlanets[] ;
//*/


public class Commerce {
  
  
  /**  Fields definitions, constructor, save/load methods-
    */
  final static float
    SUPPLY_INTERVAL = 10,
    SUPPLY_DURATION = 10,
    
    MAX_APPLICANTS = 3 ;
  
  
  final Base base ;
  
  final Table <Venue, List <Actor>> candidates = new Table() ;
  final List <Actor> migrantsIn = new List <Actor> () ;
  
  Dropship ship ;
  float nextVisitTime ;
  
  
  
  public Commerce(Base base) {
    this.base = base ;
    ship = new Dropship() ;
    ship.assignBase(base) ;
    //  TODO:  This crew will need to be updated every now and then.
    //  TODO:  Include a pilot, mechanic, et cetera?
    addCrew(ship, Vocation.SUPPLY_CORPS, Vocation.SUPPLY_CORPS) ;
    nextVisitTime = Rand.num() * SUPPLY_INTERVAL ;
  }
  
  
  public void loadState(Session s) throws Exception {
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Venue venue = (Venue) s.loadObject() ;
      final List <Actor> list = new List <Actor> () ;
      candidates.put(venue, list) ;
      
      for (int i = s.loadInt() ; i-- > 0 ;) {
        final Actor a = (Actor) s.loadObject() ;
        list.add(a) ;
      }
    }
    
    s.loadObjects(migrantsIn) ;
    ship = (Dropship) s.loadObject() ;
    nextVisitTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(candidates.size()) ;
    for (Venue venue : candidates.keySet()) {
      s.saveObject(venue) ;
      final List <Actor> list = candidates.get(venue) ;
      s.saveInt(list.size()) ;
      for (Actor a : list) s.saveObject(a) ;
    }
    
    s.saveObjects(migrantsIn) ;
    s.saveObject(ship) ;
    s.saveFloat(nextVisitTime) ;
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  public boolean genCandidate(Vocation vocation, Venue venue, int numOpen) {
    //
    //  You might want to introduce limits on the probability of finding
    //  candidates based on the relative sizes of the source and destination
    //  settlements, and the number of existing applicants for a position.
    final int numA = venue.personnel.numApplicants(vocation) ;
    if (numA >= numOpen * MAX_APPLICANTS) return false ;
    final Human candidate = new Human(vocation, venue.base()) ;
    //
    //  This requires more work on the subject of pricing.  Some will join for
    //  free, but others need enticement, depending on distance and willingness
    //  to relocate, and the friendliness of the home system.
    final int signingCost = (vocation.standing + 1) * 100 ;
    venue.personnel.applyFor(vocation, candidate, signingCost) ;
    //
    //  Insert the candidate in local records, and return.
    List <Actor> list = candidates.get(venue) ;
    if (list == null) candidates.put(venue, list = new List <Actor> ()) ;
    list.add(candidate) ;
    return true ;
  }
  
  
  public void cullCandidates(Vocation vocation, Venue venue) {
    final List <Actor> list = candidates.get(venue) ;
    if (list == null) return ;
    final int numOpenings = venue.numOpenings(vocation) ;
    if (numOpenings > 0) return ;
    for (Actor actor : list) if (actor.vocation() == vocation) {
      list.remove(actor) ;
      venue.personnel.removeApplicant(actor) ;
    }
    if (list.size() == 0) candidates.remove(venue) ;
  }
  
  
  public void addImmigrant(Actor actor) {
    migrantsIn.add(actor) ;
  }
  
  
  protected void updateCandidates() {
    //  TODO:  You want to have old applicants give up and new ones appear,
    //  given enough time.  Once or twice per day, let's say.
  }
  
  
  private void addCrew(Dropship ship, Vocation... positions) {
    for (Vocation v : positions) {
      final Human staff = new Human(new Career(v), base) ;
      staff.AI.setEmployer(ship) ;
    }
  }
  
  
  
  /**  Assessing supply and demand associated with goods-
    */
  Inventory summariseDemand(Base base) {
    //
    //  TODO:  Limit the amount of this the homeworld is willing to provide?
    final Inventory summary = new Inventory(null) ;
    final World world = base.world ;
    for (Object o : world.presences.matchesNear(base, world.tileAt(0, 0), -1)) {
      if (! (o instanceof Venue)) continue ;
      final Venue venue = (Venue) o ;
      for (Item item : venue.stocks.shortages(true)) {
        summary.addItem(item) ;
      }
    }
    for (Item item : summary.allItems()) {
      summary.removeItem(item) ;
      final float bump = (float) Math.ceil(item.amount / 5f) * 5 ;
      summary.addItem(Item.withAmount(item, bump)) ;
    }
    return summary ;
  }
  
  
  private void loadCargo(
    Dropship ship, Inventory available, final boolean imports
  ) {
    ship.cargo.removeAllItems() ;
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
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce() {
    //  ...Make this a little less frequent?
    ///screenCandidates() ;
    updateShipping() ;
  }
  
  
  protected void updateShipping() {
    
    final int shipStage = ship.flightStage() ;
    if (ship.landed()) {
      final float sinceDescent = ship.timeLanded() ;
      ///I.say("All aboard? "+ship.allAboard()+", time: "+sinceDescent) ;
      if (sinceDescent > SUPPLY_DURATION) {
        ///I.say("Ship stage: "+shipStage) ;
        if (shipStage == Dropship.STAGE_LANDED) ship.beginBoarding() ;
        if (ship.allAboard() && shipStage == Dropship.STAGE_BOARDING) {
          ship.beginAscent() ;
        }
      }
    }
    if (! ship.inWorld()) {
      final Inventory shortages = summariseDemand(base) ;
      boolean shouldVisit = migrantsIn.size() > 0 ;// || ! shortages.empty() ;
      shouldVisit &= base.world.currentTime() > nextVisitTime ;
      shouldVisit &= ship.timeAway(base.world) > SUPPLY_INTERVAL ;
      
      if (shouldVisit) {
        final Box2D zone = Dropship.findLandingSite(ship, base) ;
        if (zone == null) return ;
        I.say("Sending ship to land at: "+zone) ;
        
        while (ship.inside().size() < Dropship.MAX_PASSENGERS) {
          if (migrantsIn.size() == 0) break ;
          final Actor migrant = migrantsIn.removeFirst() ;
          ship.setInside(migrant, true) ;
        }

        loadCargo(ship, shortages, true) ;
        for (Actor c : ship.crew()) ship.setInside(c, true) ;
        
        ship.beginDescent(zone, base.world) ;
        nextVisitTime = base.world.currentTime() + SUPPLY_DURATION ;
        nextVisitTime += SUPPLY_INTERVAL * (1 + Rand.num()) ;
      }
    }
  }
}






