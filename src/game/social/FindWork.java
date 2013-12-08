


package src.game.social ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;



public class FindWork {
  
  
  private static boolean verbose = false ;
  
  
  
  //
  //  TODO:  Have this used by the Commerce class as well.  In fact, this could
  //  all stand to be 'gated' a little better.
  
  /**  Helper methods for finding other employment-
    */
  public static Application lookForWork(Human actor, Base base) {
    final Employment work = actor.mind.work() ;
    if (work instanceof Vehicle) return null ;
    //
    //  Set up key comparison variables-
    final World world = base.world ;
    Application picked = null ;
    float bestRating = 0 ;
    final Batch <Venue> batch = new Batch <Venue> () ;
    
    if (work != null) {
      picked = new Application(actor, actor.vocation(), work) ;
    }
    if (picked != null) {
      world.presences.sampleFromKeys(
        actor, world, 2, batch, picked.position, actor.vocation(), base
      ) ;
      bestRating = rateApplication(picked) * 1.5f ;
    }
    else {
      world.presences.sampleFromKeys(
        actor, world, 2, batch, actor.vocation(), base
      ) ;
    }
    
    //
    //  Assess the attractiveness of applying for jobs at each venue-
    for (Venue venue : batch) {
      final Background careers[] = venue.careers() ;
      if (careers == null) continue ;
      for (Background c : careers) if (venue.numOpenings(c) > 0) {
        final Application newApp = new Application(actor, c, venue) ;
        final float rating = rateApplication(newApp) ;
        
        if (verbose && I.talkAbout == actor) {
          I.say("Rating for "+c+" at "+venue+" is: "+rating) ;
        }
        
        if (rating > bestRating) {
          bestRating = rating ;
          picked = newApp ;
        }
      }
    }
    //
    //  If a new opening is more attractive, apply for the position-
    final Application oldApp = actor.mind.application() ;
    if (picked != null && ! picked.matches(oldApp)) {
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
    else {
      if (oldApp != null && work != oldApp.employer) {
        oldApp.employer.setApplicant(oldApp, true) ;
      }
      return null ;
    }
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
    guildFees += Background.HIRE_COSTS[app.position.standing] ;
    
    if (! app.applies.inWorld()) {
      //  ...This could potentially be much higher, depending on origin point.
      transport += 100 ;
    }
    else if (app.applies.mind.work() == null) {
      guildFees = 0 ;
    }
    
    if (app.employer instanceof Venue) {
      final Venue venue = (Venue) app.employer ;
      int numEmployed = venue.personnel.numPositions(app.position) ;
      if (numEmployed == 0) {
        guildFees = 0 ;
        transport /= 2 ;
      }
      else if (numEmployed == 1) {
        guildFees /= 2 ;
      }
      else guildFees *= 1 + ((numEmployed - 2) / 2f) ;
    }
    
    return guildFees + transport + incentive ;
  }
  
}



