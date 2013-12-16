/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.user.* ;
import src.util.* ;



public class Combat extends Plan implements Abilities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose        = false,
    reportEvents   = false,
    reportStrength = false ;
  
  final static int
    STYLE_RANGED = 0,  //TODO:  Replace with hit-and-run/skirmish
    STYLE_MELEE  = 1,  //TODO:  Replace with stand ground/direct assault
    STYLE_EITHER = 2,
    ALL_STYLES[] = { 0, 1, 2 },
    
    OBJECT_SUBDUE  = 0,
    OBJECT_DESTROY = 1,
    OBJECT_EITHER  = 2,
    ALL_OBJECTS[] = { 0, 1, 2 } ;

  final static String STYLE_NAMES[] = {
    "Ranged",
    "Melee",
    "Either"
  } ;
  final static String OBJECT_NAMES[] = {
    "Capture",
    "Destroy",
    "Neutralise"
  } ;
  
  
  final Element target ;  //, concern. (in case of defensive action.)
  final int style, object ;
  
  
  public Combat(Actor actor, Element target) {
    this(actor, target, STYLE_EITHER, OBJECT_EITHER) ;
  }
  
  
  public Combat(
    Actor actor, Element target, int style, int object
  ) {
    super(actor, target) ;
    this.target = target ;
    this.style = style ;
    this.object = object ;
  }
  
  
  public Combat(Session s) throws Exception {
    super(s) ;
    this.target = (Element) s.loadObject() ;
    this.style = s.loadInt() ;
    this.object = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(target) ;
    s.saveInt(style) ;
    s.saveInt(object) ;
  }
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  public float priorityFor(Actor actor) {
    if (isDead(target)) return 0 ;

    final boolean report = verbose && begun() && I.talkAbout == actor ;
    
    if (target instanceof Actor) {
      final Actor struck = (Actor) target ;
      float BP = combatPriority(actor, struck, priorityMod, PARAMOUNT, report) ;
      return BP <= 0 ? 0 : BP + ROUTINE ;
    }
    if (target instanceof Venue) {
      final Venue struck = (Venue) target ;
      float BP = (priorityMod - actor.mind.relation(struck.base())) * ROUTINE ;
      return BP <= 0 ? 0 : BP + ROUTINE ;
    }
    return -1 ;
  }
  
  
  public boolean valid() {
    if (target instanceof Mobile && ((Mobile) target).indoors()) return false ;
    return super.valid() ;
  }
  
  
  public static boolean isDead(Element subject) {
    if (subject instanceof Actor)
      return ((Actor) subject).health.dying() ;
    if (subject instanceof Venue)
      return ((Venue) subject).structure.destroyed() ;
    return false ;
  }
  
  
  protected static float combatPriority(
    Actor actor, Actor enemy, float winReward, float lossCost, boolean report
  ) {
    if (actor == enemy) return 0 ;
    //
    //  Here, we estimate the danger presented by actors in the area, and
    //  thereby guage the odds of survival/victory-
    //  TODO:  Fix up the Danger Map.  Needs to be smoother, area-wise.
    final Batch <Element> known = actor.mind.awareOf() ;
    known.include(enemy) ;
    float danger = Retreat.dangerAtSpot(enemy, actor, enemy, known) ;
    if (actor.base() != null) {
      danger += Plan.dangerPenalty(enemy, actor) ;
    }
    final float chance = Visit.clamp(1 - danger, 0.1f, 0.9f) ;
    //
    //  Then we calculate the risk/reward ratio associated with the act-
    float appeal = 0 ;
    final float ER = 0 - actor.mind.relation(enemy) * Plan.PARAMOUNT ;
    lossCost -= ER / 2 ;
    winReward += ER / 2 ;
    appeal += winReward ;
    appeal *= actor.traits.scaleLevel(AGGRESSIVE) ;
    //appeal /= actor.traits.scaleLevel(NERVOUS   ) ;  //Covered by Retreat.
    
    
    if (report) {
      I.say(
        "  "+actor+" considering COMBAT with "+enemy+
        ", time: "+actor.world().currentTime()
      ) ;
      I.say(
        "  Danger level: "+danger+", relation: "+ER+
        "\n  Appeal before chance: "+appeal+", chance: "+chance
      ) ;
    }
    if (chance <= 0) {
      if (report) I.say("  No chance of victory!\n") ;
      return 0 ;
    }
    appeal = (appeal * chance) - ((1 - chance) * lossCost) ;
    if (report) I.say("  Final appeal: "+appeal+"\n") ;
    return appeal ;
  }
  
  
  
  //public static float threatFrom(Actor actor, Actor from) {
    
    /*
    float baseThreat = 0 - actor.mind.relation(from), threatMult = 1 ;
    
    final Target victim = from.targetFor(Combat.class) ;
    if (victim instanceof Actor) {
      baseThreat += actor.mind.relation((Actor) victim) ;
    }
    else if (victim instanceof Venue) {
      baseThreat += actor.mind.relation((Venue) victim) ;
    }
    else if (from.isDoing(Retreat.class, null)) {
      baseThreat -= actor.mind.relation(from.base()) ;
      threatMult = 0.25f ;
    }
    else {
      baseThreat -= actor.mind.relation(from.base()) ;
      threatMult = 0.5f ;
    }
    
    if (baseThreat > 0) return threatMult ;
    if (baseThreat < 0) return 0 - threatMult ;
    return 0 ;
    //*/
  //}
  
  
  //
  //  TODO:  Actors may need to cache this value?  Maybe later.  Not urgent at
  //  the moment.
  //
  //  Note:  it's acceptable to pass null as the enemy argument, for a general
  //  estimate of combat prowess.  (TODO:  Put in a separate method for that?)
  public static float combatStrength(Actor actor, Actor enemy) {
    float strength = 0 ;
    strength += (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f ;
    strength *= actor.health.maxHealth() / 10 ;
    strength *= (1 - actor.health.injuryLevel()) ;
    strength *= 1 - actor.health.stressPenalty() ;
    //
    //  Scale by general ranks in relevant skills-
    if (reportStrength) I.sayAbout(actor, "Native strength: "+strength) ;
    strength *= (
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(MARKSMANSHIP)
    ) / 20 ;
    strength *= (
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(STEALTH_AND_COVER)
    ) / 20 ;
    if (reportStrength) I.sayAbout(actor, "With skills: "+strength) ;
    //
    //  And if you're measuring combat against a specific adversary, scale by
    //  chance of victory-
    if (enemy != null) {
      final Skill attack, defend ;
      if (actor.gear.meleeWeapon()) {
        attack = defend = HAND_TO_HAND ;
      }
      else {
        attack = MARKSMANSHIP ;
        defend = STEALTH_AND_COVER ;
      }
      final float chance = actor.traits.chance(attack, enemy, defend, 0) ;
      if (reportStrength && I.talkAbout == enemy) {
        I.say("Chance to injure "+enemy+" is: "+chance) ;
        I.say("Native strength: "+strength) ;
      }
      strength *= 2 * chance ;
    }
    return strength ;
  }
  
  
  public static float stealthValue(Element e, Actor looks) {
    if (e instanceof Actor) {
      final Actor a = (Actor) e ;
      if (a.base() == looks.base()) return 0 ;
      float stealth = a.traits.traitLevel(STEALTH_AND_COVER) / 2f ;
      stealth *= a.health.moveLuck() ;
      stealth /= (0.5f + a.health.moveRate()) ;
      return stealth ;
    }
    if (e instanceof Installation) {
      final Installation i = (Installation) e ;
      return i.structure().cloaking() ;
    }
    return 0 ;
  }
  
  
  
  
  /**  Actual behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (reportEvents && begun()) {
      I.sayAbout(actor, "NEXT COMBAT STEP "+this.hashCode()) ;
    }
    //
    //  This might need to be tweaked in cases of self-defence, where you just
    //  want to see off an attacker.
    if (isDead(target) || priorityFor(actor) < 0) {
      if (reportEvents && begun()) I.sayAbout(actor, "COMBAT COMPLETE") ;
      return null ;
    }
    Action strike = null ;
    final boolean melee = actor.gear.meleeWeapon() ;
    final boolean razes = target instanceof Venue ;
    final float danger = Retreat.dangerAtSpot(
      actor.origin(), actor, null, actor.mind.awareOf()
    ) ;
    
    final String strikeAnim = melee ?
      Action.STRIKE :
      Action.FIRE ;
    if (razes) {
      strike = new Action(
        actor, target,
        this, "actionSiege",
        strikeAnim, "Razing "+target
      ) ;
    }
    else {
      strike = new Action(
        actor, target,
        this, "actionStrike",
        strikeAnim, "Striking at "+target
      ) ;
    }
    //
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction(strike, razes, danger) ;
    else configRangedAction(strike, razes, danger) ;
    return strike ;
  }
  
  
  private void configMeleeAction(Action strike, boolean razes, float danger) {
    //if (reportEvents) I.sayAbout(actor, "Configuring melee attack.\n") ;
    final World world = actor.world() ;
    strike.setProperties(Action.QUICK) ;
    if (razes) {
      if (! Spacing.adjacent(actor, target)) {
        strike.setMoveTarget(Spacing.nearestOpenTile(target, actor, world)) ;
      }
      else if (Rand.num() < 0.2f) {
        strike.setMoveTarget(Spacing.pickFreeTileAround(target, actor)) ;
      }
      else strike.setMoveTarget(actor.origin()) ;
    }
  }
  
  
  private void configRangedAction(Action strike, boolean razes, float danger) {
    
    final float range = actor.health.sightRange() ;
    boolean underFire = actor.world().activities.includes(actor, Combat.class) ;
    if (razes && Rand.num() < 0.1f) underFire = true ;
    
    boolean dodges = false ;
    if (actor.mind.hasSeen(target)) {
      final float distance = Spacing.distance(actor, target) / range ;
      //
      //  If not under fire, consider advancing for a clearer shot-
      if (Rand.num() < distance && ! underFire) {
        final Target AP = Retreat.pickWithdrawPoint(
          actor, range, target, -0.1f
        ) ;
        if (AP != null) { dodges = true ; strike.setMoveTarget(AP) ; }
      }
      //
      //  Otherwise, consider falling back for cover-
      if (underFire && Rand.num() > distance) {
        final Target WP = Retreat.pickWithdrawPoint(
          actor, range, target, 0.1f
        ) ;
        if (WP != null) { dodges = true ; strike.setMoveTarget(WP) ; }
      }
    }
    
    if (dodges) strike.setProperties(Action.QUICK) ;
    else strike.setProperties(Action.RANGED | Action.QUICK) ;
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.dying()) return false ;
    //
    //  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND) ;
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER) ;
    }
    return true ;
  }
  
  
  public boolean actionSiege(Actor actor, Venue target) {
    if (target.structure.destroyed()) return false ;
    performSiege(actor, target) ;
    return true ;
  }
  
  
  static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence
  ) {
    //
    //  TODO:  Allow for wear and tear to weapons/armour over time...
    final boolean success = actor.health.conscious() ? actor.traits.test(
      offence, target, defence, 0 - rangePenalty(actor, target), 1
    ) : true ;
    if (success) {
      float damage = actor.gear.attackDamage() * Rand.avgNums(2) ;
      damage -= target.gear.armourRating() * Rand.avgNums(2) ;
      
      final float oldDamage = damage ;
      damage = target.gear.afterShields(damage, actor.gear.physicalWeapon()) ;
      final boolean hit = damage > 0 ;
      if (damage != oldDamage) {
        OutfitType.applyFX(target.gear.outfitType(), target, actor, hit) ;
      }
      if (hit) target.health.takeInjury(damage) ;
    }
    DeviceType.applyFX(actor.gear.deviceType(), actor, target, success) ;
  }
  
  
  static void performSiege(
    Actor actor, Venue besieged
  ) {
    boolean accurate = false ;
    if (actor.gear.meleeWeapon()) {
      accurate = actor.traits.test(HAND_TO_HAND, 0, 1) ;
    }
    else {
      final float penalty = rangePenalty(actor, besieged) ;
      accurate = actor.traits.test(MARKSMANSHIP, penalty, 1) ;
    }
    
    float damage = actor.gear.attackDamage() * Rand.avgNums(2) * 1.5f ;
    if (accurate) damage *= 1.5f ;
    else damage *= 0.5f ;
    
    final float armour = besieged.structure.armouring() ;
    damage -= armour * (Rand.avgNums(2) + 0.25f) ;
    damage *= 5f / (5 + armour) ;
    
    I.say("Armour/Damage: "+armour+"/"+damage) ;
    
    if (damage > 0) besieged.structure.takeDamage(damage) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, besieged, true) ;
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final float range = Spacing.distance(a, t) / 2 ;
    return range * 5 / (a.health.sightRange() + 1f) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("In combat with ") ;
    d.append(target) ;
  }
}




/*
  public static boolean strikeCheck(
    Actor actor, Actor opponent, boolean offensive
  ) {
    final SkillType strikeSkill, defendSkill ;
    final int defendMove ;
    //
    //  TODO:  If the opponent has no melee weapon, they can't use close combat
    //  to defend themselves.
    float penalty = 0, bonus = 0 ;
    if (actor.equipment.meleeWeapon()) {
      strikeSkill = Vocation.CLOSE_COMBAT ;
      defendSkill = Vocation.CLOSE_COMBAT ;
      defendMove = MOVE_BLOCK ;
      bonus += foesPenalty(opponent) ;
    }
    else {
      strikeSkill = Vocation.MARKSMANSHIP ;
      defendSkill = Vocation.EVASION ;
      defendMove = MOVE_DODGE ;
      penalty += coverBonus(actor, actor, opponent) ;
    }
    if (offensive) bonus += timingPenalty(opponent, actor) ;
    if (moveFor(opponent) == defendMove) penalty += 5 ;
    //
    //  If the opponent is not focused on combat, or on the actor, then the
    //  check becomes much easier (TODO:  Implement that.)
    final boolean success = actor.training.skillTest(
      strikeSkill, bonus, STRIKE_XP,
      defendSkill, opponent, penalty
    ) ;
    return success ;
  }
  
  
  public static boolean dealDamage(
    Actor actor, Target toStrike, boolean critical
  ) {
    if (toStrike instanceof Actor) {
      final Actor foe = (Actor) toStrike ;
      final boolean
        melee = actor.equipment.meleeWeapon(),
        physical = actor.equipment.physicalWeapon() ;
      float damage = actor.equipment.attackDamage() * Rand.avgNums(2) ;
      if (! melee) {
        damage = foe.equipment.afterShields(actor, damage, physical) ;
      }
      if (critical) {
        damage *= MIN_HITS + (Rand.num() * MAX_HITS) ;
      }
      damage = foe.equipment.afterArmour(actor, damage, physical) ;
      if (damage <= 0) return false ;
      foe.health.takeInjury(damage) ;
      return true ;
    }
    if (toStrike instanceof Venue) {
      final Venue besieged = (Venue) toStrike ;
      float damage = actor.equipment.attackDamage() * Rand.avgNums(2) ;
      if (critical) damage *= MIN_HITS + (Rand.num() * MAX_HITS) ;
      besieged.integrity.takeDamage(damage) ;
      return true ;
    }
    return false ;
  }
  
  
//*/