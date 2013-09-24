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
    Foundry.class, "media/Buildings/merchant/audit_office.png", 2.5f, 2
  ) ;
  
  
  public AuditOffice(Base base) {
    super(3, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public AuditOffice(Session s) throws Exception {
    super(s) ;
  }
  
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    
    if (actor.vocation() == Background.AUDITOR) {
      final Venue toAudit = Auditing.getNextAuditFor(actor) ;
      if (toAudit == null) return null ;
      else return new Auditing(actor, toAudit) ;
    }
    if (actor.vocation() == Background.PROPAGANDIST) {
      if (stocks.amountOf(PRESSFEED) >= 10) return null ;
      return new Manufacture(actor, this, PLASTICS_TO_PRESSFEED, null) ;
    }
    
    return null ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.AUDITOR) return nO + 1 ;
    if (v == Background.PROPAGANDIST ) return nO + 1 ;
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    float needPower = 2 ;
    if (! isManned()) needPower = 0 ;
    stocks.forceDemand(POWER, needPower, 0) ;
    stocks.bumpItem(POWER, needPower * -0.1f) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.AUDITOR, Background.PROPAGANDIST } ;
  }
  
  
  public Service[] services() {
    return new Service[] { PRESSFEED } ;
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





