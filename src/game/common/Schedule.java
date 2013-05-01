/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.util.* ;
import src.util.Sorting.* ;



/**  A Schedule is used to sort events in order of timing, and dispatch them as
  *  they occur.  The schedule using the last recorded current time from
  *  schedule advancement for calculating the proper time for new events.
  *  Consequently, the shedule should be advanced at regular intervals.
  */
public class Schedule {
  
  
  final static int MAX_UPDATE_INTERVAL = 5 ;


  public static interface Updates extends Session.Saveable {
    float scheduledInterval() ;
    void updateAsScheduled(int numUpdates) ;
  }
  
  private static class Event {
    private float time ;
    private int numUpdates ;
    private Updates updates ;
    
    public String toString() {
      return updates+" Update for: "+time ;
    }
  }
  
  final Sorting <Event> events = new Sorting <Event> () {
    public int compare(Event a, Event b) {
      if (a.updates == b.updates) return 0 ;
      return a.time > b.time ? 1 : -1 ;
    }
  } ;
  
  final Table <Updates, Object>
    allUpdates = new Table <Updates, Object> (1000) ;
  
  private float currentTime = 0 ;
  
  
  protected void saveTo(Session s) throws Exception {
    s.saveFloat(currentTime) ;
    s.saveInt(allUpdates.size()) ;
    for (Object node : allUpdates.values()) {
      final Event event = events.refValue(node) ;
      s.saveFloat(event.time) ;
      s.saveInt(event.numUpdates) ;
      s.saveObject(event.updates) ;
    }
  }
  
  protected void loadFrom(Session s) throws Exception {
    currentTime = s.loadFloat() ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Event event = new Event() ;
      event.time = s.loadFloat() ;
      event.numUpdates = s.loadInt() ;
      event.updates = (Updates) s.loadObject() ;
      allUpdates.put(event.updates, events.insert(event)) ;
    }
  }
  
  
  /**  Registers the given Updates object to be called upon at regular intervals.
    */
  public void scheduleForUpdates(Updates updates) {
    if (allUpdates.get(updates) != null)
      I.complain(updates+" ALREADY REGISTERED FOR UPDATES!") ;
    final Event event = new Event() ;
    event.time = currentTime + Rand.num() * updates.scheduledInterval() ;
    //I.say("Registering for updates: "+updates) ;
    ///I.say("Event time is: "+event.time) ;
    event.updates = updates ;
    allUpdates.put(updates, events.insert(event)) ;
  }
  
  /**  Unregisters the given Updates object from the schedule.
    */
  public void unschedule(Updates updates) {
    final Object node = allUpdates.get(updates) ;
    //I.say("...Unscheduling "+updates+" node okay? "+(node != null)) ;
    if (node == null) return ;
    events.deleteRef(node) ;
    allUpdates.remove(updates) ;
  }
  
  
  private long initTime = -1 ;
  
  /**  Returns whether the schedule has reached it's CPU quota for this update
    *  interval.
    */
  public boolean timeUp() {
    return (System.nanoTime() - initTime) > (MAX_UPDATE_INTERVAL * 1000000) ;
  }
  
  /**  Advances the schedule of events in accordance with the current time in
    *  the host world.
    */
  void advanceSchedule(final float currentTime) {
    this.currentTime = currentTime ;
    //  Find the current time, and descend to all events left of that dividing
    //  line (i.e, earlier.)
    //final Batch <Event> happened = new Batch <Event> () ;
    initTime = System.nanoTime() ;
    while (true) {
      if (timeUp()) break ;
      final Object leastRef = events.leastRef() ;
      if (leastRef == null) break ;
      final Event event = events.refValue(leastRef) ;
      if (event.time > currentTime) break ;
      events.deleteRef(leastRef) ;
      event.time += event.updates.scheduledInterval() ;
      allUpdates.put(event.updates, events.insert(event)) ;
      event.updates.updateAsScheduled(event.numUpdates++) ;
    }
  }
}




