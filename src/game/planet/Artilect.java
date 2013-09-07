


package src.game.planet ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.game.wild.Tripod;
import src.game.building.* ;
import src.graphics.common.Model;
import src.graphics.jointed.MS3DModel;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;




public abstract class Artilect extends Actor {

  
  
  /**  Construction and save/load methods-
    */
  final static String
    IMG_DIR = "media/Actors/artilects/",
    XML_PATH = IMG_DIR+"ArtilectModels.xml" ;
  final public static Model
    
    MODEL_TRIPOD = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "Tripod.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "Tripod"),
    
    MODEL_DEFENCE_DRONE = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "DefenceDrone.ms3d", 0.015f
    ).loadXMLInfo(XML_PATH, "DefenceDrone"),
    MODEL_RECON_DRONE = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "ReconDrone.ms3d", 0.015f
    ).loadXMLInfo(XML_PATH, "ReconDrone"),
    MODEL_BLAST_DRONE = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "BlastDrone.ms3d", 0.015f
    ).loadXMLInfo(XML_PATH, "BlastDrone"),
    DRONE_MODELS[] = {
      MODEL_DEFENCE_DRONE, MODEL_RECON_DRONE, MODEL_BLAST_DRONE
    }
    
    
    ;
  
  
  
  protected Artilect() {
    super() ;
  }
  
  
  public Artilect(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  

  protected ActorAI initAI() {
    final Artilect actor = this ;
    return new ActorAI(actor) {
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.weightedPick(0) ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
        //
        //  If your home is under assault, defend it.
      }
      
      
      protected Behaviour reactionTo(Mobile seen) {
        if (seen instanceof Actor) return nextDefence((Actor) seen) ;
        return nextDefence(null) ;
      }
      
      
      public float relation(Actor other) {
        if (actor.base() != null && other.base() == actor.base()) return 0.5f ;
        if (other instanceof Artilect) return 1.0f ;
        return -1.0f ;
      }
    } ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    return new Combat(this, near) ;
  }
  
  
  protected void addChoices(Choice choice) {
    //
    //  Patrol around your base and see off intruders.
    if (AI.home() == null) choice.add(new Patrolling(this, this, 6)) ;
    else choice.add(new Patrolling(this, AI.home(), 6)) ;
    
    //
    //  Return to base for repairs/recharge.
    //
    //  Perform repairs on another artilect, or construct a new model.
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  (Tripod speciality.)
    //
    //  Perform reconaissance.
    //  (Drone speciality.)
    //
    //  Capture specimens for experiment/dissection/interrogation/conversion.
    //  (Cranial speciality.)
  }
  
  //
  //  ...They can't be capable of indefinite reconstruction- or if they are,
  //  there needs to be a reason for why they haven't taken over the world
  //  already.  Stuck in an infinite loop?
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.append("Is: ") ;
    super.describeStatus(d) ;
    d.append("\n\n") ;
    d.append(helpInfo()) ;
  }
  
  
  protected static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base) ;
    for (int n = 4 ; n-- > 0 ;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10))) ;
      else nB.append((char) ('A'+Rand.index(26))) ;
    }
    return nB.toString() ;
  }
}





