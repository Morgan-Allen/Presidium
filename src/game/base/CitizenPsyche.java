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
 Strike Team.
 Security Team.
 Recovery Team.
 Recon Team.
 Contact Team.
 Covert Team.
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


public class CitizenPsyche extends ActorPsyche {
  
  
  
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
    //final Human actor = this ;
    final Choice choice = new Choice(actor) ;
    //
    //  Find all nearby items or actors and consider reacting to them.
    final Tile o = actor.origin() ;
    final int sightRange = actor.health.sightRange() ;
    final Box2D bound = new Box2D().set(o.x, o.y, 0, 0) ;
    for (Tile t : actor.world().tilesIn(bound.expandBy(sightRange), true)) {
      if (Spacing.distance(t, o) > sightRange) continue ;
      for (Mobile m : t.inside()) addReactions(m, choice) ;
    }
    //
    //  Find the next jobs waiting for you at work or home.
    if (work != null) {
      Behaviour atWork = work.jobFor(actor) ;
      if (atWork != null) choice.add(atWork) ;
    }
    if (home != null) {
      Behaviour atHome = home.jobFor(actor) ;
      if (atHome != null) choice.add(atHome) ;
    }
    //
    //  If you've no other business to attend to, try and find any untaken job
    //  nearby and attend to it-
    
    //
    //  Try a range of other spontaneous behaviours, include relaxation and
    //  spontaneous missions-
    final Action wander = (Action) new Patrolling(actor, actor, 5).nextStep() ;
    wander.setPriority(Plan.IDLE) ;
    choice.add(wander) ;
    //
    //  Finally, return whatever activity seems most urgent or appealing.
    return choice.weightedPick() ;
  }
  
  //
  //  TODO:  Pass a Choice object here instead, and add all possibilities...
  protected void addReactions(Mobile m, Choice choice) {
    if (m instanceof Actor) {
      final Actor other = (Actor) m ;
      if (Dialogue.canTalk(other, actor)) {
        choice.add(new Dialogue(actor, other)) ;
      }
    }
  }
}





