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

//
//  TODO:  Implement shifts.  Actors should not be asked to work more than
//  eight hours per day, and in some cases, those shifts will need to be
//  alternated.

//  In addition, any openings available immediately after placement should be
//  filled automatically.  (But you need to give the scenario a chance to fill
//  those slots independantly, so maybe delay by *one* world-update...)


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
  
  
  VenuePersonnel(Venue venue) {
    this.venue = venue ;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(workers) ;
    s.loadObjects(residents) ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(workers) ;
    s.saveObjects(residents) ;
  }
  
  
  public List <Actor> workers() {
    return workers ;
  }
  
  
  public List <Actor> residents() {
    return residents ;
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
    //
    //  If there's an unfilled opening, look for someone to fill it.
    //  ...Include some random timing-factor here?
    if (numUpdates % 10 == 0) {
      final World world = venue.world() ;
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
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    for (Vocation v : venue.careers()) {
      final int numOpen = venue.numOpenings(v) ;
      if (numOpen <= 0) continue ;
      for (int i = numOpen ; i-- > 0 ;) {
        final Human worker = new Human(v, venue.base()) ;
        worker.AI.setEmployer(venue) ;
        venue.base().commerce.addImmigrant(worker) ;
      }
    }
  }
  
  
  //protected void onCompletion() {
  //}
  
  
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


