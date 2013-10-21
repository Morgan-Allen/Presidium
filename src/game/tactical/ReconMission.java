


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
  
  int areaSize ;
  Tile inRange[] ;
  boolean done = false ;
  
  
  public ReconMission(Base base, Tile subject) {
    super(
      base, subject,
      MissionsTab.RECON_MODEL.makeSprite(),
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    ) ;
    inRange = Exploring.grabExploreArea(
      base.intelMap, subject, World.SECTOR_SIZE / 2f
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
    float reward = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    float priority = Exploring.rateExplorePoint(actor, centre, reward) ;
    //I.sayAbout(actor, "Recon mission priority is: "+priority) ;
    //I.sayAbout(actor, "Reward priority is: "+reward+", amount: "+rewardAmount(actor)) ;
    return priority ;
  }
  
  
  int count = 0 ;

  public Behaviour nextStepFor(Actor actor) {
    ///I.say("Getting next step in recon, for "+actor) ;
    //
    //  TODO:  Refresh the list of tiles to explore every 10 seconds or so?
    final IntelMap map = base.intelMap ;
    Tile lookedAt = null ;
    float bestRating = 0 ;
    //float minFog = 1 ;
    
    
    for (Tile t : inRange) if (! t.blocked()) {
      final float fog = map.fogAt(t) ;
      float rating = fog < 1 ? 1 : 0 ;
      
      for (Role role : this.roles) if (role.applicant != actor) {
        Target looks = role.applicant.targetFor(Exploring.class) ;
        if (looks == null) looks = role.applicant ;
        rating *= (10 + Spacing.distance(actor, looks)) / 10f ;
      }
      if (rating > bestRating) {
        lookedAt = t ;
        bestRating = rating ;
      }
    }
    if (lookedAt == null) {
      done = true ;
      return null ;
    }
    ///I.say(actor+" assigned to look at "+lookedAt) ;
    
    final Exploring e = new Exploring(actor, base, lookedAt) ;
    e.priorityMod = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    return e ;
  }
  
  
  public boolean finished() {
    return done ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Recon Mission", this) ;
    final Tile tile = (Tile) subject ;
    d.append(" around "+tile) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    Selection.renderPlane(
      rendering, subject.position(null), World.SECTOR_SIZE / 2f,
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_SQUARE
    ) ;
  }
}




