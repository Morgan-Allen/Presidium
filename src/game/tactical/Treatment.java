

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.Description;


//
//  So, what about carrying the injured back to a safe haven?  ...I need a new
//  animation for that.

public class Treatment extends Plan implements ActorConstants {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static int
    FIRST_AID    = 0,
    MEDICATION   = 1 ;/*,
    SURGERY      = 2,
    GENE_THERAPY = 3,
    CONDITIONING = 4 ;
  //*/
  
  final Actor patient ;
  final int type ;
  Trait applied = null ;
  

  public Treatment(Actor actor, Actor patient) {
    this(actor, patient, FIRST_AID, null) ;
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
    //  ...You need to include distance and danger factors, et cetera.
    return patient.health.injuryLevel() * PARAMOUNT ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Treatment t = (Treatment) p ;
    return t.type == this.type && t.applied == this.applied ;
  }
  
  
  protected Behaviour getNextStep() {
    switch (type) {
      case (FIRST_AID) :
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
    /*
    if ((! patient.health.okay()) && (! patient.indoors())) {
      Venue shelter = (Venue) actor.realm().nearestService(
        actor, Economy.HEALTHCARE, -1
      ) ;
      if (shelter == null) shelter = actor.AI.home() ;
      if (shelter != null) {
        final Delivery transport = new Delivery(patient, shelter) ;
        transport.bindActor(boundActor()) ;
        return transport ;
      }
    }
    //*/
    return null ;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = 20 * patient.health.injuryLevel() ;
    //  IF IN A HOSPITAL, OR HAVE MEDICINE, HALVE THE DIFFICULTY
    if (actor.traits.test(ANATOMY, DC, 10.0f)) {
      patient.health.liftInjury(0) ;
    }
    return true ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    super.describeBehaviour(d) ;
  }
}












