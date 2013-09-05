/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.wild ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.actors.* ;
import src.util.* ;



public class Quud extends Fauna {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  public Quud() {
    super(Species.QUUD) ;
  }
  
  
  public Quud(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(5, 2, 1) ;
    health.initStats(
      1,    //lifespan
      1,     //bulk bonus
      0.35f, //sight range
      0.15f  //speed rate
    ) ;
    gear.setDamage(2) ;
    gear.setArmour(15) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Behaviour implementations.
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (health.conscious() && numUpdates % 10 == 0) {
      float eaten = origin().habitat().moisture() / 100f ;
      health.takeSustenance(eaten, 1) ;
    }
    if (! amDoing("actionHunker")) gear.setArmour(15) ;
  }
  

  protected void addChoices(Choice choice) {
    final Behaviour defence = nextDefence(null) ;
    if (defence != null) {
      if (! amDoing("actionHunker")) choice.add(defence) ;
      return ;
    }
    super.addChoices(choice) ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    final float danger = Retreat.dangerAtSpot(origin(), this, AI.seen()) ;
    if (danger <= 0) return null ;
    final Action hunker = new Action(
      this, this,
      this, "actionHunker",
      Action.FALL, "Hunkering Down"
    ) ;
    hunker.setProperties(Action.QUICK) ;
    hunker.setPriority(Action.CRITICAL) ;
    return hunker ;
  }
  
  
  public boolean actionHunker(Quud actor, Quud doing) {
    if (actor != this || doing != this) I.complain("No outside access.") ;
    doing.gear.setArmour(25) ;
    return true ;
  }
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return 4.0f ; }
}






