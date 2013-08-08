/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.sfx.* ;
import src.user.* ;
import src.util.* ;




public class Human extends Actor implements ActorConstants {
  
  
  /**  Methods and constants related to preparing media and sprites-
    */
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
  
  final public static Texture
    BASE_FACES = Texture.loadTexture(FILE_DIR+"face_portraits.png") ;
  
  final static Texture BLOOD_SKINS[] = Texture.loadTextures(
    FILE_DIR+"desert_blood.gif",
    FILE_DIR+"tundra_blood.gif",
    FILE_DIR+"forest_blood.gif",
    FILE_DIR+"wastes_blood.gif"
  ) ;
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  Career career ;
  
  
  public Human(Vocation vocation, Base base) {
    this(new Career(vocation), base) ;
  }
  
  
  public Human(Career career, Base base) {
    this.career = career ;
    assignBase(base) ;
    career.applyCareer(this) ;
    initSpriteFor(this) ;
  }
  
  
  public Human(Session s) throws Exception {
    super(s) ;
    career = new Career(this) ;
    career.loadState(s) ;
    initSpriteFor(this) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    career.saveState(s) ;
  }
  
  
  protected ActorPsyche initPsyche() { return new CitizenPsyche(this) ; }
  
  public Vocation vocation() { return career.vocation() ; }
  
  public Career career() { return career ; }
  
  public Object species() { return Species.HUMAN ; }
  
  
  
  /**  Supplementary methods for behaviour-
    */
  public float attraction(Actor otherA) {
    if (! (otherA instanceof Human)) return 0 ;
    final Human actor = (Human) this ;
    final Human other = (Human) otherA ;
    //
    //  TODO:  Create exceptions based on age and kinship modifiers.
    //
    //  First, we establish a few facts about each actor's sexual identity:
    float actorG = 0, otherG = 0 ;
    if (actor.traits.hasTrait(GENDER, "Male"  )) actorG = -1 ;
    if (actor.traits.hasTrait(GENDER, "Female")) actorG =  1 ;
    if (other.traits.hasTrait(GENDER, "Male"  )) otherG = -1 ;
    if (other.traits.hasTrait(GENDER, "Female")) otherG =  1 ;
    float attraction = other.traits.trueLevel(HANDSOME) * 3.33f ;
    attraction += otherG * other.traits.trueLevel(FEMININE) * 3.33f ;
    attraction *= (actor.traits.scaleLevel(DEBAUCHED) + 1f) / 2 ;
    //
    //  Then compute attraction based on orientation-
    final String descO = actor.traits.levelDesc(ORIENTATION) ;
    float matchO = 0 ;
    if (descO.equals("Heterosexual")) {
      matchO = (actorG * otherG < 0) ? 1 : 0.33f ;
    }
    else if (descO.equals("Bisexual")) {
      matchO = 0.66f ;
    }
    else if (descO.equals("Homosexual")) {
      matchO = (actorG * otherG > 0) ? 1 : 0.33f ;
    }
    return attraction * matchO ;
  }
  
  
  
  /**  Utility methods/constants for creating human-citizen sprites and
    *  portraits-
    */
  //
  //  All this stuff is intimately dependant on the layout of the collected
  //  portraits image specified- do not modify without close inspection.
  final static int
    CHILD_FACE_OFF[] = {2, 0},
    ELDER_FACE_OFF[] = {2, 1},
    F_AVG_FACE_OFF[] = {1, 1},
    F_HOT_FACE_OFF[] = {0, 1},
    M_AVG_FACE_OFF[] = {1, 0},
    M_HOT_FACE_OFF[] = {0, 0} ;
  
  final static int
    M_HAIR_OFF[][] = {{5, 5}, {4, 5}, {3, 5}, {3, 4}, {4, 4}, {5, 4}},
    F_HAIR_OFF[][] = {{2, 5}, {1, 5}, {0, 5}, {0, 4}, {1, 4}, {2, 4}} ;
  
  final static int BLOOD_FACE_OFFSETS[][] = {
    {3, 4}, {0, 4}, {3, 2}, {0, 2}
  } ;
  final static int BLOOD_TONE_SHADES[] = { 3, 1, 2, 0 } ;
  
  
  private static int bloodID(Human c) {
    int ID = 0 ;
    float highest = 0 ;
    for (int i = 4 ; i-- > 0 ;) {
      final float blood = c.traits.trueLevel(BLOOD_TRAITS[i]) ;
      if (blood > highest) { ID = i ; highest = blood ; }
    }
    return ID ;
  }
  
  
  private static Composite faceComposite(Human c, BaseUI UI) {
    
    final Composite composite = new Composite(UI) ;
    final int bloodID = bloodID(c) ;
    final boolean male = c.traits.hasTrait(Trait.GENDER, "Male") ;
    final int ageStage = c.health.agingStage() ;
    ///I.say("Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage) ;
    
    int faceOff[], bloodOff[] = BLOOD_FACE_OFFSETS[bloodID] ;
    if (ageStage == 0) faceOff = CHILD_FACE_OFF ;
    else {
      int looks = (int) c.traits.trueLevel(Trait.HANDSOME) + 2 - ageStage ;
      if (looks > 0) faceOff = male ? M_HOT_FACE_OFF : F_HOT_FACE_OFF ;
      else if (looks == 0) faceOff = male ? M_AVG_FACE_OFF : F_AVG_FACE_OFF ;
      else faceOff = ELDER_FACE_OFF ;
    }
    
    final int UV[] = new int[] {
      0 + (faceOff[0] + bloodOff[0]),
      5 - (faceOff[1] + bloodOff[1])
    } ;
    composite.addLayer(BASE_FACES, UV[0], UV[1], 6, 6) ;
    
    if (ageStage > ActorHealth.AGE_JUVENILE) {
      int hairID = c.traits.geneValue("hair", 6) ;
      if (hairID < 0) hairID *= -1 ;
      hairID = Visit.clamp(hairID + BLOOD_TONE_SHADES[bloodID], 6) ;
      
      if (ageStage >= ActorHealth.AGE_SENIOR) hairID = 5 ;
      else if (hairID == 5) hairID-- ;
      int fringeOff[] = (male ? M_HAIR_OFF : F_HAIR_OFF)[hairID] ;
      composite.addLayer(BASE_FACES, fringeOff[0], fringeOff[1], 6, 6) ;
      
      Texture portrait = c.career.vocation().portrait ;
      if (portrait == null) portrait = c.career.birth().portrait ;
      composite.addLayer(portrait, 0, 0, 1, 1) ;
    }
    
    return composite ;
  }
  
  
  private static void initSpriteFor(Human c) {
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
    Texture costume = c.career.vocation().costume ;
    if (costume == null) costume = c.career.birth().costume ;
    s.overlayTexture(costume) ;
    for (String groupName : ((JointModel) s.model()).groupNames()) {
      if (! "Main Body".equals(groupName))
        s.toggleGroup(groupName, false) ;
    }
  }
  
  
  
  /**  More usual rendering and interface methods-
    */
  private TalkFX talkFX ;
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base) ;
    //
    //  If you don't have a TalkFX already, create one, add dialogue, and
    //  render it-
    //  So... this creates a problem.  This should probably be considered a
    //  form of ephemera.  But this type would need to track the actor.
    
    /*
    if (talkFX == null) talkFX = new TalkFX() ;
    if (talkFX.numPhrases() == 0) {
      talkFX.addPhrase("Testing, testing...", false) ;
      talkFX.addPhrase("Hello World!", true) ;
      talkFX.addPhrase("(Credits +25)", false) ;
    }
    viewPosition(talkFX.position) ;
    talkFX.position.z += height() ;
    talkFX.update() ;
    rendering.addClient(talkFX) ;
    //*/
  }
  
  
  protected float spriteScale() {
    //
    //  TODO:  make this a general scaling vector, and incorporate other
    //  physical traits.
    final int stage = health.agingStage() ;
    if (stage == 0) return 0.8f ;
    if (stage == 2) return 0.95f ;
    return 1f ;
  }


  public String fullName() {
    return career.fullName() ;//+" the "+career.vocation().name ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return faceComposite(this, UI) ;
  }
  

  public String helpInfo() {
    return null ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new ActorPanel(UI, this, false) ;
  }
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "OUTFIT", "SKILLS", "PSYCH" } ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    if (categoryID == 0) describeStatus(d, UI) ;
    if (categoryID == 1) describeOutfit(d, UI) ;
    if (categoryID == 2) describeSkills(d, UI) ;
    if (categoryID == 3) describePsych (d, UI) ;
  }
  
  
  private void describeStatus(Description d, BaseUI UI) {
    //
    //  Describe your job, place of work, and current residence:
    d.append("Occupation: ") ;
    if (psyche.work() != null) {
      d.append(psyche.work()) ;
    }
    else d.append("Unemployed") ;
    d.append("\nResidence: ") ;
    if (psyche.home() != null) {
      d.append(psyche.home()) ;
    }
    else d.append("Homeless") ;
    //
    //  Describe your current health, outlook, or special FX.
    d.append("\n\nCondition: ") ;
    d.append("\n  Injury: "+health.injuryLevel()) ;
    d.append("\n  Fatigue: "+health.fatigueLevel()) ;
    d.append("\n  Stress: "+health.stressLevel()) ;
    //*
    final Batch <Condition> conditions = traits.conditions() ;
    //if (conditions.size() == 0) d.append("\n  Okay") ;
    for (Condition c : conditions) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(c)) ;
    }
    //*/
    //
    //  Describe your current assignment or undertaking.
    d.append("\n\nCurrently:") ;
    final Behaviour rootB = psyche.rootBehaviour() ;
    if (rootB != null) {
      d.append("\n  ") ;
      rootB.describeBehaviour(d) ;
    }
    else d.append("\n  "+health.stateName()) ;
    
    d.append("\n  Age: "+health.exactAge()) ;
  }
  
  
  private void describeOutfit(Description d, BaseUI UI) {
    //
    //
    //  Describe your current weapon or implement, and armour or dress.  Rate
    //  your current encumbrance, and any other special bonuses or effects.
    d.append("Inventory: ") ;

    final Item device = gear.deviceEquipped() ;
    if (device != null) d.append("\n  "+device) ;
    else d.append("\n  No device") ;
    d.append(" ("+((int) gear.attackDamage())+")") ;
    
    final Item outfit = gear.outfitEquipped() ;
    if (outfit != null) d.append("\n  "+outfit) ;
    else d.append("\n  Nothing worn") ;
    d.append(" ("+((int) gear.armourRating())+")") ;
    
    d.append("\n") ;
    final Batch <Item> carried = gear.allItems() ;
    if (carried.size() > 0) {
      d.append("   Carrying:") ;
      for (Item item : carried) {
        d.append("\n    "+item) ;
      }
    }
    else d.append("  Nothing carried") ;
    
    d.append("\n  "+((int) gear.credits())+" Credits") ;
    /*
    d.append("\n  Fuel Cells: "+((int) gear.fuelCells)) ;
    d.append("\n  Rations: "+((int) gear.currentRations)) ;
    //*/
    
    //  Describe your overall appearance and physique.
    d.append("\n\nPhysique: ") ;
    d.append("\n  "+health.agingDesc()) ;
    for (Trait t : traits.physique()) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(t)) ;
    }
    d.append("\n  "+BLOOD_TRAITS[bloodID(this)]) ;
  }
  
  
  private void describeSkills(Description d, BaseUI UI) {
    //
    //  Describe attributes, skills and psyonic techniques.
    d.append("Attributes: ") ;
    for (Skill skill : traits.attributes()) {
      final int level = (int) traits.trueLevel(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.attDesc(level), Skill.skillTone(level)) ;
    }
    d.append("\n\nSkills: ") ;
    for (Skill skill : traits.skillSet()) {
      final int level = (int) traits.trueLevel(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.skillDesc(level), Skill.skillTone(level)) ;
    }
    /*
    d.append("\n\nTechniques: ") ;
    for (Power p : traits.powers()) {
      d.append("\n  "+p.name) ;
    }
    //*/
  }
  
  
  private void describePsych(Description d, BaseUI UI) {
    //
    //  Describe background, personality, relationships and memories.
    //  TODO:  Allow for a chain of arbitrary vocations in a career?
    d.append("Background: ") ;
    d.append("\n  "+traits.levelDesc(ORIENTATION)) ;
    d.append(" "+traits.levelDesc(GENDER)) ;
    d.append("\n  "+career.birth()+" on "+career.homeworld()) ;
    d.append("\n  Trained as "+career.vocation()) ;
    
    d.append("\n\nPersonality: ") ;
    for (Trait t : traits.personality()) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(t)) ;
    }
    
    d.append("\n\nRelationships: ") ;
    for (Relation r : psyche.relations()) {
      d.append("\n  ") ;
      d.append(r.subject) ;
      d.append(" ("+r.descriptor()+")") ;
    }
    
    /*
    d.append("\n\nMemories: ") ;
    //  TODO:  Refer to Memories directly, so you can reconstruct the plan
    //  from the signature, and link to affected targets/subjects.
    int numM = 0 ;
    for (Class planClass : psyche.recentActivities()) {
      d.append("\n  "+planClass.getSimpleName()) ;
      if (++numM >= 5) break ;
    }
    //*/
  }
}









