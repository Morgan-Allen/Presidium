


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.graphics.common.Model ;
import src.user.* ;
import src.util.* ;



public class Mining extends Plan implements VenueConstants {
  
  
  
  private MineFace opening ;
  
  
  Mining(Actor actor, MineFace opening) {
    super(actor, opening.parent) ;
    this.opening = opening ;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s) ;
    opening = (MineFace) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(opening) ;
  }



  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    if (opening.promise == -1) {
      if (oresCarried(actor) > 0) return nextDelivery() ;
      return null ;
    }
    if (oresCarried(actor) >= 10) return nextDelivery() ;
    return new Action(
      actor, opening,
      this, "actionMine",
      Model.AnimNames.BUILD, "mining at "+opening.origin()
    ) ;
  }
  
  
  public boolean actionMine(Actor actor, MineFace opening) {
    
    final Terrain terrain = actor.world().terrain() ;
    final int mineChance = 100 ;
    boolean success = actor.training.test(GEOPHYSICS, 5, 1) ;
    success &= actor.training.test(HARD_LABOUR, 15, 1) ;
    success &= Rand.index(100) < mineChance ;
    
    I.say("Mining "+opening.origin()+", success... "+success) ;
    
    if (success) {
      final Tile t = opening.origin() ;
      final byte rockType = terrain.mineralType(t) ;
      float amount = terrain.mineralsAt(t, rockType) ;
      terrain.setMinerals(t, rockType, Terrain.DEGREE_TAKEN) ;
      
      Item.Type itemType = null ;
      switch (rockType) {
        case (Terrain.TYPE_CARBONS ) : itemType = CARBONS  ; break ;
        case (Terrain.TYPE_METALS  ) : itemType = METALS   ; break ;
        case (Terrain.TYPE_ISOTOPES) : itemType = ISOTOPES ; break ;
      }
      final Item mined = new Item(itemType, amount) ;
      actor.equipment.addItem(mined) ;
      
      opening.parent.openFace(opening) ;
      ///I.say("Finished extraction...") ;
      return true ;
    }
    return false ;
  }
  
  
  private float oresCarried(Actor actor) {
    float total = 0 ;
    total += actor.equipment.amountOf(CARBONS ) ;
    total += actor.equipment.amountOf(METALS  ) ;
    total += actor.equipment.amountOf(ISOTOPES) ;
    return total ;
  }
  
  
  private Action nextDelivery() {
    I.say("Scheduling ores delivery...") ;
    return new Action(
      actor, opening.parent,
      this, "actionDeliverOres",
      Model.AnimNames.REACH_DOWN, "returning ores"
    ) ;
  }
  
  
  public boolean actionDeliverOres(Actor actor, MineShaft shaft) {
    I.say("Delivering ores to shaft...") ;
    actor.equipment.transfer(CARBONS , shaft) ;
    actor.equipment.transfer(METALS  , shaft) ;
    actor.equipment.transfer(ISOTOPES, shaft) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Mining at "+opening.origin()) ;
  }
}








