/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;




//  Older version of this code, here temporarily for reference purposes.
/*
  float
    BASE_INCOME_TAX  = 0.50f,
    BASE_SAVINGS_TAX = 0.05f,
    BASE_CAPITAL_TAX = 0.50f,
    MAX_INFLATION    = 1.10f,
    BASE_ALMS_PAY    = 10,
    BASE_SAVINGS     = 1000,
    BASE_CAPITAL     = 100 ;



  private void assessRealm() {
    //I.say("Performing Audit...") ;
    real = currency = 0 ;
    for (Mobile mobile : realm.world.allActive()) {
      if (mobile.realm() != this.realm) continue ;
      if (! (mobile instanceof Owner)) continue ;
      assessOwner((Owner) mobile) ;
    }
    for (Venue venue : realm.allVenues()) {
      assessOwner(venue) ;
      for (Item item : venue.stocks.allItems()) {
        real += item.price() ;
      }
    }
    currency += realm.credits() ;
    //I.say("Total currency in circulation is: "+currency+" credits.") ;
    //I.say("Total value of goods in settlement is: "+real+" credits.") ;
    //real *= 2 ;
  }
  
  private void assessOwner(Owner owns) {
    for (Item item : owns.inventory().allItems()) {
      real += item.price() ;
      currency += owns.inventory().credits() ;
    }
  }
  
  
  
  private void distributeWealth() {
    //  Here, we need to assess the adjustments to alms and taxation neccesary
    //  to ensure a reasonable supply of currency through the settlement.  (We
    //  also have to make allowance for the possibility of negative credit.)
    float almsBonus = 1, taxPenalty = 1 ;
    I.say("PERFORMING AUDIT.") ;
    if (real > currency) {
      if (currency < (real / 2)) almsBonus = MAX_INFLATION ;
      else almsBonus = Math.min(MAX_INFLATION, real / currency) ;
      I.say("Alms bonus is: "+almsBonus) ;
    }
    if (currency > real) {
      if (currency < 0) taxPenalty = 0 ;  //  ...Can't happen?
      else taxPenalty = Math.min(MAX_INFLATION, currency / real) ;
    }
    float tax, alms, totalTax = 0, totalAlms = 0 ;
    int numWorkers = 0 ;
    //  Likewise with local businesses.
    for (Venue venue : realm.allVenues()) {
      float capital = venue.stocks.credits() ;
      if (capital < BASE_CAPITAL) {
        totalAlms += alms = BASE_CAPITAL - capital ;
        venue.stocks.incCredits(alms * almsBonus) ;
      }
      else {
        totalTax += tax = (capital - BASE_CAPITAL) * BASE_CAPITAL_TAX ;
        venue.stocks.incCredits(0 - tax) ;
      }
      venue.stocks.taxDone() ;
      numWorkers += venue.membership.workers.size() ;
    }
    //  We pay workers a little more if there's plenty of cash in the coffers-
    final float
      surplus = Math.max(0, realm.credits() - (10000 + (numWorkers * 100))),
      almsAmount = BASE_ALMS_PAY + (surplus * BASE_SAVINGS_TAX / numWorkers) ;
    //  Now we visit all actors native to this realm, collect their taxes, and
    //  dispense any salary or alms they may be due...
    for (Mobile mobile : realm.world.allActive()) {
      if (mobile.realm() != this.realm) continue ;
      if (! (mobile instanceof Actor)) continue ;
      final Actor actor = (Actor) mobile ;
      if (actor.AI.work() == null) continue ;
      //  Having established this is an eligible actor...
      final ActorEquipment assets = actor.inventory() ;
      totalTax += tax = assets.unTaxed() * BASE_INCOME_TAX ;
      assets.incCredits(0 - tax) ;
      totalAlms += alms = almsAmount ;
      assets.incCredits(alms * almsBonus) ;
      //  We also take a small cut of the actor's permanent savings, to keep the
      //  currency in circulation and discourage hoarding-
      final float savings = assets.credits - BASE_SAVINGS ;
      if (savings > 0) assets.incCredits(savings * BASE_SAVINGS_TAX) ;
      assets.taxDone() ;
    }
    realm.incCredits((totalTax / taxPenalty) - totalAlms) ;
  }
  
//*/



public class Auditing extends Plan {
  
  
  
  /**  Data fields, constructors and save/load functions-
    */
  final static float
    BASE_INCOME_TAX  = 0.50f,
    BASE_SAVINGS_TAX = 0.05f,
    BASE_CAPITAL_TAX = 0.50f,
    MAX_INFLATION    = 1.10f,
    BASE_ALMS_PAY    = 10,
    BASE_SAVINGS     = 1000,
    BASE_CAPITAL     = 100 ;
  final static int
    STAGE_EVAL   = -1,
    STAGE_AUDIT  =  0,
    STAGE_REPORT =  1,
    STAGE_DONE   =  2 ;
  
  private int stage = STAGE_EVAL ;
  private Venue audited ;
  private float balance = 0 ;
  
  
  public Auditing(Actor actor, Venue firstAudit) {
    super(actor) ;
    this.audited = firstAudit ;
  }
  
  
  public Auditing(Session s) throws Exception {
    super(s) ;
    stage = s.loadInt() ;
    audited = (Venue) s.loadObject() ;
    balance = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(stage) ;
    s.saveObject(audited) ;
    s.saveFloat(balance) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public static Venue getNextAuditFor(Actor actor) {
    
    final World world = actor.world() ;
    final PresenceMap map = world.presences.mapFor(actor.base()) ;
    final float range = World.DEFAULT_SECTOR_SIZE ;
    final Venue work = (Venue) actor.AI.work() ;
    
    int maxTried = 5, numTried = 0 ;
    final Batch <Venue> batch = new Batch <Venue> () ;
    
    for (Object o : map.visitNear(work, range, null)) {
      if (o instanceof Venue) batch.add((Venue) o) ;
      if (++numTried > maxTried) break ;
    }
    for (numTried = maxTried ; numTried-- > 0 ;) {
      final Object o = map.pickRandomAround(work, range) ;
      if (o instanceof Venue) batch.add((Venue) o);
    }
    
    Venue picked = null ;
    float bestRating = 0, rating ;
    for (Venue v : batch) {
      rating = v.inventory().unTaxed() / 100f ;
      rating -= Plan.rangePenalty(v, actor) ;
      rating -= Plan.competition(Auditing.class, v, world) ;
      if (rating > bestRating) { bestRating = rating ; picked = v ; }
    }
    return picked ;
  }
  
  

  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //if (audited.stocks.unTaxed() == 0) return null ;
    if (stage == STAGE_EVAL) {
      if (audited == null && balance < 1000) audited = getNextAuditFor(actor) ;
      if (audited != null) stage = STAGE_AUDIT ;
      else stage = STAGE_REPORT ;
    }
    if (stage == STAGE_AUDIT) {
      final Action audit = new Action(
        actor, audited,
        this, "actionAudit",
        Action.TALK, "Auditing "+audited
      ) ;
      return audit ;
    }
    if (stage == STAGE_REPORT) {
      final Action report = new Action(
        actor, actor.AI.work(),
        this, "actionFileReport",
        Action.TALK, "Filing Report"
      ) ;
      return report ;
    }
    return null ;
  }
  
  
  public boolean actionAudit(Actor actor, Venue audited) {
    //
    //  TODO:  You need to assess the value of the property and assets of the
    //  inhabitants, plus add the possibility of dispensing alms.
    //
    //  TODO:  Involve a skill check of some kind here, so as to minimise waste
    //  due to corruption/inefficiency...
    final float taxes = audited.stocks.unTaxed() * BASE_INCOME_TAX ;
    audited.stocks.incCredits(0 - taxes) ;
    audited.stocks.taxDone() ;
    balance += taxes ;
    stage = STAGE_EVAL ;
    this.audited = null ;
    return true ;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    office.base().incCredits(balance) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_AUDIT && audited != null) {
      d.append("Auditing: ") ;
      d.append(audited) ;
    }
    if (stage == STAGE_REPORT) {
      d.append("Filing a financial report at ") ;
      d.append(actor.AI.work()) ;
    }
  }
}






