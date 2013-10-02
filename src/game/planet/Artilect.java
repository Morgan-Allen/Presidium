


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
    ).loadXMLInfo(XML_PATH, "Defence Drone"),
    MODEL_RECON_DRONE = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "ReconDrone.ms3d", 0.015f
    ).loadXMLInfo(XML_PATH, "Recon Drone"),
    MODEL_BLAST_DRONE = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "BlastDrone.ms3d", 0.015f
    ).loadXMLInfo(XML_PATH, "Blast Drone"),
    DRONE_MODELS[] = {
      MODEL_DEFENCE_DRONE, MODEL_RECON_DRONE, MODEL_BLAST_DRONE
    },
    
    MODEL_CRANIAL = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "Cranial.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "Cranial"),
    
    MODEL_TESSERACT = MS3DModel.loadMS3D(
      Artilect.class, IMG_DIR, "Tesseract.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "Tesseract")
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
      
      
      protected Behaviour reactionTo(Element seen) {
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
    final Combat defence = new Combat(this, near) ;
    defence.priorityMod = Plan.ROUTINE ;
    return defence ;
  }
  
  
  protected void addChoices(Choice choice) {
    //
    //  Patrol around your base and see off intruders.
    if (mind.home() == null) {
      //
      //  TODO:  Wander aimlessly?
      choice.add(Patrolling.securePerimeter(this, this, world)) ;
    }
    else choice.add(Patrolling.securePerimeter(this, mind.home(), world)) ;
    
    for (Element e : mind.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e)) ;
    }
    choice.add(new Retreat(this)) ;
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
    
    d.append("\n\nCondition: ") ;
    final Batch <String> healthDesc = health.conditionsDesc() ;
    for (String desc : healthDesc) {
      d.append("\n  "+desc) ;
    }
    final Batch <Condition> conditions = traits.conditions() ;
    for (Condition c : conditions) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(c)) ;
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay") ;
    }

    d.append("\n\nSkills: ") ;
    for (Skill skill : traits.skillSet()) {
      final int level = (int) traits.traitLevel(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.skillDesc(level), Skill.skillTone(level)) ;
    }
    
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





