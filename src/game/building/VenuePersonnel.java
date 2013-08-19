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
  
  
  protected void onCompletion() {
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
    else residents.include(c) ;
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


