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
 Purchasing new equipment and home items.
//*/



//
//  TODO:  You need to have some generalised routines for getting actors
//  and venues for consideration.


public class CitizenAI extends ActorAI implements ActorConstants {
  
  
  
  /**  Constructor and save/load functions-
    */
  protected CitizenAI(Actor actor) {
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
  protected Behaviour createBehaviour() {
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
      addPurchases(choice) ;
    }
    
    final Behaviour chosen = choice.weightedPick(whimsy()) ;
    applyForMissions(chosen) ;
    return chosen ;
  }
  
  
  protected void updateAI(int numUpdates) {
    super.updateAI(numUpdates) ;
    if (numUpdates % 10 == 0) {
      if (this.work == null) {
        //  TODO:  Apply for a new position.
      }
      if (this.home == null && this.work instanceof Venue) {
        ///I.say("Home is: "+home) ;
        final Holding newHome = Holding.findHoldingFor(actor) ;
        ///I.say("NEW home is: "+home) ;
        if (newHome != null) {
          if (! newHome.inWorld()) {
            newHome.clearSurrounds() ;
            newHome.enterWorld() ;
            newHome.structure.setState(VenueStructure.STATE_INSTALL, 0.1f) ;
          }
          setHomeVenue(newHome) ;
        }
      }
    }
  }
  

  protected Behaviour reactionTo(Mobile seen) {
    return null ;
  }
  
  
  protected void applyForMissions(Behaviour chosen) {
    if (mission != null || actor.base() == null) return ;
    for (Mission mission : actor.base().allMissions()) {
      if (! mission.open()) continue ;
      if (couldSwitch(chosen, mission)) {
        mission.setApplicant(actor, true) ;
      }
      else mission.setApplicant(actor, false) ;
    }
  }
  
  
  //  TODO:  This method is poorly named.  Also, the list of stuff worth
  //  reacting to should be made accessible.
  protected void addReactions(Choice choice) {
    //
    //  Find all nearby items or actors and consider reacting to them.
    final PresenceMap mobiles = actor.world().presences.mapFor(Mobile.class) ;
    final int reactLimit = (int) (actor.traits.trueLevel(INSIGHT) / 2) ;
    final Batch <Actor> actorB = new Batch <Actor> () ;
    int numR = 0 ;
    for (Target t : mobiles.visitNear(actor, -1, null)) {
      if (t instanceof Actor) actorB.add((Actor) t) ;
      if (++numR > reactLimit) break ;
    }
    //
    //  Consider retreat based on ambient danger levels, defence & treatment of
    //  self or others, or dialogue.
    for (Actor near : actorB) {
      choice.add(new Combat(actor, near)) ;
      choice.add(new Treatment(actor, near)) ;
      choice.add(new Dialogue(actor, near, true)) ;
    }
    choice.add(new Retreat(actor)) ;
    choice.add(new SickLeave(actor)) ;
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
    //
    //  Consider repairing nearby buildings-
    choice.add(Building.getNextRepairFor(actor)) ;
	}
  
  
  protected void addLeisure(Choice choice) {
    //
    //  Try a range of other spontaneous behaviours, include relaxation,
    //  helping out and spontaneous missions-
    final Action wander = (Action) new Patrolling(actor, actor, 5).nextStep() ;
    wander.setPriority(Plan.IDLE) ;
    choice.add(wander) ;
    //
    //  Consider going home to rest, or finding a recreational facility of
    //  some kind.  That requires iterating over various venues.
    choice.add(new Resting(actor, Resting.pickRestPoint(actor))) ;
    //
    //  As hobbies, consider hunting, exploration, assistance, and dialogue,
    //  with one chosen target each.
    
  }
  
  
  private void addPurchases(Choice choice) {
    //
    //  If you already have something ordered, don't place a second order.
    if (rootBehaviour() instanceof Purchase) return ;
    for (Behaviour b : todoList) if (b instanceof Purchase) return ;
    //
    //  Otherwise, consider upgrading weapons or armour.
    final Service DT = actor.gear.deviceType() ;
    if (DT != null) {
      final int DQ = actor.gear.deviceEquipped().quality ;
      if (DQ < Item.MAX_QUALITY) {
        final Item nextDevice = Item.withQuality(DT, DQ + 1) ;
        final Venue shop = Purchase.findVenue(actor, nextDevice) ;
        if (shop != null) choice.add(new Purchase(actor, nextDevice, shop)) ;
      }
    }
    final Service OT = actor.gear.outfitType() ;
    if (OT != null) {
      final int OQ = actor.gear.outfitEquipped().quality ;
      if (OQ < Item.MAX_QUALITY) {
        final Item nextOutfit = Item.withQuality(OT, OQ + 1) ;
        final Venue shop = Purchase.findVenue(actor, nextOutfit) ;
        if (shop != null) choice.add(new Purchase(actor, nextOutfit, shop)) ;
      }
    }
    //
    //  Also, consider buying new items for your home, either individually or
    //  together at the stock exchange.
    if (home instanceof Holding) {
      final Item items[] = ((Holding) home).goodsNeeded().toArray(Item.class) ;
      final Venue shop = Delivery.findBestVenue(actor, items) ;
      if (shop != null) {
        choice.add(new Delivery(items, shop, home)) ;
      }
    }
  }
}







