/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



public class Citizen extends Actor implements ActorConstants {
  
  
  /**  Methods and constants related to preparing media and sprites-
    */
  //
  //  TODO:  All this graphics/media related code might be more productively
  //  kept elsewhere.
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
  
  //
  //  All this stuff is intimately dependant on the layout of the collected
  //  portraits image specified- do not modify without close inspection.
  final public static Texture
    BASE_FACES = Texture.loadTexture(FILE_DIR+"face_portraits.png") ;
  final static int
    CHILD_FACE_OFF[] = {2, 0},
    UGLYS_FACE_OFF[] = {2, 1},
    F_AVG_FACE_OFF[] = {1, 1},
    F_HOT_FACE_OFF[] = {0, 1},
    M_AVG_FACE_OFF[] = {1, 0},
    M_HOT_FACE_OFF[] = {0, 0} ;
  
  final static int
    M_HAIR_OFF[][] = {{0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}},
    F_HAIR_OFF[][] = {{0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}} ;
  
  final static Texture BLOOD_SKINS[] = Texture.loadTextures(
    FILE_DIR+"desert_blood.gif",
    FILE_DIR+"tundra_blood.gif",
    FILE_DIR+"forest_blood.gif",
    FILE_DIR+"wastes_blood.gif"
  ) ;
  final static int BLOOD_FACE_OFFSETS[][] = {
    {3, 4}, {0, 4}, {3, 2}, {0, 2}
  } ;
  
  
  private static int bloodID(Citizen c) {
    int ID = 0 ;
    float highest = 0 ;
    for (int i = 4 ; i-- > 0 ;) {
      final float blood = c.traits.level(BLOOD_TRAITS[i]) ;
      if (blood > highest) { ID = i ; highest = blood ; }
    }
    return ID ;
  }
  
  
  private static Composite faceComposite(Citizen c, BaseUI UI) {
    
    final Composite composite = new Composite(UI) ;
    final int bloodID = bloodID(c) ;
    final boolean male = c.traits.hasTrait(Trait.GENDER, "Male") ;
    final int ageStage = c.health.agingStage() ;
    ///I.say("Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage) ;
    
    int faceOff[], bloodOff[] = BLOOD_FACE_OFFSETS[bloodID] ;
    if (ageStage == 0) faceOff = CHILD_FACE_OFF ;
    else {
      int looks = (int) c.traits.level(Trait.HANDSOME) + 1 - ageStage ;
      //int looks = 2 ;
      if (looks > 0) faceOff = male ? M_HOT_FACE_OFF : F_HOT_FACE_OFF ;
      else if (looks == 0) faceOff = male ? M_AVG_FACE_OFF : F_AVG_FACE_OFF ;
      else faceOff = UGLYS_FACE_OFF ;
    }
    //
    //  TODO:  You may want a layer for the back of the costume or hairdo.
    
    final int UV[] = new int[] {
      0 + (faceOff[0] + bloodOff[0]),
      5 - (faceOff[1] + bloodOff[1])
    } ;
    composite.addLayer(BASE_FACES, UV[0], UV[1], 6, 6) ;
    
    if (ageStage > 0) {
      int hairID = c.career.fullName.hashCode() % 3 ;
      if (hairID < 0) hairID *= -1 ;
      if (ageStage >= 2) hairID = 2 ;
      boolean haveBeard = (c.career.fullName.hashCode() % 2) == 0 ;
      if (! male) haveBeard = false ;
      int fringeOff[] = (haveBeard ? M_HAIR_OFF : F_HAIR_OFF)[hairID] ;
      composite.addLayer(BASE_FACES, fringeOff[0], fringeOff[1], 6, 6) ;
    }
    
    Texture portrait = c.career.vocation().portrait ;
    if (portrait == null) portrait = c.career.birth.portrait ;
    composite.addLayer(portrait, 0, 0, 1, 1) ;
    
    return composite ;
  }
  
  
  private static void initSpriteFor(Citizen c) {
    final boolean male = c.traits.hasTrait(Trait.GENDER, "Male") ;
    final JointSprite s ;
    if (c.sprite() == null) {
      s = (JointSprite) (
        male ? MODEL_FEMALE : MODEL_MALE
      ).makeSprite() ;
      c.attachSprite(s) ;
    }
    else s = (JointSprite) c.sprite() ;
    
    s.overlayTexture(BLOOD_SKINS[bloodID(c)]) ;
    Texture costume = c.career.vocation.costume ;
    if (costume == null) costume = c.career.birth.costume ;
    s.overlayTexture(costume) ;
    for (String groupName : ((JointModel) s.model()).groupNames()) {
      if (! "Main Body".equals(groupName))
        s.toggleGroup(groupName, false) ;
    }
  }
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  Venue home ;
  List <Citizen> family = new List <Citizen> () ;
  List <Employment> employment = new List <Employment> () ;
  
  final public Career career = new Career() ;
  
  
  
  public Citizen(Vocation vocation, Base base) {
    assignBase(base) ;
    career.genCareer(vocation, this) ;
    initSpriteFor(this) ;
  }
  
  
  public Citizen(Session s) throws Exception {
    super(s) ;
    home = (Venue) s.loadObject() ;
    s.loadObjects(employment) ;
    career.loadState(s) ;
    initSpriteFor(this) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(home) ;
    s.saveObjects(employment) ;
    career.saveState(s) ;
  }
  
  
  
  /**  Setting employment and residency-
    */
  public static interface Employment extends Session.Saveable {
    Behaviour jobFor(Citizen actor) ;
    void setWorker(Citizen actor, boolean is) ;
  }
  
  
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
  //  TODO:  MOVE THESE TO THE ACTORPSYCHE CLASS.
  
  public boolean couldSwitch(Behaviour last, Behaviour next) {
    if (next == null) return false ;
    if (last == null) return true ;
    return next.priorityFor(this) >= (last.priorityFor(this) + 2) ;
  }
  
  
  public Behaviour nextBehaviour() {
    final Citizen actor = this ;
    final Choice choice = new Choice(this) ;
    //
    //  Find all nearby items or actors and consider reacting to them.
    final Tile o = origin() ;
    final int sightRange = health.sightRange() ;
    final Box2D bound = new Box2D().set(o.x, o.y, 0, 0) ;
    for (Tile t : world.tilesIn(bound.expandBy(sightRange), true)) {
      if (Spacing.distance(t, o) > sightRange) continue ;
      for (Mobile m : t.inside()) addReactions(m, choice) ;
    }
    //
    //  Find the next jobs waiting for you at work or home.
    for (Employment work : employment) {
      Behaviour atWork = work.jobFor(actor) ;
      if (atWork != null) choice.add(atWork) ;
    }
    if (home != null) {
      Behaviour atHome = home.jobFor(actor) ;
      if (atHome != null) choice.add(atHome) ;
    }
    //
    //  If you've no other business to attend to, try and find any untaken job
    //  nearby and attend to it-
    
    //
    //  Try a range of other spontaneous behaviours, include relaxation and
    //  spontaneous missions-
    final Action wander = (Action) new Patrolling(actor, actor, 5).nextStep() ;
    wander.setPriority(Plan.IDLE) ;
    choice.add(wander) ;
    //
    //  Finally, return whatever activity seems most urgent or appealing.
    return choice.weightedPick() ;
  }
  
  //
  //  TODO:  Pass a Choice object here instead, and add all possibilities...
  protected void addReactions(Mobile m, Choice choice) {
    if (m instanceof Actor) {
      final Actor other = (Actor) m ;
      if (Dialogue.canTalk(other, this)) {
        choice.add(new Dialogue(this, other)) ;
      }
    }
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return career.fullName+" the "+career.vocation.name ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return faceComposite(this, UI) ;
  }
  

  public String helpInfo() {
    return null ;
  }
  
  
  
  
  //
  //  TODO:  You need to to break up this information into more categories.
  //
  //  Condition, injury/fatigue/stress, vocation and present activity.
  //  Attributes, skills and psyonic abilities.
  //  Equipment, carried items, encumbrance and physique.
  //  Background, personality, relationships and family.
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "OUTFIT", "SKILLS", "PSYCH" } ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    if (categoryID == 0) describeStatus(d, UI) ;
    if (categoryID == 1) describeOutfit(d, UI) ;
    if (categoryID == 2) describeSkills(d, UI) ;
    if (categoryID == 3) describePsych(d, UI) ;
  }
  
  
  private void describeStatus(Description d, BaseUI UI) {
    //
    //  Describe your job, place of work, and current residence:
    //  TODO:  Allow a single employer and mission.
    d.append("Vocation: ") ;
    d.append(career.vocation()) ;
    if (employment.size() > 0) {
      d.appendList("Works at: ", employment) ;
    }
    else d.append("\nUnemployed") ;
    if (home != null) { d.append("\nHome: ") ; d.append(home) ; }
    else d.append("\nHomeless") ;
    //
    //  Describe your current health, outlook, or special FX.
    d.append("\n\nCondition: ") ;
    final Batch <Condition> conditions = traits.conditions() ;
    if (conditions.size() == 0) d.append("\n  Okay") ;
    else for (Condition c : conditions) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(c)) ;
    }
    //
    //  Describe your current assignment or undertaking.
    d.append("\n\nCurrently:") ;
    final Behaviour rootB = rootBehaviour() ;
    if (rootB != null) {
      d.append("\n  ") ;
      rootB.describeBehaviour(d) ;
    }
    else d.append("\n  Idle") ;
  }
  
  
  private void describeOutfit(Description d, BaseUI UI) {
    //
    //  Describe your overall appearance and physique.
    
    //
    //  Describe your current weapon or implement, and armour or dress.  Rate
    //  your current encumbrance, and any other special bonuses or effects.
    
    //
    //  Finally, describe any other items you're carrying, rations, and both
    //  taxed and untaxed credits.
  }
  
  
  private void describeSkills(Description d, BaseUI UI) {
    
  }
  
  
  private void describePsych(Description d, BaseUI UI) {
    
  }
}



/*
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

if (! gear.empty()) {
  d.append("\n\nInventory:") ;
  gear.writeInformation(d) ;
}

d.append("\n\nSkills and Traits:") ;
traits.writeInformation(d) ;
//*/





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






