/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.planet ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class Forestry extends Plan implements BuildConstants {
  
  
  final static int
    STAGE_INIT     = -1,
    STAGE_GET_SEED =  0,  //  TODO:  Implement this.
    STAGE_PLANTING =  1,
    STAGE_CUTTING  =  2,
    STAGE_RETURN   =  3,
    STAGE_DONE     =  4 ;
  
  final BotanicalStation nursery ;
  
  private int stage = STAGE_INIT ;
  private Tile toPlant = null ;
  private Flora toCut = null ;
  
  
  public Forestry(Actor actor, BotanicalStation nursery) {
    super(actor, nursery) ;
    this.nursery = nursery ;
  }
  
  
  public Forestry(Session s) throws Exception {
    super(s) ;
    this.nursery = (BotanicalStation) s.loadObject() ;
    this.stage = s.loadInt() ;
    toPlant = (Tile) s.loadTarget() ;
    toCut = (Flora) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(nursery) ;
    s.saveInt(stage) ;
    s.saveTarget(toPlant) ;
    s.saveObject(toCut) ;
  }

  
  
  /**  Behaviour implementation-
    */
  private boolean configured() {
    if (stage != STAGE_INIT) return true ;
    final float abundance = actor.world().ecology().globalFertility() ;
    ///I.sayAbout(actor, "ABUNDANCE IS: "+abundance) ;
    if (Rand.num() < abundance) {
      //
      //  TODO:  Collect seeds first.
      stage = STAGE_PLANTING ;
      toPlant = findPlantTile(actor) ;
      if (toPlant == null) { abortBehaviour() ; return false ; }
    }
    else {
      stage = STAGE_CUTTING ;
      toCut = findCutting(actor) ;
      if (toCut == null) { abortBehaviour() ; return false ; }
    }
    return true ;
  }
  
  
  public float priorityFor(Actor actor) {
    if (! configured()) return 0 ;
    float impetus = CASUAL ;
    impetus += actor.traits.traitLevel(NATURALIST) * 1.5f ;
    impetus -= actor.traits.traitLevel(INDOLENT) ;
    impetus += actor.traits.traitLevel(OPTIMISTIC) / 2 ;
    //
    //  TODO:  Include danger values as well.
    if (toPlant != null) {
      impetus -= Plan.rangePenalty(toPlant, actor) / 2f ;
    }
    if (toCut != null) {
      impetus -= Plan.rangePenalty(toCut, actor) / 2f ;
    }
    return Visit.clamp(impetus, IDLE, ROUTINE) ;
  }
  
  
  public Behaviour getNextStep() {
    if (! configured()) return null ;
    ///I.sayAbout(actor, "Getting next forestry action... "+stage) ;
    if (stage == STAGE_GET_SEED) {
      
    }
    if (stage == STAGE_PLANTING) {
      final Action plants = new Action(
        actor, toPlant,
        this, "actionPlant",
        Action.BUILD, "Planting"
      ) ;
      return plants ;
    }
    if (stage == STAGE_CUTTING) {
      final Action cuts = new Action(
        actor, toCut,
        this, "actionCutting",
        Action.BUILD, "Cutting"
      ) ;
      cuts.setMoveTarget(Spacing.nearestOpenTile(toCut.origin(), actor)) ;
      return cuts ;
    }
    if (stage == STAGE_RETURN) {
      final Action returns = new Action(
        actor, nursery,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning Harvest"
      ) ;
      return returns ;
    }
    return null ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Venue depot) {
    //  TODO:  Implement this.  From the nearest nursery, which must be
    //  refreshed using gene seed.
    return true ;
  }
  
  
  public boolean actionPlant(Actor actor, Tile t) {
    //
    //  TODO:  Include skill checks here to ensure health of the seedling.
    final Flora f = new Flora(t.habitat()) ;
    f.setPosition(t.x, t.y, t.world) ;
    if (! f.canPlace()) {
      abortBehaviour() ;
      return false ;
    }
    f.enterWorld() ;
    f.incGrowth(1 + Rand.num(), t.world, false) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionCutting(Actor actor, Flora cut) {
    final Item lumber = Item.withType(SAMPLES, cut) ;
    actor.gear.addItem(lumber) ;
    //
    //  TODO:  Include skill checks here.  Optionally, you might only harvest
    //  a portion of the plant?  Knock back growth, but not kill it?
    //
    //  TODO:  INCLUDE A BARGE HERE...
    cut.setAsDestroyed() ;
    stage = STAGE_RETURN ;
    return true ;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    for (Item item : actor.gear.matches(SAMPLES)) {
      final Flora cut = (Flora) item.refers ;
      int stage = cut.growStage() ;
      
      depot.stocks.bumpItem(PETROCARBS, stage * 2) ;
      depot.stocks.bumpItem(GREENS, 1 + Rand.num()) ;
      
      //
      //  TODO:  Use handicrafts skill here?
      if (Rand.index(100) == 0) {
        final Item rareWood = Item.withType(TROPHIES, cut) ;
        depot.stocks.addItem(rareWood) ;
      }
      actor.gear.removeItem(item) ;
    }
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Performing Forestry") ;
  }
  
  

  
  /**  Utility methods for finding suitable plant/harvest targets-
    */
  private Tile findPlantTile(Actor actor) {
    Tile init = Spacing.pickRandomTile(
      nursery, World.DEFAULT_SECTOR_SIZE * 2, actor.world()
    ) ;
    init = Spacing.nearestOpenTile(toPlant, actor) ;
    if (init == null) return null ;
    //
    //  TODO:  You should try rating the attractiveness of different areas,
    //  based on low danger, high fertility and lack of existing flora.
    
    final Flora f = new Flora(init.habitat()) ;
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > 4) return false ;
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        f.setPosition(t.x, t.y, t.world) ;
        if (f.canPlace()) {
          return true ;
        }
        return false ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) return f.origin() ;
    return null ;
  }
  
  
  private Flora findCutting(Actor actor) {
    Series <Target> sample = actor.world().presences.sampleFromKey(
      actor, actor.world(), 10, null, Flora.class
    ) ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    Flora picked = null ;
    for (Target t : sample) {
      final Flora f = (Flora) t ;
      float rating = 0 - Spacing.distance(t, actor) ;
      rating -= actor.base().dangerMap.valAt(f.origin()) ;
      rating += f.growStage() * 10 ;
      if (rating > bestRating) { picked = f ; bestRating = rating ; }
    }
    return picked ;
  }
}






