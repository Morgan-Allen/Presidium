

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;


//
//  First Aid is for minor injuries (less than half health), or bleeding.
//  Medication is for: Illness, Spice Addiction, and Rage Infection.
//  Psych Eval is to get a report on personality and engram backups.
//  Surgery is for serious injuries (more than half health,) or death.
//  Gene Therapy is for: Cancer and Albedan Strain, or eugenics.
//  Conditioning is for re-programming, rehabilitation or engram fusion.
//
//  Focus on the extension of first aid and medication for now.
//
//  TODO:  You also need to carry the clinically dead back for treatment or
//  storage, depending on your upgrade level.

//
//  ...There's also an occasional bug where more than one actor can wind up
//  trying to deliver the patient.  That'll likely have to be sorted out with
//  suspensor-persistence in general, since they're related.


public class Treatment extends Plan implements Aptitudes, Economy {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static int
    TYPE_FIRST_AID    = 0, FIRST_AID_DC    = 5 , FIRST_AID_XP    = 10,
    TYPE_MEDICATION   = 1, MEDICATION_DC   = 10, MEDICATION_XP   = 20,
    TYPE_PSYCH_EVAL   = 2, PSYCH_EVAL_DC   = 15, PSYCH_EVAL_XP   = 40,
    TYPE_SURGERY      = 3, SURGERY_DC      = 20, SURGERY_XP      = 75,
    TYPE_GENE_THERAPY = 4, GENE_THERAPY_DC = 25, GENE_THERAPY_XP = 150,
    TYPE_CONDITIONING = 5, CONDITIONING_DC = 30, CONDITIONING_XP = 250 ;
  final static int
    STAGE_NONE      = 0,
    STAGE_EMERGENCY = 1,
    STAGE_TRANSPORT = 2,
    STAGE_FOLLOW_UP = 3 ;
  final static float
    SHORT_DURATION  = World.STANDARD_DAY_LENGTH,
    MEDIUM_DURATION = World.STANDARD_DAY_LENGTH * 10,
    LONG_DURATION   = World.STANDARD_DAY_LENGTH * 100 ;
  
  private static boolean verbose = false ;
  
  
  final Actor patient ;
  final int type ;
  
  private Trait applied = null ;
  private float effectiveness = -1 ;
  private Item treatItem = null ;
  
  
  
  
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
    applied = tID < 0 ? null : Aptitudes.ALL_TRAIT_TYPES[tID] ;
    effectiveness = s.loadFloat() ;
    treatItem = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(patient) ;
    s.saveInt(type) ;
    s.saveInt(applied == null ? -1 : applied.traitID) ;
    s.saveFloat(effectiveness) ;
    Item.saveTo(s, treatItem) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Treatment t = (Treatment) p ;
    return t.type == this.type && t.applied == this.applied ;
  }
  
  
  
  /**  Evaluating targets and priorities-
    */
  public float priorityFor(Actor actor) {
    
    //
    //  TODO:  Not necessarily...
    if (patient.health.deceased()) return 0 ;
    
    float competition = Plan.competition(Treatment.class, patient, actor) ;
    //if (Plan.competition(Treatment.class, patient, actor) > 0) return 0 ;
    //
    //  TODO:  Vary priority based on fitness for the task- skill and DC.
    //  ...You also need to include distance and danger factors, et cetera.
    
    if (type == TYPE_MEDICATION) {
      return needForMedication(patient) ;
    }
    if (type == TYPE_FIRST_AID && patient.health.injuryLevel() > 0) {
      if (
        (patient.indoors() || patient.health.conscious())
        && ! patient.health.bleeding()
      ) {
        if (verbose && I.talkAbout == actor) {
          I.say("  PATIENT NO LONGER NEEDS FIRST AID "+patient) ;
          I.say("    Indoors? "+patient.indoors()) ;
          I.say("    Conscious? "+patient.health.conscious()) ;
          I.say("    Bleeding? "+patient.health.bleeding()) ;
          I.say("    State is: "+patient.health.stateDesc()) ;
        }
        return 0 ;
      }
      
      float urgency = actor.mind.relation(patient) ;
      urgency *= patient.health.injuryLevel() * PARAMOUNT ;
      if (patient.health.bleeding()) urgency += ROUTINE ;
      if (! begun()) urgency -= competition * ROUTINE ;
      urgency *= actor.traits.scaleLevel(EMPATHIC) ;
      
      ///I.sayAbout(actor, "Treatment urgency: "+urgency) ;
      return Visit.clamp(urgency, ROUTINE, PARAMOUNT) ;
    }
    return 0 ;
  }
  
  
  private void configTreatment(Actor actor, Condition condition) {
    
    //
    //  Pick the right skill and the right DC for the trait in question.
    if (condition == ILLNESS) {
      
    }
    
  }
  
  
  /*
  public static Treatment nextTreatment(Actor treats) {
    
  }
  //*/
  
  
  public static Treatment treatmentFor(Actor patient, Trait applied) {
    return treatmentAmong(patient.gear.matches(SERVICE_TREAT), applied) ;
  }
  
  
  private static Treatment treatmentAmong(Batch <Item> matches, Trait applied) {
    for (Item match : matches) {
      final Action action = (Action) match.refers ;
      final Treatment treatment = (Treatment) action.basis ;
      if (treatment.applied == applied) return treatment ;
    }
    return null ;
  }
  
  
  
  /**  General implementation of behaviour-
    */
  protected Behaviour getNextStep() {
    switch (type) {
      case (TYPE_FIRST_AID) : return nextFirstAid() ;
      case (TYPE_MEDICATION) : return nextMedication() ;
    }
    return null ;
  }
  
  
  public boolean valid() {
    if (patient.health.deceased()) return false ;
    return super.valid() ;
  }
  
  
  
  /**  First Aid implementation-
    */
  Behaviour nextFirstAid() {
    if (patient.health.deceased()) return null ;
    if (patient.health.bleeding()) {
      final Action firstAid = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Performing first aid"
      ) ;
      firstAid.setProperties(Action.QUICK) ;
      return firstAid ;
    }
    if (actor == patient) {
      //
      //  TODO:  Consider checking yourself in at the sickbay anyway?
      return null ;
    }
    if (! Suspensor.canCarry(actor, patient)) return null ;
    if ((! patient.health.conscious()) && (! patient.indoors())) {
      final Target haven = Retreat.nearestHaven(patient, Sickbay.class) ;
      if (haven instanceof Venue) {
        final Delivery transport = new Delivery(patient, (Venue) haven) ;
        ///I.say(actor+" must deliver "+patient+" to "+haven) ;
        return transport ;
      }
      else if (verbose) I.sayAbout(actor, "No haven found!") ;
    }
    return null ;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = FIRST_AID_DC + (10 * patient.health.injuryLevel()) ;
    float bonus = 0 ;
    if (actor.aboard() instanceof Sickbay) {
      bonus += 2 + Plan.upgradeBonus(actor.aboard(), Sickbay.SURGERY_THEATRE) ;
    }
    //
    //  TODO:  IF YOU HAVE MEDICINE, LOWER DIFFICULTY/INCREASE QUALITY
    if (actor.traits.test(ANATOMY, DC - bonus, FIRST_AID_XP)) {
      patient.health.liftInjury(1) ;
      if (! patient.health.bleeding()) {
        //
        //  TODO:  Since this is first aid, limit the quality to basic or
        //  standard at best.  You need to perform surgery for better results.
        
        //  ...Internal/external bleeding?
        
        effectiveness = Rand.avgNums(3) * 2 ;
        effectiveness *= actor.traits.traitLevel(ANATOMY) / 20 ;
        applied = INJURY ;
        treatItem = Item.withReference(SERVICE_TREAT, new Action(
          patient, patient,
          this, "actionTreatEffect",
          Action.STAND, "for injury"
        )) ;
        treatItem = Item.withQuality(treatItem, (int) (effectiveness * 4)) ;
        //
        //  Don't apply this if a pre-existing treatment is of higher quality.
        final Treatment previous = treatmentFor(patient, INJURY) ;
        if (previous != null) {
          if (previous.effectiveness >= effectiveness) return false ;
          else patient.gear.removeItem(previous.treatItem) ;
        }
        patient.gear.addItem(treatItem) ;
      }
    }
    return true ;
  }
  
  
  
  /**  Medication implementation-
    */
  public static float needForMedication(Actor patient) {
    if (! patient.health.diseased()) return 0 ;
    float priority = 0 ;
    final Batch <Item> treatments = patient.gear.matches(SERVICE_TREAT) ;
    for (Trait d : DISEASES) {
      if (treatmentAmong(treatments, d) != null) continue ;
      final float serious = ((Condition) d).virulence / 2f ;
      priority += patient.traits.traitLevel(d) * serious ;
    }
    if (priority == 0) return 0 ;
    return Visit.clamp(priority, IDLE, PARAMOUNT) ;
  }
  
  
  Behaviour nextMedication() {
    if (treatItem != null || ! patient.health.diseased()) return null ;
    //
    //  If the actor is sick, prepare a suitable treatment (least serious
    //  ailments first-)
    for (Trait d : DISEASES) if (patient.traits.traitLevel(d) > 0) {
      this.applied = d ;
      break ;
    }
    this.treatItem = Item.withReference(SERVICE_TREAT, new Action(
      patient, patient,
      this, "actionTreatEffect",
      Action.STAND, "for "+patient.traits.levelDesc(applied)
    )) ;
    if (patient.gear.amountOf(treatItem) > 0) return null ;
    //
    //  TODO:  IF YOU HAVE MEDICINE, LOWER DIFFICULTY/INCREASE QUALITY.
    //
    //  And deliver to the patient-
    final Action medicate = new Action(
      actor, patient,
      this, "actionMedicate",
      Action.TALK_LONG, "Medicating "+patient
    ) ;
    return medicate ;
  }
  
  
  public boolean actionMedicate(Actor actor, Actor patient) {
    //
    //  Calculate the difficulty of treating the condition.
    float difficulty = MEDICATION_DC + (((Condition) applied).virulence / 2) ;
    difficulty += (patient.traits.traitLevel(applied) - 1) * 5 ;
    if (actor.aboard() instanceof Sickbay) {
      float bonus = 2 + Plan.upgradeBonus(actor.aboard(), Sickbay.APOTHECARY) ;
      bonus *= 2 ;
      difficulty -= bonus ;
    }
    //
    //  Attempt the treatment, and give the prescription to the patient-
    patient.gear.addItem(treatItem) ;
    if (actor.traits.test(PHARMACY, difficulty, MEDICATION_XP)) {
      this.effectiveness = Rand.num() * 2 ;
      return true ;
    }
    else {
      this.effectiveness = 0.1f ;
      return false ;
    }
  }
  
  
  
  /**  Implements the longer-term effects of treatment:
    */
  public boolean actionTreatEffect(Actor patient, Actor p) {
    //
    //  We assume that treatment last for 1/3 of a day or so...
    
    if (type == TYPE_FIRST_AID) {
      float regen = ActorHealth.INJURY_REGEN_PER_DAY ;
      regen *= 3 * effectiveness * patient.health.maxHealth() ;
      //I.sayAbout(patient, "  Regen bonus per day: "+regen) ;
      //I.sayAbout(patient, "  Effectiveness: "+effectiveness) ;
      regen /= World.STANDARD_DAY_LENGTH ;
      patient.health.liftInjury(regen) ;
      patient.gear.removeItem(Item.withAmount(treatItem, 1f / SHORT_DURATION)) ;
    }
    
    if (type == TYPE_MEDICATION) {
      final float inc = 1f / MEDIUM_DURATION ;
      patient.traits.incLevel(applied, -inc * 3 * effectiveness) ;
      if (patient.traits.traitLevel(applied) < 0) {
        patient.traits.setLevel(applied, 0) ;
      }
      patient.gear.removeItem(Item.withAmount(treatItem, inc)) ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Treating ") ;
    d.append(patient) ;
  }
}











