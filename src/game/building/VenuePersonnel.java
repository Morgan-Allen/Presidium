/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;



public class VenuePersonnel {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Venue venue ;
  
  static class Opening {
    Vocation position ;
    Actor applies ;
    int salary ;
  }
  
  final List <Opening>
    applications = new List <Opening> () ;
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
  public void applyFor(Vocation v, Actor applies) {
    final Opening a = new Opening() ;
    a.position = v ;
    a.applies = applies ;
    //  TODO:  You might want to vary this a bit, based on the actor's
    //  personality and how much they like the settlement's reputation.
    a.salary = ((v.standing + 1) * 100) ;// + Rand.index(100) - 50 ;
  }
  
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void onCompletion() {
    //  Soon, you'll want an explicit process of screening applicants for a
    //  given position.
    if (GameSettings.hireFree) for (Vocation v : venue.careers()) {
      int num = venue.numOpenings(v) ;
      while (num-- > 0) recruitWorker(v) ;
    }
  }
  
  
  protected void onDecommission() {
    for (Actor c : workers()) c.psyche.setEmployer(null) ;
    for (Actor c : residents()) c.psyche.setHomeVenue(null) ;
  }
  
  
  public void recruitWorker(Vocation v) {
    //
    // You also need to determine the worker's home planet environment, full
    // name, and maybe links to family or one or two past career events.
    final Career career = new Career(v) ;
    final Human citizen = new Human(career, venue.base()) ;
    citizen.psyche.setEmployer(venue) ;
    if (GameSettings.hireFree) {
      final Tile t = venue.mainEntrance() ;
      citizen.enterWorldAt(t.x, t.y, venue.world()) ;
    }
    else I.complain("SORT OUT THE FREIGHTER.  CRIPES.") ;
    //else venue.base.offworld.addImmigrant(citizen) ;
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







