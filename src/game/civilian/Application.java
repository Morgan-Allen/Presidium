

package src.game.civilian ;
import src.game.common.* ;
import src.game.actors.* ;



//
//  Used by actors to apply for either Missions or for Employment.
//
//  TODO:  In principle, actors need to be able to apply for jobs offworld as
//  well.  In which case, Employment may need to be more... abstract.  Eh, just
//  use the existing system.


public class Application implements Session.Saveable {
  
  final public Actor applies ;
  final public Background position ;
  final public Employment employer ;
  private int hiringFee ;
  
  
  public Application(Actor a, Background p, Employment e) {
    applies = a ;
    position = p ;
    employer = e ;
  }
  
  
  public Application(Session s) throws Exception {
    s.cacheInstance(this) ;
    applies = (Actor) s.loadObject() ;
    position = Background.ALL_BACKGROUNDS[s.loadInt()] ;
    employer = (Employment) s.loadObject() ;
    hiringFee = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(applies) ;
    s.saveInt(position.ID) ;
    s.saveObject(employer) ;
    s.saveInt(hiringFee) ;
  }
  
  
  public int hiringFee() {
    return hiringFee ;
  }
  
  
  public void setHiringFee(int fee) {
    this.hiringFee = fee ;
  }
  
  
  public boolean matches(Application a) {
    if (a == null) return false ;
    return
      a.applies == applies &&
      a.position == position &&
      a.employer == employer ;
  }
  
  
  public boolean valid() {
    return
      employer.inWorld() &&
      employer.structure().intact() &&
      employer.numOpenings(position) > 0 ;
  }
}



