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
  
  
  public boolean complete() {
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
    action = nextPlantAction() ;
    if (action != null) return action ;
    //
    //  Find the next unplanted, unattended crop-
    action = nextHarvestAction() ;
    if (action != null) return action ;
    //
    //  If there's nothing left to harvest, return what's collected-
    action = returnHarvestAction(0) ;
    if (action != null) return action ;
    return null ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Venue depot) {
    //  TODO:  Implement this?
    return true ;
  }
  
  
  private Action nextPlantAction() {
    //  TODO:  You'll also want to re-grab a planting area when you return to
    //  the depot?  Maybe.
    
    //  If the number of tiles planted is less than half the tiles originally
    //  grabbed, grab the planting area again.
    final int
      grabbed = nursery.onceGrabbed(),
      planted = nursery.planted.size(),
      toPlant = nursery.toPlant.size() ;
    if ((grabbed == 0) || (planted + toPlant) < (grabbed / 2)) {
      nursery.grabPlantArea() ;
    }
    final World world = actor.world() ;
    Tile nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    //
    //  Find the nearest unworked tile-
    for (Tile t : nursery.toPlant) {
      if (! nursery.canPlant(t)) { nursery.toPlant.remove(t) ; continue ; }
      if (world.activities.includes(t, Farming.class)) continue ;
      final float dist = Spacing.distance(t, actor) ;
      if (dist < minDist) { minDist = dist ; nearest = t ; }
    }
    if (nearest != null) {
      final Action plantAction = new Action(
        actor, nearest,
        this, "actionPlant",
        Action.REACH_DOWN, "Planting"
      ) ;
      plantAction.setMoveTarget(Spacing.nearestOpenTile(nearest, actor)) ;
      return plantAction ;
    }
    return null ;
  }
  
  
  public boolean actionPlant(Actor actor, Tile t) {
    //
    //  Check to ensure that placement here is possible.  If so, remove any
    //  previous occupant of the tile.
    if (! nursery.canPlant(t)) {
      nursery.toPlant.remove(t) ;
      return false ;
    }
    if (t.owner() != null) t.owner().exitWorld() ;
    //
    //  Get a species for the crop and plant it-
    final int varID = BotanicalStation.pickSpecies(t, nursery) ;
    final Crop crop = new Crop(nursery, varID) ;
    crop.enterWorldAt(t.x, t.y, actor.world()) ;
    crop.health = actor.traits.test(CULTIVATION, 10, 1) ? 2 : 1 ;
    crop.health += actor.traits.test(HARD_LABOUR, 5, 1) ? 1 : 0 ;
    nursery.toPlant.remove(t) ;
    nursery.planted.addLast(crop) ;
    return true ;
  }
  
  
  private Action nextHarvestAction() {
    Crop nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    final World world = actor.world() ;
    //
    //  Find the nearest mature crop-
    for (Crop c : nursery.planted) {
      if (! c.inWorld()) { nursery.planted.remove(c) ; continue ; }
      if (c.growStage < Crop.MAX_GROWTH) continue ;
      if (world.activities.includes(c, Farming.class)) continue ;
      final float dist = Spacing.distance(c, actor) ;
      if (dist < minDist) { minDist = dist ; nearest = c ; }
    }
    //
    //
    if (nearest != null) {
      final Action harvestAction = new Action(
        actor, nearest,
        this, "actionHarvest",
        Action.REACH_DOWN, "Harvesting"
      ) ;
      harvestAction.setMoveTarget(Spacing.nearestOpenTile(
        nearest.origin(), actor
      )) ;
      return harvestAction ;
    }
    return null ;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    if (! crop.inWorld()) return false ;
    final float yield = crop.health ;
    actor.gear.addItem(BotanicalStation.speciesYield(crop.varID), yield) ;
    crop.exitWorld() ;
    actionPlant(actor, crop.origin()) ;
    return true ;
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    if (
      actor.inventory().amountOf(STARCHES) <= amountNeeded &&
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
    actor.gear.transfer(STARCHES, depot) ;
    actor.gear.transfer(GREENS, depot) ;
    return true ;
  }
}






