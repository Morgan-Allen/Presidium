


package src.game.actors ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;



//
//  TODO:  You will need to do this, plus the sickbay, plus the surveillance
//  post, plus the whole arcology vs. edifice vs. arcade thing.  That's all.


public class Profile {
  
  
  final public Actor actor ;
  
  //Background position ;
  float wagesDue = 0 ;
  float lastPsychEval = -1 ;
  //  Stack <Crime> knownOffences, sentencing ;
  
  
  public Profile(Actor actor) {
    this.actor = actor ;
  }
  
  
  public static Profile loadProfile(Session s) throws Exception {
    final Profile p = new Profile((Actor) s.loadObject()) ;
    p.wagesDue = s.loadFloat() ;
    p.lastPsychEval = s.loadFloat() ;
    return p ;
  }
  
  
  public static void saveProfile(Profile p, Session s) throws Exception {
    s.saveObject(p.actor) ;
    s.saveFloat(p.wagesDue) ;
    s.saveFloat(p.lastPsychEval) ;
  }
  
  
  
  
  
  
  public float daysSincePsychEval(World world) {
    ///I.sayAbout(actor, "Last time: "+lastPsychEval) ;
    final float interval ;
    if (lastPsychEval == -1) interval = World.STANDARD_YEAR_LENGTH  ;
    else interval = world.currentTime() - lastPsychEval ;
    return interval / World.STANDARD_DAY_LENGTH ;
  }
  
  
  public void setPsychEvalTime(float time) {
    lastPsychEval = time ;
  }
  
  
  
  public float wagesDue() {
    return wagesDue ;
  }
  
  
  public void incWagesDue(float inc) {
    wagesDue += inc ;
  }
  
  
  public void clearWages() {
    wagesDue = 0 ;
  }
}




