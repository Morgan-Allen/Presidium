


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




public class Tripod extends Artilect implements Economy {
  
  
  final String name ;
  
  
  public Tripod() {
    super() ;
    
    traits.initAtts(30, 10, 5) ;
    health.initStats(
      100, //lifespan
      5.0f,//bulk bonus
      1.0f,//sight range
      0.6f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    gear.setDamage(20) ;
    gear.setArmour(20) ;
    traits.setLevel(MARKSMANSHIP, 10) ;
    traits.setLevel(HAND_TO_HAND, 10) ;
    gear.equipOutfit(Item.withQuality(ARTILECT_ARMOUR, 0)) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_ENERGY_WEAPON, 0)) ;
    
    attachSprite(MODEL_TRIPOD.makeSprite()) ;
    name = nameWithBase("Tripod ") ;
  }
  
  
  public Tripod(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
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
      "Tripods are among the more feared of the artilect guardians wandering "+
      "the landscape.  Even in a decrepit state, they are well-armed and "+
      "will attack organics with scant provocation." ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
}




