/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;



public class Mining extends Plan implements BuildConstants {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  private MineFace face ;
  
  
  Mining(Actor actor, MineFace face) {
    super(actor, face.parent) ;
    this.face = face ;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s) ;
    face = (MineFace) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(face) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    if (face.promise == -1) {
      if (oresCarried(actor) > 0) return nextDelivery() ;
      return null ;
    }
    if (oresCarried(actor) > 0) return nextDelivery() ;
    if (face.workDone >= 100) { I.say("WORKED OUT") ; return null ; }
    //
    //  TODO:  To facilitate pathfinding, break this into two steps- one where
    //  the actor goes to the shaft entrance, and another where they migrate to
    //  the mine face (underground.)
    return new Action(
      actor, face,
      this, "actionMine",
      Action.BUILD, "mining at "+face.origin()
    ) ;
  }
  
  
  public boolean actionMine(Actor actor, MineFace face) {
    
    final Terrain terrain = actor.world().terrain() ;
    int success = 1 ;
    success += actor.traits.test(GEOPHYSICS , 5 , 1) ? 1 : 0 ;
    success *= actor.traits.test(HARD_LABOUR, 15, 1) ? 2 : 1 ;
    face.workDone = Math.min(100, (success * 5 * Rand.num()) + face.workDone) ;
    ///I.say("Mining "+face.origin()+", work done: "+face.workDone) ;
    
    if (face.workDone >= 100) {
      final Tile t = face.origin() ;
      final byte rockType = terrain.mineralType(t) ;
      final float amount = terrain.mineralsAt(t, rockType) ;
      terrain.setMinerals(t, rockType, Terrain.DEGREE_TAKEN) ;
      
      Service itemType = null ;
      switch (rockType) {
        case (Terrain.TYPE_CARBONS ) : itemType = CARBONS  ; break ;
        case (Terrain.TYPE_METALS  ) : itemType = METALS   ; break ;
        case (Terrain.TYPE_ISOTOPES) : itemType = ISOTOPES ; break ;
      }
      face.parent.openFace(face) ;
      if (itemType == null || amount <= 0) {
        I.say("No minerals at "+face.origin()) ;
        return false ;
      }
      else {
        final float bonus = face.parent.structure.upgradeBonus(itemType) + 2 ;
        final Item mined = Item.withAmount(itemType, amount * bonus / 2) ;
        I.say("Successfully mined: "+mined) ;
        actor.gear.addItem(mined) ;
        return true ;
      }
    }
    return false ;
  }
  
  
  private float oresCarried(Actor actor) {
    float total = 0 ;
    total += actor.gear.amountOf(CARBONS ) ;
    total += actor.gear.amountOf(METALS  ) ;
    total += actor.gear.amountOf(ISOTOPES) ;
    return total ;
  }
  
  
  private Action nextDelivery() {
    I.say("Scheduling ores delivery...") ;
    return new Action(
      actor, face.parent,
      this, "actionDeliverOres",
      Action.REACH_DOWN, "returning ores"
    ) ;
  }
  
  
  public boolean actionDeliverOres(Actor actor, ExcavationShaft shaft) {
    I.say("Delivering ores to shaft...") ;
    actor.gear.transfer(CARBONS , shaft) ;
    actor.gear.transfer(METALS  , shaft) ;
    actor.gear.transfer(ISOTOPES, shaft) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Mining at "+face.origin()+" ("+(int) face.workDone+"%)") ;
  }
}








