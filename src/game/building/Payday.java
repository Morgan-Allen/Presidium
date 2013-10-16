/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.social.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;
import src.game.actors.ActorMind.Employment ;



public class Payday extends Plan implements Economy {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final Employment pays ;
  private Venue admin ;
  private float balance ;
  
  
  public Payday(Actor actor, ActorMind.Employment pays, Venue admin) {
    super(actor, pays) ;
    this.pays = pays ;
    this.admin = admin ;
  }
  
  
  public Payday(Session s) throws Exception {
    super(s) ;
    this.pays = (Employment) s.loadObject() ;
    admin = (Venue) s.loadObject() ;
    balance = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(pays) ;
    s.saveObject(admin) ;
    s.saveFloat(balance) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    if (! (pays instanceof Venue)) return 0 ;
    if (balance > 0) {
      return CASUAL * actor.traits.scaleLevel(DUTIFUL) ;
    }
    
    final Venue venue = (Venue) pays ;
    final Profile p = venue.base.profiles.profileFor(actor) ;
    float impetus = (p.daysSinceWageEval(venue.world()) - 1) * ROUTINE ;
    if (impetus < 0) impetus = 0 ;
    impetus += actor.mind.greedFor((int) p.paymentDue()) * ROUTINE ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  public static Payday nextPaydayFor(Actor actor) {
    Venue admin = Audit.nearestAdminFor(actor, false) ;
    if (admin == null) return null ;
    final Employment work = actor.mind.work() ;
    if (work != null) return new Payday(actor, work, admin) ;
    admin = Audit.nearestAdminFor(actor, true) ;
    if (admin != null) return new Payday(actor, admin, admin) ;
    return null ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (admin == null) return null ;
    if (balance != 0) {
      final Action reports = new Action(
        actor, admin,
        this, "actionReport",
        Action.TALK, "Filing Report"
      ) ;
      return reports ;
    }
    final Action getPaid = new Action(
      actor, pays,
      this, "actionGetPaid",
      Action.TALK_LONG, "Getting Paid"
    ) ;
    return getPaid ;
  }
  
  
  public boolean actionGetPaid(Actor actor, Venue venue) {
    final Profile p = venue.base.profiles.profileFor(actor) ;
    
    if (p.paymentDue() == 0 && p.daysSinceWageEval(venue.world()) > 1) {
      if (venue instanceof AuditOffice) {
        ((AuditOffice) venue).dispenseRelief(actor) ;
      }
      else {
        balance = Audit.auditEmployer(actor, venue) ;
        venue.stocks.incCredits(0 - balance) ;
      }
    }
    
    final float wages = p.paymentDue() ;
    venue.stocks.incCredits(0 - wages) ;
    actor.gear.incCredits(wages) ;
    p.clearWages(venue.world()) ;
    return true ;
  }
  
  
  public boolean actionReport(Actor actor, Venue admin) {
    Audit.fileReport(actor, admin, balance) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Collecting wages at ") ;
    d.append(pays) ;
  }
}









