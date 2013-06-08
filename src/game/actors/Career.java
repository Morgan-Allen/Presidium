


package src.game.actors ;
import src.game.campaign.Naming;
import src.game.common.* ;
import src.util.*  ;



public class Career implements ActorConstants {
  
  
  Actor subject ;
  Vocation vocation, birth, homeworld ;
  String fullName = null ;
  
  
  Career() {
  }
  
  
  public void loadState(Session s) throws Exception {
    subject = (Actor) s.loadObject() ;
    vocation = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    birth = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    homeworld = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    fullName = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(subject) ;
    s.saveInt(vocation.ID) ;
    s.saveInt(birth.ID) ;
    s.saveInt(homeworld.ID) ;
    s.saveString(fullName) ;
  }
  
  
  public Vocation vocation() {
    return vocation ;
  }
  
  
  public Vocation birth() {
    return birth ;
  }
  
  
  public Vocation homeworld() {
    return homeworld ;
  }
  
  
  
  /**  TODO:  Make this the constructor?
    */
  protected void genCareer(Vocation root, Citizen actor) {
    subject = actor ;
    //
    //  Firstly, determine a basic background suitable to the root vocation-
    vocation = root ;
    Batch <Float> weights = new Batch <Float> () ;
    for (Vocation v : Vocation.ALL_CLASSES) {
      weights.add(rateSimilarity(root, v)) ;
    }
    birth = (Vocation) Rand.pickFrom(
      Vocation.ALL_CLASSES, weights.toArray()
    ) ;
    weights.clear() ;
    for (Vocation v : Vocation.ALL_PLANETS) {
      weights.add(rateSimilarity(root, v)) ;
    }
    homeworld = (Vocation) Rand.pickFrom(
      Vocation.ALL_PLANETS, weights.toArray()
    ) ;
    applyVocation(homeworld, actor) ;
    applyVocation(birth    , actor) ;
    applyVocation(vocation , actor) ;
    //
    //  TODO:  Try adding 1 - 3 family members, possibly as potential migrants?
    actor.traits.initDNA(0) ;
    //
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality and/or physical traits.
    while (true) {
      final int numP = actor.traits.personality().size() ;
      if (numP >= 5) break ;
      final Trait t = (Trait) Rand.pickFrom(PERSONALITY_TRAITS) ;
      actor.traits.setLevel(t, Rand.range(-2, 2)) ;
      if (numP >= 3 && Rand.yes()) break ;
    }
    actor.traits.setLevel(HANDSOME, Rand.rangeAvg(-2, 4, 2)) ;
    actor.traits.setLevel(TALL    , Rand.rangeAvg(-3, 3, 2)) ;
    actor.traits.setLevel(STOUT   , Rand.rangeAvg(-4, 2, 2)) ;
    applySex(actor) ;
    //
    //  Finally, specify name and (TODO:) a few other details of appearance.
    for (String name : Naming.namesFor(actor)) {
      if (fullName == null) fullName = name ;
      else fullName+=" "+name ;
    }
    I.say("Full name: "+fullName) ;
  }
  
  
  private void applySex(Citizen actor) {
    //
    //  TODO:  Some of these traits need to be rendered 'dormant' in younger
    //  citizens...
    if (Rand.yes()) {
      actor.traits.setLevel(GENDER, "Female") ;
      actor.traits.setLevel(FEMININE, Rand.rangeAvg(-1, 3, 2)) ;
    }
    else {
      actor.traits.setLevel(GENDER, "Male") ;
      actor.traits.setLevel(FEMININE, Rand.rangeAvg(-3, 1, 2)) ;
    }
    actor.traits.setLevel(
      ORIENTATION,
      Rand.index(10) != 0 ? "Heterosexual" :
      (Rand.yes() ? "Homosexual" : "Bisexual")
    ) ;
  }
  
  
  //
  //  TODO:  Rate the actor's similarity, rather than the vocation's?  And
  //  check for similar traits?
  private float rateSimilarity(Vocation next, Vocation prior) {
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
    //  Favour transition to more presitigous vocations-
    if (next.standing < prior.standing) return 0 ;
    //if (next.standing == prior.standing) rating /= 5 ;
    return rating ;
  }
  
  
  private float rateSimilarity(Skill s, Vocation a, Vocation b) {
    Integer aL = a.baseSkills.get(s), bL = b.baseSkills.get(s) ;
    if (aL == null || bL == null) return 0 ;
    return (aL > bL) ? (bL / aL) : (aL / bL) ;
  }
  
  
  private void applyVocation(Vocation v, Actor actor) {
    
    for (Skill s : v.baseSkills.keySet()) {
      final int level = v.baseSkills.get(s) ;
      actor.traits.setLevel(s, level + (Rand.num() * 5)) ;
    }
    
    for (Trait t : v.traitChances.keySet()) {
      final float chance = v.traitChances.get(t) ;
      if (Rand.num() < Math.abs(chance)) {
        I.say("Adding trait: "+t) ;
        actor.traits.incLevel(t, chance > 0 ? 1 : -1) ;
      }
      else actor.traits.incLevel(t, chance * Rand.num()) ;
    }
  }
}










