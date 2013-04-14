/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.util.* ;



public class Trait implements Condition, ActorConstants {
  
  
  
  static Batch <Trait>
    skillsSoFar = new Batch <Trait> (),
    allSkills   = new Batch <Trait> () ;
  
  static Trait[] from(Batch <Trait> types) {
    final Trait t[] = (Trait[]) types.toArray(Trait.class) ;
    types.clear() ;
    return t ;
  }
  
  static Skill[] skillsSoFar() {
    final Skill t[] = (Skill[]) skillsSoFar.toArray(Skill.class) ;
    skillsSoFar.clear() ;
    return t ;
  }
  
  
  private static int nextID = 0 ;
  final public int traitID ;
  
  final int type ;
  final int minVal, maxVal ;
  final String descriptors[] ;
  final int descValues[] ;
  
  
  protected Trait(int type, String... descriptors) {
    this.traitID = nextID++ ;
    this.type = type ;
    this.descriptors = descriptors ;
    this.descValues = new int[descriptors.length] ;
    
    int zeroIndex = 0, min = 100, max = 0, val ;
    for (String s : descriptors) {
      if (s == null) break ;
      else zeroIndex++ ;
    }
    for (int i = descriptors.length ; i-- > 0 ;) {
      val = descValues[i] = descriptors.length - (i + zeroIndex + 1) ;
      if (val > max) max = val ;
      if (val < min) min = val ;
    }
    this.minVal = min ;
    this.maxVal = max ;

    skillsSoFar.add(this) ;
    allSkills.add(this) ;
  }
  
  
  /**  Returns the appropriate description for the given trait-level.
    */
  public static String descriptionFor(Trait trait, int level) {
    for (int i = 0 ; i < trait.descriptors.length ; i++) {
      if (trait.descValues[i] == level) return trait.descriptors[i] ;
    }
    return null ;
  }
  

  public void affect(Actor a) {}
}
