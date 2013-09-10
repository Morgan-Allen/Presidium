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




public class AuditOffice extends Venue implements BuildConstants {
  

  final public static Model MODEL = ImageModel.asIsometricModel(
    Foundry.class, "media/Buildings/merchant/audit_office.png", 3, 2
  ) ;
  
  
  public AuditOffice(Base base) {
    super(3, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 2, 200,
      VenueStructure.NORMAL_MAX_UPGRADES, false
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
    if (! personnel.onShift(actor)) return null ;
    
    if (actor.vocation() == Vocation.AUDITOR) {
      final Venue toAudit = Auditing.getNextAuditFor(actor) ;
      if (toAudit == null) return null ;
      else return new Auditing(actor, toAudit) ;
    }
    if (actor.vocation() == Vocation.CENSOR) {
      if (stocks.amountOf(PRESSFEED) >= 10) return null ;
      return new Manufacture(actor, this, PLASTICS_TO_PRESSFEED, null) ;
    }
    
    return null ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.AUDITOR) return nO + 1 ;
    if (v == Vocation.CENSOR ) return nO + 1 ;
    return 0 ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.AUDITOR, Vocation.CENSOR } ;
  }
  
  
  protected Service[] services() {
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





