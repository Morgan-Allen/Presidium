


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;





public class Building extends Plan implements ActorConstants {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final Venue built ;
  
  
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
  
  
  
  /**  Assessing targets and priority-
    */
  public float priorityFor(Actor actor) {
    //
    //  TODO:  Don't factor competition if you're already at the site.
    if (built.base().credits() <= 0) return 0 ;
    float competition = Plan.competition(Building.class, built, actor) ;
    competition /= 1 + (built.structure.maxIntegrity() / 100f) ;
    
    float priority = Visit.clamp(actor.traits.useLevel(ASSEMBLY) / 10, 0, 1) ;
    float needRepair = (1 - built.structure.repairLevel()) * 1.33f ;
    if (! built.structure.intact()) needRepair = 1.0f ;
    if (built.structure.needsUpgrade()) needRepair += 0.5f ;
    
    //
    //  
    if (needRepair > 0.5f) {
      //
      //  If damage is higher than 50%, priority converges to max, regardless
      //  of competency.
      if (needRepair > 1) needRepair = 1 ;
      final float scale = (1 - needRepair) * 2 ;
      priority += (1 - priority) * scale ;
    }
    //  TODO:  You also need to modify by community spirit.  Use competency as
    //         your baseline then.
    
    priority *= actor.AI.relation(built.base()) ;
    return (needRepair * priority * URGENT) + priorityMod - competition ;
  }
  
  
  public static Building getNextRepairFor(Actor client) {
    final World world = client.world() ;
    final PresenceMap repairs = world.presences.mapFor("damaged") ;
    final Choice choice = new Choice(client) ;
    
    final int reactLimit = 10 ;
    int numR = 0 ;
    
    for (Target t : repairs.visitNear(client, -1, null)) {
      final Venue near = (Venue) t ;
      choice.add(new Building(client, near)) ;
      if (++numR > reactLimit) break ;
    }
    return (Building) choice.weightedPick(client.AI.whimsy()) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished() || built.base().credits() <= 0) return true ;
    if (built.structure.hasWear()) return false ;
    if (built.structure.needsUpgrade()) return false ;
    return true ;
  }
  
  
  protected Behaviour getNextStep() {
    if (built.base().credits() <= 0) return null ;
    if (built.structure.needsUpgrade() && built.structure.goodCondition()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      ) ;
      return upgrades ;
    }
    if (built.structure.hasWear()) {
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
    return null ;
  }
  
  
  public boolean actionBuild(Actor actor, Venue built) {
    //
    //  Double the rate of repair again if you have proper tools and materials.
    final boolean salvage = built.structure.needsSalvage() ;
    int success = 1 ;
    //
    //  TODO:  Don't deduct credits during the install phase.  In principle,
    //  that's already accounted for.
    if (salvage) {
      success *= actor.traits.test(ASSEMBLY, 5, 1) ? 2 : 1 ;
      final float amount = built.structure.repairBy(success * 5f / -2) ;
      final float cost = amount * built.structure.buildCost() ;
      built.base().incCredits(cost * 0.5f) ;
    }
    else {
      success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
      success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
      final boolean intact = built.structure.intact() ;
      final float amount = built.structure.repairBy(success * 5f / 2) ;
      final float cost = amount * built.structure.buildCost() ;
      built.base().incCredits((0 - cost) * (intact ? 0.5f : 1)) ;
    }
    return true ;
  }
  
  
  public boolean actionUpgrade(Actor actor, Venue built) {
    final Upgrade upgrade = built.structure.upgradeInProgress() ;
    ///if (upgrade == null) I.say("NO UPGRADE!") ;
    if (upgrade == null) return false ;
    ///I.say("Advancing upgrade: "+upgrade.name) ;
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    final float amount = built.structure.advanceUpgrade(success * 1f / 100) ;
    final float cost = amount * upgrade.buildCost ;
    built.base().incCredits((0 - cost)) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ") ;
    d.append(built) ;
  }
}








