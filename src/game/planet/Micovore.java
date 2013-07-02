/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.planet ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.util.* ;



public class Micovore extends Fauna {
  
  
  
  /**  Constructors, setup and save/load methods-
    */
  public Micovore() {
    super(Species.MICOVORE) ;
  }
  
  
  public Micovore(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(20, 15, 5) ;
    health.initStats(
      20,  //lifespan
      2.5f,//bulk bonus
      1.5f,//sight range
      1.5f //move speed
    ) ;
    gear.setDamage(15) ;
    gear.setArmour(5) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour nextFeeding() {
    final Batch <Fauna> prey = specimens(
      origin(), PEER_SAMPLE_RANGE, null, Species.Type.BROWSER, 10
    ) ;
    if (prey.size() == 0) return null ;
    
    Fauna pickedPrey = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    for (Fauna f : prey) {
      //  Choose closer, younger, less heavily armed/armoured targets.
      float danger = f.gear.armourRating() + f.gear.attackDamage() ;
      danger *= f.health.maxHealth() / 100 ;
      float dist = Spacing.distance(f, this) / PEER_SAMPLE_RANGE ;
      float rating = (1 - dist) / danger ;
      if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
    }
    
    if (pickedPrey == null) return null ;
    final Hunting hunting = new Hunting(this, pickedPrey, Hunting.TYPE_FEEDS) ;
    return hunting ;
  }
  
  
  protected void fightWith(Fauna competes) {
    final Hunting fight = new Hunting(this, competes, Hunting.TYPE_FEEDS) ;
    fight.setPriority(Plan.CRITICAL) ;
    if (psyche.couldSwitchTo(fight)) {
      psyche.assignBehaviour(fight) ;
    }
  }
  

  protected float rateMigratePoint(Tile point) {
    float rating = super.rateMigratePoint(point) ;
    final Batch <Fauna> nearPrey = specimens(
      point, PEER_SAMPLE_RANGE, null, Species.Type.BROWSER, 3
    ) ;
    if (nearPrey.size() == 0) return rating ;
    float avgDistance = 0 ;
    for (Fauna f : nearPrey) avgDistance += Spacing.distance(point, f) ;
    avgDistance /= nearPrey.size() * PEER_SAMPLE_RANGE ;
    return rating * (2 - avgDistance) ;
  }
  
  
  protected Target findRestPoint() {
    return origin() ;
  }
  
  
  //
  //  TODO:  Consider routines for placing Lairs, maintaining them, and letting
  //  them decay over time.  TODO: MOVE THOSE TO THE MICOVORE CLASS.
  protected Tile findLairLocation() {
    //
    //  Look for an area that doesn't have any rocks in the way, and place a
    //  2x2 lair there if possible.
    return null ;
  }
  
  
  public boolean actionGatherMaterials(Vareen actor, Tile source) {
    //actor.inventory().addItem(new Item(DUST, 0.2f * Rand.num())) ;
    return true ;
  }
  
  
  public boolean actionBuildNest(Vareen actor, Lair nest) {
    //nest.stocks.repairBy(1, actor.inventory()) ;
    return true ;
  }
}



