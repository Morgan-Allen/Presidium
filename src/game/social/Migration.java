


package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;
import src.game.actors.ActorAI.* ;



//
//  Unemployed actors should try to leave the world.
//
//  TODO:  Generalise this into a class for seeking alternative employment,
//  including at different venues in the same world.




public class Migration extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final static int
    BOARD_PRICE = 100 ;
  
  float initTime = -1 ;
  Dropship ship ;
  
  
  public Migration(Actor actor) {
    super(actor) ;
  }
  
  
  public Migration(Session s) throws Exception {
    super(s) ;
    initTime = s.loadFloat() ;
    ship = (Dropship) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(initTime) ;
    s.saveObject(ship) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    //
    //  DISABLING FOR NOW  TODO:  Restore once job-applications are sorted out.
    if (true) return -1 ;
    
    if (actor.AI.work() != null) return 0 ;
    if (initTime == -1) return ROUTINE ;
    final float timeSpent = actor.world().currentTime() + 10 - initTime ;
    float impetus = ROUTINE * timeSpent / World.STANDARD_DAY_LENGTH ;
    impetus *= ((1 - actor.health.moraleLevel()) + 1) / 2 ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  public boolean finished() {
    return actor != null && priorityFor(actor) <= 0 ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (actor.AI.work() != null) {
      abortBehaviour() ;
      return null ;
    }
    if (actor.gear.credits() < BOARD_PRICE) return null ;
    if (initTime == -1) {
      final Action thinks = new Action(
        actor, actor.aboard(),
        this, "actionConsider",
        Action.TALK_LONG, "Thinking about migrating"
      ) ;
      return thinks ;
    }
    if (ship == null || ! ship.inWorld()) {
      final Visit <Dropship> picks = new Visit <Dropship> () {
        public float rate(Dropship t) {
          return 0 - Spacing.distance(actor, t) ;
        }
      } ;
      ship = picks.pickBest(actor.base().commerce.allVessels()) ;
    }
    if (ship == null || ! ship.inWorld()) return null ;
    final Action boards = new Action(
      actor, ship,
      this, "actionBoardVessel",
      Action.TALK, "Leaving on "+ship
    ) ;
    return boards ;
  }
  
  
  public boolean actionConsider(Actor actor, Target t) {
    if (initTime == -1) {
      initTime = actor.world().currentTime() ;
      I.sayAbout(actor, "Setting initial time "+this.hashCode()) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionBoardVessel(Actor actor, Dropship leaves) {
    actor.AI.setHomeVenue(null) ;
    if (actor.aboard() == leaves) return true ;
    final int price = BOARD_PRICE ;
    if (actor.gear.credits() < price) return false ;
    actor.gear.incCredits(0 - price) ;
    leaves.cargo.incCredits(price) ;
    actor.goAboard(leaves, actor.world()) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Migrating off-planet") ;
  }
  
  
  
  /**  Helper methods for finding suitable employment elsewhere-
    */
  static Batch <Venue> nearbyEmployers() {
    
    
    return null ;
  }
  
  
  
  public static float rateEmployer(Venue location, Background position) {
    
    
    return 0 ;
  }
}














