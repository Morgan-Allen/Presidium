


package src.game.campaign ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;



public class BaseProfiles {
  
  
  
  final Base base ;
  Table <Actor, Profile> allProfiles = new Table <Actor, Profile> (1000) ;
  
  
  
  public BaseProfiles(Base base) {
    this.base = base ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Profile p = Profile.loadProfile(s) ;
      allProfiles.put(p.actor, p) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(allProfiles.size()) ;
    for (Profile p : allProfiles.values()) {
      Profile.saveProfile(p, s) ;
    }
  }
  
  
  
  public Profile profileFor(Actor actor) {
    Profile match = allProfiles.get(actor) ;
    if (match == null) { allProfiles.put(actor, match = new Profile(actor)) ; }
    return match ;
  }
  
  
}











