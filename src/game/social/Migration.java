


package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;
import src.game.actors.ActorMind.* ;



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
  
  private static boolean verbose = true ;
  
  
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
    //if (true) return -1 ;
    
    if (actor.mind.work() != null) return 0 ;
    if (initTime == -1) return ROUTINE ;
    final float timeSpent = actor.world().currentTime() + 10 - initTime ;
    float impetus = ROUTINE * timeSpent / World.STANDARD_DAY_LENGTH ;
    impetus *= ((1 - actor.health.moraleLevel()) + 1) / 2 ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  public boolean finished() {
    return actor != null && priorityFor(actor) <= 0 ;
  }
  
  
  public static Migration migrationFor(Actor actor) {
    if (actor.mind.hasToDo(Migration.class)) return null ;
    return new Migration(actor) ;
  }
  
  
  
  /**  Helper methods for finding other employment-
    */
  public static Application lookForJob(Human actor, Base base) {
    //
    //  Set up key comparison variables-
    final World world = base.world ;
    Application picked = null ;
    float bestRating = 0 ;
    Application oldApp = actor.mind.application() ;
    if (oldApp == null && actor.mind.work() != null) {
      oldApp = new Application(actor, actor.vocation(), actor.mind.work()) ;
    }
    //
    //  Sample the venues nearby that might offer a suitable job-
    final Batch <Venue> batch = new Batch <Venue> () ;
    if (oldApp != null) {
      world.presences.sampleFromKeys(
        actor, world, 2, batch, oldApp.position, actor.vocation(), base
      ) ;
      picked = oldApp ;
      bestRating = rateApplication(oldApp) * 1.5f ;
    }
    else world.presences.sampleFromKeys(
      actor, world, 2, batch, actor.vocation(), base
    ) ;
    //
    //  Assess the attractiveness of applying for jobs at each venue-
    for (Venue venue : batch) {
      final Background careers[] = venue.careers() ;
      if (careers == null) continue ;
      for (Background c : careers) if (venue.numOpenings(c) > 0) {
        final Application newApp = new Application(actor, c, venue) ;
        final float rating = rateApplication(newApp) ;
        if (rating > bestRating) {
          bestRating = rating ;
          picked = newApp ;
        }
      }
    }
    //
    //  If a new opening is more attractive, apply for the position-
    if (picked != null && picked != oldApp) {
      if (oldApp != null) oldApp.employer.setApplicant(oldApp, false) ;
      final int signingCost = signingCost(picked) ;
      picked.setHiringFee(signingCost) ;
      picked.employer.setApplicant(picked, true) ;
      actor.mind.setApplication(picked) ;
      
      if (verbose && I.talkAbout == actor) {
        I.say(actor+" is applying for new position: "+picked.position) ;
        I.say("Venue/signing cost: "+picked.employer+"/"+signingCost) ;
      }
      
      return picked ;
    }
    return null ;
  }
  
  
  private static float rateApplication(Application app) {
    
    final Actor a = app.applies ;
    if (! Career.qualifies(a, app.position)) return -1 ;
    
    float rating = 1 ;
    rating *= Career.ratePromotion(app.position, a) ;
    
    if (a.mind.home() != null) {
      rating /= 1 + Spacing.distance(a.mind.home(), app.employer) ;
    }
    
    return rating ;
  }
  
  
  public static int signingCost(Application app) {
    //
    //  TODO:  Signing cost is based on transport factors, attraction, no.
    //  already employed, etc.  Implement all of that.
    int transport = 0, incentive = 0, guildFees = 0 ;
    
    if (! app.applies.inWorld()) {
      //  ...This could potentially be much higher, depending on origin point.
      transport += 200 ;
    }
    guildFees += Background.HIRE_COSTS[app.position.standing] ;
    
    if (app.employer instanceof Venue) {
      final Venue venue = (Venue) app.employer ;
      int numEmployed = venue.personnel.numPositions(app.position) ;
      if (numEmployed == 0) {
        guildFees = 0 ;
        transport /= 2 ;
      }
      else guildFees *= 1 + ((numEmployed - 1) / 2f) ;
    }
    
    return guildFees + transport + incentive ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (actor.mind.work() != null) {
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
    actor.mind.setHomeVenue(null) ;
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
}














