


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



public class Drone extends Artilect implements Economy {
  
  
  /**  Construction and save/load methods-
    */
  final String name ;
  
  
  public Drone() {
    super() ;
    
    traits.initAtts(15, 10, 5) ;
    health.initStats(
      10,  //lifespan
      0.65f,//bulk bonus
      0.65f,//sight range
      1.25f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;

    gear.setDamage(10) ;
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
  
  
  
  /**  Physical properties-
    */
  public float aboveGroundHeight() {
    return 0.5f ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
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



