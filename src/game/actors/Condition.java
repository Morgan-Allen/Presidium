/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.social.Resting;
import src.util.* ;



public class Condition extends Trait {
  
  
  
  final public float latency, virulence, spread ;
  final public Trait affected[] ;
  final public int modifiers[] ;
  
  
  public Condition(String... names) {
    this(0, 0, 0, new Table(), names) ;
  }
  
  
  public Condition(
    float latency, float virulence, float spread,
    Table effects,
    String... names
  ) {
    super(Aptitudes.CONDITION, names) ;
    this.latency   = latency   ;
    this.virulence = virulence ;
    this.spread    = spread    ;
    
    this.affected  = new Trait[effects.size()] ;
    this.modifiers = new int[effects.size()] ;
    int i = 0 ; for (Object k : effects.keySet()) {
      modifiers[i] = (Integer) effects.get(k) ;
      affected[i++] = (Trait) k ;
    }
  }
  
  
  private static float cleanFactor(Actor actor) {
    float factor = 1 ;
    if (actor.aboard() instanceof src.game.base.Sickbay) {
      factor /= 2 ;
    }
    if (actor.gear.outfitType() == src.game.building.Economy.SEALSUIT) {
      factor /= 5 ;
    }
    if (actor.aboard() == actor.mind.home) {
      factor /= 1 + Resting.ratePoint(actor, actor.mind.home) ;
    }
    return factor ;
  }
  
  
  public static void checkContagion(Actor actor) {
    if (! actor.health.organic()) return ;
    final Tile o = actor.origin() ;
    final float squalor = actor.world().ecology().squalorRating(o) ;
    
    for (Object d : Aptitudes.SPONTANEOUS_DISEASE) {
      final Condition c = (Condition) d ;
      //
      //  Let's say that under average squalor, you have a 10% chance of
      //  contracting an illness per day.  (Before immune function kicks in.)
      float infectChance = 0.1f * cleanFactor(actor) ;
      //
      //  Let's say that perfect hygiene reduces the chance by a factor of 10,
      //  and perfect squalor multiplies by 10.
      if (squalor > 0) infectChance *= 1 + (10 * squalor) ;
      else infectChance /= 1 - (10 * squalor) ;
      //
      //  Finally, let's say that each 5 points in virulence reduces the chance
      //  of contraction by half.  And that the chance is multiplied by spread.
      infectChance /= 1 << (int) ((c.virulence / 5) - 1) ;
      infectChance *= (c.spread + 0.1f) / 10.2f ;
      
      ///I.sayAbout(actor, "Contract chance/day for "+c+" is: "+infectChance) ;
      if (Rand.num() > (infectChance / World.STANDARD_DAY_LENGTH)) continue ;
      if (actor.traits.test(VIGOUR, c.virulence - 10, 1.0f)) continue ;
      
      I.say("INFECTING "+actor+" WITH "+c) ;
      actor.traits.incLevel(c, 0.1f) ;
    }
  }
  
  //
  //  Assuming a lifespan of 50 years of 60 days each, then a 2/10000 chance
  //  for cancer gives a 3000/5000 ~= 60% chance of getting cancer some day,
  //  assuming no immune response.  However, an adult with average vigour of 10
  //  has a 75% chance to fight it off, even without acquired resistance.
  
  
  
  public void affect(Actor a) {
    //
    //  TODO:  Give a mild recovery bonus to younger actors.
    final float ageBonus = 1.5f - a.health.ageLevel() ;
    
    //
    //  If this is contagious, consider spreading to nearby actors.
    if (Rand.index(10) < spread * cleanFactor(a)) {
      //
      //  TODO:  Increase the risk substantially if you're fighting or fucking
      //  the subject.
      for (Object o : a.world().presences.matchesNear(Mobile.class, a, 2)) {
        if (o instanceof Actor) {
          final Actor near = (Actor) o ;
          if (! near.health.organic()) continue ;
          if (near.species() != a.species() && Rand.index(10) != 0) continue ;
          if (near.traits.test(VIGOUR, virulence / 2, 1)) continue ;
          near.traits.incLevel(this, 0.1f) ;
        }
      }
    }
    //
    //  Grow in intensity based on latency.  If your presence is advanced
    //  enough, the host's immune system will start to fight back.
    //
    //  TODO:  You'll need to give the host some kind of 'acquired immunity',
    //  or the trait might never go away.  Use the bonus system for this.
    
    //
    //  Okay.  As the disease progresses, the trait advances.  As immune
    //  response accrues, the bonus to it decreases.  Once the disease is gone,
    //  the bonus is transformed into a permanent immunity.
    
    
    float immunePenalty = 5 * (3 - a.traits.traitLevel(this)) ;
    if (! a.traits.test(VIGOUR, virulence + immunePenalty, 0.1f)) {
      a.traits.incLevel(this, 2f / (latency * World.STANDARD_DAY_LENGTH)) ;
    }
    else a.traits.incLevel(this, -1f / World.STANDARD_DAY_LENGTH) ;
    
    //
    //  Impose penalties/bonuses to various traits/skills, if still around.
    final float progress = a.traits.traitLevel(this) ;
    if (progress <= 0) a.traits.setLevel(this, 0) ;
    else for (int i = affected.length ; i-- > 0 ;) {
      a.traits.incBonus(affected[i], modifiers[i] * progress / 2) ;
    }
  }
}



