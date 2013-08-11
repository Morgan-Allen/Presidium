

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;


//
//  So, what about carrying the injured back to a safe haven?  ...I need a new
//  animation for that.

public class Treatment extends Plan implements ActorConstants {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static int
    TYPE_FIRST_AID    = 0, FIRST_AID_DC    = 5 , FIRST_AID_XP    = 10,
    TYPE_MEDICATION   = 1, MEDICATION_DC   = 10, MEDICATION_XP   = 20,
    TYPE_PSYCH_EVAL   = 2, PSYCH_EVAL_DC   = 15, PSYCH_EVAL_XP   = 40 ;/*,
    TYPE_SURGERY      = 3, SURGERY_DC      = 20, SURGERY_XP      = 75,
    TYPE_GENE_THERAPY = 4, GENE_THERAPY_DC = 25, GENE_THERAPY_XP = 150,
    TYPE_CONDITIONING = 5, CONDITIONING_DC = 30, CONDITIONING_XP = 200 ;
  //*/
  
  final Actor patient ;
  final int type ;
  Trait applied = null ;
  
  
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
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(patient) ;
    s.saveInt(type) ;
    s.saveInt(applied == null ? -1 : applied.traitID) ;
  }
  
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    //  ...You need to include distance and danger factors, et cetera.  Also,
    //  patients not bleeding and indoors are okay.
    if (patient.indoors() && ! patient.health.bleeding()) return 0 ;
    return patient.health.injuryLevel() * PARAMOUNT ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Treatment t = (Treatment) p ;
    return t.type == this.type && t.applied == this.applied ;
  }
  
  
  protected Behaviour getNextStep() {
    switch (type) {
      case (TYPE_FIRST_AID) :
        if (patient.health.bleeding()) {
          final Action firstAid = new Action(
            actor, patient,
            this, "actionFirstAid",
            Action.BUILD, "Performing first aid"
          ) ;
          return firstAid ;
        }
      break ;
    }
    
    if ((! patient.health.conscious()) && (! patient.indoors())) {
      Venue haven = Retreat.nearestHaven(actor, Sickbay.class) ;
      
      if (haven == null) haven = actor.psyche.home() ;
      if (haven != null) {
        final Delivery transport = new Delivery(patient, haven) ;
        return transport ;
      }
    }
    return null ;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = FIRST_AID_DC + (10 * patient.health.injuryLevel()) ;
    //  IF IN A HOSPITAL, OR HAVE MEDICINE, HALVE THE DIFFICULTY
    if (actor.traits.test(ANATOMY, DC, FIRST_AID_XP)) {
      patient.health.liftInjury(1) ;
    }
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Treating ") ;
    d.append(patient) ;
  }
}







