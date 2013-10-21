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
import src.game.tactical.Combat;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.sfx.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;




public class Human extends Actor implements Abilities {
  
  
  /**  Methods and constants related to preparing media and sprites-
    */
  final static String
    FILE_DIR = "media/Actors/human/",
    XML_PATH = FILE_DIR+"HumanModels.xml" ;
  final public static Model
    MODEL_MALE = MS3DModel.loadMS3D(
      Human.class, FILE_DIR, "male_final.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "MalePrime"),
    MODEL_FEMALE = MS3DModel.loadMS3D(
      Human.class, FILE_DIR, "female_final.ms3d", 0.025f
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
  
  
  public Human(Background vocation, Base base) {
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
  
  
  protected ActorMind initAI() { return new HumanMind(this) ; }
  
  public Background vocation() { return career.vocation() ; }
  
  public Career career() { return career ; }
  
  public Object species() { return Species.HUMAN ; }
  
  public void setVocation(Background b) {
    career.recordVocation(b) ;
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
      final float blood = c.traits.traitLevel(BLOOD_TRAITS[i]) ;
      if (blood > highest) { ID = i ; highest = blood ; }
    }
    return ID ;
  }
  
  
  private static Composite faceComposite(Human c, HUD UI) {
    
    final Composite composite = new Composite(UI) ;
    final int bloodID = bloodID(c) ;
    final boolean male = c.traits.male() ;
    final int ageStage = c.health.agingStage() ;
    ///I.say("Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage) ;
    
    int faceOff[], bloodOff[] = BLOOD_FACE_OFFSETS[bloodID] ;
    if (ageStage == 0) faceOff = CHILD_FACE_OFF ;
    else {
      int looks = (int) c.traits.traitLevel(Trait.HANDSOME) + 2 - ageStage ;
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
      
      Texture portrait = c.career.vocation().portraitFor(c) ;
      if (portrait == null) portrait = c.career.birth().portraitFor(c) ;
      composite.addLayer(portrait, 0, 0, 1, 1) ;
    }
    
    return composite ;
  }
  
  
  private static void initSpriteFor(Human c) {
    final boolean male = c.traits.male() ;
    final JointSprite s ;
    if (c.sprite() == null) {
      s = (JointSprite) (male ? MODEL_MALE : MODEL_FEMALE).makeSprite() ;
      c.attachSprite(s) ;
    }
    else s = (JointSprite) c.sprite() ;
    
    s.overlayTexture(BLOOD_SKINS[bloodID(c)]) ;
    Texture costume = c.career.vocation().costumeFor(c) ;
    if (costume == null) costume = c.career.birth().costumeFor(c) ;
    s.overlayTexture(costume) ;
    toggleSpriteGroups(c, s) ;
  }
  
  
  //
  //  You might want to call this at more regular intervals?
  private static void toggleSpriteGroups(Human human, JointSprite sprite) {
    for (String groupName : ((JointModel) sprite.model()).groupNames()) {
      boolean valid = "Main Body".equals(groupName) ;
      final DeviceType DT = human.gear.deviceType() ;
      if (DT != null && DT.groupName.equals(groupName)) valid = true ;
      if (! valid) sprite.toggleGroup(groupName, false) ;
    }
  }
  
  
  
  /**  More usual rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    
    //
    //  If you're in combat, equip the right gear-
    final DeviceType DT = gear.deviceType() ;
    final boolean IC = isDoing(Combat.class, null) ;
    if (DT != null) ((JointSprite) sprite()).toggleGroup(DT.groupName, IC) ;
    
    //
    //  If you're in dialogue, and selected, render the chat-
    if (isDoing(Dialogue.class, null) && BaseUI.isPicked(this)) {
      final Dialogue d = (Dialogue) mind.rootBehaviour() ;
      d.chat.position.setTo(this.position) ;
      d.chat.position.z += this.height() ;
      d.chat.update() ;
      rendering.addClient(d.chat) ;
    }
    
    super.renderFor(rendering, base) ;
  }
  
  
  protected float spriteScale() {
    //
    //  TODO:  make this a general 3D scaling vector, and incorporate other
    //  physical traits.
    final int stage = health.agingStage() ;
    final float scale = (float) Math.pow(traits.scaleLevel(TALL), 0.1f) ;
    if (stage == 0) return 0.8f * scale ;
    if (stage >= 2) return 0.95f * scale ;
    return 1f * scale ;
  }


  public String fullName() {
    return career.fullName() ;
  }
  
  
  public Composite portrait(HUD UI) {
    return faceComposite(this, UI) ;
  }
  

  public String helpInfo() {
    return null ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new ActorPanel(UI, this, false) ;
  }
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "SKILLS", "PROFILE" } ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 0) describeStatus(d, UI) ;
    ///if (categoryID == 1) describeOutfit(d, UI) ;
    if (categoryID == 1) describeSkills(d, UI) ;
    if (categoryID == 2) describeProfile(d, UI) ;
  }
  
  
  //
  //  Some of this could be outsourced to the ActorGear classes, et cetera.
  private void describeStatus(Description d, HUD UI) {
    //
    //  Describe your job, place of work, and current residence:
    d.append("Is: ") ; describeStatus(d) ;
    final String VN = vocation().nameFor(this) ;
    d.append("\nVocation: ") ;
    if (mind.work() != null) {
      d.append(VN+" at ") ;
      d.append(mind.work()) ;
    }
    else d.append("Unemployed "+VN) ;
    d.append("\nResidence: ") ;
    if (mind.home() != null) {
      d.append(mind.home()) ;
    }
    else d.append("Homeless") ;
    //
    //  Describe your current health, outlook, or special FX.
    d.append("\n\nCondition: ") ;
    final Batch <String> healthDesc = health.conditionsDesc() ;
    for (String desc : healthDesc) {
      d.append("\n  "+desc) ;
    }
    final Batch <Condition> conditions = traits.conditions() ;
    for (Condition c : conditions) {
      final String desc = traits.levelDesc(c) ;
      if (desc != null) d.append("\n  "+desc) ;
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay") ;
    }
    //
    //  Describe your current gear and anything carried.
    d.append("\n\nInventory: ") ;
    
    final Item device = gear.deviceEquipped() ;
    if (device != null) d.append("\n  "+device) ;
    else d.append("\n  No device") ;
    d.append(" ("+((int) gear.attackDamage())+")") ;
    
    final Item outfit = gear.outfitEquipped() ;
    if (outfit != null) d.append("\n  "+outfit) ;
    else d.append("\n  Nothing worn") ;
    d.append(" ("+((int) gear.armourRating())+")") ;
    
    for (Item item : gear.allItems()) d.append("\n  "+item) ;
    d.append("\n  "+((int) gear.credits())+" Credits") ;
    ///d.append("\n  Fuel Cells: "+((int) gear.fuelCells)) ;
    ///d.append("\n  Rations: "+((int) gear.currentRations)) ;
  }
  
  
  private void describeSkills(Description d, HUD UI) {
    //
    //  Describe attributes, skills and psyonic techniques.
    d.append("Attributes: ") ;
    for (Skill skill : traits.attributes()) {
      final int level = (int) traits.traitLevel(skill) ;
      final int bonus = (int) traits.effectBonus(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.attDesc(level), Skill.skillTone(level)) ;
      d.append((bonus >= 0 ? " (+" : " (-")+Math.abs(bonus)+")") ;
    }
    d.append("\n\nSkills: ") ;
    final List <Skill> sorting = new List <Skill> () {
      protected float queuePriority(Skill skill) {
        return traits.traitLevel(skill) ;
      }
    } ;
    
    for (Skill skill : traits.skillSet()) sorting.add(skill) ;
    sorting.queueSort() ;
    for (Skill skill : sorting) {
      final int level = (int) traits.traitLevel(skill) ;
      final int bonus = (int) (
          traits.rootBonus(skill) +
          traits.effectBonus(skill)
      ) ;
      final Colour tone = Skill.skillTone(level) ;
      d.append("\n  "+skill.name+" "+level+" ", tone) ;
      d.append((bonus >= 0 ? "(+" : "(-")+Math.abs(bonus)+")") ;
    }
    
    /*
    d.append("\n\nTechniques: ") ;
    for (Power p : traits.powers()) {
      d.append("\n  "+p.name) ;
    }
    //*/
  }
  
  
  private void describeProfile(Description d, HUD UI) {
    //
    //  Describe background, personality, relationships and memories.
    //  TODO:  Allow for a chain of arbitrary vocations in a career?
    d.append("Background: ") ;
    d.append("\n  "+career.birth()+" on "+career.homeworld()) ;
    d.append("\n  Trained as "+career.vocation().nameFor(this)) ;
    d.append("\n  "+traits.levelDesc(ORIENTATION)) ;
    d.append(" "+traits.levelDesc(GENDER)) ;
    d.append("\n  Age: "+health.exactAge()+" ("+health.agingDesc()+")") ;
    
    d.appendList("\n\nAppearance: " , descTraits(traits.physique   ())) ;
    d.appendList("\n\nPersonality: ", descTraits(traits.personality())) ;
    d.appendList("\n\nMutations: "  , descTraits(traits.mutations  ())) ;
    
    d.append("\n\nRelationships: ") ;
    for (Relation r : mind.relations()) {
      d.append("\n  ") ;
      d.append(r.subject) ;
      d.append(" ("+r.descriptor()+")") ;
    }
  }
  
  private Batch <String> descTraits(Batch <Trait> traits) {
    final Actor actor = this ;
    final List <Trait> sorting = new List <Trait> () {
      protected float queuePriority(Trait r) {
        return 0 - actor.traits.traitLevel(r) ;
      }
    } ;
    for (Trait t : traits) sorting.queueAdd(t) ;
    final Batch <String> desc = new Batch <String> () ;
    for (Trait t : sorting) desc.add(this.traits.levelDesc(t)) ;
    return desc ;
  }
}








