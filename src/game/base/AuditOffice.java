/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



//
//  However, the state has the power to increase cash in circulation based on
//  the evaluation of total housing values.  This keeps prices more-or-less
//  constant.  This happens at the Audit Office, directly.


public class AuditOffice extends Venue implements BuildConstants {
  

  final public static Model MODEL = ImageModel.asIsometricModel(
    Foundry.class, "media/Buildings/merchant/audit_office.png", 2.75f, 2
  ) ;
  
  
  public AuditOffice(Base base) {
    super(3, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 2, 200,
      Structure.SMALL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public AuditOffice(Session s) throws Exception {
    super(s) ;
  }
  
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    AuditOffice.class, "audit_office_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    PRESS_CORPS = new Upgrade(
      "Press Corps",
      "Assists in the production of pressfeed and brings Propagandists into "+
      "your employ, helping to fortify base morale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    LEGAL_DEPARTMENT = new Upgrade(
      "Legal Department",
      "Allows civil disputes and criminal prosecutions to be processed "+
      "quickly and expands the number of Auditors in your employ.",
      300, null, 1, null, ALL_UPGRADES
    ),
    CURRENCY_ADJUSTMENT = new Upgrade(
      "Currency Adjustment",
      "Permits the office to inject credits into circulation to reflect "+
      "the settlement's property values.",
      200, PLASTICS, 2, PRESS_CORPS, ALL_UPGRADES
    ),
    ALMS_HEARINGS = new Upgrade(
      "Alms Hearings",
      "Allows homeless or unemployed citizens a chance to claim financial "+
      "support, with a portion of funding coming from offworld.",
      100, null, 1, LEGAL_DEPARTMENT, ALL_UPGRADES
    )
  ;
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    
    if (actor.vocation() == Background.AUDITOR) {
      final Venue toAudit = Auditing.getNextAuditFor(actor) ;
      if (toAudit != null) {
        choice.add(new Auditing(actor, toAudit)) ;
      }
      //
      //  TODO:  If someone is waiting to be heard (captive or plaintiff), see
      //  to them.
    }
    
    if (actor.vocation() == Background.PROPAGANDIST) {
      if (stocks.amountOf(PRESSFEED) >= 10) return null ;
      final Manufacture mP = stocks.nextManufacture(
        actor, PLASTICS_TO_PRESSFEED
      ) ;
      if (mP != null) {
        mP.checkBonus = (structure.upgradeLevel(PRESS_CORPS) - 1) * 5 ;
        choice.add(mP) ;
      }
    }
    
    return null ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.AUDITOR) {
      return nO + 1 + (structure.upgradeLevel(LEGAL_DEPARTMENT) / 2) ;
    }
    if (v == Background.PROPAGANDIST) {
      return nO + 1 + (structure.upgradeLevel(PRESS_CORPS) / 2) ;
    }
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    float needPower = 2 ;
    if (! isManned()) needPower = 0 ;
    stocks.forceDemand(POWER, needPower, VenueStocks.TIER_CONSUMER) ;
    stocks.bumpItem(POWER, needPower * -0.1f) ;
    
    //
    //  TODO:  Output additional credits if you have the plastics for it, and
    //  the right upgrades, and the wealth of the settlement merits it.  (But
    //  if it's being printed physically, how do you distribute it?)
    
    stocks.translateDemands(1, PLASTICS_TO_PRESSFEED) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.AUDITOR, Background.PROPAGANDIST } ;
  }
  
  
  public Service[] services() {
    return new Service[] { PRESSFEED, SERVICE_ADMIN } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Audit Office" ;
  }
  

  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/audit_office_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Audit Office regulates financial data and press releases "+
      "pertinent to your settlement's welfare, thus collecting taxes and "+
      "disseminating propaganda." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}





