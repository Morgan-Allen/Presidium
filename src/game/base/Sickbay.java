


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
import src.util.I;
import src.util.Index;



public class Sickbay extends Venue implements Economy {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    Sickbay.class, "media/Buildings/physician/physician_clinic.png", 3, 2
  ) ;
  
  
  public Sickbay(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      200, 2, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
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
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    APOTHECARY = new Upgrade(
      "Apothecary",
      "A selection of therapeutic drugs and immune modulators help to curb "+
      "the spread of contagious disease and assist in birth control.",
      250,
      null, 1, null, ALL_UPGRADES
    ),
    SURGERY_THEATRE = new Upgrade(
      "Surgery Theatre",
      "Surgical tools, anaesthetics and plasma reserves ensure that serious "+
      "injuries, endogenous tumours and parasitism can be deal with quickly.",
      300,
      null, 1, null, ALL_UPGRADES
    ),
    MINDER_STATION = new Upgrade(
      "Minder Station",
      "Minders are essential to monitoring patients' condition and tending "+
      "to diet and sanitary needs, but are only familiar with more common "+
      "medications and standard emergency protocol.",
      50,
      Background.MINDER, 1, APOTHECARY, ALL_UPGRADES
    ),
    NEURAL_SCANNING = new Upgrade(
      "Psych Unit",
      "Permits neural scans and basic psych evaluation, aiding in detection "+
      "of mental disturbance or subversion, and permitting engram backups in "+
      "case of death.  Mandatory for key personnel.",
      350,
      null, 1, SURGERY_THEATRE, ALL_UPGRADES
    ),
    INTENSIVE_CARE = new Upgrade(
      "Intensive Care",
      "Intensive care allows a chance for patients on death's door to make a "+
      "gradual comeback, covering everything from life support and tissue "+
      "grafting to cybernetic prosthesis and engram fusion.",
      400,
      null, 1, MINDER_STATION, ALL_UPGRADES
    ),
    PHYSICIAN_STATION = new Upgrade(
      "Physician Station",
      "Physicians undergo extensive education in every aspect of human "+
      "metabolism and anatomy, are adept as surgeons, and can tailor their "+
      "treatments to the idiosyncracies of a given patient.",
      150,
      Background.PHYSICIAN, 1, SURGERY_THEATRE, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  You should also supervise the venue if patients need looking at-
    if (
      numPatients() > 0 && (! personnel.onShift(actor)) &&
      Plan.competition(Supervision.class, this, actor) == 0
    ) {
      final Supervision s = new Supervision(actor, this) ;
      s.priorityMod = Plan.IDLE ;
      choice.add(s) ;
    }
    if (! personnel.onShift(actor)) return choice.weightedPick(0) ;
    //
    //  If anyone is waiting for treatment, tend to them- including outside the
    //  building.
    for (Element m : actor.mind.awareOf()) if (m instanceof Actor) {
      final Actor patient = (Actor) m ;
      choice.add(new Treatment(
        actor, patient, Treatment.TYPE_FIRST_AID, null
      )) ;
    }
    for (Mobile m : this.inside()) if (m instanceof Actor) {
      final Actor patient = (Actor) m ;
      if (patient.health.diseased()) choice.add(new Treatment(
        actor, patient, Treatment.TYPE_MEDICATION, null
      )) ;
    }
    //
    //  TODO:  You also need to cover treatment of the dead and crazy...
    final Behaviour picked = choice.weightedPick(actor.mind.whimsy()) ;
    if (picked != null) return picked ;
    //
    //  Otherwise, just tend the desk...
    return new Supervision(actor, this) ;
  }
  
  
  private int numPatients() {
    int count = 0 ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor actor = (Actor) m ;
      if (actor.health.injuryLevel() > 0) count++ ;
      else if (actor.health.diseased()) count++ ;
    }
    return count ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    final int numU = (1 + structure.numUpgrades()) / 2 ;
    int medNeed = 2 + numU, powerNeed = 2 + numU ; 
    //
    //  Sickbays consumes medicine and power based on current upgrade level,
    //  and have a mild positive effect on ambience-
    stocks.incDemand(MEDICINE, medNeed, VenueStocks.TIER_CONSUMER, 1) ;
    stocks.forceDemand(POWER, powerNeed, VenueStocks.TIER_CONSUMER) ;
    world.ecology().impingeSqualor(0 - numU, this, true) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.MINDER, Background.PHYSICIAN } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.MINDER) return nO + 1 ;
    if (v == Background.PHYSICIAN) return nO + 1 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Sickbay" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/hospice_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Sickbay allows your citizens' injuries, diseases and trauma to be "+
      "treated quickly and effectively.  It also helps regulate population"+
      "growth and provide basic maternity and psych evaluation facilities." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}






