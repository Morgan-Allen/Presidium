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
  
  
  final BotanicalStation nursery ;
  
  
  Farming(Actor actor, BotanicalStation nursery) {
    super(actor, nursery) ;
    this.nursery = nursery ;
  }
  
  public Farming(Session s) throws Exception {
    super(s) ;
    nursery = (BotanicalStation) s.loadObject() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(nursery) ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public boolean finished() {
    return false ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Farming around ") ;
    d.append(nursery) ;
  }
  
  
  public Behaviour getNextStep() {
    //
    //  If you've harvested enough, bring it back to the depot-
    Action action = returnHarvestAction(10) ;
    if (action != null) return action ;
    //
    //  Find the next tile for planting.
    action = nextFarmAction(true) ;
    if (action != null) return action ;
    //
    //  Find the next unplanted, unattended crop-
    action = nextFarmAction(false) ;
    if (action != null) return action ;
    //
    //  If there's nothing left to harvest, return what's collected-
    action = returnHarvestAction(0) ;
    if (action != null) return action ;
    return null ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Venue depot) {
    //  TODO:  Implement this.
    return true ;
  }
  
  
  private Action nextFarmAction(boolean planting) {
    final World world = actor.world() ;
    Crop nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    //
    //  Find the nearest unworked tile-
    for (Plantation p : nursery.allotments) {
      if (p.type != Plantation.TYPE_BED) continue ;
      if (world.activities.includes(p, Farming.class)) continue ;
      for (Crop c : p.planted()) {
        if (planting && c.growStage != Crop.NOT_PLANTED) continue ;
        if (c.growStage < Crop.MAX_GROWTH && ! planting) continue ;
        final float dist = Spacing.distance(c, actor) ;
        if (dist < minDist) { minDist = dist ; nearest = c ; }
      }
    }
    if (nearest != null) {
      final Action plants = new Action(
        actor, nearest,
        this, planting ? "actionPlant" : "actionHarvest",
        Action.REACH_DOWN, "Planting"
      ) ;
      plants.setMoveTarget(Spacing.nearestOpenTile(nearest, actor, world)) ;
      return plants ;
    }
    return null ;
  }
  
  
  public boolean actionPlant(Actor actor, Crop crop) {
    //
    //  Check to ensure that placement here is possible.  If so, remove any
    //  previous occupant of the tile.
    //
    //  Get a species for the crop and plant it-
    //final int varID = BotanicalStation.pickSpecies(t, nursery) ;
    //final Crop crop = new Crop(nursery, varID) ;
    //crop.enterWorldAt(t.x, t.y, actor.world()) ;
    if (! crop.inWorld()) crop.enterWorld() ;
    crop.growStage = 0 ;
    crop.health = actor.traits.test(CULTIVATION, 10, 1) ? 1 : 0 ;
    crop.health += actor.traits.test(HARD_LABOUR, 5, 1) ? 1 : 0 ;
    crop.health += nursery.growBonus(crop.origin(), crop.varID) ;
    //nursery.toPlant.remove(t) ;
    //nursery.planted.addLast(crop) ;
    return true ;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    if (! crop.inWorld()) return false ;
    final float yield = crop.health ;
    actor.gear.bumpItem(BotanicalStation.speciesYield(crop.varID), yield) ;
    crop.exitWorld() ;
    actionPlant(actor, crop) ;
    return true ;
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    if (
      actor.inventory().amountOf(CARBS) <= amountNeeded &&
      actor.inventory().amountOf(GREENS  ) <= amountNeeded
    ) return null ;
    final Action returnAction = new Action(
      actor, nursery,
      this, "actionReturnHarvest",
      Action.REACH_DOWN, "Returning Harvest"
    ) ;
    return returnAction;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(CARBS, depot) ;
    actor.gear.transfer(GREENS, depot) ;
    return true ;
  }
}






