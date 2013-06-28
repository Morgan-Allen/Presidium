/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.actors.* ;
import src.util.* ;



public class Combat {
  
  
  
  
  //
  //  How do you measure the damage dealt by wild animals?
  
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