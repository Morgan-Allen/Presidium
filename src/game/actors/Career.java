


package src.game.actors ;
import src.game.campaign.Naming;
import src.game.common.* ;
import src.util.*  ;



public class Career {
  
  
  Actor bound ;
  Vocation vocation, birth, homeworld ;
  String fullName = "" ;
  
  
  Career() {
  }
  
  public void loadState(Session s) throws Exception {
    bound = (Actor) s.loadObject() ;
    vocation = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    birth = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    homeworld = Vocation.ALL_VOCATIONS[s.loadInt()] ;
    fullName = s.loadString() ;
  }
  
  public void saveState(Session s) throws Exception {
    s.saveObject(bound) ;
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
    bound = actor ;
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
    //  For now, we apply gender at random, though this might be tweaked a bit
    //  later.  We also assign some random personality or physical traits.
    while (true) {
      final int numP = actor.traits.personality().size() ;
      if (numP >= 5) break ;
      final Trait t = (Trait) Rand.pickFrom(Trait.ALL_PERSONALITY_TRAITS) ;
      actor.traits.setLevel(t, Rand.range(-2, 2)) ;
      if (numP >= 3 && Rand.yes()) break ;
    }
    actor.traits.setLevel(Trait.HANDSOME, Rand.rangeAvg(-2, 4, 2)) ;
    actor.traits.setLevel(Trait.TALL    , Rand.rangeAvg(-3, 3, 2)) ;
    actor.traits.setLevel(Trait.STOUT   , Rand.rangeAvg(-4, 2, 2)) ;
    actor.traits.setLevel(Trait.GENDER, Rand.yes() ? "Female" : "Male") ;
    
    for (String name : Naming.namesFor(actor)) fullName+=" "+name ;
    I.say("Full name: "+fullName) ;
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










