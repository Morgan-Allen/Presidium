/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;



public class DrillingIsotopes extends Drilling {
  
  
  public DrillingIsotopes(Base belongs) {
    super(belongs, 0) ;
  }
  
  public DrillingIsotopes(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
}
