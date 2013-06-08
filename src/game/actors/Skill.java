/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;



//
//  Consider folding these into the Trait class.
public class Skill extends Trait {
  
  final public String name ;
  final public int form ;
  final public Skill parent ;
  
  
  Skill(String name, int form, Skill parent) {
    super(SKILL, name) ;
    this.name = name ;
    this.form = form ;
    this.parent = parent ;
  }
}

