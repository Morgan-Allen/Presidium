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
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;
///import src.game.common.WorldSections.Section ;



public class Base implements Session.Saveable, Schedule.Updates {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public World world ;
  final public Offworld offworld = new Offworld(this) ;  //Move to world.
  
  Actor ruler ;
  Venue commandPost ;
  final List <Mission> missions = new List <Mission> () ;
  float communitySpirit, alertLevel, crimeLevel ;
  
  final public Paving paving ;
  //  TODO:  also include a map for repairs?
  final public IntelMap intelMap = new IntelMap(this) ;
  
  
  
  public Base(World world) {
    this.world = world ;
    paving = new Paving(world) ;
    intelMap.initFog(world) ;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.world = s.world() ;

    offworld.loadState(s) ;
    ruler = (Actor) s.loadObject() ;
    s.loadObjects(missions) ;
    
    communitySpirit = s.loadFloat() ;
    alertLevel = s.loadFloat() ;
    crimeLevel = s.loadFloat() ;
    
    paving = new Paving(world) ;
    paving.loadState(s) ;
    intelMap.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    
    offworld.saveState(s) ;
    s.saveObject(ruler) ;
    s.saveObjects(missions) ;
    
    s.saveFloat(communitySpirit) ;
    s.saveFloat(alertLevel) ;
    s.saveFloat(crimeLevel) ;
    
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
  
  
  
  /**  Dealing with admin functions-
    */
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    offworld.updateEvents() ;
    for (Mission mission : missions) {
      mission.updateMission() ;
    }
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





















