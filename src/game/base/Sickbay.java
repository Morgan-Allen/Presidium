


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.Index;



public class Sickbay extends Venue implements BuildConstants {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Sickbay.class, "media/Buildings/physician/physician_clinic.png", 3, 2
  ) ;
  
  
  public Sickbay(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
    attachSprite(MODEL.makeSprite()) ;
    structure.setupStats(
      200, 2, 350,
      VenueStructure.NORMAL_MAX_UPGRADES, false
    ) ;
  }
  
  
  public Sickbay(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */

  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Sickbay.class, "sickbay_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    APOTHECARY = new Upgrade(
      "Apothecary",
      "A selection of therapeutic drugs and immune modulators help to curb "+
      "the spread of contagious disease and assist in birth control.",
      null, 1, null, ALL_UPGRADES
    ),
    SURGERY_WARD = new Upgrade(
      "Surgery Ward",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "injuries and life-threatening ailments can be treated quickly.",
      null, 1, null, ALL_UPGRADES
    ),
    MINDER_QUARTERS = new Upgrade(
      "Minder Quarters",
      "Minders are essential to monitoring patients' condition and tending "+
      "to diet and sanitary needs, but are only familiar with more common "+
      "medications and standard emergency protocol.",
      Vocation.MINDER, 2, APOTHECARY, ALL_UPGRADES
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Intensive care allows a chance for patients on death's door to make a "+
      "gradual comeback, covering everything from life support and tissue "+
      "reconstruction to cybernetic prosthesis and engram backups.",
      null, 1, SURGERY_WARD, ALL_UPGRADES
    ),
    PSYCH_UNIT = new Upgrade(
      "Psych Unit",
      "A separate ward for sufferers of mental illness or degredation allows "+
      "symptoms to be managed without interfering with other sickbay "+
      "functions, while maximising chances for recovery.",
      null, 1, MINDER_QUARTERS, ALL_UPGRADES
    ),
    PHYSICIAN_QUARTERS = new Upgrade(
      "Physicians Quarters",
      "Physicians undergo extensive education in every aspect of human "+
      "metabolism and anatomy, are adept as surgeons, and can tailor their "+
      "treatments to the idiosyncracies of individual patients.",
      Vocation.PHYSICIAN, 1, SURGERY_WARD, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    //
    //  You can skip some of these if another person is covering the problem.
    //
    //  If anyone is waiting for treatment, tend to them.
    for (Mobile m : this.inside()) if (m instanceof Actor) {
      final Actor patient = (Actor) m ;
      if (patient.health.bleeding()) return new Treatment(
        actor, patient, Treatment.TYPE_FIRST_AID, null
      ) ;
    }
    for (Mobile m : this.inside()) if (m instanceof Actor) {
      final Actor patient = (Actor) m ;
      if (patient.health.diseased()) return new Treatment(
        actor, patient, Treatment.TYPE_MEDICATION, null
      ) ;
    }
    //
    //  You also need to cover treatment of the dead and crazy...
    //
    //  Otherwise, just tend the desk...
    return new Supervision(actor, this) ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.MINDER, Vocation.PHYSICIAN } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.MINDER) return nO + 2 ;
    if (v == Vocation.PHYSICIAN) return nO + 1 ;
    return 0 ;
  }
  
  
  protected Service[] services() {
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "The Sickbay" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/hospice_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Sickbay allows your citizens' injuries, diseases and trauma to be"+
      "treated quickly and effectively.  It also helps to regulate "+
      "population growth and provide basic daycare and education facilities." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}






