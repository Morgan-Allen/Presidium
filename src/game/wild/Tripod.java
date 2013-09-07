


package src.game.wild ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.Rand;




public class Tripod extends Artilect implements BuildConstants {
  
  
  /**  Construction and save/load methods-
    */
  final static String IMG_DIR = "media/Actors/artilects/" ;
  final static Model
    MODEL = MS3DModel.loadMS3D(
      Tripod.class, IMG_DIR, "Tripod.ms3d", 0.025f
    ).loadXMLInfo(IMG_DIR+"ArtilectModels.xml", "Tripod") ;
  
  
  final String name ;
  
  
  public Tripod() {
    super() ;
    //profile.addTrait(BackgroundVocations.ARTILECT) ;
    //profile.addTrait(BackgroundVocations.MINDLESS) ;
    //training.raiseSkill(BackgroundVocations.REFLEX, 5) ;
    //training.raiseSkill(BackgroundVocations.MARKSMANSHIP, 5) ;
    //inventory().equipImplement(new Item(Economy.DISINTEGRATOR, this, 1, 1)) ;
    //inventory().equipOutfit(new Item(Economy.GOLEM_ARMOUR, this, 1, 1)) ;
    
    traits.initAtts(30, 10, 5) ;
    health.initStats(
      100, //lifespan
      5.0f,//bulk bonus
      1.0f,//sight range
      0.6f,//move speed,
      false//organic
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    attachSprite(MODEL.makeSprite()) ;
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
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Tripods are among the more feared of the artilect guardians wandering "+
      "the landscape.  Even in a decrepit state, they are well-armed and "+
      "will attack organics with little provocation." ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base) ;
    for (int n = 4 ; n-- > 0 ;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10))) ;
      else nB.append((char) ('A'+Rand.index(26))) ;
    }
    return nB.toString() ;
  }
}




