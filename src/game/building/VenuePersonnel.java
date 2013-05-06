/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;



public class VenuePersonnel {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Venue venue ;
  final List <Citizen>
    workers   = new List <Citizen> (),
    residents = new List <Citizen> () ;
  
  
  VenuePersonnel(Venue venue) {
    this.venue = venue ;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(workers  ) ;
    s.loadObjects(residents) ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(workers  ) ;
    s.saveObjects(residents) ;
  }
  
  
  public List <Citizen> workers() {
    return workers ;
  }
  

  public List <Citizen> residents() {
    return residents ;
  }
  
  
  /**  Life cycle, recruitment and updates-
    */
  protected void onWorldEntry() {
    //  TODO:  This is a temporary measure.  Abolish later.
    //  Soon, you'll want an explicit process of screening applicants for a
    //  given position.
    for (Vocation v : venue.careers()) recruitWorker(v) ;
  }
  
  
  protected void onWorldExit() {
    for (Citizen c : workers()) c.removeEmployer(venue) ;
    for (Citizen c : residents()) c.setHomeVenue(null) ;
  }
  
  
  protected void setWorker(Citizen c, boolean is) {
    if (is) workers.include(c) ;
    else workers.remove(c) ;
  }
  
  protected void setResident(Citizen c, boolean is) {
    if (is) residents.include(c) ;
    else residents.include(c) ;
  }
  
  
  public void recruitWorker(Vocation v) {
    //
    //  You also need to determine the worker's home planet environment, full
    //  name, and maybe links to family or one or two past career events.
    final Citizen citizen = new Citizen(v, venue.base()) ;
    citizen.addEmployer(venue) ;
    //citizen.setWorkVenue(venue) ;
    
    venue.base.offworld.addImmigrant(citizen) ;
    
    //final Tile t = venue.entrances()[0] ;
    //citizen.enterWorldAt(t.x, t.y, venue.world()) ;
    //I.say("Recruited "+citizen+" at: "+t) ;
    //((BaseUI) PlayLoop.currentUI()).setSelection(citizen) ;
  }
}



