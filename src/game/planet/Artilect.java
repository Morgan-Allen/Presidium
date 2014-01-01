


package src.game.planet ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
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
  

  protected ActorMind initAI() {
    final Artilect actor = this ;
    return new ActorMind(actor) {
      
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.pickMostUrgent() ;
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
    if (near == null) return null ;
    final Combat defence = new Combat(this, near) ;
    defence.priorityMod = Plan.ROUTINE ;
    //I.sayAbout(this, "Have just seen: "+near) ;
    //I.sayAbout(this, "Defence priority: "+defence.priorityFor(this)) ;
    return defence ;
  }
  
  
  protected void addChoices(Choice choice) {
    
    //I.say("Creating new choices for "+this) ;
    //
    //  Patrol around your base and see off intruders.
    Element guards = mind.home() == null ? this : (Element) mind.home() ;
    final Patrolling p = Patrolling.securePerimeter(this, guards, world) ;
    p.priorityMod = Plan.IDLE ;
    ///I.say("Patrolling priority: "+p.priorityFor(this)) ;
    choice.add(p) ;
    
    for (Element e : mind.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e)) ;
    }
    
    //
    //
    //  Perform reconaissance or patrolling.
    //  Retreat and return to base.
    //  (Drone specialties.)
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  Capture specimens and bring back to lair.
    //  (Tripod specialties.)
    choice.add(nextAssault()) ;
    choice.add(new Retreat(this)) ;
    //
    //  Experiment upon/dissect/interrogate/convert any captives.
    //  Perform repairs on another artilect, or refurbish a new model.
    //  (Cranial specialties.)
    //
    //  Defend home site or retreat to different site (all).
    //  Respond to obelisk or tesseract presence (all).
  }
  
  
  protected Behaviour nextAssault() {
    if (! (mind.home() instanceof Venue)) return null ;
    final Venue lair = (Venue) mind.home() ;
    final Batch <Venue> sampled = new Batch <Venue> () ;
    world.presences.sampleFromKey(this, world, 10, sampled, Venue.class) ;
    
    final int SS = World.SECTOR_SIZE ;
    Venue toAssault = null ;
    float bestRating = 0 ;
    
    for (Venue venue : sampled) {
      if (venue.base() == this.base()) continue ;
      final float dist = Spacing.distance(venue, lair) ;
      float rating = SS / (SS + dist) ;
      rating += 1 - mind.relation(venue) ;
      if (rating > bestRating) { bestRating = rating ; toAssault = venue ; }
    }
    
    if (toAssault == null) return null ;
    final Combat siege = new Combat(this, toAssault) ;
    
    //
    //  TODO:  Base priority on proximity to your lair, along with total
    //  settlement size.
    return siege ;
  }
  
  
  
  
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





