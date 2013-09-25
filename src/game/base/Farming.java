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
  
  
  final Plantation nursery ;
  
  
  Farming(Actor actor, Plantation plantation) {
    super(actor, plantation) ;
    this.nursery = plantation ;
  }
  
  
  public Farming(Session s) throws Exception {
    super(s) ;
    nursery = (Plantation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(nursery) ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    if ((! begun()) && nursery.belongs.personnel.assignedTo(this) > 0) {
      return 0 ;
    }
    //
    //  Vary priority based on competence for the task and how many crops
    //  actually need attention-
    final float need = nursery.needForTending() ;
    if (need <= 0) return 0 ;
    float chance = 1.0f ;
    chance *= actor.traits.chance(HARD_LABOUR, ROUTINE_DC) * 2 ;
    chance *= actor.traits.chance(CULTIVATION, MODERATE_DC) * 2 ;
    final float min = actor.gear.amountOf(GENE_SEED) > 0 ? ROUTINE : 0 ;
    return Visit.clamp(chance * (CASUAL + (need * ROUTINE)), min, URGENT) ;
  }
  
  
  private boolean canPlant() {
    if (! GameSettings.hardCore) return true ;
    return
      nursery.stocks.amountOf(GENE_SEED) > 0 ||
      actor.gear.amountOf(GENE_SEED) > 0 ;
  }
  
  
  private Item seedMatch(Crop crop) {
    Item seedMatch = null ; for (Item seed : actor.gear.matches(GENE_SEED)) {
      final Crop type = (Crop) seed.refers ;
      if (type.varID == crop.varID) { seedMatch = seed ; break ; }
    }
    return seedMatch ;
  }
  
  
  public Behaviour getNextStep() {
    ///I.sayAbout(actor, actor+" getting next farm action...") ;
    //
    //  If you've harvested enough, bring it back to the depot-
    Action action = returnHarvestAction(5) ;
    if (action != null) return action ;
    //
    //  If you're out of gene seed, and there's any in the nursery, pick up
    //  some more-
    if (
      actor.gear.amountOf(GENE_SEED) <= 0 &&
      nursery.stocks.amountOf(GENE_SEED) > 0
    ) {
      final Action pickup = new Action(
        actor, nursery,
        this, "actionCollectSeed",
        Action.REACH_DOWN, "Collecting seed"
      ) ;
      return pickup ;
    }
    //
    //  Find the next tile for seeding, tending or harvest.
    final boolean canPlant = canPlant() ;
    float minDist = Float.POSITIVE_INFINITY ;
    Crop picked = null ;
    for (Plantation p : nursery.strip) for (Crop c : p.planted) {
      if (c == null) continue ;
      if (c.growStage == Crop.NOT_PLANTED && ! canPlant) continue ;
      if (c.needsTending()) {
        final float dist = Spacing.distance(actor, c.tile) ;
        if (dist < minDist) { picked = c ; minDist = dist ; }
      }
    }
    if (picked != null) {
      final String actionName, anim ;
      if (picked.infested) {
        actionName = "actionDisinfest" ;
        anim = Action.REACH_DOWN ;
      }
      else if (picked.growStage >= Crop.MIN_HARVEST) {
        actionName = "actionHarvest" ;
        anim = Rand.yes() ? Action.REACH_DOWN : Action.BUILD ;
      }
      else {
        actionName = "actionPlant" ;
        anim = Action.BUILD ;
      }
      final Action plants = new Action(
        actor, picked,
        this, actionName,
        anim, "Planting"
      ) ;
      plants.setMoveTarget(Spacing.nearestOpenTile(picked.tile, actor)) ;
      return plants ;
    }
    //
    //  If there's nothing left to harvest, return what's collected-
    action = returnHarvestAction(0) ;
    if (action != null) return action ;
    //
    //  And if everything else is done, put back the seed stock-
    if (actor.gear.amountOf(GENE_SEED) > 0) {
      ///I.say("Gene seed is: "+actor.gear.amountOf(GENE_SEED)) ;
      action = new Action(
        actor, nursery,
        this, "actionReturnSeed",
        Action.REACH_DOWN, "Returning seed"
      ) ;
      return action ;
    }
    return null ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Plantation nursery) {
    for (Item seed : nursery.stocks.matches(GENE_SEED)) {
      actor.gear.addItem(Item.withAmount(seed, 1f)) ;
    }
    return true ;
  }
  
  
  public boolean actionPlant(Actor actor, Crop crop) {
    //
    //  Initial seed quality has a substantial impact on crop health.
    final Item seedMatch = seedMatch(crop) ;
    float plantDC = ROUTINE_DC ;
    if (seedMatch != null) {
      crop.health = 0.5f + (seedMatch.quality / 2f) ;
      actor.gear.removeItem(Item.withAmount(seedMatch, 0.1f)) ;
    }
    else {
      if (GameSettings.hardCore) return false ;
      plantDC += 5 ;
      crop.health = 0 ;
    }
    //
    //  So does expertise and elbow grease.
    crop.health += actor.traits.test(CULTIVATION, plantDC, 1) ? 1 : 0 ;
    crop.health += actor.traits.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0 ;
    crop.health = Visit.clamp(crop.health, 0, 5) ;
    crop.growStage = Crop.MIN_GROWTH ;
    crop.varID = Plantation.pickSpecies(crop.tile, nursery.belongs) ;
    //
    //  Update and return-
    crop.parent.refreshCropSprites() ;
    crop.parent.checkCropStates() ;
    return true ;
  }
  
  
  public boolean actionDisinfest(Actor actor, Crop crop) {
    final Item seedMatch = seedMatch(crop) ;
    int success = seedMatch != null ? 2 : 0 ;
    if (actor.traits.test(CULTIVATION, MODERATE_DC, 1)) success++ ;
    if (actor.traits.test(CHEMISTRY, ROUTINE_DC, 1)) success++ ;
    if (Rand.index(5) <= success) {
      crop.infested = false ;
      if (seedMatch != null) {
        actor.gear.removeItem(Item.withAmount(seedMatch, 0.1f)) ;
      }
    }
    return true ;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    final float yield = crop.health * crop.growStage / Crop.MIN_HARVEST ;
    actor.gear.bumpItem(Plantation.speciesYield(crop.varID), yield) ;
    actionPlant(actor, crop) ;
    return true ;
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    final float sumHarvest =
      actor.gear.amountOf(CARBS) +
      actor.gear.amountOf(GREENS) +
      actor.gear.amountOf(PROTEIN) ;
    if (sumHarvest <= amountNeeded && actor.health.encumbrance() < 1) {
      return null ;
    }
    final Action returnAction = new Action(
      actor, nursery.belongs,
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
  
  
  public boolean actionReturnSeed(Actor actor, Venue depot) {
    for (Item seed : actor.gear.matches(GENE_SEED)) {
      if (seed.refers != null) actor.gear.removeItem(seed) ;
    }
    depot.stocks.transfer(CARBS, actor) ;
    depot.stocks.transfer(GREENS, actor) ;
    depot.stocks.transfer(PROTEIN, actor) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Farming around ") ;
    d.append(nursery) ;
  }
}






