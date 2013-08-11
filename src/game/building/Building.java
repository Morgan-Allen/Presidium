


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;





public class Building extends Plan implements ActorConstants {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final Venue built ;
  
  
  //
  //  TODO:  Eventually, this should work off arbitrary Installations, and
  //  perhaps even road tiles.
  public Building(Actor actor, Venue repaired) {
    super(actor, repaired) ;
    this.built = repaired ;
  }
  
  
  public Building(Session s) throws Exception {
    super(s) ;
    built = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(built) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    final float
      chance = Visit.clamp(actor.traits.useLevel(ASSEMBLY) / 20, 0, 1),
      damage = (1 - built.structure.repairLevel()) * 2 ;
    float appeal = 1 ;
    
    //  TODO:  Certain structures should be considered particularly important.
    //         Community Spirit should factor in here?
    //
    //  Repairing non-base (or enemy) structures should be less important.
    //  ...That's an aspect of your general 'relationship' function.
    if (built.base != actor.assignedBase()) appeal /= 2 ;
    
    return Visit.clamp(chance * damage, 0, 1) * ROUTINE * appeal ;
  }
  
  
  //  Actors need to check for a behaviour's completion more frequently than
  //  just when an action completes.
  /*
  public boolean complete() {
    if (super.complete()) return true ;
    return built.structure.repairLevel() >= 1 ;
  }
  //*/


  protected Behaviour getNextStep() {
    if (built.structure.repairLevel() >= 1) return null ;
    final Action building = new Action(
      actor, built,
      this, "actionBuild",
      Action.BUILD, "Assembling "+built
    ) ;
    if (! Spacing.adjacent(actor.origin(), built) || Rand.num() < 0.2f) {
      final Tile t = Spacing.pickFreeTileAround(built, actor) ;
      building.setMoveTarget(t) ;
    }
    else building.setMoveTarget(actor.origin()) ;
    return building ;
  }
  
  
  public boolean actionBuild(Actor actor, Venue built) {
    //
    //  TODO:  Double the rate of repair again if you have tools and materials.
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    built.structure.repairBy(success * 5 / 2f) ;
    return true ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ") ;
    d.append(built) ;
  }
}






