


package src.game.campaign ;
import src.game.base.* ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;



public class Commerce implements Economy {
  
  
  /**  Fields definitions, constructor, save/load methods-
    */
  final public static float
    SUPPLY_INTERVAL = World.STANDARD_DAY_LENGTH / 2f,
    SUPPLY_DURATION = SUPPLY_INTERVAL / 2f,
    
    APPLY_INTERVAL  = World.STANDARD_DAY_LENGTH / 2f,
    UPDATE_INTERVAL = 10,
    DEMAND_INC      = 0.15f,
    MAX_APPLICANTS  = 3 ;
  
  private static boolean verbose = false ;
  
  
  final Base base ;
  System homeworld ;
  List <System> partners = new List <System> () ;
  
  final static int NUM_J = Background.ALL_BACKGROUNDS.length ;
  final float
    jobSupply[] = new float[NUM_J],
    jobDemand[] = new float[NUM_J] ;
  
  
  final List <Actor> candidates = new List <Actor> () ;
  final List <Actor> migrantsIn = new List <Actor> () ;
  
  final Inventory
    shortages = new Inventory(null),
    surpluses = new Inventory(null) ;
  final Table <Service, Float>
    importPrices = new Table <Service, Float> (),
    exportPrices = new Table <Service, Float> () ;
  
  private Dropship ship ;
  private float nextVisitTime ;
  
  
  
  public Commerce(Base base) {
    this.base = base ;
    
    for (Service type : ALL_COMMODITIES) {
      importPrices.put(type, (float) type.basePrice) ;
      exportPrices.put(type, (float) type.basePrice) ;
    }
    
    ship = new Dropship() ;
    ship.assignBase(base) ;
    addCrew(ship,
      Background.SHIP_CAPTAIN,
      Background.SHIP_MECHANIC
    ) ;
    nextVisitTime = Rand.num() * SUPPLY_INTERVAL ;
  }
  
  
  public void loadState(Session s) throws Exception {
    
    final int hID = s.loadInt() ;
    homeworld = hID == -1 ? null : (System) Background.ALL_BACKGROUNDS[hID] ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      partners.add((System) Background.ALL_BACKGROUNDS[s.loadInt()]) ;
    }
    
    for (int i = NUM_J ; i-- > 0 ;) {
      jobSupply[i] = s.loadFloat() ;
      jobDemand[i] = s.loadFloat() ;
    }
    
    shortages.loadState(s) ;
    surpluses.loadState(s) ;
    for (Service type : ALL_COMMODITIES) {
      importPrices.put(type, s.loadFloat()) ;
      exportPrices.put(type, s.loadFloat()) ;
    }
    
    s.loadObjects(candidates) ;
    s.loadObjects(migrantsIn) ;
    ship = (Dropship) s.loadObject() ;
    nextVisitTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(homeworld == null ? -1 : homeworld.ID) ;
    s.saveInt(partners.size()) ;
    for (System p : partners) s.saveInt(p.ID) ;
    
    for (int i = NUM_J ; i-- > 0 ;) {
      s.saveFloat(jobSupply[i]) ;
      s.saveFloat(jobDemand[i]) ;
    }
    
    shortages.saveState(s) ;
    surpluses.saveState(s) ;
    for (Service type : ALL_COMMODITIES) {
      s.saveFloat(importPrices.get(type)) ;
      s.saveFloat(exportPrices.get(type)) ;
    }
    
    s.saveObjects(candidates) ;
    s.saveObjects(migrantsIn) ;
    s.saveObject(ship) ;
    s.saveFloat(nextVisitTime) ;
  }
  
  
  public void assignHomeworld(System s) {
    homeworld = s ;
    togglePartner(s, true) ;
  }
  
  
  public System homeworld() {
    return homeworld ;
  }
  
  
  public void togglePartner(System s, boolean is) {
    if (is) {
      partners.include(s) ;
    }
    else {
      partners.remove(s) ;
      if (s == homeworld) homeworld = null ;
    }
  }
  
  
  public List <System> partners() {
    return partners ;
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  public void addImmigrant(Actor actor) {
    migrantsIn.add(actor) ;
  }
  
  
  protected void updateCandidates(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return ;
    
    final float inc = DEMAND_INC, timeGone = UPDATE_INTERVAL / APPLY_INTERVAL ;
    for (Background b : Background.ALL_BACKGROUNDS) {
      final int i = b.ID ;
      jobDemand[i] *= (1 - inc) ;
      jobDemand[i] = Math.max(jobDemand[i] - (inc / 100), 0) ;
      jobSupply[i] = 0 ;
    }
    
    for (Actor c : candidates) {
      final Application a = c.mind.application() ;
      if (a == null) continue ;
      jobSupply[a.position.ID]++ ;
    }
    
    for (Background b : Background.ALL_BACKGROUNDS) {
      final float supply = jobSupply[b.ID], demand = jobDemand[b.ID] ;
      if (demand == 0) continue ;
      //float applyChance = (demand * MAX_APPLICANTS) - supply ;
      float applyChance = demand / (supply + demand) ;
      applyChance *= timeGone ;
      
      if (verbose) {
        I.say("  Hire chance for "+b+" is "+applyChance) ;
        I.say("  Supply/demand "+supply+" / "+demand) ;
      }
      
      while (Rand.num() < applyChance) {
        final Human applies = new Human(b, base) ;
        candidates.addFirst(applies) ;
        FindWork.lookForWork((Human) applies, base) ;
        applyChance-- ;
      }
    }
    
    //
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    
    for (ListEntry e = candidates ; (e = e.nextEntry()) != candidates ;) {
      final Human c = (Human) e.refers ;
      final Application a = c.mind.application() ;
      float quitChance = 1 ;
      if (a != null) {
        final Background b = c.mind.application().position ;
        final float
          supply = jobSupply[b.ID],
          demand = jobDemand[b.ID] ;
        quitChance = supply / (supply + demand) ;
        quitChance *= timeGone ;
        
        if (verbose) {
          I.say("Quit chance for "+a.position+" "+c+" is: "+quitChance) ;
        }
      }
      if (Rand.num() > quitChance) {
        if (Rand.num() < timeGone) {
          FindWork.lookForWork((Human) c, base) ;
        }
      }
      else {
        candidates.removeEntry(e) ;
        if (a != null) a.employer.personnel().setApplicant(a, false) ;
      }
    }
  }
  
  
  public void incDemand(Background b, float amount, int period) {
    jobDemand[b.ID] +=
      amount * (period / UPDATE_INTERVAL) * DEMAND_INC * MAX_APPLICANTS ;
  }
  
  
  
  /**  Assessing supply and demand associated with goods-
    */
  private void summariseDemand(Base base) {
    shortages.removeAllItems() ;
    surpluses.removeAllItems() ;
    
    final World world = base.world ;
    final Tile t = world.tileAt(0, 0) ;
    
    for (Object o : world.presences.matchesNear(SupplyDepot.class, t, -1)) {
      final SupplyDepot venue = (SupplyDepot) o ;
      if (venue.base() != base) continue ;
      for (Service type : ALL_COMMODITIES) {
        final float shortage = venue.importShortage(type) ;
        if (shortage > 0) shortages.addItem(Item.withAmount(type, shortage)) ;
        final float surplus = venue.exportSurplus(type) ;
        if (surplus  > 0) surpluses.addItem(Item.withAmount(type, surplus )) ;
      }
    }
    
    for (Item item : shortages.allItems()) {
      shortages.removeItem(item) ;
      final float bump = (float) Math.ceil(item.amount / 5f) * 5 ;
      shortages.addItem(Item.withAmount(item, bump)) ;
    }
  }
  
  
  private void calculatePrices() {
    //
    //  Typically speaking, exports have their value halved and imports have
    //  their price doubled if it's coming from offworld.  Anything coming from
    //  another sector of your own planet has much milder cost differences, and
    //  your homeworld will also cut you some slack, at least initially.
    //
    //  In addition, prices improve for exports particularly valued by your
    //  partners (and worsen if already abundant,) and vice versa for imports.
    //  Finally, the value of exports decreases, and of imports increases, with
    //  volume, but this is only likely to be significant for larger
    //  settlements.
    //  TODO:  Charge more for smuggler vessels, and less for Spacers.
    //  TODO:  Implement trade with settlements on the same planet(?)
    
    //
    //  TODO:  Have price levels be global for the settlement as a whole, rather
    //  than calculated at specific structures.  Vendors make money by charging
    //  more in general.
    
    for (Service type : ALL_COMMODITIES) {
      ///final boolean offworld = true ; //For now.
      float
        basePrice = 1 * type.basePrice,
        importMul = 2 + (shortages.amountOf(type) / 1000f),
        exportDiv = 2 + (surpluses.amountOf(type) / 1000f) ;
      
      for (System system : partners) {
        if (Visit.arrayIncludes(system.goodsMade, type)) {
          basePrice *= 0.75f ;
          if (system == homeworld) importMul /= 1.50f ;
        }
        if (Visit.arrayIncludes(system.goodsNeeded, type)) {
          basePrice *= 1.5f ;
          if (system == homeworld) exportDiv *= 0.75f ;
        }
      }
      
      if (homeworld != null) {
        final float sizeBonus = base.communitySpirit() ;
        importMul *= (1 - sizeBonus) ;
        exportDiv = (1 * sizeBonus) + (exportDiv * (1 - sizeBonus)) ;
      }
      
      importPrices.put(type, basePrice * importMul) ;
      exportPrices.put(type, basePrice / exportDiv) ;
    }
  }
  
  
  public float importPrice(Service type) {
    final Float price = importPrices.get(type) ;
    if (price == null) return type.basePrice * 10f ;
    return price ;
  }
  
  
  public float exportPrice(Service type) {
    final Float price = exportPrices.get(type) ;
    if (price == null) return type.basePrice / 10f ;
    return price ;
  }
  
  
  
  
  /**  Dealing with shipping and crew complements-
    */
  private void refreshCrew(Dropship ship) {
    //
    //  TODO:  This crew will need to be updated every now and then- in the
    //         sense of changing the roster.
    //
    //  Get rid of fatigue and hunger, modulate mood, et cetera- account for
    //  the effects of time spent offworld.
    for (Actor works : ship.crew()) {
      final float MH = works.health.maxHealth() ;
      works.health.liftFatigue(MH * Rand.num()) ;
      works.health.takeSustenance(MH, 0.25f + Rand.num()) ;
      works.health.adjustMorale(Rand.num() / 2f) ;
      works.mind.clearAgenda() ;
    }
  }
  
  
  private void addCrew(Dropship ship, Background... positions) {
    for (Background v : positions) {
      final Human staff = new Human(new Career(v), base) ;
      staff.mind.setWork(ship) ;
      //staff.mind.setHomeVenue(ship) ;
    }
  }
  
  
  private void loadCargo(
    Dropship ship, Inventory available, final boolean imports
  ) {
    ship.cargo.removeAllItems() ;
    I.say("Loading cargo...") ;
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
      I.say("Loading cargo: "+item) ;
    }
  }
  
  
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> () ;
    vessels.add(ship) ;
    return vessels ;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce(int numUpdates) {
    updateCandidates(numUpdates) ;
    if (numUpdates % 10 == 0) {
      summariseDemand(base) ;
      calculatePrices() ;
    }
    updateShipping() ;
  }
  
  
  protected void updateShipping() {
    
    final int shipStage = ship.flightStage() ;
    if (ship.landed()) {
      final float sinceDescent = ship.timeLanded() ;
      if (sinceDescent > SUPPLY_DURATION) {
        if (shipStage == Dropship.STAGE_LANDED) ship.beginBoarding() ;
        if (ship.allAboard() && shipStage == Dropship.STAGE_BOARDING) {
          ship.beginAscent() ;
          nextVisitTime = base.world.currentTime() ;
          nextVisitTime += SUPPLY_INTERVAL * (0.5f + Rand.num()) ;
        }
      }
    }
    if (! ship.inWorld()) {
      boolean shouldVisit = migrantsIn.size() > 0 ;
      if ((! shortages.empty()) || (! surpluses.empty())) shouldVisit = true ;
      shouldVisit &= base.world.currentTime() > nextVisitTime ;
      shouldVisit &= ship.timeAway(base.world) > SUPPLY_INTERVAL ;
      
      if (shouldVisit) {
        I.say("SENDING SHIP...") ;
        final boolean canLand = ship.findLandingSite(base) ;
        if (! canLand) return ;
        /*
        final Box2D zone = Dropship.findLandingSite(ship, base) ;
        if (zone == null) return ;
        //*/
        I.say("Sending ship to land at: "+ship.dropPoint()) ;
        
        while (ship.inside().size() < Dropship.MAX_PASSENGERS) {
          if (migrantsIn.size() == 0) break ;
          final Actor migrant = migrantsIn.removeFirst() ;
          ship.setInside(migrant, true) ;
        }
        
        loadCargo(ship, shortages, true) ;
        refreshCrew(ship) ;
        for (Actor c : ship.crew()) ship.setInside(c, true) ;
        
        ship.beginDescent(base.world) ;
      }
    }
  }
}






/*
public boolean genCandidate(Background vocation, Venue venue, int numOpen) {
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
  final int signingCost = Background.HIRE_COSTS[vocation.standing] ;
  venue.personnel.applyFor(vocation, candidate, signingCost) ;
  //
  //  Insert the candidate in local records, and return.
  List <Actor> list = candidates.get(venue) ;
  if (list == null) candidates.put(venue, list = new List <Actor> ()) ;
  list.add(candidate) ;
  return true ;
}


public void cullCandidates(Background vocation, Venue venue) {
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
//*/
