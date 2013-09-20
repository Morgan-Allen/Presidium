/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.campaign.System ;
import src.game.common.* ;
import src.util.* ;



public class Career implements ActorConstants {
  
  
  
  private Actor subject ;
  private Background vocation, birth, homeworld ;
  private String fullName = null ;
  
  
  public Career(Background root) {
    vocation = root ;
  }
  
  
  public Career(Actor subject) {
    this.subject = subject ;
  }
  
  
  public void loadState(Session s) throws Exception {
    subject = (Actor) s.loadObject() ;
    vocation = Background.ALL_BACKGROUNDS[s.loadInt()] ;
    birth = Background.ALL_BACKGROUNDS[s.loadInt()] ;
    homeworld = Background.ALL_BACKGROUNDS[s.loadInt()] ;
    fullName = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(subject) ;
    s.saveInt(vocation.ID) ;
    s.saveInt(birth.ID) ;
    s.saveInt(homeworld.ID) ;
    s.saveString(fullName) ;
  }
  
  
  public Background vocation() {
    return vocation ;
  }
  
  
  public Background birth() {
    return birth ;
  }
  
  
  public Background homeworld() {
    return homeworld ;
  }
  
  public String fullName() {
    return fullName ;
  }
  
  
  
  /**  Binds this career to a specific in-world actor and configures their
    *  physique, aptitudes and motivations:
    */
  public void applyCareer(Human actor) {
    subject = actor ;
    Background root = vocation ;
    //
    //  Firstly, determine a basic background suitable to the root vocation-
    if (root.standing == Background.RULER_CLASS && Rand.index(10) > 0) {
      birth = Background.HIGH_BIRTH ;
      if (Visit.arrayIncludes(Background.RULING_POSITIONS, vocation)) {
        homeworld = actor.base().commerce.homeworld() ;
      }
    }
    else {
      final Batch <Float> weights = new Batch <Float> () ;
      for (Background v : Background.OPEN_CLASSES) {
        weights.add(rateSimilarity(root, v)) ;
      }
      birth = (Background) Rand.pickFrom(
        Background.OPEN_CLASSES, weights.toArray()
      ) ;
    }
    if (homeworld == null) {
      final Batch <Float> weights = new Batch <Float> () ;
      for (Background v : Background.ALL_PLANETS) {
        weights.add(rateSimilarity(root, v)) ;
      }
      homeworld = (Background) Rand.pickFrom(
        Background.ALL_PLANETS, weights.toArray()
      ) ;
    }
    applyVocation(homeworld, actor) ;
    applyVocation(birth    , actor) ;
    applyVocation(vocation , actor) ;
    setupAttributes(actor) ;
    //
    //  We top up basic attributes to match.
    //
    //  TODO:  Try adding 1 - 3 family members, possibly as potential
    //  co-migrants?
    actor.traits.initDNA(0) ;
    actor.health.setupHealth(
      Visit.clamp(Rand.avgNums(2), 0.26f, 0.94f),
      1, 0
    ) ;
    //
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality and/or physical traits.
    while (true) {
      final int numP = actor.traits.personality().size() ;
      if (numP >= 5) break ;
      final Trait t = (Trait) Rand.pickFrom(PERSONALITY_TRAITS) ;
      actor.traits.incLevel(t, Rand.range(-2, 2)) ;
      if (numP >= 3 && Rand.yes()) break ;
    }
    actor.traits.incLevel(HANDSOME, Rand.rangeAvg(-3, 3, 2)) ;
    actor.traits.incLevel(TALL    , Rand.rangeAvg(-3, 3, 2)) ;
    actor.traits.incLevel(STOUT   , Rand.rangeAvg(-3, 3, 2)) ;
    applySex(actor) ;
    //
    //  Finally, specify name and (TODO:) a few other details of appearance.
    for (String name : Wording.namesFor(actor)) {
      if (fullName == null) fullName = name ;
      else fullName+=" "+name ;
    }
    ///I.say("Full name: "+fullName) ;
    //
    //  Along with current wealth and equipment-
    applyGear(vocation, actor) ;
  }
  
  
  private void setupAttributes(Actor actor) {
    for (Skill s : actor.traits.skillSet()) {
      final float level = actor.traits.trueLevel(s) ;
      actor.traits.raiseLevel(s.parent, level + Rand.index(10) - 5) ;
      if (s.form == FORM_COGNITIVE) {
        actor.traits.raiseLevel(INTELLECT, 5 + Rand.index(10)) ;
        actor.traits.raiseLevel(WILL     , 5 + Rand.index(10)) ;
      }
      if (s.form == FORM_SENSITIVE) {
        actor.traits.raiseLevel(REFLEX   , 5 + Rand.index(10)) ;
        actor.traits.raiseLevel(INSIGHT  , 5 + Rand.index(10)) ;
      }
      if (s.form == FORM_PHYSICAL) {
        actor.traits.raiseLevel(VIGOUR   , 5 + Rand.index(10)) ;
        actor.traits.raiseLevel(BRAWN    , 5 + Rand.index(10)) ;
      }
    }
    for (int i = 0 ; i < ATTRIBUTES.length ; i++) {
      final Skill att = ATTRIBUTES[i] ;
      actor.traits.incLevel(att, Rand.index(10) - 5) ;
      final float minVal = Math.max(
        actor.traits.trueLevel(ATTRIBUTES[(i + 1) % 6]),
        actor.traits.trueLevel(ATTRIBUTES[(i + 5) % 6])
      ) / 2f ;
      actor.traits.raiseLevel(att, minVal) ;
      actor.traits.raiseLevel(att, 5 + Rand.index(10)) ;
    }
  }
  
  
  private void applySex(Human actor) {
    //
    //  TODO:  Some of these traits need to be rendered 'dormant' in younger
    //  citizens...
    float ST = Visit.clamp(Rand.rangeAvg(-1, 3, 2), 0, 3) ;
    if (Rand.index(20) == 0) ST *= -1 ;
    if (Rand.yes()) {
      actor.traits.setLevel(GENDER, "Female") ;
      actor.traits.setLevel(FEMININE, ST) ;
    }
    else {
      actor.traits.setLevel(GENDER, "Male") ;
      actor.traits.setLevel(FEMININE, 0 - ST) ;
    }
    actor.traits.setLevel(
      ORIENTATION,
      Rand.index(10) != 0 ? "Heterosexual" :
      (Rand.yes() ? "Homosexual" : "Bisexual")
    ) ;
  }
  
  
  //
  //  TODO:  Try incorporating these trait-FX into the rankings first.
  private void applySystem(System world, Actor actor) {
    //
    //  Assign skin texture based on prevailing climate-
    //  TODO:  Blend these a bit more, once you have the graphics in order.
    final Trait bloods[] = {
      DESERT_BLOOD,
      FOREST_BLOOD,
      TUNDRA_BLOOD,
      WASTES_BLOOD
    } ;
    Trait pickBlood = null ;
    for (int n = 4 ; n-- > 0 ;) {
      if (bloods[n] == world.climate) {
        final float roll = Rand.num() ;
        final int index ;
        if (roll < 0.65f) index = 0 ;
        else if (roll < 0.80f) index = 1 ;
        else if (roll < 0.95f) index = 3 ;
        else index = 2 ;
        pickBlood = bloods[(n + index) % 4] ;
      }
    }
    if (pickBlood != null) actor.traits.setLevel(pickBlood, 1) ;
    //
    //  Vary height/build based on gravity-
    //  TODO:  Have the citizen models actually reflect this.
    actor.traits.incLevel(TALL, Rand.num() * -1 * world.gravity) ;
    actor.traits.incLevel(STOUT, Rand.num() * 1 * world.gravity) ;
  }
  
  
  //
  //  TODO:  Rate the actor's similarity, rather than the vocation's?  And
  //  check for similar traits?
  private float rateSimilarity(Background next, Background prior) {
    float rating = 1 ;
    //
    //  Check for similar skills.
    for (Skill s : next.baseSkills.keySet()) {
      rating += rateSimilarity(s, next, prior) ;
    }
    for (Skill s : prior.baseSkills.keySet()) {
      rating += rateSimilarity(s, next, prior) ;
    }
    rating /= 1 + next.baseSkills.size() + prior.baseSkills.size() ;
    //
    //  Favour transition to more prestigous vocations-
    if (next.standing < prior.standing) return rating / 10f ;
    //if (next.standing == prior.standing) rating /= 5 ;
    return rating ;
  }
  
  
  private float rateSimilarity(Skill s, Background a, Background b) {
    Integer aL = a.baseSkills.get(s), bL = b.baseSkills.get(s) ;
    if (aL == null || bL == null) return 0 ;
    return (aL > bL) ? (bL / aL) : (aL / bL) ;
  }
  
  
  private void applyVocation(Background v, Actor actor) {
    ///I.say("Applying vocation: "+v) ;
    
    for (Skill s : v.baseSkills.keySet()) {
      final int level = v.baseSkills.get(s) ;
      actor.traits.raiseLevel(s, level + (Rand.num() * 5)) ;
    }
    
    for (Trait t : v.traitChances.keySet()) {
      final float chance = v.traitChances.get(t) ;
      if (Rand.num() < Math.abs(chance)) {
        ///I.say("Adding trait: "+t) ;
        actor.traits.incLevel(t, chance > 0 ? 1 : -1) ;
      }
      else actor.traits.incLevel(t, chance * Rand.num()) ;
    }
  }
  
  
  private void applyGear(Background v, Actor actor) {
    for (Service gear : v.gear) {
      if (gear instanceof DeviceType) {
        actor.gear.equipDevice(Item.withQuality(gear, 2)) ;
      }
      else if (gear instanceof OutfitType) {
        actor.gear.equipOutfit(Item.withQuality(gear, 2)) ;
      }
      else actor.gear.addItem(Item.withAmount(gear, 2)) ;
    }
    actor.gear.incCredits(50 + Rand.index(100)) ;
  }
}
















