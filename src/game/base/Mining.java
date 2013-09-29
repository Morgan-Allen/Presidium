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
  private static boolean verbose = false ;
  
  private MineFace face ;
  private int numMined = 0 ;
  
  
  Mining(Actor actor, MineFace face) {
    super(actor, face.shaft) ;
    this.face = face ;
  }
  
  
  public Mining(Session s) throws Exception {
    super(s) ;
    face = (MineFace) s.loadObject() ;
    numMined = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(face) ;
    s.saveInt(numMined) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Mining m = (Mining) p ;
    return m.face == this.face ;
  }
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    //
    //  In the event that the face is worked out, and you're still on your
    //  shift, try finding a new face to work on.
    final ExcavationSite shaft = face.shaft ;
    final Boardable at = actor.aboard() ;
    boolean quits = false ;
    
    if (oresCarried(actor) > 0) return nextReturnAction() ;
    if (face.workDone >= 100 || face.promise == -1) {
      if (verbose) I.sayAbout(actor, "Face is exhausted.") ;
      quits = true ;
      if (shaft.personnel.onShift(actor) && numMined <= 1 + Rand.index(4)) {
        final Mining m = shaft.nextMiningFor(actor) ;
        if (m != null) { this.face = m.face ; quits = false ; }
      }
    }
    if (quits) {
      if (at == shaft) return null ;
      else return nextReturnAction() ;
    }
    //
    //  To facilitate pathfinding, we break this into two steps- one where
    //  the actor goes to the shaft entrance, and another where they migrate to
    //  the mine face (underground.  Underground tiles don't mesh nicely with
    //  the normal above-ground pathing-cache system, you see.)
    if (at == face.shaft || at instanceof MineFace) {
      return new Action(
        actor, face,
        this, "actionMine",
        Action.BUILD, "mining at "+face.origin()
      ) ;
    }
    else {
      return new Action(
        actor, face.shaft,
        this, "actionEnterShaft",
        Action.BUILD, "Entering mine shaft"
      ) ;
    }
  }
  
  
  private Action nextReturnAction() {
    return new Action(
      actor, face.shaft,
      this, "actionDeliverOres",
      Action.REACH_DOWN, "returning ores"
    ) ;
  }
  
  
  public boolean actionEnterShaft(Actor actor, ExcavationSite shaft) {
    actor.goAboard(shaft, shaft.world()) ;
    //
    //  TODO:  Consider introducing security/safety measures here?
    return true ;
  }
  
  
  public boolean actionMine(Actor actor, MineFace face) {
    //
    //  Don't mine anything already mined out (which can happen with multiple
    //  actors in rapid succession.)
    if (face.promise == -1) return false ;
    
    final Terrain terrain = actor.world().terrain() ;
    int success = 1 ;
    success += actor.traits.test(GEOPHYSICS , 5 , 1) ? 1 : 0 ;
    success *= actor.traits.test(HARD_LABOUR, 15, 1) ? 2 : 1 ;
    //
    //  TODO:  Make this slower in general, but faster in soft soils
    
    face.workDone = Math.min(100, (success * 5 * Rand.num()) + face.workDone) ;
    
    if (face.workDone >= 100) {
      final Tile t = face.origin() ;
      final byte rockType = terrain.mineralType(t) ;
      float amount = terrain.mineralsAt(t, rockType) ;
      
      terrain.setMinerals(t, rockType, Terrain.DEGREE_TAKEN) ;
      face.shaft.openFace(face) ;
      numMined++ ;
      
      Service itemType = null ;
      switch (rockType) {
        case (Terrain.TYPE_CARBONS ) : itemType = PETROCARBS ; break ;
        case (Terrain.TYPE_METALS  ) : itemType = METAL_ORE  ; break ;
        case (Terrain.TYPE_ISOTOPES) : itemType = FUEL_CORES ; break ;
      }
      if (itemType == null || amount <= 0) {
        if (verbose) I.sayAbout(actor, "Minerals lacking at "+face.origin()) ;
        if (GameSettings.hardCore || Rand.yes()) return false ;
        
        itemType = (Service) Rand.pickFrom(new Object[] {
          PETROCARBS, METAL_ORE, METAL_ORE, FUEL_CORES
        }) ;
        amount = 0.5f * Rand.num() ;
      }
      
      final float bonus = face.shaft.structure.upgradeBonus(itemType) + 2 ;
      Item sample = Item.withReference(SAMPLES, itemType) ;
      sample = Item.withAmount(sample, amount * bonus / 2) ;
      
      if (verbose) I.sayAbout(actor, "Managed to mine: "+sample) ;
      actor.gear.addItem(sample) ;
      actor.gear.addItem(Item.withAmount(itemType, amount / 2)) ;
      return true ;
    }
    return false ;
  }
  
  
  private float oresCarried(Actor actor) {
    float total = 0 ;
    total += actor.gear.amountOf(SAMPLES) ;
    total += actor.gear.amountOf(PETROCARBS) ;
    total += actor.gear.amountOf(METAL_ORE ) ;
    total += actor.gear.amountOf(FUEL_CORES) ;
    return total ;
  }
  
  
  //
  //  TODO:  You want to deliver samples, instead of delivering ores directly.
  
  public boolean actionDeliverOres(Actor actor, ExcavationSite shaft) {
    if (verbose) I.sayAbout(actor, "Returning to mine shaft.") ;
    //
    //  TODO:  Make sure these samples are of ores, not anything else!
    actor.gear.transfer(PETROCARBS, shaft) ;
    actor.gear.transfer(METAL_ORE , shaft) ;
    actor.gear.transfer(FUEL_CORES, shaft) ;
    actor.gear.transfer(SAMPLES, shaft) ;
    actor.goAboard(shaft, shaft.world()) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Mining "+face.origin()+" ("+(int) face.workDone+"%)") ;
  }
}








