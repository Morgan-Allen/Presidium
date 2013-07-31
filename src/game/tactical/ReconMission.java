

package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.Terrain;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;



public class ReconMission extends Mission {
  
  
  public ReconMission(Base base, Tile subject) {
    super(
      base, subject,
      MissionsTab.RECON_MODEL.makeSprite(),
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    ) ;
  }
  
  
  public ReconMission(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return actor.psyche.greedFor(rewardAmount()) * ROUTINE ;
  }


  public Behaviour nextStepFor(Actor actor) {
    final Vec3D pos = subject.position(null) ;
    Box2D area = new Box2D().set(pos.x, pos.y, 0, 0) ;
    area.expandBy(Terrain.SECTOR_SIZE / 2) ;
    return new Exploring(actor, area) ;
  }
  
  
  public boolean complete() {
    return false ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(description) ;
  }
}








