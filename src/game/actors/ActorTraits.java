/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.actors ;
import src.game.common.* ;
import src.util.* ;
import src.user.* ;



public class ActorTraits implements ActorConstants {
  
  
  
  /**  Common fields, constructors, and save/load methods-
    */
  final Actor actor ;
  final private Table <Trait, Level> levels = new Table <Trait, Level> () ;
  
  
  private static class Level {
    float value ;
  }
  
  
  protected ActorTraits(Actor actor) {
    this.actor = actor ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Trait type = ALL_TRAIT_TYPES[s.loadInt()] ;
      final Level level = new Level() ;
      level.value = s.loadFloat() ;
      levels.put(type, level) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(levels.size()) ;
    for (Trait type : levels.keySet()) {
      s.saveInt(type.traitID) ;
      final Level level = levels.get(type) ;
      s.saveFloat(level.value) ;
    }
  }
  
  
  
  /**  Methods for querying and modifying the levels of assorted traits-
    */
  public float level(Trait type) {
    Level level = levels.get(type) ;
    return (level == null) ? 0 : level.value ;
  }
  
  
  public boolean hasTrait(Trait type, String desc) {
    int i = 0 ; for (String s : type.descriptors) {
      if (desc.equals(s)) {
        final float value = type.descValues[i] ;
        if (value > 0) return level(type) >= value ;
        else return level(type) <= value ;
      }
      else i++ ;
    }
    return false ;
  }
  
  
  public void setLevel(Trait type, float toLevel) {
    Level level = levels.get(type) ;
    if (level == null) levels.put(type, level = new Level()) ;
    level.value = toLevel ;
  }
  
  
  public float incLevel(Trait type, float boost) {
    Level level = levels.get(type) ;
    if (level == null) levels.put(type, level = new Level()) ;
    level.value += boost ;
    return level.value ;
  }
  
  
  public void setLevel(Trait type, String desc) {
    int i = 0 ; for (String s : type.descriptors) {
      if (desc.equals(s)) {
        setLevel(type, type.descValues[i]) ;
        return ;
      }
      else i++ ;
    }
  }
  
  
  private Batch <Trait> getMatches(Batch <Trait> traits, Trait[] types) {
    if (traits == null) traits = new Batch <Trait> () ;
    for (Trait t : types) {
      final Level l = levels.get(t) ;
      if (l == null || Math.abs(l.value) < 0.5f) continue ;
      traits.add(t) ;
    }
    return traits ;
  }
  
  
  public Batch <Trait> personality() {
    return getMatches(null, ActorConstants.ALL_PERSONALITY_TRAITS) ;
  }
  
  
  public Batch <Trait> physique() {
    return getMatches(null, ActorConstants.ALL_PHYSICAL_TRAITS) ;
  }
  
  
  public Batch <Skill> attributes() {
    return (Batch) getMatches(null, ActorConstants.ALL_ATTRIBUTES) ;
  }
  
  
  public Batch <Skill> skillSet() {
    final Batch <Trait> matches = new Batch <Trait> () ;
    getMatches(matches, ActorConstants.ALL_INSTINCTS ) ;
    getMatches(matches, ActorConstants.ALL_PHYSICAL  ) ;
    getMatches(matches, ActorConstants.ALL_SENSITIVE ) ;
    getMatches(matches, ActorConstants.ALL_COGNITIVE ) ;
    getMatches(matches, ActorConstants.ALL_PYSONIC   ) ;
    return (Batch) matches ;
  }
  
  
  
  /**  Methods for performing actual skill tests against both static and active
    *  opposition-
    */
  public float chance(
    Skill checked,
    Actor b, Skill opposed,
    float bonus
  ) {
    float bonusA = level(checked) + Math.max(0, bonus) ;
    float bonusB = 0 - Math.min(0, bonus) ;
    if (b != null) bonusB += b.traits.level(opposed) ;
    final float chance = Visit.clamp(bonusA + 10 - bonusB, 0, 20) / 20 ;
    ///I.say("Test chance for "+actor+" is: "+chance) ;
    return chance ;
  }
  
  
  public int test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float duration,
    int range
  ) {
    final float chance = chance(checked, b, opposed, bonus) ;
    int success = 0 ;
    for (int tried = 0 ; tried < range ; tried++) {
      if (Rand.num() < chance) success++ ;
    }
    final float practice = chance * (1 - chance) * duration / 10 ;
    practice(checked, practice) ;
    if (b != null) b.traits.practice(opposed, practice) ;
    return success ;
  }
  
  
  public boolean test(
    Skill checked, Actor b, Skill opposed,
    float bonus, float fullXP
  ) {
    return test(checked, b, opposed, bonus, fullXP, 1) > 0 ;
  }
  
  
  public boolean test(Skill checked, float difficulty, float duration) {
    return test(checked, null, null, 0 - difficulty, duration, 1) > 0 ;
  }
  
  
  public void practice(Skill skillType, float practice) {
    incLevel(skillType, practice / (level(skillType) + 1)) ;
    if (skillType.parent != null) practice(skillType.parent, practice) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d) {
    for (Skill s : attributes()) {
      d.append("\n  "+s.name+" "+((int) level(s))) ;
    }
    d.append("\n") ;
    for (Skill s : skillSet()) {
      d.append("\n  "+s.name+" "+((int) level(s))) ;
    }
    d.append("\n") ;
    for (Trait t : personality()) {
      d.append("\n  "+Trait.descriptionFor(t, level(t))) ;
    }
    d.append("\n") ;
    for (Trait t : physique()) {
      d.append("\n  "+Trait.descriptionFor(t, level(t))) ;
    }
  }
}














