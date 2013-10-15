


package src.game.actors ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.campaign.* ;
import src.game.social.Audit;
import src.game.social.BaseProfiles;
import src.util.* ;



public class Profile {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public Actor actor ;
  
  //Background position ;
  float paymentDue = 0 ;
  float lastWageEval = -1 ;
  float lastPsychEval = -1 ;
  //  Stack <Pledge> pledgesMade ;
  //  Stack <Crime> knownOffences ;
  
  
  public Profile(Actor actor, BaseProfiles bP) {
    this.actor = actor ;
    if (bP != null) lastWageEval = bP.base.world.currentTime() ;
  }
  
  
  public static Profile loadProfile(Session s) throws Exception {
    final Profile p = new Profile((Actor) s.loadObject(), null) ;
    p.paymentDue = s.loadFloat() ;
    p.lastPsychEval = s.loadFloat() ;
    p.lastWageEval = s.loadFloat() ;
    return p ;
  }
  
  
  public static void saveProfile(Profile p, Session s) throws Exception {
    s.saveObject(p.actor) ;
    s.saveFloat(p.paymentDue) ;
    s.saveFloat(p.lastPsychEval) ;
    s.saveFloat(p.lastWageEval) ;
  }
  
  
  
  /**  Psych evaluation-
    */
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
  
  
  
  /**  Wages and payments due-
    */
  public float salary() {
    //
    //  TODO:  This needs to be negotiated, or at least modified, based on
    //  reluctance to settle or personal dislike.
    final int standing = actor.vocation().standing ;
    if (standing == Background.SLAVE_CLASS) return 0 ;
    //
    //  Rulers draw directly on the finances of the state if they have any
    //  particular shortage of funds.
    if (standing == Background.RULER_CLASS) {
      if (actor.gear.credits() < Audit.RULER_STIPEND) {
        return Audit.RULER_STIPEND - actor.gear.credits() ;
      }
      else return 0 ;
    }
    return Background.HIRE_COSTS[standing] ;
  }
  
  
  public float paymentDue() {
    return paymentDue ;
  }
  
  
  public void incPaymentDue(float inc) {
    paymentDue += inc ;
  }
  
  
  public float daysSinceWageEval(World world) {
    final float interval = world.currentTime() - lastWageEval ;
    return interval / World.STANDARD_DAY_LENGTH ;
  }
  
  
  public void clearWages(World world) {
    paymentDue = 0 ;
    lastWageEval = world.currentTime() ;
  }
}