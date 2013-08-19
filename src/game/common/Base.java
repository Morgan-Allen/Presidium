/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.campaign.* ;
import src.game.tactical.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Try implementing Accountable.  Also, bases need to have official
//  relations with other bases.
public class Base implements
  Session.Saveable, Schedule.Updates, Accountable
{
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public World world ;
  final public Commerce commerce = new Commerce(this) ;
  int credits = 0 ;
  
  Actor ruler ;
  Venue commandPost ;
  final List <Mission> missions = new List <Mission> () ;
  
  float communitySpirit, alertLevel, crimeLevel ;
  final Table <Accountable, Relation> baseRelations = new Table() ;
  
  final public Paving paving ;
  //  ...You should also have a PresenceMap just for construction.
  final public IntelMap intelMap = new IntelMap(this) ;
  
  
  
  public Base(World world) {
    this.world = world ;
    paving = new Paving(world) ;
    intelMap.initFog(world) ;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.world = s.world() ;
    commerce.loadState(s) ;
    credits = s.loadInt() ;

    ruler = (Actor) s.loadObject() ;
    s.loadObjects(missions) ;
    
    communitySpirit = s.loadFloat() ;
    alertLevel = s.loadFloat() ;
    crimeLevel = s.loadFloat() ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Relation r = Relation.loadFrom(s) ;
      baseRelations.put(r.subject, r) ;
    }
    paving = new Paving(world) ;
    paving.loadState(s) ;
    intelMap.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    commerce.saveState(s) ;
    s.saveInt(credits) ;
    
    s.saveObject(ruler) ;
    s.saveObjects(missions) ;
    
    s.saveFloat(communitySpirit) ;
    s.saveFloat(alertLevel) ;
    s.saveFloat(crimeLevel) ;
    
    s.saveInt(baseRelations.size()) ;
    for (Relation r : baseRelations.values()) Relation.saveTo(s, r) ;
    paving.saveState(s) ;
    intelMap.saveState(s) ;
  }
  
  
  
  /**  Dealing with missions amd personnel-
    */
  public List <Mission> allMissions() {
    return missions ;
  }
  
  
  public void addMission(Mission t) {
    missions.include(t) ;
  }
  
  
  public void removeMission(Mission t) {
    missions.remove(t) ;
  }
  
  
  
  /**  Dealing with finances, trade and taxation-
    */
  public int credits() {
    return credits ;
  }
  
  
  public void incCredits(int inc) {
    credits += inc ;
  }
  
  
  public boolean hasCredits(int sum) {
    return credits >= sum ;
  }
  
  
  //  Summarise total supply/demand for all goods here.
  
  
  
  /**  Dealing with admin functions-
    */
  public void setRelation(Base base, float attitude) {
    baseRelations.put(base, new Relation(this, base, attitude)) ;
  }
  
  
  public float relationWith(Base base) {
    final Relation r = baseRelations.get(base) ;
    if (r == null) return 0 ;
    return r.value() ;
  }
  
  
  public Base base() { return this ; }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    commerce.updateCommerce() ;
    for (Mission mission : missions) mission.updateMission() ;
    //paving.distribute(VenueConstants.ALL_PROVISIONS) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering) {
    final Viewport port = rendering.port ;
    
    for (Mission mission : missions) {
      final Sprite flag = mission.flagSprite() ;
      if (! port.intersects(flag.position, 2)) continue ;
      rendering.addClient(flag) ;
    }
  }
  
  
  public Mission pickedMission(BaseUI UI, Viewport port) {
    Mission closest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Mission mission : missions) {
      final Sprite flag = mission.flagSprite() ;
      float dist = port.isoToScreen(new Vec3D().setTo(flag.position)).z ;
      if (port.mouseIntersects(flag.position, 0.5f, UI)) {
        if (dist < minDist) { minDist = dist ; closest = mission ; }
      }
    }
    return closest ;
  }
}





















