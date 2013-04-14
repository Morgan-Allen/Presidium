/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.WorldSections.Section ;
import src.util.* ;



public class WorldPathing {
  
  
  
  static class Path {
    Area from, to ;
    int lastCheckTime ;
    float cost ;
  }
  
  
  static class Area {
    Section section ;
    Area borders[] ;
    Tile core ;
    Path paths[] ;
  }
  
  
  //
  //  Basically, all pathing attempts should be between adjacent sections, and
  //  may not escape those sections.  If such a pathing attempt fails, it
  //  follows that either the origin or target tiles are 'walled off', and
  //  need a separate area.  Do this on a 'need to know' basis.
  
  //  TODO:  Wait.  What happens when you need to save/load?
  
  
  public Tile[] refreshPath(Tile origin, Tile destination) {
    
    
    //  First, get the area path (and ensure it exists.)
    
    
    //  Then, take the first 2/3 steps, and path between them on a tile level.
    
    //
    //  ...You'll have to test this explicitly.
    
    
    return null ;
  }
}













