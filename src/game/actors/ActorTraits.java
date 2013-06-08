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
  final static int 
    DNA_SIZE = 16,
    DNA_LETTERS = 26,
    MUTATION_PERCENT = 5 ;
  
  final Actor actor ;
  private String DNA = null ;
  private int geneHash = -1 ;
  private Table <Trait, Level> levels = new Table <Trait, Level> () ;
  
  
  
  private static class Level {
    float value ;
  }
  
  
  protected ActorTraits(Actor actor) {
    this.actor = actor ;
  }
  
  
  public void loadState(Session s) throws Exception {
    DNA = s.loadString() ;
    geneHash = s.loadInt() ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Trait type = ALL_TRAIT_TYPES[s.loadInt()] ;
      final Level level = new Level() ;
      level.value = s.loadFloat() ;
      levels.put(type, level) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveString(DNA) ;
    s.saveInt(geneHash) ;
    s.saveInt(levels.size()) ;
    for (Trait type : levels.keySet()) {
      s.saveInt(type.traitID) ;
      final Level level = levels.get(type) ;
      s.saveFloat(level.value) ;
    }
  }
  
  
  
  /**  Methods for dealing with DNA, used as a random seed for certain
    *  cosmetic or inherited traits, along with checks for inbreeding.
    */
  public void initDNA(int mutationMod, Actor... parents) {
    //
    //  First, if required, we sample the genetic material of the parents-
    final boolean free = parents == null || parents.length == 0 ;
    final char material[][] ;
    if (free) material = null ;
    else {
      material = new char[parents.length][] ;
      for (int n = parents.length ; n-- > 0 ;) {
        material[n] = parents[n].traits.DNA.toCharArray() ;
      }
    }
    //
    //  Then, we merge the source material along with a certain mutation rate.
    final StringBuffer s = new StringBuffer() ;
    for (int i = 0 ; i < DNA_SIZE ; i++) {
      if (free || Rand.index(100) < (MUTATION_PERCENT + mutationMod)) {
        s.append((char) ('a' + Rand.index(DNA_LETTERS))) ;
      }
      else {
        s.append(material[Rand.index(parents.length)][i]) ;
      }
    }
    DNA = s.toString() ;
    geneHash = DNA.hashCode() ;
  }
  
  
  public float inbreedChance(Actor a, Actor b) {
    final char[]
      mA = a.traits.DNA.toCharArray(),
      mB = b.traits.DNA.toCharArray() ;
    int matches = 0 ;
    for (int n = DNA_SIZE ; n-- > 0 ;) if (mA[n] == mB[n]) matches++ ;
    return matches * 1f / DNA_SIZE ;
  }
  
  
  public int geneValue(String gene, int range) {
    final int value = (gene.hashCode() + geneHash) % range ;
    return (value > 0) ? value : (0 - value) ;
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
  
  
  public String levelDesc(Trait type) {
    return Trait.descriptionFor(type, level(type)) ;
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
    return getMatches(null, ActorConstants.PERSONALITY_TRAITS) ;
  }
  
  
  public Batch <Trait> physique() {
    return getMatches(null, ActorConstants.PHYSICAL_TRAITS) ;
  }
  
  
  public Batch <Skill> attributes() {
    return (Batch) getMatches(null, ActorConstants.ATTRIBUTES) ;
  }
  
  
  public Batch <Skill> skillSet() {
    final Batch <Trait> matches = new Batch <Trait> () ;
    getMatches(matches, ActorConstants.INSTINCT_SKILLS ) ;
    getMatches(matches, ActorConstants.PHYSICAL_SKILLS ) ;
    getMatches(matches, ActorConstants.SENSITIVE_SKILLS) ;
    getMatches(matches, ActorConstants.COGNITIVE_SKILLS) ;
    getMatches(matches, ActorConstants.PYSONIC_SKILLS  ) ;
    return (Batch) matches ;
  }
  
  
  public Batch <Condition> conditions() {
    return (Batch) getMatches(null, ActorConstants.CONDITIONS) ;
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














