/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.actors ;
import src.game.common.* ;
import src.util.* ;



public class Choice {
  
  

  
  final Actor actor ;
  final Batch <Behaviour> plans = new Batch <Behaviour> () ;
  final Batch <Float> weights = new Batch <Float> () ;
  
  
  public Choice(Actor actor) {
    this.actor = actor ;
  }
  
  public Choice(Actor actor, Series <Behaviour> plans) {
    this.actor = actor ;
    for (Behaviour p : plans) add(p) ;
  }
  
  
  
  private boolean isValid(Behaviour plan) {
    if (plan == null || plan.complete()) return false ;
    if (plan.nextStepFor(actor) == null) return false ;
    return true ;
  }
  
  
  public void addCurrentBehaviour(Behaviour plan) {
    if (! isValid(plan)) return ;
    plans.add(plan) ;
    weights.add(plan.priorityFor(actor) + 2) ;
  }
  
  
  public void addOtherBehaviour(Behaviour plan) {
    if (! isValid(plan)) return ;
    plans.add(plan) ;
    weights.add(plan.priorityFor(actor) + (Rand.range(-1, 1) * 1)) ;
  }
  
  
  public void add(Behaviour plan) {
    add(plan, plan.priorityFor(actor)) ;
  }
  
  
  public void add(Behaviour plan, float weight) {
    if (! isValid(plan)) return ;
    plans.add(plan) ;
    weights.add(Visit.clamp(weight, 0, Behaviour.PARAMOUNT)) ;
  }
  
  
  /**  Picks a plan from those assigned earlier using priorities to weight the
    *  likelihood of their selection.
    */
  public Behaviour weightedPick() {
    final Behaviour picked = (Behaviour) Rand.pickFrom(plans, weights) ;
    return picked ;
  }
  
  
  public Behaviour pickMostUrgent() {
    float bestPick = 0 ;
    Behaviour picked = null ;
    final Float weightsA[] = (Float[]) weights.toArray(Float.class) ;
    final Behaviour plansA[] = (Behaviour[]) plans.toArray(Behaviour.class) ;
    for (int i = 0 ; i < plansA.length ; i++) {
      final float w = weightsA[i] ;
      if (w > bestPick) { bestPick = w ; picked = plansA[i] ; }
    }
    return picked ;
  }
}






