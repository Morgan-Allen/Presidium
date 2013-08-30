

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;


//
//  So, what about carrying the injured back to a safe haven?  ...I need a new
//  animation for that.

public class Treatment extends Plan implements ActorConstants, BuildConstants {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static int
    TYPE_FIRST_AID    = 0, FIRST_AID_DC    = 5 , FIRST_AID_XP    = 10,
    TYPE_MEDICATION   = 1, MEDICATION_DC   = 10, MEDICATION_XP   = 20,
    TYPE_PSYCH_EVAL   = 2, PSYCH_EVAL_DC   = 15, PSYCH_EVAL_XP   = 40 ;/*,
    TYPE_SURGERY      = 3, SURGERY_DC      = 20, SURGERY_XP      = 75,
    TYPE_GENE_THERAPY = 4, GENE_THERAPY_DC = 25, GENE_THERAPY_XP = 150,
    TYPE_CONDITIONING = 5, CONDITIONING_DC = 30, CONDITIONING_XP = 250 ;
  //*/
  final static Table CONDITION_DCS = Table.make(
    ILLNESS, 0,
    CANCER, 5,
    SPICE_ADDICTION, 5,
    RAGE_INFECTION, 10,
    ALBEDAN_STRAIN, 15,
    SILVER_PLAGUE, 20
  ) ;
  final static int
    STAGE_NONE = 0,
    STAGE_EMERGENCY = 1,
    STAGE_TRANSPORT = 2,
    STAGE_FOLLOW_UP = 3 ;
  
  
  final Actor patient ;
  final int type ;
  
  private Trait applied = null ;
  ///private int stage = STAGE_NONE ;
  private float effectiveness = -1 ;
  private Item treatment = null ;
  
  
  
  
  public Treatment(Actor actor, Actor patient) {
    this(actor, patient, TYPE_FIRST_AID, null) ;
  }
  
  
  public Treatment(Actor actor, Actor patient, int type, Trait applied) {
    super(actor, patient) ;
    this.patient = patient ;
    this.type = type ;
    this.applied = applied ;
  }
  
  
  public Treatment(Session s) throws Exception {
    super(s) ;
    patient = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    final int tID = s.loadInt() ;
    applied = tID < 0 ? null : ActorConstants.ALL_TRAIT_TYPES[tID] ;
    effectiveness = s.loadFloat() ;
    treatment = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(patient) ;
    s.saveInt(type) ;
    s.saveInt(applied == null ? -1 : applied.traitID) ;
    s.saveFloat(effectiveness) ;
    Item.saveTo(s, treatment) ;
  }
  
  
  
  /**  Evaluating targets and priorities-
    */
  public static float needForTreatment(Actor patient) {
    if (! patient.health.diseased()) return 0 ;
    float priority = 0 ;
    for (Trait d : DISEASES) {
      if (hasMedication(patient, d)) continue ;
      final float serious = MEDICATION_DC + (Integer) CONDITION_DCS.get(d) ;
      priority += patient.traits.trueLevel(d) * serious / 5f ;
    }
    if (priority == 0) return 0 ;
    return Visit.clamp(priority, IDLE, CRITICAL) ;
  }
  
  
  private static boolean hasMedication(Actor actor, Trait disease) {
    for (Item match : actor.gear.matches(SERVICE_TREAT)) {
      final Action action = (Action) match.refers ;
      final Treatment treatment = (Treatment) action.basis ;
      if (treatment.applied == disease) return true ;
    }
    return false ;
  }
  
  
  public float priorityFor(Actor actor) {
    //
    //  ...You need to include distance and danger factors, et cetera.
    if (type == TYPE_MEDICATION) {
      return needForTreatment(patient) ;
    }
    if (type == TYPE_FIRST_AID) {
      if (patient.indoors() && ! patient.health.bleeding()) return 0 ;
      return patient.health.injuryLevel() * PARAMOUNT ;
    }
    return 0 ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Treatment t = (Treatment) p ;
    return t.type == this.type && t.applied == this.applied ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    switch (type) {
      case (TYPE_FIRST_AID) : return nextFirstAid() ;
      case (TYPE_MEDICATION) : return nextMedication() ;
    }
    return null ;
  }
  
  
  Behaviour nextFirstAid() {
    if (patient.health.bleeding()) {
      final Action firstAid = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Performing first aid"
      ) ;
      firstAid.setProperties(Action.QUICK) ;
      return firstAid ;
    }
    if ((! patient.health.conscious()) && (! patient.indoors())) {
      final Target haven = Retreat.nearestHaven(actor, Sickbay.class) ;
      if (haven instanceof Venue) {
        final Delivery transport = new Delivery(patient, (Venue) haven) ;
        return transport ;
      }
    }
    return null ;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = FIRST_AID_DC + (10 * patient.health.injuryLevel()) ;
    float bonus = 0 ;
    if (actor.aboard() instanceof Sickbay) {
      bonus += 2 + Plan.upgradeBonus(actor.aboard(), Sickbay.SURGERY_WARD) ;
    }
    //  IF YOU HAVE MEDICINE, LOWER DIFFICULTY.
    if (actor.traits.test(ANATOMY, DC - bonus, FIRST_AID_XP)) {
      patient.health.liftInjury(1) ;
    }
    return true ;
  }
  
  
  Behaviour nextMedication() {
    if (treatment != null || ! patient.health.diseased()) return null ;
    //
    //  If the actor is sick, prepare a suitable treatment (least serious
    //  ailments first-)
    for (Trait d : DISEASES) if (patient.traits.trueLevel(d) > 0) {
      this.applied = d ;
      break ;
    }
    this.treatment = Item.withType(SERVICE_TREAT, new Action(
      patient, patient,
      this, "actionTreatEffect",
      Action.STAND, "for "+patient.traits.levelDesc(applied)
    )) ;
    if (patient.gear.amountOf(treatment) > 0) return null ;
    //  IF YOU HAVE MEDICINE, LOWER DIFFICULTY.
    //
    //  And deliver to the patient-
    final Action medicate = new Action(
      actor, patient,
      this, "actionMedicate",
      Action.TALK_LONG, "Medicating "+patient
    ) ;
    return medicate ;
  }
  
  
  public boolean actionTreatEffect(Actor patient, Actor p) {
    patient.traits.incLevel(applied, -0.1f * effectiveness) ;
    if (patient.traits.trueLevel(applied) < 0) {
      patient.traits.setLevel(applied, 0) ;
    }
    patient.gear.removeItem(Item.withAmount(treatment, 0.1f)) ;
    return true ;
  }
  
  
  public boolean actionMedicate(Actor actor, Actor patient) {
    //
    //  Calculate the difficulty of treating the condition.
    int difficulty = (Integer) CONDITION_DCS.get(applied) + MEDICATION_DC ;
    difficulty += (patient.traits.trueLevel(applied) - 1) * 5 ;
    if (actor.aboard() instanceof Sickbay) {
      float bonus = 2 + Plan.upgradeBonus(actor.aboard(), Sickbay.APOTHECARY) ;
      bonus *= 2 ;
      difficulty -= bonus ;
    }
    //
    //  Attempt the treatment, and give the prescription to the patient-
    patient.gear.addItem(treatment) ;
    if (actor.traits.test(PHARMACY, difficulty, MEDICATION_XP)) {
      this.effectiveness = Rand.num() * 2 ;
      return true ;
    }
    else {
      this.effectiveness = 0.1f ;
      return false ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Treating ") ;
    d.append(patient) ;
  }
}




