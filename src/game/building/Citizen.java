/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



public class Citizen extends Actor {
  
  
  final static String
    FILE_DIR = "media/Actors/human/",
    XML_PATH = FILE_DIR+"HumanModels.xml" ;
  final public static Model
    MODEL_MALE = MS3DModel.loadMS3D(
      Actor.class, FILE_DIR, "MaleAnimNewSkin.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "MalePrime"),
    MODEL_FEMALE = MS3DModel.loadMS3D(
      Actor.class, FILE_DIR, "FemaleAnimNewSkin.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "FemalePrime") ;
  
  
  public static interface Employment extends Session.Saveable {
    Behaviour jobFor(Citizen actor) ;
    void setWorker(Citizen actor, boolean is) ;
  }
  
  
  private Vocation vocation ;
  final List <Employment> employment = new List <Employment> () ;
  //private Venue home, work ;
  private Venue home ;
  
  
  
  public Citizen(Vocation vocation, Base base) {
    this.vocation = vocation ;
    vocation.configTraits(this) ;
    initSprite() ;
    assignBase(base) ;
  }
  
  
  public Citizen(Session s) throws Exception {
    super(s) ;
    s.loadObjects(employment) ;
    //home = (Venue) s.loadObject() ;
    //work = (Venue) s.loadObject() ;
    vocation = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    initSprite() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(employment) ;
    //s.saveObject(home) ;
    //s.saveObject(work) ;
    s.saveInt(vocation.ID) ;
  }
  
  
  /*
  public void setWorkVenue(Venue work) {
    final Venue old = this.work ;
    if (old != null) old.personnel.setWorker(this, false) ;
    this.work = work ;
    if (work != null) work.personnel.setWorker(this, true) ;
  }
  //*/
  
  
  public void addEmployer(Employment e) {
    employment.include(e) ;
    e.setWorker(this, true) ;
  }
  
  public void removeEmployer(Employment e) {
    employment.remove(e) ;
    e.setWorker(this, false) ;
  }
  
  
  public void setHomeVenue(Venue home) {
    final Venue old = this.home ;
    if (old != null) old.personnel.setResident(this, false) ;
    this.home = home ;
    if (home != null) home.personnel.setResident(this, true) ;
  }
  
  
  public Venue home() {
    return home ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean switchBehaviour(Behaviour next, Behaviour last) {
    return next.priorityFor(this) >= (last.priorityFor(this) + 2) ;
  }
  
  
  public Behaviour nextBehaviour() {
    final Citizen actor = this ;
    //  TODO:  Use a choice for this.
    for (Employment work : employment) {
    //if (work != null) {
      //I.say("Getting next work step...") ;
      Behaviour atWork = work.jobFor(actor) ;
      if (atWork != null) return atWork ;
    }
    if (home != null) {
      Behaviour atHome = home.jobFor(actor) ;
      if (atHome != null) return atHome ;
    }
    //
    //  If you've no other business to attend to, try and find any untaken job
    //  nearby and attend to it-
    
    //
    //  Try a range of other spontaneous behaviours, include relaxation and
    //  wandering-
    return new Patrolling(actor, actor, 5) ;
  }
  
  
  /**  Rendering and interface methods-
    */
  private void initSprite() {
    final boolean male = training.level(Trait.FEMININE) < 0 ;
    final JointSprite s ;
    if (sprite() == null) {
      s = (JointSprite) (
        male ? MODEL_FEMALE : MODEL_MALE
      ).makeSprite() ;
      this.attachSprite(s) ;
    }
    else s = (JointSprite) sprite() ;
    /*
    s.overlayTexture(Texture.loadTexture(
      "media/Actors/human/tundra_blood.gif"
    )) ;
    //*/
    s.overlayTexture(vocation.costume) ;
    for (String groupName : ((JointModel) s.model()).groupNames()) {
      if (! "Main Body".equals(groupName))
        s.toggleGroup(groupName, false) ;
    }
  }
  
  
  public String fullName() {
    return vocation.name ;
  }
  
  
  public Texture portrait() {
    //  TODO:
    //  Get the portrait associated with your ethnicity, age, sex, and looks.
    //  Overlay the texture for your vocation-costume.
    //  Return that.
    return null ;
  }
  

  public String helpInfo() {
    return null ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID) {
    //
    //  Health, activity, home and work venues.
    //  Current inventory.
    //  Traits and skills.
    
    d.append("\n\nCurrent Activity:") ;
    int indent = 0 ; for (Behaviour b : currentBehaviours()) {
      d.append("\n") ;
      for (int i = ++indent ; i-- > 0 ;) d.append("  ") ;
      b.describeBehaviour(d) ;
    }
    d.append("\n") ;
    if (employment.size() > 0) {
      d.appendList("Works at: ", employment) ;
    }
    else d.append("\nUnemployed") ;
    if (home != null) { d.append("\nHome: ") ; d.append(home) ; }
    else d.append("\nHomeless") ;
    
    if (! equipment.empty()) {
      d.append("\n\nInventory:") ;
      equipment.writeInformation(d) ;
    }
    
    d.append("\n\nSkills and Traits:") ;
    training.writeInformation(d) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append(fullName()) ;
  }
}


/*
*  Is there a pressing, life-threatening emergency?
   Running from an enemy, or raising an alarm.
   Treating/sheltering/defending someone injured or attacked.
   Getting food and sleep.

*  Have you been assigned or embarked on a mission?
   (Embarking on said missions, or accepting the rewards involved.)
   (Might be specified by player, or started spontaneously.)
   Strike Team.
   Security Team.
   Recovery Team.
   Recon Team.
   Contact Team.
   Covert Team.
   Accepting a promotion.
   Accepting ceremonial honours.
   Accepting license to marry.

*  Do you have work assigned by your employer?
   (Derived from home or work venues.)
   Seeding & Harvest.
   Excavation or Drilling.
   Hunting.
   Transport.
   Manufacture.
   Construction & Salvage.
   Patrolling/Enforcement.
   Treatment & Sick Leave.
   Entertainment.

*  Do you have spare time?
   (Procreation, Relaxation, Self-improvement, Helping out.)
   Relaxation/conversation/sex in public, at home, or at the Cantina.
   Matches/debates/spectation at the Arena or Senate Chamber.
   Learning new skills through apprenticeship or research at the Archives.
//*/







/*
public boolean monitor(Actor actor) {
  //  Get the next step regardless every 2 seconds or so, compare priority
  //  with the actor's current plan, and switch if the difference is big
  //  enough.
  //  TODO:  Consider checking after every discrete action?  That might be
  //  more granular.
  return true ;
}
//*/






