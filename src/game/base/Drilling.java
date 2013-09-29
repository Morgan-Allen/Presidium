


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Drilling extends Plan implements ActorConstants {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  final DrillYard drillsAt ;
  

  public Drilling(Actor actor, DrillYard drillsAt) {
    super(actor, drillsAt) ;
    this.drillsAt = drillsAt ;
  }
  
  
  public Drilling(Session s) throws Exception {
    super(s) ;
    drillsAt = (DrillYard) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(drillsAt) ;
  }
  
  
  
  /**  Evaluating targets and priorities-
    */
  public float priorityFor(Actor actor) {
    final Venue work = (Venue) actor.mind.work() ;
    if (work.personnel.shiftFor(actor) != Venue.SECONDARY_SHIFT) {
      return 0 ;
    }
    
    float impetus = 0 ;
    final float pacifism = 0 - actor.traits.traitLevel(AGGRESSIVE) ;
    impetus -= (pacifism > 0) ? pacifism : (pacifism / 2f) ;
    
    impetus += actor.traits.traitLevel(DUTIFUL) ;
    impetus -= actor.traits.traitLevel(INDOLENT) ;
    if (actor.vocation().guild == Background.GUILD_MILITANT) {
      impetus += CASUAL ;
    }
    else {
      impetus = (impetus + IDLE) / 2 ;
    }
    
    impetus -= Plan.rangePenalty(actor, drillsAt) ;
    impetus -= Plan.dangerPenalty(drillsAt, actor) ;
    return Visit.clamp(impetus, 0, ROUTINE) ;
  }
  
  
  public static Drilling nextDrillFor(Actor actor) {
    if (actor.base() == null) return null ;
    if (! (actor.mind.work() instanceof Venue)) return null ;
    //
    //  TODO:  Use the batch of venues the actor is aware of?
    
    I.sayAbout(actor, "Getting drill sites") ;
    
    final World world = actor.world() ;
    final Batch <DrillYard> yards = new Batch <DrillYard> () ;
    world.presences.sampleFromKey(actor, world, 5, yards, DrillYard.class) ;
    
    final Choice choice = new Choice(actor) ;
    for (DrillYard yard : yards) if (yard.base() == actor.base()) {
      I.sayAbout(actor, "Can drill at "+yard) ;
      final Drilling drilling = new Drilling(actor, yard) ;
      choice.add(drilling) ;
    }
    return (Drilling) choice.weightedPick(0) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final int drillType = drillsAt.drillType() ;
    //
    //  Shoot.  I'm not sure how to set up the targets here.  The practice
    //  dummies will need to implement the Target/Saveable interface, most
    //  likely.
    
    final Target
      moveTarget = drillsAt.nextMoveTarget(drillType, actor),
      lookTarget = drillsAt.nextLookTarget(drillType, moveTarget) ;
    if (moveTarget == null || lookTarget == null) return null ;
    
    final String actionName, animName ;
    
    switch (drillType) {
      case (DrillYard.STATE_DRILL_MELEE) :
        actionName = "actionDrillMelee" ;
        animName = Action.STRIKE ;
      break ;
      case (DrillYard.STATE_DRILL_RANGED) :
        actionName = "actionDrillRanged" ;
        animName = Action.FIRE ;
      break ;
      case (DrillYard.STATE_DRILL_PILOT_SIM) :
        actionName = "actionDrillPilotSim" ;
        animName = Rand.yes() ? Action.LOOK : Action.BUILD ;
      break ;
      case (DrillYard.STATE_DRILL_SURVIVAL) :
        actionName = "actionDrillSurvival" ;
        animName = Rand.yes() ? Action.MOVE_FAST : Action.MOVE_SNEAK ;
      break ;
      default : return null ;
    }
    
    final Action drill = new Action(
      actor, lookTarget,
      this, actionName,
      animName, "Drilling"
    ) ;
    drill.setMoveTarget(moveTarget) ;
    return drill ;
  }
  
  
  public boolean actionDrillMelee(Actor actor, Target dummy) {
    final int DC = drillsAt.drillDC(DrillYard.STATE_DRILL_MELEE) ;
    actor.traits.test(HAND_TO_HAND, DC, 0.5f) ;
    actor.traits.test(SHIELD_AND_ARMOUR, DC - 5, 0.5f) ;
    
    //  ...Only with an officer present...
    //actor.traits.test(FORMATION_COMBAT, DC - 10, 0.5f) ;
    return true ;
  }
  
  
  public boolean actionDrillRanged(Actor actor, Target target) {
    final int DC = drillsAt.drillDC(DrillYard.STATE_DRILL_MELEE) ;
    actor.traits.test(MARKSMANSHIP, DC, 0.5f) ;
    actor.traits.test(SURVEILLANCE, DC - 5, 0.5f) ;
    
    //  ...Only with an officer present...
    ///actor.traits.test(FORMATION_COMBAT, DC - 10, 0.5f) ;
    return true ;
  }
  
  
  public boolean actionDrillPiloting(Actor actor, Target target) {
    final int DC = drillsAt.drillDC(DrillYard.STATE_DRILL_PILOT_SIM) ;
    actor.traits.test(PILOTING, DC, 0.5f) ;
    actor.traits.test(ASSEMBLY, DC - 5, 0.5f) ;
    
    //  ...Only with an officer present...
    //actor.traits.test(BATTLE_TACTICS, DC - 10, 0.5f) ;
    return true ;
  }
  
  
  public boolean actionDrillSurvival(Actor actor, Target target) {
    final int DC = drillsAt.drillDC(DrillYard.STATE_DRILL_SURVIVAL) ;
    actor.traits.test(ATHLETICS, DC, 0.5f) ;
    actor.traits.test(STEALTH_AND_COVER, DC - 5, 0.5f) ;
    
    //  ...Only with an officer present...
    //actor.traits.test(XENOZOOLOGY, DC - 10, 0.5f) ;
    return true ;
  }
  
  
  public boolean actionDrillCommand(Actor actor, DrillYard yard) {
    //
    //  ...Intended for the officer corps.
    //
    //  TODO:  Incorporate a bonus to skill acquisition when you have an
    //  officer present with enough expertise and the Command skill.
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Drilling at ") ;
    d.append(drillsAt) ;
  }
}














