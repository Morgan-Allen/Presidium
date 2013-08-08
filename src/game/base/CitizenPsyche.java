/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.util.* ;






//OUTLINE FOR DECISION-MAKING:
/*
*  Is there a pressing, life-threatening emergency?
 Running from an enemy, or raising an alarm.
 Treating/sheltering/defending someone injured or attacked.
 Getting food and sleep.

*  Have you been assigned or embarked on a mission?
 (Embarking on said missions, or accepting the rewards involved.)
 (Might be specified by player, or started spontaneously.)
 Strike Mission.
 Security Mission.
 Recovery Mission.
 Recon Mission.
 Contact Mission.
 Covert Mission.
 Accepting a promotion/ceremonial honours/license to marry.

*  Do you have work assigned by your employer?
 (Derived from home or work venues.)
 Seeding & Harvest.
 Excavation or Drilling.
 Hunting.
 Transport.
 Manufacture.
 Construction & Salvage.
 Patrolling/Enforcement.
 Treatment & Sick Leave.
 Entertainment.

*  Do you have spare time?
 (Procreation, Relaxation, Self-improvement, Helping out.)
 Relaxation/conversation/sex in public, at home, or at the Cantina.
 Matches/debates/spectation at the Arena or Senate Chamber.
 Learning new skills through apprenticeship or research at the Archives.
//*/



/*
public boolean monitor(Actor actor) {
//Get the next step regardless every 2 seconds or so, compare priority
//with the actor's current plan, and switch if the difference is big
//enough.
//TODO:  Consider checking after every discrete action?  That might be
//more granular.
return true ;
}
//*/


public class CitizenPsyche extends ActorPsyche implements ActorConstants {
  
  
  
  /**  Constructor and save/load functions-
    */
  protected CitizenPsyche(Actor actor) {
    super(actor) ;
  }
  
  protected void loadState(Session s) throws Exception {
    super.loadState(s) ;
  }
  
  protected void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  

  /**  Behaviour implementation-
    */
  public Behaviour nextBehaviour() {
    final Choice choice = new Choice(actor) ;
    addReactions(choice) ;
    
    if (mission != null && mission.active()) {
      choice.add(mission) ;
    }
    if (mission != null && mission.complete()) {
      I.say("Mission completed: "+mission.fullName()) ;
      mission = null ;
    }
    if (mission == null) {
  	  addWork(choice) ;
  	  addLeisure(choice) ;
    }
    
    final Behaviour chosen = choice.weightedPick() ;
    applyForMissions(chosen) ;
    return chosen ;
  }
  
  
  protected void applyForMissions(Behaviour chosen) {
    if (mission != null || actor.assignedBase() == null) return ;
    for (Mission mission : actor.assignedBase().allMissions()) {
      if (! mission.open()) continue ;
      if (couldSwitch(chosen, mission)) {
        mission.setApplicant(actor, true) ;
      }
      else mission.setApplicant(actor, false) ;
    }
  }
  
  
  protected void addReactions(Choice choice) {
    //
    //  Find all nearby items or actors and consider reacting to them.
    final PresenceMap
      mobiles = actor.world().presences.mapFor(Mobile.class),
      repairs = actor.world().presences.mapFor("damaged") ;
    final int reactLimit = (int) (actor.traits.trueLevel(INSIGHT) / 2) ;
    
    
    final Batch <Actor> actorB = new Batch <Actor> () ;
    int numR = 0 ;
    for (Target t : mobiles.visitNear(actor, -1, null)) {
      if (t instanceof Actor) actorB.add((Actor) t) ;
      if (++numR > reactLimit) break ;
    }
    //
    //  Consider retreat or surrender based on all nearby actors.
    //choice.add(new Retreat(actor, considered)) ;
    //
    //  Consider defence & treatment of self or others, or dialogue.
    for (Actor near : actorB) {
      choice.add(new Combat(actor, near)) ;
      choice.add(new Treatment(actor, near)) ;
      //choice.add(new Dialogue(actor, near)) ;
    }
    //
    //  As hobbies, consider hunting, exploration, assistance, and dialogue,
    //  with one chosen target each.
    //  ALSO, TRY REPAIRING NEARBY BUILDINGS
    
    final Batch <Venue> venueB = new Batch <Venue> () ;
    numR = 0 ;
    for (Target t : repairs.visitNear(actor, -1, null)) {
      if (t instanceof Venue) venueB.add((Venue) t) ;
      if (++numR > reactLimit) break ;
    }
    for (Venue near : venueB) {
      choice.add(new Building(actor, near)) ;
    }
  }
  
  
  protected void addWork(Choice choice) {
    //
    //  Find the next jobs waiting for you at work or home.
    //  TODO:  What about shifts?  Only certain venues need those, but they
    //  are important.
    if (work != null) {
      Behaviour atWork = work.jobFor(actor) ;
      if (atWork != null) choice.add(atWork) ;
    }
    if (home != null) {
      Behaviour atHome = home.jobFor(actor) ;
      if (atHome != null) choice.add(atHome) ;
    }
	}
  
  
  protected void addLeisure(Choice choice) {
    //
    //  Try a range of other spontaneous behaviours, include relaxation,
    //  helping out and spontaneous missions-
    final Action wander = (Action) new Patrolling(actor, actor, 5).nextStep() ;
    wander.setPriority(Plan.IDLE) ;
    choice.add(wander) ;
  }
}








