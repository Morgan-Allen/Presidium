/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.social ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



public class Audit extends Plan implements Economy {
  
  
  
  /**  Data fields, constructors and save/load functions-
    */
  final public static float
    MILITANT_BONUS   = 2.0f,
    MILITANT_RATION  = 50,
    RULER_STIPEND    = 1000,
    BASE_BRIBE_SIZE  = 50 ;
  
  
  final static int
    STAGE_EVAL   = -1,
    STAGE_AUDIT  =  0,
    STAGE_REPORT =  1,
    STAGE_DONE   =  2 ;
  
  private static boolean verbose = false ;
  
  
  private int stage = STAGE_EVAL ;
  private Venue audited ;
  private float balance = 0 ;
  
  public int checkBonus = 0 ;
  
  
  public Audit(Actor actor, Venue firstAudit) {
    super(actor) ;
    this.audited = firstAudit ;
  }
  
  
  public Audit(Session s) throws Exception {
    super(s) ;
    stage = s.loadInt() ;
    audited = (Venue) s.loadObject() ;
    balance = s.loadFloat() ;
    checkBonus = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(stage) ;
    s.saveObject(audited) ;
    s.saveFloat(balance) ;
    s.saveInt(checkBonus) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public float priorityFor(Actor actor) {
    return ROUTINE ;
  }
  
  
  public static Venue getNextAuditFor(Actor actor) {
    
    final World world = actor.world() ;
    final Venue work = (Venue) actor.mind.work() ;
    final Batch <Venue> batch = new Batch <Venue> () ;
    world.presences.sampleFromKey(work, world, 10, batch, work.base()) ;
    
    Venue picked = null ;
    float bestRating = 0, rating ;
    for (Venue v : batch) {
      rating = Math.abs(v.inventory().credits()) / 100f ;
      rating -= Plan.rangePenalty(v, actor) ;
      rating -= Plan.competition(Audit.class, v, actor) ;
      if (rating > bestRating) { bestRating = rating ; picked = v ; }
    }
    //I.sayAbout(actor, "Chosen for audit: "+picked) ;
    return picked ;
  }
  
  
  public static Venue nearestAdminFor(Actor actor) {
    final World world = actor.world() ;
    Venue admin = null ;
    for (Object o : world.presences.sampleFromKey(
      actor, world, 5, null, SERVICE_ADMIN
    )) {
      final Venue v = (Venue) o ;
      if (v.base() == actor.base()) { admin = v ; break ; }
    }
    return admin ;
  }
  
  

  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    I.sayAbout(actor, "Getting next audit step... "+this.hashCode()) ;
    I.sayAbout(actor, "Stage was: "+stage) ;
    
    if (stage == STAGE_EVAL) {
      if (audited == null && balance < 1000) audited = getNextAuditFor(actor) ;
      if (audited != null) stage = STAGE_AUDIT ;
      else stage = STAGE_REPORT ;
    }
    I.sayAbout(actor, "Stage is now: "+stage) ;
    
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
        actor, actor.mind.work(),
        this, "actionFileReport",
        Action.TALK, "Filing Report"
      ) ;
      return report ;
    }
    return null ;
  }
  
  
  public boolean actionAudit(Actor actor, Venue audited) {
    final float balance = auditEmployer(actor, audited) ;
    audited.stocks.incCredits(0 - balance) ;
    audited.stocks.taxDone() ;
    this.balance += balance ;
    stage = STAGE_EVAL ;
    
    I.sayAbout(actor, "Just audited: "+audited) ;
    this.audited = null ;
    return true ;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    fileReport(actor, office, balance) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  
  public static void fileReport(Actor actor, Venue office, float balance) {
    final Base base = office.base() ;
    if (balance > 0) {
      base.incCredits(balance) ;
      office.chat.addPhrase((int) balance+" credits in profit") ;
    }
    if (balance < 0) {
      base.incCredits(balance) ;
      office.chat.addPhrase((int) (0 - balance)+" credits in debt") ;
    }
  }
  
  
  
  public static float auditEmployer(Actor audits, Venue venue) {
    if (venue.privateProperty() || venue.base() == null) return 0 ;
    float sumWages = 0, sumSalaries = 0, sumSplits = 0 ;
    
    final BaseProfiles BP = venue.base().profiles ;
    final int numW = venue.personnel.workers().size() ;
    final Profile profiles[] = new Profile[numW] ;
    final float salaries[] = new float[numW] ;
    
    int i = 0 ; for (Actor works : venue.personnel.workers()) {
      final Profile p = BP.profileFor(works) ;
      final int KR    = BP.querySetting(AuditOffice.KEY_RELIEF) ;
      
      final float
        salary = p.salary(),
        relief = AuditOffice.RELIEF_AMOUNTS[KR],
        payInterval = p.daysSinceWageEval(venue.world()),
        wages = ((salary / 10f) + relief) * payInterval ;
      
      profiles[i] = p ;
      salaries[i++] = salary ;
      p.incPaymentDue(wages) ;
      sumWages += wages ;
      sumSalaries += salary ;
    }
    venue.stocks.incCredits(0 - sumWages) ;
    
    
    final float surplus = venue.stocks.unTaxed() ;
    i = 0 ; if (surplus > 0) for (Profile p : profiles) {
      final float split = salaries[i++] * surplus / (sumSalaries * 10) ;
      p.incPaymentDue(split) ;
      sumSplits += split ;
    }
    venue.stocks.incCredits(0 - sumSplits) ;
    
    
    final Base base = venue.base() ;
    float
      balance = venue.stocks.credits(),
      waste = (Rand.num() + base.crimeLevel()) / 2f ;
    final float honesty =
      (audits.traits.traitLevel(HONOURABLE) / 2f) -
      (audits.traits.traitLevel(ACQUISITIVE) * waste) ;
    
    if (Rand.num() > honesty) {
      waste *= 1.5f ;
      final float bribe = BASE_BRIBE_SIZE * waste ;
      balance -= bribe ;
      audits.gear.incCredits(bribe) ;
    }
    else {
      if (audits.traits.test(ACCOUNTING, 15, 5)) waste = 0 ;
      if (audits.traits.test(ACCOUNTING, 5, 5)) waste /= 2 ;
    }
    
    final int
      profit = (int) (balance / (1 + waste)),
      losses = (int) (balance * (1 + waste)) ;
    if (profit > 0) return profit ;
    if (losses < 0) return losses ;
    return 0 ;
  }
  
  

  //
  //  ...Actors need to pay a share of income tax at their home.  Out of
  //  savings.
  
  public static float propertyValue(Venue venue) {
    float value = 0 ;
    if (venue instanceof Holding) {
      final Holding home = (Holding) venue ;
      value += home.upgradeLevel() * 25 ;
      value += home.structure.buildCost() ;
    }
    return value ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_AUDIT && audited != null) {
      d.append("Auditing: ") ;
      d.append(audited) ;
    }
    else if (stage == STAGE_REPORT) {
      d.append("Filing a financial report at ") ;
      d.append(actor.mind.work()) ;
    }
    else d.append("Auditing "+audited) ;
  }
}






/*
if (verbose && I.talkAbout == venue) {
  I.say(venue+" ALLOCATING WAGES, "+positions.size()+" WORK?") ;
  I.say("  CREDITS ARE: "+venue.stocks.credits()) ;
}
//
//  Firstly, allocate basic salary plus minimum wage.  (Ruler-class
//  citizens receive no wage, drawing directly on the resources of the
//  state.)  TODO:  Should that be the case...?
float sumWages = 0 ;
for (Position p : positions) if (p.wages >= 0) {
  if (p.role.standing == Background.RULER_CLASS) {
    p.wages = 0 ;
    continue ;
  }
  float wage = (p.salary / 10f) + Audit.BASE_ALMS_PAY ;
  if (p.role.guild == Background.GUILD_MILITANT) {
    wage *= Audit.MILITANT_BONUS ;
  }
  wage *= AUDIT_INTERVAL * 1f / World.STANDARD_DAY_LENGTH ;
  p.wages += wage ;
  if (verbose) I.sayAbout(venue, "Wages for "+p.works+" are: "+p.wages) ;
  sumWages += wage ;
}
venue.stocks.incCredits(0 - sumWages) ;
//
//  Then split a portion of surplus between employees in proportion to
//  basic salary.
final float surplus = venue.stocks.unTaxed() ;
if (surplus > 0) {
  float sumSalaries = 0, sumShared = 0 ;
  for (Position p : positions) if (p.wages >= 0) {
    if (p.role.standing == Background.RULER_CLASS) continue ;
    sumSalaries += p.salary ;
  }
  for (Position p : positions) if (p.wages >= 0) {
    if (p.role.standing == Background.RULER_CLASS) continue ;
    final float shared = surplus * 0.1f * p.salary / sumSalaries ;
    p.wages += shared ;
    sumShared += shared ;
  }
  venue.stocks.incCredits(0 - sumShared) ;
}
//
//  Finally, report your debts or windfall back to the base.  Debts may
//  be exaggerated and/or profits under-reported, unless skilled (and
//  honest) auditors get there first.
final Base base = venue.base() ;
final float balance = venue.stocks.credits() ;
final float waste = (Rand.num() + base.crimeLevel()) / 2f ;
if (verbose) I.sayAbout(venue, "   BALANCE/WASTE: "+balance+"/"+waste) ;
final int
  profit = (int) (balance / (1 + waste)),
  losses = (int) ((0 - balance) * (1 + waste)) ;
if (profit > 0) {
  base.incCredits(profit) ;
  venue.chat.addPhrase((int) profit+" credits in profit") ;
  venue.stocks.incCredits(0 - profit) ;
}
if (losses > 0) {
  base.incCredits(0 - losses) ;
  venue.chat.addPhrase((int) losses+" credits in debt") ;
  venue.stocks.incCredits(losses) ;
}
venue.stocks.taxDone() ;

if (verbose && I.talkAbout == venue) for (Position p : positions) {
  if (p.wages > 0) I.say(p.works+" has "+p.wages+" credits in wages") ;
}
//*/


//Older version of this code, here temporarily for reference purposes.
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
//*/





