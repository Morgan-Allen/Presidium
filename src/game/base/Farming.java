/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class Farming extends Plan implements BuildConstants {
  
  
  final Plantation plantation ;
  
  
  Farming(Actor actor, Plantation plantation) {
    super(actor, plantation) ;
    this.plantation = plantation ;
  }
  
  
  public Farming(Session s) throws Exception {
    super(s) ;
    plantation = (Plantation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(plantation) ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    final float need = plantation.needForFarming() ;
    if (need <= 0) return 0 ;
    if (Plan.competition(Farming.class, plantation, actor) > 0) return 0 ;
    //
    //  Vary priority based on how many crops actually need attention.
    float chance = 1.0f ;
    chance *= actor.traits.chance(HARD_LABOUR, ROUTINE_DC) * 2 ;
    chance *= actor.traits.chance(CULTIVATION, MODERATE_DC) * 2 ;
    return Visit.clamp(chance * (CASUAL + (need * ROUTINE)), 0, URGENT) ;
  }
  
  
  public Behaviour getNextStep() {
    ///I.sayAbout(actor, actor+" getting next farm action...") ;
    //
    //  If you've harvested enough, bring it back to the depot-
    Action action = returnHarvestAction(5) ;
    if (action != null) return action ;
    //
    //  Find the next tile for planting.
    action = nextFarmAction(true) ;
    if (action != null) return action ;
    //
    //  Find the next crop ready for harvest.
    action = nextFarmAction(false) ;
    if (action != null) return action ;
    //
    //  If there's nothing left to harvest, return what's collected-
    action = returnHarvestAction(0) ;
    if (action != null) return action ;
    return null ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Venue depot) {
    //  TODO:  Implement this.  From the nearest nursery, which must be
    //  refreshed using gene seed.
    return true ;
  }
  
  
  private Action nextFarmAction(boolean planting) {
    final Crop nearest = plantation.nextToFarm(planting, actor) ;
    if (nearest != null) {
      final Action plants = new Action(
        actor, nearest,
        this, planting ? "actionPlant" : "actionHarvest",
        planting ? Action.BUILD : Action.REACH_DOWN, "Planting"
      ) ;
      plants.setMoveTarget(Spacing.nearestOpenTile(nearest.tile, actor)) ;
      return plants ;
    }
    return null ;
  }
  
  
  public boolean actionPlant(Actor actor, Crop crop) {
    crop.growStage = Crop.MIN_GROWTH ;
    crop.varID = Plantation.pickSpecies(crop.tile, plantation.belongs) ;
    crop.health = actor.traits.test(CULTIVATION, 10, 1) ? 1 : 0 ;
    crop.health += actor.traits.test(HARD_LABOUR, 5, 1) ? 1 : 0 ;
    crop.health += plantation.belongs.growBonus(crop.tile, crop.varID, false) ;
    crop.parent.refreshCropSprites() ;
    return true ;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    final float yield = crop.health ;
    actor.gear.bumpItem(Plantation.speciesYield(crop.varID), yield) ;
    crop.growStage = Crop.MIN_GROWTH ;
    actionPlant(actor, crop) ;
    return true ;
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    //
    //  TODO:  Also return if the actor can't carry any more!
    final float sumHarvest =
      actor.gear.amountOf(CARBS) +
      actor.gear.amountOf(GREENS) +
      actor.gear.amountOf(PROTEIN) ;
    if (sumHarvest <= amountNeeded) return null ;
    final Action returnAction = new Action(
      actor, plantation.belongs,
      this, "actionReturnHarvest",
      Action.REACH_DOWN, "Returning Harvest"
    ) ;
    return returnAction ;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(CARBS, depot) ;
    actor.gear.transfer(GREENS, depot) ;
    actor.gear.transfer(PROTEIN, depot) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Farming around ") ;
    d.append(plantation) ;
  }
}






