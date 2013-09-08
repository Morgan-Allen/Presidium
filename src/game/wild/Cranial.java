


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



public class Cranial extends Artilect implements BuildConstants {
  
  
  /**  Construction and save/load methods-
    */
  final String name ;
  
  
  public Cranial() {
    super() ;
    
    traits.initAtts(15, 15, 25) ;
    health.initStats(
      1000,//lifespan
      1.5f,//bulk bonus
      1.0f,//sight range
      0.3f,//move speed,
      false//organic
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    gear.setDamage(15) ;
    gear.setArmour(10) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_MELEE, 0)) ;
    traits.setLevel(CLOSE_COMBAT, 5) ;
    traits.setLevel(ASSEMBLY, 10) ;
    traits.setLevel(ANATOMY, 20) ;
    
    attachSprite(MODEL_CRANIAL.makeSprite()) ;
    name = nameWithBase("Cranial ") ;
  }
  
  
  public Cranial(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Cranials are cunning, quasi-organic machine intelligences with a "+
      "propensity for abducting living creatures for purposes of dissection, "+
      "interrogation and ultimate assimilation." ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
}



