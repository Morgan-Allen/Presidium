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



public class Micovore extends Organism {
  
  
  
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
    ///if (true) return null ;
    //
    //  Find the nearest prey specimen, run it down, and eat it.
    //  ...That may require multiple actions.  So, use a plan.
    final Actor prey = nearestSpecimen(
      origin(), PREDATOR_RANGE, null, Species.Type.BROWSER
    ) ;
    if (prey == null) return null ;
    final Hunting hunting = new Hunting(this, prey, Hunting.TYPE_FEEDING) ;
    return hunting ;
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



