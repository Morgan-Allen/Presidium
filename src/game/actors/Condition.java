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
  
  
  private static boolean verbose = false ;
  
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
    super(Abilities.CONDITION, names) ;
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
      factor /= 1 + Math.max(0, Resting.ratePoint(actor, actor.mind.home)) ;
    }
    return factor ;
  }
  
  
  //
  //  Assuming a lifespan of 50 years of 60 days each, then a 2/10000 chance
  //  for cancer gives a 3000/5000 ~= 60% chance of getting cancer some day,
  //  assuming no immune response.  However, an adult with average vigour of 10
  //  has a 75% chance to fight it off, even without acquired resistance.
  
  public static void checkContagion(Actor actor) {
    if (! actor.health.organic()) return ;
    final Tile o = actor.origin() ;
    final float squalor = actor.world().ecology().squalorRating(o) ;
    
    for (Object d : Abilities.SPONTANEOUS_DISEASE) {
      final Condition c = (Condition) d ;
      //
      //  Let's say that under average squalor, you have a 10% chance of
      //  contracting an illness per day.  (Before immune function kicks in.)
      float infectChance = 0.1f * cleanFactor(actor) ;
      //
      //  Let's say that perfect hygiene reduces the chance by a factor of 2,
      //  and perfect squalor multiplies by a factor of 5.
      if (squalor > 0) infectChance *= 1 + (5 * squalor) ;
      else infectChance /= 1 - (2 * squalor) ;
      //
      //  Finally, let's say that each 5 points in virulence reduces the chance
      //  of contraction by half.  And that the chance is multiplied by spread.
      infectChance /= 1 << (int) ((c.virulence / 5) - 1) ;
      infectChance *= (c.spread + 0.1f) / 10.2f ;
      
      ///I.sayAbout(actor, "Contract chance/day for "+c+" is: "+infectChance) ;
      if (Rand.num() > (infectChance / World.STANDARD_DAY_LENGTH)) continue ;
      if (actor.traits.test(VIGOUR, c.virulence - 10, 1.0f)) continue ;
      
      if (verbose) I.say("INFECTING "+actor+" WITH "+c) ;
      actor.traits.incLevel(c, 0.1f) ;
    }
  }
  
  
  
  public void affect(Actor a) {
    //
    //  If this is contagious, consider spreading to nearby actors.
    if (spread > 0 && Rand.index(10) < spread * cleanFactor(a)) {
      //
      //  TODO:  Increase the risk substantially if you're fighting or fucking
      //  the subject.
      for (Object o : a.world().presences.matchesNear(Mobile.class, a, 2)) {
        if (o instanceof Actor) {
          final Actor near = (Actor) o ;
          if (! near.health.organic()) continue ;
          if (near.species() != a.species() && Rand.index(10) != 0) continue ;
          //
          //  Check if the actor already has the condition, or is immune to it-
          if (near.traits.traitLevel(this) != 0) continue ;
          if (near.traits.test(VIGOUR, virulence / 2, 0.1f)) continue ;
          near.traits.incLevel(this, 0.1f) ;
        }
      }
    }
    //
    //  Next, consider effects upon the host-
    final float
      progress = a.traits.traitLevel(this),
      response = 0 - a.traits.effectBonus(this),
      noticeDC = 5 * (3 - (progress + response)),
      ageBonus = 1.5f - a.health.ageLevel(),
      immuneDC = (virulence + noticeDC) / (1 + ageBonus) ;
    final float
      inc = 1 * 1f / (latency * World.STANDARD_DAY_LENGTH) ;
    //
    //  If you've acquired an immunity, have it fade over time-
    if (progress <= 0) {
      a.traits.setLevel(this, Visit.clamp(progress + (inc / 5), -1, 0)) ;
    }
    //
    //  Otherwise, see if your immune system can respond, based on how much of
    //  an immune response is already marshalled, and how advanced the disease
    //  is-
    else if (a.traits.test(VIGOUR, immuneDC, inc)) {
      a.traits.incBonus(this, 0 - (inc * 2)) ;
      a.traits.incLevel(this, 0 - inc) ;
      if (a.traits.useLevel(this) < 0) {
        final float immunity = virulence / -10f ;
        a.traits.setLevel(this, immunity) ;
        a.traits.setBonus(this, 0) ;
      }
    }
    //
    //  If that fails, advance the disease-
    else {
      a.traits.setBonus(this, Visit.clamp((inc / 2) - response, -3, 0)) ;
      a.traits.setLevel(this, Visit.clamp(progress + inc, 0, 3)) ;
    }
    //
    //  Impose penalties/bonuses to various attributes, if still symptomatic-
    final float symptoms = progress - response ;
    if (symptoms > 0) for (int i = affected.length ; i-- > 0 ;) {
      a.traits.incBonus(affected[i], modifiers[i] * symptoms / 2) ;
    }
    if (verbose && I.talkAbout == a) {
      I.say("Reporting on: "+this+" for "+a) ;
      I.say("  Immune DC/vigour: "+immuneDC+"/"+a.traits.useLevel(VIGOUR)) ;
      I.say("  Test chance: "+a.traits.chance(VIGOUR, immuneDC)) ;
      I.say("  Progress/response: "+progress+"/"+response) ;
    }
  }
}












