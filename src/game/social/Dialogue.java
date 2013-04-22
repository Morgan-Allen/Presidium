/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.social ;
import src.game.actors.* ;
import src.game.building.* ;



public class Dialogue {
  
  
  //
  //  There should be a few basic 'moves' here-
  //    First Impressions/Attraction
  //    Small Talk/Introduction
  //    Anecdotes/Gossip
  //    Advice/Assistance
  //    Goodbyes/Fight Escalation
  //    Kinship Modifiers/Exceptions
  
  
  
  
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
}

