/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;





public class EnforcerBloc extends Venue implements Economy {
  

  final public static Model MODEL = ImageModel.asSolidModel(
    EnforcerBloc.class, "media/Buildings/military/enforcer_bloc.png", 3, 3
  ) ;
  
  
  public EnforcerBloc(Base base) {
    super(3, 3, ENTRANCE_EAST, base) ;
    structure.setupStats(
      450, 2, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public EnforcerBloc(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    EnforcerBloc.class, "audit_office_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    PRESS_OFFICE = new Upgrade(
      "Press Office",
      "Assists in the production of pressfeed and brings Advertisers into "+
      "your employ, helping to gather information and fortify base morale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    FILE_SYSTEM = new Upgrade(
      "File System",
      "Allows tax documents, support claims and criminal prosecutions to be "+
      "processed quickly and expands the number of Auditors in your employ.",
      300, null, 1, null, ALL_UPGRADES
    ),
    RELIEF_AUDIT = new Upgrade(
      "Relief Audit",
      "Allows local homeless or unemployed to apply for basic income, while "+
      "sourcing a portion of support funding from offworld.",
      150, null, 1, FILE_SYSTEM, ALL_UPGRADES
    ),
    CURRENCY_ADJUSTMENT = new Upgrade(
      "Currency Injection",
      "Permits the office to modify total credits circulation to reflect "+
      "the settlement's property values.",
      200, null, 1, PRESS_OFFICE, ALL_UPGRADES
    )
  ;
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    
    final int linkMod = (int) (5 * (1 - stocks.shortagePenalty(DATALINKS))) ;
    
    if (actor.vocation() == Background.AUDITOR) {
      final Venue toAudit = Audit.nextToAuditFor(actor) ;
      if (toAudit != null) {
        final Audit a = new Audit(actor, toAudit) ;
        a.checkBonus = ((structure.upgradeLevel(FILE_SYSTEM) - 1) * 5) / 2 ;
        a.checkBonus += linkMod ;
        choice.add(a) ;
      }
    }
    
    return choice.weightedPick() ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.AUDITOR) {
      return nO + 1 + (structure.upgradeLevel(FILE_SYSTEM) / 2) ;
    }
    if (v == Background.ADVERTISER) {
      return nO + 1 + (structure.upgradeLevel(PRESS_OFFICE) / 2) ;
    }
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.AUDITOR } ;
  }
  
  
  public Service[] services() {
    return new Service[] { SERVICE_ADMIN } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  
  
  public String fullName() {
    return "Enforcer Bloc" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/audit_office_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "" ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MILITANT ;
  }
}





