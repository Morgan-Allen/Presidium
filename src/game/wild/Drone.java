


package src.game.wild ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Drone extends Artilect implements EconomyConstants {
  
  
  /**  Construction and save/load methods-
    */
  final String name ;
  
  
  public Drone() {
    super() ;
    
    traits.initAtts(10, 5, 2) ;
    health.initStats(
      10,  //lifespan
      0.6f,//bulk bonus
      0.6f,//sight range
      0.6f,//move speed,
      false//organic
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;

    gear.setDamage(5) ;
    gear.setArmour(10) ;
    traits.setLevel(MARKSMANSHIP, 5) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_ENERGY_WEAPON, 0)) ;
    
    final Model model = DRONE_MODELS[Rand.index(3)] ;
    attachSprite(model.makeSprite()) ;
    name = nameWithBase("Drone ") ;
  }
  
  
  public Drone(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public float aboveGroundHeight() {
    return 0.5f ;
  }
  
  
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Defence Drones are simple, disposable automatons capable of limited "+
      "field operations without supervision." ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
}



