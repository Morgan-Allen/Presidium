/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.base.* ;
import src.game.campaign.* ;
import src.game.common.* ;
import src.game.planet.Planet ;
import src.game.social.* ;
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
  final static int
    REFRESH_INTERVAL = 10,
    AUDIT_INTERVAL   = World.STANDARD_DAY_LENGTH / 10 ;
  
  private static boolean verbose = false ;
  
  
  final Venue venue ;
  final List <Application>
    applications = new List <Application> () ;
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
    s.loadObjects(applications) ;
    s.loadObjects(workers) ;
    s.loadObjects(residents) ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(shiftType) ;
    s.saveObjects(applications) ;
    s.saveObjects(workers) ;
    s.saveObjects(residents) ;
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
  public int shiftFor(Actor worker) {
    if (shiftType == -1) return Venue.OFF_DUTY ;
    if (shiftType == Venue.SHIFTS_ALWAYS) {
      return Venue.PRIMARY_SHIFT ;
    }
    final World world = venue.world() ;
    
    //
    //  Simplified versions in use for the present...
    if (shiftType == Venue.SHIFTS_BY_HOURS) {
      final int day = (int) (world.currentTime() / World.STANDARD_DAY_LENGTH) ;
      final int index = (workers.indexOf(worker) + day) % 3 ;
      final int hour =
        Planet.isMorning(world) ? 1 :
        (Planet.isEvening(world) ? 2 : 0) ;
      
      if (index == hour) return Venue.PRIMARY_SHIFT ;
      else if (index == (hour + 1 % 3)) return Venue.SECONDARY_SHIFT ;
      else return Venue.OFF_DUTY ;
    }
    
    if (shiftType == Venue.SHIFTS_BY_DAY) {
      final int day = (int) (world.currentTime() / World.STANDARD_DAY_LENGTH) ;
      final int index = workers.indexOf(worker) ;
      
      if (Planet.isNight(world)) return Venue.OFF_DUTY ;
      else if ((index % 3) == (day % 3) || Planet.dayValue(world) < 0.5f) {
        return Venue.SECONDARY_SHIFT ;
      }
      else return Venue.PRIMARY_SHIFT ;
    }
    
    if (shiftType == Venue.SHIFTS_BY_CALENDAR) {
      I.complain("CALENDAR NOT IMPLEMENTED YET.") ;
    }
    
    return Venue.OFF_DUTY ;
  }
  
  public boolean onShift(Actor worker) {
    return shiftFor(worker) == Venue.PRIMARY_SHIFT ;
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
      for (Behaviour b : actor.mind.agenda()) if (b instanceof Plan) {
        if (((Plan) b).matchesPlan(matchPlan)) {
          count++ ;
        }
      }
    }
    return count ;
  }
  
  
  
  /**  Handling applications and recruitment-
    */
  public List <Application> applications() {
    return applications ;
  }
  
  
  public void setApplicant(Application app, boolean is) {
    if (is) applications.include(app) ;
    else applications.remove(app) ;
  }
  
  
  public void confirmApplication(Application a) {
    venue.base().incCredits(0 - a.hiringFee()) ;
    final Actor works = a.applies ;
    //
    //  TODO:  Once you have incentives worked out, restore this-
    //works.gear.incCredits(app.salary / 2) ;
    //works.gear.taxDone() ;
    works.setVocation(a.position) ;
    works.mind.setEmployer(venue) ;
    //
    //  If there are no remaining openings for this background, cull any
    //  existing applications.  Otherwise, refresh signing costs.
    for (Application oA : applications) if (oA.position == a.position) {
      if (venue.numOpenings(oA.position) == 0) {
        a.applies.mind.setApplication(null) ;
        applications.remove(oA) ;
      }
      else {
        oA.setHiringFee(FindWork.signingCost(oA)) ;
      }
    }
    //
    //  If the actor needs transport, arrange it-
    if (! works.inWorld()) {
      final Commerce commerce = venue.base().commerce ;
      commerce.addImmigrant(works) ;
    }
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void updatePersonnel(int numUpdates) {
    if (numUpdates % REFRESH_INTERVAL == 0) {
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
        final int numOpenings = venue.numOpenings(v) ;
        if (numOpenings > 0) {
          if (GameSettings.hireFree) {
            final Human citizen = new Human(v, venue.base()) ;
            citizen.mind.setEmployer(venue) ;
            final Tile t = venue.mainEntrance() ;
            citizen.enterWorldAt(t.x, t.y, world) ;
          }
          else {
            venue.base.commerce.incDemand(v, numOpenings, REFRESH_INTERVAL) ;
          }
        }
      }
    }
  }
  
  
  protected void onCommission() {
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.mind.setEmployer(null) ;
    for (Actor c : residents()) c.mind.setHomeVenue(null) ;
  }
  
  
  public void setWorker(Actor c, boolean is) {
    for (Application a : applications) if (a.applies == c) {
      applications.remove(a) ;
    }
    if (is) workers.include(c) ;
    else workers.remove(c) ;
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
        worker.mind.setEmployer(venue) ;
        
        if (GameSettings.hireFree) {
          final Tile e = venue.mainEntrance() ;
          worker.enterWorldAt(e.x, e.y, venue.world()) ;
        }
        else {
          venue.base.commerce.addImmigrant(worker) ;
        }
      }
    }
  }
}


