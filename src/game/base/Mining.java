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



//
//  TODO:  Introduce strip mining and mantle drilling.

public class Mining extends Plan implements Economy {
  
  
  
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
  
  
  private Service mineralType(int rockType) {
    Service mineral = null ; switch (rockType) {
      case (Terrain.TYPE_CARBONS ) : mineral = PETROCARBS ; break ;
      case (Terrain.TYPE_METALS  ) : mineral = METAL_ORE  ; break ;
      case (Terrain.TYPE_ISOTOPES) : mineral = FUEL_CORES ; break ;
    }
    return mineral ;
  }
  
  
  private Item mineralsFrom(Tile t) {
    final Terrain terrain = t.world.terrain() ;
    final byte rockType = terrain.mineralType(t) ;
    float amount = terrain.mineralsAt(t, rockType) ;
    if (rockType == Terrain.TYPE_NOTHING || amount <= 0) return null ;
    terrain.setMinerals(t, rockType, Terrain.DEGREE_TAKEN) ;
    return Item.withAmount(mineralType(rockType), amount) ;
  }
  
  
  private float successCheck(Tile t) {
    float success = 1 ;
    success += actor.traits.test(GEOPHYSICS , 5 , 1) ? 1 : 0 ;
    success *= actor.traits.test(HARD_LABOUR, 15, 1) ? 2 : 1 ;
    if (t != null) {
      success *= (0.5f + (1 - t.habitat().minerals() / 10f)) / 2 ;
    }
    return success ;
  }
  
  
  public boolean actionMine(Actor actor, MineFace face) {
    //
    //  Don't mine anything already mined out (which can happen with multiple
    //  actors in rapid succession.)  Progress is faster in softer soils.
    if (face.promise == -1) return false ;
    final float success = successCheck(face.origin()) ;
    face.workDone = Math.min(100, (success * 5 * Rand.num()) + face.workDone) ;
    //
    //  If the face is mined out, try harvesting the minerals within-
    if (face.workDone >= 100) {
      Item mined = mineralsFrom(face.origin()) ;
      if (mined == null) {
        if (verbose) I.sayAbout(actor, "Minerals lacking at "+face.origin()) ;
        if (GameSettings.hardCore || Rand.yes()) return false ;
        
        mined = Item.withAmount((Service) Rand.pickFrom(new Object[] {
          PETROCARBS, METAL_ORE, METAL_ORE, FUEL_CORES
        }), 0.5f * Rand.num()) ;
      }
      //
      //  TODO:  There should also be a random chance of uncovering relics/
      //  artifacts/antiques this way-
      
      final float bonus = face.shaft.structure.upgradeBonus(mined.type) + 2 ;
      Item sample = Item.withReference(SAMPLES, mined.type) ;
      sample = Item.withAmount(sample, mined.amount * bonus / 2) ;
      
      if (verbose) I.sayAbout(actor, "Managed to mine: "+sample) ;
      actor.gear.addItem(sample) ;
      actor.gear.addItem(Item.withAmount(mined.type, mined.amount / 2)) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionStripMine(Actor actor, Target stripped) {
    if (! stripped.inWorld()) return false ;
    final float success = successCheck(null) ;
    if (success <= 0) return false ;
    
    if (stripped instanceof Tile) {
      //
      //
      if (Rand.index(10) < success) {
        final Tile t = (Tile) stripped ;
        t.world.terrain().setHabitat(t, Habitat.STRIP_MINING) ;
        final Item mined = mineralsFrom(t) ;
        actor.gear.addItem(mined) ;
        
        //
        //  TODO:  There should also be a random chance of uncovering relics/
        //  artifacts/antiques this way-
        return true ;
      }
      else return false ;
    }
    if (stripped instanceof Outcrop) {
      //
      //  
      final Outcrop o = (Outcrop) stripped ;
      final float bulk = o.size * o.size * o.high ;
      float takes = success * Rand.num() / (10 * bulk) ;
      takes = Math.min(takes, o.condition()) ;
      o.incCondition(0 - takes) ;
      
      final Service mineral = mineralType(o.mineralType()) ;
      if (mineral != null) {
        final Item mined = Item.withAmount(mineral, Rand.num()) ;
        actor.gear.addItem(mined) ;
      }
      return true ;
    }
    if (stripped instanceof Flora) {
      //
      //
      final Flora f = (Flora) stripped ;
      f.incGrowth(0 - success / 2f, f.world(), false) ;
      final Item sample = Item.withAmount(
        Item.withReference(SAMPLES, PETROCARBS), success * 10
      ) ;
      actor.gear.addItem(sample) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionDrill(Actor actor, MineFace drillsAt) {
    if (! drillsAt.inWorld()) return false ;
    float success = successCheck(null) ;
    if (success <= 0) return false ;
    success *= 1 + face.shaft.structure.upgradeLevel(
      ExcavationSite.MANTLE_DRILLING
    ) ;
    final Item
      cores = Item.withAmount(Smelter.SAMPLE_FUEL , success * Rand.num()),
      metal = Item.withAmount(Smelter.SAMPLE_METAL, success * Rand.num()) ;
    actor.gear.addItem(cores) ;
    actor.gear.addItem(metal) ;
    return true ;
  }
  
  
  private float oresCarried(Actor actor) {
    float total = 0 ;
    for (Service type : Smelter.MINED_TYPES) {
      total += actor.gear.amountOf(type) ;
    }
    for (Item sample : Smelter.SAMPLE_TYPES) {
      final Item match = actor.gear.matchFor(sample) ;
      if (match != null) total += match.amount ;
    }
    return total ;
  }
  
  
  public boolean actionDeliverOres(Actor actor, ExcavationSite shaft) {
    if (verbose) I.sayAbout(actor, "Returning to mine shaft.") ;
    for (Service type : Smelter.MINED_TYPES) {
      actor.gear.transfer(type, shaft) ;
    }
    for (Item sample : Smelter.SAMPLE_TYPES) {
      actor.gear.transfer(sample, shaft) ;
    }
    actor.goAboard(shaft, shaft.world()) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Mining "+face.origin()+" ("+(int) face.workDone+"%)") ;
  }
}








