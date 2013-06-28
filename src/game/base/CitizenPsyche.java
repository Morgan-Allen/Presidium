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





