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
  
  
  
  /**
    * Life cycle, recruitment and updates-
    */
  protected void onWorldEntry() {
    // TODO: This is a temporary measure. Abolish later.
    // Soon, you'll want an explicit process of screening applicants for a
    // given position.
    for (Vocation v : venue.careers()) recruitWorker(v) ;
  }
  
  
  protected void onWorldExit() {
    for (Actor c : workers()) c.psyche.setEmployer(null) ;
    for (Actor c : residents()) c.psyche.setHomeVenue(null) ;
  }
  
  
  public void setWorker(Actor c, boolean is) {
    if (is) workers.include(c) ;
    else workers.remove(c) ;
  }
  
  
  public void setResident(Actor c, boolean is) {
    if (is) residents.include(c) ;
    else residents.include(c) ;
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
    else venue.base.offworld.addImmigrant(citizen) ;
  }
  
  
  public int numPositions(Vocation... match) {
    int num = 0 ; for (Actor c : workers) {
      for (Vocation v : match) if (c.vocation() == v) num++ ;
    }
    return num ;
  }
}







