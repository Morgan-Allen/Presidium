


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
    float
      chance = Visit.clamp(actor.traits.useLevel(ASSEMBLY) / 20, 0, 1),
      damage = (1 - built.structure.repairLevel()) * 2 ;
    if (built.structure.needsUpgrade() && damage < 1) damage = 1 ;
    float appeal = 1 ;
    appeal *= actor.AI.relation(built.base()) ;
    //  TODO:  Certain structures should be considered particularly important.
    //         Community Spirit should factor in here?
    
    return Visit.clamp(chance * damage, 0, 1) * ROUTINE * appeal ;
  }


  protected Behaviour getNextStep() {
    if (built.structure.needsRepair()) {
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
    if (built.structure.needsUpgrade()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      ) ;
      return upgrades ;
    }
    return null ;
  }
  
  
  public boolean actionBuild(Actor actor, Venue built) {
    //
    //  TODO:  Double the rate of repair again if you have tools and materials.
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    built.structure.repairBy(success * 5f / 2) ;
    return true ;
  }
  
  
  public boolean actionUpgrade(Actor actor, Venue built) {
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    built.structure.advanceUpgrade(success * 1f / 100) ;
    return true ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ") ;
    d.append(built) ;
  }
}








