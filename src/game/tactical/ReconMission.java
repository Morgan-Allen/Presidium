


package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class ReconMission extends Mission {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  Tile inRange[] ;
  boolean done = false ;
  
  
  public ReconMission(Base base, Tile subject) {
    super(
      base, subject,
      MissionsTab.RECON_MODEL.makeSprite(),
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    ) ;
    inRange = Exploring.grabExploreArea(
      base.intelMap, subject, World.DEFAULT_SECTOR_SIZE / 2f
    ) ;
  }
  
  
  public ReconMission(Session s) throws Exception {
    super(s) ;
    inRange = (Tile[]) s.loadTargetArray(Tile.class) ;
    done = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTargetArray(inRange) ;
    s.saveBool(done) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    final Tile centre = (Tile) subject ;
    float reward = actor.AI.greedFor(rewardAmount()) * ROUTINE ;
    return Exploring.rateExplorePoint(actor, centre, reward) ;
  }


  public Behaviour nextStepFor(Actor actor) {
    //
    //  TODO:  Refresh the list of tiles to explore every 10 seconds or so?
    final IntelMap map = base.intelMap ;
    Tile lookedAt = null ;
    float minFog = 1 ;
    for (Tile t : inRange) {
      final float fog = map.fogAt(t) ;
      if (fog < minFog && ! t.blocked()) {
        lookedAt = t ;
        minFog = fog ;
      }
    }
    if (lookedAt == null) {
      done = true ;
      return null ;
    }
    return new Exploring(actor, base, lookedAt) ;
  }
  
  
  public boolean complete() {
    return done ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Recon Mission", this) ;
    final Tile tile = (Tile) subject ;
    d.append(" around "+tile.habitat().name+" at "+tile.x+" "+tile.y) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    Selection.renderPlane(
      rendering, subject.position(null), World.DEFAULT_SECTOR_SIZE / 2f,
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_SQUARE
    ) ;
  }
}




