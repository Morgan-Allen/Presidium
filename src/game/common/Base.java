/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.campaign.* ;
import src.graphics.common.* ;
import src.util.* ;



public class Base implements Session.Saveable, Schedule.Updates {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public World world ;
  final public Offworld offworld = new Offworld(this) ;
  Actor ruler ;
  
  final List <Actor> personnel = new List <Actor> () ;
  final Table <Object, Flagging> services = new Table <Object, Flagging> () ;
  final public Paving paving ;
  
  Texture fogMap ;  //Create a dedicated fogmap later.
  final public PathingCache pathingCache ;
  
  
  
  public Base(World world) {
    this.world = world ;
    pathingCache = new PathingCache(world) ;
    paving = new Paving(world) ;
    //paving = new Paving2(this) ;
    
    initFog() ;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.world = s.world() ;

    offworld.loadState(s) ;
    ruler = (Actor) s.loadObject() ;
    
    s.loadObjects(personnel) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Flagging f = (Flagging) s.loadObject() ;
      services.put(f.key, f) ;
    }
    paving = new Paving(world) ;
    //paving = new Paving2(this) ;
    //paving.loadState(s) ;
    
    initFog() ;
    pathingCache = new PathingCache(world) ;
    //pathingCache.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    offworld.saveState(s) ;
    s.saveObject(ruler) ;
    
    s.saveObjects(personnel) ;
    s.saveInt(services.size()) ;
    for (Flagging f : services.values()) s.saveObject(f) ;
    //paving.saveState(s) ;
    
    //pathingCache.saveState(s) ;
  }
  
  
  private void initFog() {
    final int size = world.size ;
    fogMap = Texture.createTexture(size, size) ;
    byte vals[] = new byte[size * size * 4] ;
    fogMap.putBytes(vals) ;
  }
  
  
  public Texture fogMap() {
    return fogMap ;
  }
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 10 ;
  }
  
  public void updateAsScheduled(int numUpdates) {
    offworld.updateEvents() ;
    //paving.distribute(VenueConstants.ALL_PROVISIONS) ;
  }
  
  
  /**  Locating goods and services on offer or pledgeMade-
    */
  public void toggleBelongs(Venue venue, boolean is) {
    toggleForService(venue, this, is) ;
    toggleForService(venue, venue.getClass(), is) ;
    final Object s[] = venue.services() ;
    if (s != null) for (Object service : s) {
      toggleForService(venue, service, is) ;
    }
  }
  
  
  public void toggleForService(Target target, Object service, boolean is) {
    ///I.say("Toggling "+target+" for service "+service+": "+is) ;
    Flagging flagged = services.get(service) ;
    if (flagged == null) {
      services.put(service, flagged = new Flagging(world, service)) ;
    }
    if (is) {
      flagged.toggleMember(target, true) ;
    }
    else {
      flagged.toggleMember(target, false) ;
      if (flagged.population() == 0) services.remove(service) ;
    }
  }
  
  
  public Iterable servicesNear(Object service, Target client, float range) {
    Flagging flagged = services.get(service) ;
    if (flagged == null) return new Stack() ;
    return flagged.visitNear(client, range, null) ;
  }
  
  public Iterable servicesNear(Object service, Target client, Box2D area) {
    Flagging flagged = services.get(service) ;
    if (flagged == null) return new Stack() ;
    return flagged.visitNear(client, -1, area) ;
  }
  
  public Venue nearestService(Object service, Target client, float range) {
    for (Object o : servicesNear(service, client, range)) return (Venue) o ;
    return null ;
  }
  
  public Venue randomServiceNear(Object service, Target client, float range) {
    Flagging flagged = services.get(service) ;
    if (flagged == null) return null ;
    return (Venue) flagged.pickRandomAround(client, range) ;
  }
}













