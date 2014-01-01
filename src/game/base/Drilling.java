/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Drilling extends Plan implements Economy {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  private static boolean verbose = false ;
  
  final DrillYard yard ;
  
  

  public Drilling(
    Actor actor, DrillYard grounds
  ) {
    super(actor, grounds) ;
    this.yard = grounds ;
  }
  
  
  public Drilling(Session s) throws Exception {
    super(s) ;
    yard = (DrillYard) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(yard) ;
  }
  
  
  
  /**  Priority and target evaluation-
    */
  public float priorityFor(Actor actor) {
    final Venue work = (Venue) actor.mind.work() ;
    if (work.personnel.shiftFor(actor) != Venue.SECONDARY_SHIFT) {
      if (! actor.isDoing("actionEquipYard", null)) return 0 ;
    }
    
    //
    //  TODO:  You can't drill if you lack an appropriate device type!
    
    float impetus = 0 ;
    //
    //  TODO:  Relevant traits might vary depending on type.
    final float pacifism = 0 - actor.traits.traitLevel(AGGRESSIVE) ;
    switch (yard.drillType()) {
      case (DrillYard.DRILL_MELEE) :
      case (DrillYard.DRILL_RANGED) :
        impetus -= (pacifism > 0) ? pacifism : (pacifism / 2f) ;
      break ;
      case (DrillYard.DRILL_AID) :
        impetus += pacifism / 2f ;
      break ;
    }
    
    impetus += actor.traits.traitLevel(DUTIFUL) ;
    impetus -= actor.traits.traitLevel(INDOLENT) ;
    if (actor.vocation().guild == Background.GUILD_MILITANT) {
      impetus += CASUAL ;
    }
    else {
      impetus = (impetus + IDLE) / 2 ;
    }
    //
    //  TODO:  Modify by importance of associated skills-
    
    impetus -= Plan.rangePenalty(actor, yard) ;
    impetus -= Plan.dangerPenalty(yard, actor) ;
    return Visit.clamp(impetus, 0, ROUTINE) ;
  }
  
  
  public static Drilling nextDrillFor(Actor actor) {
    if (actor.base() == null) return null ;
    if (! (actor.mind.work() instanceof Venue)) return null ;
    
    final World world = actor.world() ;
    final Batch <DrillYard> yards = new Batch <DrillYard> () ;
    world.presences.sampleFromKey(actor, world, 5, yards, DrillYard.class) ;
    
    final Choice choice = new Choice(actor) ;
    for (DrillYard yard : yards) if (yard.base() == actor.base()) {
      ///I.sayAbout(actor, "Can drill at "+yard) ;
      final Drilling drilling = new Drilling(actor, yard) ;
      choice.add(drilling) ;
    }
    return (Drilling) choice.pickMostUrgent() ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final int drillType = yard.drillType() ;
    if (
      (yard.nextDrill != yard.drill && ! yard.belongs.destroyed()) &&
      (Plan.competition(Drilling.class, yard.belongs, actor) < 1)
    ) {
      final Action pickup = new Action(
        actor, yard.belongs,
        this, "actionPickupEquipment",
        Action.REACH_DOWN, "Picking up equipment"
      ) ;
      final Action equips = new Action(
        actor, yard,
        this, "actionEquipYard",
        Action.BUILD, "Installing equipment"
      ) ;
      final Element dummy = (Element) Rand.pickFrom(yard.dummies) ;
      //equips.setMoveTarget(Spacing.nearestOpenTile(dummy.origin(), actor)) ;
      final Steps steps = new Steps(
        actor, this, Plan.ROUTINE, pickup, equips
      ) ;
      return steps ;
    }
    if (yard.equipQuality <= 0) return null ;
    
    //
    //  TODO:  If you're a commanding officer, consider supervising the drill-
    
    final Target
      dummy = yard.nextDummyFree(drillType, actor),
      moveTarget = yard.nextMoveTarget(dummy, actor) ;
    if (dummy == null || moveTarget == null) return null ;
    
    final String actionName, animName ;
    
    switch (drillType) {
      case (DrillYard.DRILL_MELEE) :
        actionName = "actionDrillMelee" ;
        animName = Action.STRIKE ;
      break ;
      case (DrillYard.DRILL_RANGED) :
        actionName = "actionDrillRanged" ;
        animName = Action.FIRE ;
      break ;
      case (DrillYard.DRILL_ENDURANCE) :
        actionName = "actionDrillEndurance" ;
        animName = Action.LOOK ;
      break ;
      case (DrillYard.DRILL_AID) :
        actionName = "actionDrillAid" ;
        animName = Action.BUILD ;
      break ;
      default : return null ;
    }
    
    final String DN = DrillYard.DRILL_STATE_NAMES[drillType] ;
    final Action drill = new Action(
      actor, dummy,
      this, actionName,
      animName, "Training "+DN
    ) ;
    drill.setProperties(Action.QUICK) ;
    drill.setMoveTarget(moveTarget) ;
    return drill ;
  }
  
  
  public boolean actionPickupEquipment(Actor actor, Garrison store) {
    if (verbose) I.sayAbout(actor, "Picking up drill equipment...") ;
    final Item equipment = Item.withReference(SAMPLES, store) ;
    final Upgrade bonus = yard.bonusFor(yard.nextDrill) ;
    final int quality = bonus == null ? 0 :
      (1 + store.structure.upgradeLevel(bonus)) ;
    
    actor.gear.addItem(Item.withQuality(equipment, quality)) ;
    return true ;
  }


  public boolean actionEquipYard(Actor actor, Element dummy) {
    if (verbose) I.sayAbout(actor, "Installing drill equipment...") ;
    final Item equipment = actor.gear.bestSample(SAMPLES, yard.belongs, 1) ;
    yard.drill = yard.nextDrill ;
    yard.equipQuality = (int) (equipment.quality * 5) ;
    actor.gear.removeItem(equipment) ;
    
    yard.updateSprite() ;
    return true ;
  }
  
  
  public boolean actionDrillMelee(Actor actor, Target dummy) {
    final int DC = yard.drillDC(DrillYard.DRILL_MELEE) ;
    actor.traits.test(HAND_TO_HAND, DC, 0.5f) ;
    actor.traits.test(SHIELD_AND_ARMOUR, DC - 5, 0.5f) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, dummy, true) ;
    return true ;
  }


  public boolean actionDrillRanged(Actor actor, Target dummy) {
    final int DC = yard.drillDC(DrillYard.DRILL_RANGED) ;
    actor.traits.test(MARKSMANSHIP, DC, 0.5f) ;
    actor.traits.test(SURVEILLANCE, DC - 5, 0.5f) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, dummy, true) ;
    return true ;
  }


  public boolean actionDrillEndurance(Actor actor, Target target) {
    final int DC = yard.drillDC(DrillYard.DRILL_ENDURANCE) ;
    actor.traits.test(ATHLETICS, DC, 0.5f) ;
    actor.traits.test(STEALTH_AND_COVER, DC - 5, 0.5f) ;
    return true ;
  }
  
  
  public boolean actionDrillAid(Actor actor, Target target) {
    final int DC = yard.drillDC(DrillYard.DRILL_AID) ;
    actor.traits.test(PHARMACY, DC, 0.5f) ;
    actor.traits.test(ANATOMY, DC - 5, 0.5f) ;
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
    final int DT = yard.drillType() ;
    if ((! describedByStep(d)) && DT >= 0) {
      d.append("Training "+DrillYard.DRILL_STATE_NAMES[DT]) ;
    }
    d.append(" at ") ;
    Target t = lastStepTarget() ;
    if (t == null || Visit.arrayIncludes(yard.dummies, t)) t = yard ;
    d.append(t) ;
  }
}





