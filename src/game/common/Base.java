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
//  As your settlement grows, however, a larger and larger portion of your
//  revenue is siphoned back to the homeworld as tribute.  Getting your
//  settlement into debt is an excellent way to lower your standing (get fired
//  and demoted to some smaller or more dangerous post.)

public class Base implements
  Session.Saveable, Schedule.Updates, Accountable
{
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public World world ;
  final public Commerce commerce = new Commerce(this) ;
  float credits = 0 ;
  
  Actor ruler ;
  Venue commandPost ;
  final List <Mission> missions = new List <Mission> () ;
  
  float communitySpirit, alertLevel, crimeLevel ;
  final Table <Accountable, Relation> baseRelations = new Table() ;
  
  final public Paving paving ;
  //  ...You should also have a PresenceMap just for maintenance purposes.
  
  final public DangerMap dangerMap ;
  final public IntelMap intelMap = new IntelMap(this) ;
  
  public Colour colour = Colour.BLUE ;
  
  
  
  public Base(World world) {
    this.world = world ;
    paving = new Paving(world) ;
    dangerMap = new DangerMap(world, this) ;
    intelMap.initFog(world) ;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.world = s.world() ;
    commerce.loadState(s) ;
    credits = s.loadFloat() ;

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
    
    dangerMap = new DangerMap(world, this) ;
    dangerMap.loadState(s) ;
    intelMap.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    commerce.saveState(s) ;
    s.saveFloat(credits) ;
    
    s.saveObject(ruler) ;
    s.saveObjects(missions) ;
    
    s.saveFloat(communitySpirit) ;
    s.saveFloat(alertLevel) ;
    s.saveFloat(crimeLevel) ;
    s.saveInt(baseRelations.size()) ;
    for (Relation r : baseRelations.values()) Relation.saveTo(s, r) ;
    
    paving.saveState(s) ;
    
    dangerMap.saveState(s) ;
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
    return (int) credits ;
  }
  
  
  public void incCredits(float inc) {
    credits += inc ;
  }
  
  
  public boolean hasCredits(float sum) {
    return credits >= sum ;
  }
  
  
  //  Summarise total supply/demand for all goods here.
  
  
  
  /**  Dealing with admin functions-
    */
  public void setRelation(Base base, float attitude) {
    baseRelations.put(base, new Relation(this, base, attitude, world)) ;
  }
  
  
  public float relationWith(Base base) {
    final Relation r = baseRelations.get(base) ;
    if (r == null) return 0 ;
    return r.value() ;
  }
  
  
  public Base base() { return this ; }
  
  
  public float communitySpirit() {
    return communitySpirit ;
  }
  
  
  public float crimeLevel() {
    return crimeLevel ;
  }
  
  
  public float alertLevel() {
    return alertLevel ;
  }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    //
    //  Iterate across all personnel to get a sense of citizen mood, and
    //  compute community spirit.
    commerce.updateCommerce(numUpdates) ;
    paving.distribute(BuildConstants.ALL_PROVISIONS) ;
    dangerMap.updateVals() ;
    for (Mission mission : missions) mission.updateMission() ;
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





















