/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.base.* ;
import src.game.campaign.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;





//
//  An actor's willingness to apply for an opening should be based on the
//  number of current applicants.  You don't want to overwhelm the player
//  with choices.
//
//  You'd need to assess an actor's fitness for a given Vocation, in terms of
//  both skills and personality.  (And they'd have to be given gear.)


public class VenuePersonnel {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Venue venue ;
  
  static class Position {
    Background role ;
    Actor works ;
    
    int salary ;
    float wages = -1 ;  //If not hired yet.
  }
  
  final List <Position>
    positions = new List <Position> () ;
  final List <Actor>
    workers   = new List <Actor> (),
    residents = new List <Actor> () ;
  private int
    shiftType = -1 ;
  
  
  
  VenuePersonnel(Venue venue) {
    this.venue = venue ;
  }
  
  
  void loadState(Session s) throws Exception {
    shiftType = s.loadInt() ;
    s.loadObjects(workers) ;
    s.loadObjects(residents) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Position a = new Position() ;
      a.role = Background.ALL_BACKGROUNDS[s.loadInt()] ;
      a.works = (Actor) s.loadObject() ;
      a.salary = s.loadInt() ;
      a.wages = s.loadFloat() ;
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(shiftType) ;
    s.saveObjects(workers) ;
    s.saveObjects(residents) ;
    
    s.saveInt(positions.size()) ;
    for (Position a : positions) {
      s.saveInt(a.role.ID) ;
      s.saveObject(a.works) ;
      s.saveInt(a.salary) ;
      s.saveFloat(a.wages) ;
    }
  }
  
  
  public List <Actor> workers() {
    return workers ;
  }
  
  
  public List <Actor> residents() {
    return residents ;
  }
  
  
  public void setShiftType(int type) {
    this.shiftType = type ;
  }
  
  
  
  /**  Handling shifts and being off-duty:
    */
  public boolean onShift(Actor worker) {
    if (shiftType == -1) return false ;
    if (shiftType == Venue.SHIFTS_ALWAYS) return true ;
    //
    //  Firstly, determine proper indices for the shift and the roster-
    final float time = venue.world().currentTime() / World.DEFAULT_DAY_LENGTH ;
    int currentShift = 0, shiftCycle = 0, workerIndex = 0 ;
    int numShifts = 0 ;
    if (shiftType == Venue.SHIFTS_BY_HOURS) {
      shiftCycle = (int) time ;
      currentShift = (int) ((time * 3) % 3) ;
      numShifts = 1;
    }
    if (shiftType == Venue.SHIFTS_BY_DAY) {
      if ((time % 1) > 0.5f) return false ;
      shiftCycle = (int) (time / 3) ;
      currentShift = ((int) time) % 3 ;
      numShifts = 2 ;
    }
    if (shiftType == Venue.SHIFTS_BY_CALENDAR) {
      I.complain("CALENDAR NOT IMPLEMENTED YET!") ;
    }
    for (Actor actor : workers) {
      if (actor == worker) break ;
      else workerIndex++ ;
    }
    //
    //  Then, see if they match up.  This probably requires a little more
    //  explanation...
    //
    //  Imagine we have Actors 1-through-4, and Shifts A, B, and C.  As each
    //  cycle of shifts proceeds, we want to allot actors to shifts as follows:
    //
    // (Cycle 1)(Cycle 2)(Cycle 3) ...
    //   A B C    A B C    A B C   ...
    //   1 2 3    3 4 1    1 2 3   ...
    //   4 1 2    2 3 4    4 1 2   ...etc.
    //
    //  The basic idea being that all shifts are filled as evenly as possible,
    //  without any single actor being consistently overworked.
    
    //  TODO:  ...Actually, this probably needs to be rethought.  It may not
    //  be performing as advertised, or maybe it's a bad idea.  Maybe leaving
    //  gaps in the roster is better than overworking citizens...?
    
    if (workers.size() < 3) return workerIndex == currentShift ;
    for (int period = 0 ; period < workers.size() ; period += 3) {
      for (int shiftIndex = numShifts ; shiftIndex-- > 0 ;) {
        final int index = period + currentShift + shiftIndex + shiftCycle ;
        if ((index % workers.size()) == workerIndex) return true ;
      }
    }
    return false ;
  }
  
  
  public int assignedTo(Class planClass) {
    int num = 0 ; for (Actor actor : workers) {
      if (actor.isDoing(planClass, null)) num++ ;
    }
    return num ;
  }
  
  
  public int assignedTo(Plan matchPlan) {
    if (matchPlan == null) return 0 ;
    int count = 0 ;
    for (Actor actor : workers) {
      for (Behaviour b : actor.AI.agenda()) if (b instanceof Plan) {
        if (((Plan) b).matchesPlan(matchPlan)) {
          count++ ;
        }
      }
    }
    return count ;
  }
  
  
  
  /**  Methods related to payment of wages-
    */
  protected void allocateWages() {
    if (venue.privateProperty()) return ;
    //
    //  Firstly, allocate minimum wage-
    float sumWages = 0 ;
    for (Position p : positions) if (p.wages >= 0) {
      final float wage = p.salary / 10f ;
      p.wages += wage ;
      sumWages += wage ;
    }
    venue.stocks.incCredits(0 - sumWages) ;
    //
    //  Then split a portion of surplus between employees in proportion to
    //  basic salary.
    final float surplus = venue.stocks.credits() ;
    if (surplus > 0) {
      float sumSalaries = 0 ;
      for (Position p : positions) if (p.wages >= 0) sumSalaries += p.salary ;
      for (Position p : positions) if (p.wages >= 0) {
        p.wages += surplus * 0.1f * p.salary / sumSalaries ;
      }
      venue.stocks.incCredits(0 - surplus) ;
    }
    //
    //  Finally, report your debts or windfall back to the base.  Debts may
    //  be exaggerated and/or profits under-reported, unless skilled (and
    //  honest) auditors get there first.
    final Base base = venue.base() ;
    final float balance = venue.stocks.credits() ;
    final float waste = (Rand.num() + base.crimeLevel()) / 2f ;
    if (balance > 0) {
      final float paid = balance / (1 + waste) ;
      base.incCredits(paid) ;
      venue.chat.addPhrase((int) paid+" credits in profit") ;
      venue.stocks.incCredits(0 - paid) ;
    }
    if (balance < 0) {
      final float paid = (0 - balance) * (1 + waste) ;
      base.incCredits(0 - paid) ;
      venue.chat.addPhrase((int) paid+" credits in debt") ;
      venue.stocks.incCredits(paid) ;
    }
    venue.stocks.taxDone() ;
  }
  
  
  protected void checkWagePayment() {
    for (Position p : positions) {
      if (p.works.aboard() == venue && p.wages > 0) {
        I.say("DISPENSING "+p.wages+" CREDITS IN WAGES TO "+p.works) ;
        p.works.gear.incCredits(p.wages) ;
        p.wages = 0 ;
      }
    }
  }
  
  
  
  /**  Handling applications and recruitment-
    */
  public Position applyFor(Background v, Actor applies, int signingCost) {
    final Position p = new Position() ;
    p.role = v ;
    p.works = applies ;
    p.salary = signingCost ;
    positions.add(p) ;
    return p ;
  }
  
  
  protected Position positionFor(Actor works) {
    for (Position p : positions) if (p.works == works) return p ;
    return null ;
  }
  
  
  public void removeApplicant(Actor applied) {
    final Position p = positionFor(applied) ;
    if (p != null) positions.remove(p) ;
  }
  
  
  public int numApplicants(Background v) {
    int num = 0 ;
    for (Position a : positions) if (a.role == v && a.wages < 0) num++ ;
    return num ;
  }
  
  
  protected void confirmApplication(Position app) {
    venue.base().incCredits(0 - app.salary) ;
    app.works.gear.incCredits(app.salary / 2) ;
    app.works.gear.taxDone() ;
    app.works.AI.setEmployer(venue) ;
    
    if (! app.works.inWorld()) {
      final Commerce commerce = venue.base().commerce ;
      commerce.cullCandidates(app.role, venue) ;
      commerce.addImmigrant(app.works) ;
    }
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void updatePersonnel(int numUpdates) {
    checkWagePayment() ;
    
    if (numUpdates % 10 == 0) {
      final World world = venue.world() ;
      //
      //  Clear out the office for anyone dead-
      for (Actor a : workers) if (a.destroyed()) setWorker(a, false) ;
      for (Actor a : residents) if (a.destroyed()) setResident(a, false) ;
      //
      //  If there's an unfilled opening, look for someone to fill it.
      //  TODO:  This should really be handled more from the Commerce class?
      if (venue.careers() == null) return ;
      for (Background v : venue.careers()) {
        final int numOpen = venue.numOpenings(v) ;
        if (numOpen <= 0) continue ;
        //
        //  
        if (GameSettings.hireFree) {
          final Human citizen = new Human(v, venue.base()) ;
          citizen.AI.setEmployer(venue) ;
          final Tile t = venue.mainEntrance() ;
          citizen.enterWorldAt(t.x, t.y, world) ;
        }
        //
        //  
        else {
          venue.base().commerce.genCandidate(v, venue, numOpen) ;
        }
      }
    }

    if (numUpdates % World.DEFAULT_DAY_LENGTH == 100) allocateWages() ;
  }
  
  
  protected void onCommission() {
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.AI.setEmployer(null) ;
    for (Actor c : residents()) c.AI.setHomeVenue(null) ;
  }
  
  
  public void setWorker(Actor c, boolean is) {
    if (is) {
      Position p = positionFor(c) ;
      if (p == null) {
        final int cost = Background.HIRE_COSTS[c.vocation().standing] ;
        p = applyFor(c.vocation(), c, cost) ;
      }
      p.wages = 0 ;
      workers.include(c) ;
    }
    else {
      removeApplicant(c) ;
      workers.remove(c) ;
    }
  }
  
  
  public void setResident(Actor c, boolean is) {
    if (is) residents.include(c) ;
    else residents.remove(c) ;
  }
  
  
  public int numPositions(Background... match) {
    int num = 0 ; for (Actor c : workers) {
      for (Background v : match) if (c.vocation() == v) num++ ;
    }
    return num ;
  }
  
  
  public static void fillVacancies(Venue venue) {
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    if (venue.careers() == null) return ;
    for (Background v : venue.careers()) {
      final int numOpen = venue.numOpenings(v) ;
      if (numOpen <= 0) continue ;
      for (int i = numOpen ; i-- > 0 ;) {
        final Human worker = new Human(v, venue.base()) ;
        worker.AI.setEmployer(venue) ;
        final Tile e = venue.mainEntrance() ;
        worker.enterWorldAt(e.x, e.y, venue.world()) ;
      }
    }
  }
}






/*
public void recruitWorker(Vocation v) {
  //
  // You also need to determine the worker's home planet environment, full
  // name, and maybe links to family or one or two past career events.
  final Career career = new Career(v) ;
  final Human citizen = new Human(career, venue.base()) ;
  citizen.AI.setEmployer(venue) ;
  if (GameSettings.hireFree) {
    final Tile t = venue.mainEntrance() ;
    citizen.enterWorldAt(t.x, t.y, venue.world()) ;
  }
  else I.complain("SORT OUT THE FREIGHTER.  CRIPES.") ;
  //else venue.base.offworld.addImmigrant(citizen) ;
}
//*/


