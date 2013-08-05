/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.Session;
import src.util.* ;



//
//  Merge this with the hunting class.  No- have this as a subset of the
//  hunting class!


public class Combat extends Plan implements ActorConstants {
  
  
  /**  
    */
  final Actor target ;
  
  
  public Combat(Actor actor, Actor target) {
    super(actor, target) ;
    this.target = target ;
  }
  
  
  public Combat(Session s) throws Exception {
    super(s) ;
    this.target = (Actor) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(target) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    return combatPriority(actor, target, ROUTINE, PARAMOUNT) ;
  }
  
  
  protected Behaviour getNextStep() {
    //
    //  This might need to be tweaked in cases of self-defence, where you just
    //  want to see off an attacker.
    if (target.health.deceased()) return null ;
    final Action strike = new Action(
      actor, target,
      this, "actionStrike",
      Action.STRIKE, "Striking at "+target
    ) ;
    strike.setProperties(Action.QUICK) ;
    return strike ;
  }
  
  
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.deceased()) return false ;
    //
    //  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, CLOSE_COMBAT, CLOSE_COMBAT) ;
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER) ;
    }
    return true ;
  }
  
  
  //
  //  You also need code for minor retreats.
  
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  public static float combatPriority(
    Actor actor, Actor enemy, float winReward, float lossCost
  ) {
    final float
      actorStrength = combatStrength(actor, enemy),
      enemyStrength = combatStrength(enemy, actor),
      chance = actorStrength / (actorStrength + enemyStrength) ;
    I.say(
      "Actor/enemy strength, chance: "+actorStrength+"/"+enemyStrength+
      ", "+chance
    ) ;
    float appeal = 0 ;
    appeal += winReward * chance ;
    appeal -= (1 - chance) * lossCost ;
    I.say("Final appeal: "+appeal) ;
    
    //
    //final float distance = Spacing.distance(enemy, actor) ;
    //  You also need to incorporate an estimate of dangers en-route,
    //  and subtract this (relative to the actor's combat strength.)
    //...Some of this should be moved to the general Mission class.
    //appeal /= Math.max(1, distance / Terrain.SECTOR_SIZE) ;
    return appeal ;
  }
  
  
  public static float combatStrength(Actor actor, Actor enemy) {
    float strength = 0 ;
    strength += actor.gear.armourRating() + actor.gear.attackDamage() ;
    strength *= actor.health.maxHealth() / 100 ;
    strength *= actor.traits.trueLevel(REFLEX) / 10 ;
    //
    //  You also need to include modifiers for ranged and melee attack skill,
    //  plus reflex and shields.
    //  Work this out later.
    /*
    if (enemy != null) {
      if (enemy.gear.meleeWeapon()) {
        strength *= enemy.traits.chance(CLOSE_COMBAT, actor, CLOSE_COMBAT, 0) ;
      }
      else {
        strength *= enemy.traits.chance(, actor, MARKSMANSHIP, 0) ;
      }
    }
    //*/
    return strength ;
  }
  
  
  
  
  /**  Actual behaviour implementation-
    */
  static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence
  ) {
    final boolean success = actor.traits.test(offence, target, defence, 0, 1) ;
    if (success) {
      float damage = actor.gear.attackDamage() * Rand.num() * 2 ;
      //
      //  You may also need to decrement shields.
      damage -= actor.gear.armourRating() * Rand.num() * 2 ;
      ///I.say(actor+" successfully struck "+target+", damage dealt: "+damage) ;
      if (damage > 0) {
        target.health.takeInjury(damage) ;
      }
    }
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