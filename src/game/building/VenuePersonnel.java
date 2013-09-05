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
  
  static class Application {
    Vocation position ;
    Actor applies ;
    int signingCost ;
  }
  
  final List <Application>
    applications = new List <Application> () ;
  final List <Actor>
    workers   = new List <Actor> (),
    residents = new List <Actor> () ;
  private int shiftType = -1 ;
  
  
  VenuePersonnel(Venue venue) {
    this.venue = venue ;
  }
  
  
  void loadState(Session s) throws Exception {
    shiftType = s.loadInt() ;
    s.loadObjects(workers) ;
    s.loadObjects(residents) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Application a = new Application() ;
      a.position = Vocation.ALL_CLASSES[s.loadInt()] ;
      a.applies = (Actor) s.loadObject() ;
      a.signingCost = s.loadInt() ;
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(shiftType) ;
    s.saveObjects(workers) ;
    s.saveObjects(residents) ;
    
    s.saveInt(applications.size()) ;
    for (Application a : applications) {
      s.saveInt(a.position.ID) ;
      s.saveObject(a.applies) ;
      s.saveInt(a.signingCost) ;
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
      if (actor.isDoing(planClass)) num++ ;
    }
    return num ;
  }
  
  
  
  /**  Handling applications and recruitment-
    */
  public void applyFor(Vocation v, Actor applies, int signingCost) {
    final Application a = new Application() ;
    a.position = v ;
    a.applies = applies ;
    a.signingCost = signingCost ;
    applications.add(a) ;
  }
  
  
  public void removeApplicant(Actor applied) {
    for (Application a : applications) {
      if (a.applies == applied) { applications.remove(a) ; return ; }
    }
  }
  
  
  public int numApplicants(Vocation v) {
    int num = 0 ;
    for (Application a : applications) if (a.position == v) num++ ;
    return num ;
  }
  
  
  protected void confirmApplication(Application app) {
    applications.remove(app) ;
    venue.base().incCredits(0 - app.signingCost) ;
    app.applies.gear.incCredits(app.signingCost / 2) ;
    
    app.applies.AI.setEmployer(venue) ;
    if (! app.applies.inWorld()) {
      final Commerce commerce = venue.base().commerce ;
      commerce.cullCandidates(app.position, venue) ;
      commerce.addImmigrant(app.applies) ;
    }
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void updatePersonnel(int numUpdates) {
    if (numUpdates % 10 == 0) {
      final World world = venue.world() ;
      //
      //  Clear out the office for anyone dead-
      for (Actor a : workers) if (a.destroyed()) workers.remove(a) ;
      for (Actor a : residents) if (a.destroyed()) residents.remove(a) ;
      //
      //  If there's an unfilled opening, look for someone to fill it.
      //  TODO:  This should really be handled more from the Commerce class?
      if (venue.careers() == null) return ;
      for (Vocation v : venue.careers()) {
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
  }
  
  
  protected void onCommission() {
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.AI.setEmployer(null) ;
    for (Actor c : residents()) c.AI.setHomeVenue(null) ;
  }
  
  
  public void setWorker(Actor c, boolean is) {
    if (is) workers.include(c) ;
    else workers.remove(c) ;
  }
  
  
  public void setResident(Actor c, boolean is) {
    if (is) residents.include(c) ;
    else residents.remove(c) ;
  }
  
  
  public int numPositions(Vocation... match) {
    int num = 0 ; for (Actor c : workers) {
      for (Vocation v : match) if (c.vocation() == v) num++ ;
    }
    return num ;
  }
  
  
  public static void fillVacancies(Venue venue) {
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    if (venue.careers() == null) return ;
    for (Vocation v : venue.careers()) {
      final int numOpen = venue.numOpenings(v) ;
      if (numOpen <= 0) continue ;
      for (int i = numOpen ; i-- > 0 ;) {
        final Human worker = new Human(v, venue.base()) ;
        worker.AI.setEmployer(venue) ;
        final Tile e = venue.mainEntrance() ;
        if (GameSettings.hireFree) {
          worker.enterWorldAt(e.x, e.y, venue.world()) ;
        }
        else venue.base().commerce.addImmigrant(worker) ;
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


