/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.social ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.Session;
import src.game.common.Session.Saveable;
import src.user.Description;



public class Dialogue extends Plan {
  
  
  /**  Fields, constructors and save/load methods-
    */
  
  
  public Dialogue(Actor actor, Saveable... signature) {
    super(actor, signature) ;
  }

  public Dialogue(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  //
  //  There should be a few basic 'moves' here-
  //    First Impressions/Attraction
  //    Small Talk/Introduction
  //    Anecdotes/Gossip
  //    Advice/Assistance
  //    Goodbyes/Fight Escalation
  //    Kinship Modifiers/Exceptions
  
  public static boolean interested(Actor a, Actor b) {
    //  If the actor's not doing anything more important, and/or haven't spoken
    //  recently, return true.
    return false ;
  }
  
  public float priorityFor(Actor actor) {
    return 0 ;
  }
  
  protected Behaviour getNextStep() {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
  }
}




/*
float calcImpressions(Citizen a, Citizen b) {
  
  return -1 ;
}


float calcAttraction(Citizen a, Citizen b) {
  return -1 ;
}


//  Conversational 'satisfaction' should be based on common interests-
//  traits and skills.

float calcAffinity(Citizen a, Citizen b) {
  float affinity = 0 ;
  
  return affinity ;
}
//*/

