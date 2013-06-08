/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;



//
//  Consider folding these into the Trait class.
public class Condition extends Trait {
  
  
  
  public Condition(String... names) {
    super(ActorConstants.CONDITION, names) ;
  }
}