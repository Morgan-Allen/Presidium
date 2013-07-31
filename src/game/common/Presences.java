

package src.game.common ;
import src.game.building.* ;
import src.game.planet.* ;
import src.util.* ;



public class Presences {
	
	
	
  /**  fields, constructors and save/load functionality-
    */
	final World world ;
  final Table <Object, PresenceMap> allMaps ;
  final public PresenceMap floraMap ;
  final public PresenceMap mobilesMap ;
  
  final private PresenceMap nullMap ;
  final private Stack nullStack ;
  
  
  
  Presences(World world) {
  	this.world = world ;
  	allMaps = new Table <Object, PresenceMap> () ;
  	floraMap = new PresenceMap(world, Flora.class) ;
  	mobilesMap = new PresenceMap(world, Mobile.class) ;
  	allMaps.put(Flora.class , floraMap  ) ;
  	allMaps.put(Mobile.class, mobilesMap) ;
  	
  	nullMap = new PresenceMap(world, "nothing") {
		  public void toggleMember(Target t, Tile at, boolean is) {
		  	I.complain("Cannot modify null-presence map!") ;
		  }
  	} ;
  	nullStack = new Stack() ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final PresenceMap f = (PresenceMap) s.loadObject() ;
      allMaps.put(f.key, f) ;
    }
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveInt(allMaps.size()) ;
    for (PresenceMap f : allMaps.values()) s.saveObject(f) ;
  }
  
  
  

  /**  Modifying presences-
    */
  public void togglePresence(Target t, Tile at, boolean is, Object key) {
    PresenceMap map = allMaps.get(key) ;
    if (map == null) allMaps.put(key, map = new PresenceMap(world, key)) ;
    if (is) map.toggleMember(t, at, true) ;
    else map.toggleMember(t, at, false) ;
  }
  
  
  public void togglePresence(Flora f, boolean is) {
  	floraMap.toggleMember(f, f.origin(), is) ;
  }
  
  //
  //  TODO:  Include species/species-type registration.
  public void togglePresence(Mobile m, Tile at, boolean is) {
  	mobilesMap.toggleMember(m, at, is) ;
  }
  
  
  public void togglePresence(Venue venue, boolean is, Object services[]) {
  	final Tile origin = venue.origin() ;
    togglePresence(venue, origin, is, venue.base()) ;
    togglePresence(venue, origin, is, venue.getClass()) ;
    if (services != null) for (Object service : services) {
      togglePresence(venue, origin, is, service) ;
    }
  }
  
  
  
  /**  Querying presences-
    */
  public PresenceMap mapFor(Object key) {
  	  final PresenceMap map = allMaps.get(key) ;
  	  return map == null ? nullMap : map ;
  }
  
  
  public Iterable matchesNear(Object service, Target client, float range) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return nullStack ;
    return map.visitNear(client, range, null) ;
  }
  
  
  public Iterable matchesNear(Object service, Target client, Box2D area) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return nullStack ;
    return map.visitNear(client, -1, area) ;
  }
  
  
  public Venue nearestMatch(Object service, Target client, float range) {
    for (Object o : matchesNear(service, client, range)) return (Venue) o ;
    return null ;
  }
  
  
  public Venue randomMatchNear(Object service, Target client, float range) {
    final PresenceMap map = allMaps.get(service) ;
    if (map == null) return null ;
    return (Venue) map.pickRandomAround(client, range) ;
  }
}





