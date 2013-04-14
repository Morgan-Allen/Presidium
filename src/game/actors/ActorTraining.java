/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.actors ;
import src.game.common.* ;
import src.util.* ;
import src.user.* ;



public class ActorTraining implements ActorConstants {
  
  
  
  /**  Common fields, constructors, and save/load methods-
    */
  final Actor actor ;
  final private Table <Trait, Level> levels = new Table <Trait, Level> () ;
  
  
  private static class Level {
    float practiced ;
  }
  
  
  protected ActorTraining(Actor actor) {
    this.actor = actor ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Trait type = ALL_TRAIT_TYPES[s.loadInt()] ;
      final Level level = new Level() ;
      level.practiced = s.loadFloat() ;
      levels.put(type, level) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(levels.size()) ;
    for (Trait type : levels.keySet()) {
      s.saveInt(type.traitID) ;
      final Level level = levels.get(type) ;
      s.saveFloat(level.practiced) ;
    }
  }
  
  
  
  /**  Methods for querying and modifying the levels of assorted traits-
    */
  public float level(Trait type) {
    Level level = levels.get(type) ;
    return (level == null) ? 0 : level.practiced ;
  }
  
  
  public void raiseTo(float toLevel, Trait type) {
    Level level = levels.get(type) ;
    if (level == null) levels.put(type, level = new Level()) ;
    level.practiced = toLevel ;
  }
  
  
  public float raiseBy(float boost, Trait type) {
    Level level = levels.get(type) ;
    if (level == null) levels.put(type, level = new Level()) ;
    level.practiced += boost ;
    return level.practiced ;
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
    if (b != null) bonusB += b.training.level(opposed) ;
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
    if (b != null) b.training.practice(opposed, practice) ;
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
    raiseBy(practice / (level(skillType) + 1), skillType) ;
    if (skillType.parent != null) practice(skillType.parent, practice) ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d) {
    Batch <Skill> skills = new Batch <Skill> () ;
    Batch <Trait> traits = new Batch <Trait> () ;
    
    for (Trait t : ActorConstants.ALL_TRAIT_TYPES) {
      final Level l = levels.get(t) ;
      if (l == null) continue ;
      if (t instanceof Skill) skills.add((Skill) t) ;
      else traits.add(t) ;
    }
    
    //d.append("\n\nSkills: ") ;
    for (Skill s : skills) {
      final int l = (int) levels.get(s).practiced ;
      d.append("\n  "+s.name+" "+l) ;
    }
    //d.append("\n\nTraits: ") ;
    for (Trait t : traits) {
      final int l = (int) levels.get(t).practiced ;
      if (l == 0) continue ;
      d.append("\n  "+Trait.descriptionFor(t, l)) ;
    }
  }
}














