/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class SeedTailoring extends Plan implements BuildConstants {
  
  
  final Plantation station ;
  
  
  SeedTailoring(Actor actor, Plantation plantation) {
    super(actor, plantation) ;
    this.station = plantation ;
  }
  
  
  public SeedTailoring(Session s) throws Exception {
    super(s) ;
    station = (Plantation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(station) ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  protected Behaviour getNextStep() {
    return null ;
  }


  public void describeBehaviour(Description d) {
    d.append("Tailoring seed at ") ;
    d.append(station) ;
  }
}






