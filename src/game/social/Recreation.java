


package src.game.social ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



public class Recreation extends Plan implements BuildConstants {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Venue venue ;
  
  
  public Recreation(Actor actor, Venue venue) {
    super(actor, venue) ;
    this.venue = venue ;
  }


  public Recreation(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
  }
  
  
  
  /**  Finding and evaluating targets-
    */
  public float priorityFor(Actor actor) {
    float priority = (ROUTINE * (1 - actor.health.moraleLevel())) + IDLE ;
    priority -= Plan.rangePenalty(actor, venue) ;
    priority *= rateVenue(venue, actor) / 10 ;
    return priority ;
  }
  
  
  public static Recreation findRecreation(Actor actor) {
    final Batch <Venue> venues = new Batch <Venue> () ;
    actor.world().presences.sampleFromKey(
      actor, actor.world(), 5, venues,
      Cantina.class
    ) ;
    if (actor.AI.home() != null) venues.add(actor.AI.home()) ;
    
    final Choice choice = new Choice(actor) ;
    for (Venue venue : venues) {
      choice.add(new Recreation(actor, venue)) ;
    }
    return (Recreation) choice.weightedPick(actor.AI.whimsy() * 2) ;
  }
  
  
  private static float rateVenue(Venue venue, Actor actor) {
    float rating = 0 ;
    if (venue instanceof Cantina) {
      rating += 4.0f + ((Cantina) venue).performValue() ;
      if (venue.stocks.amountOf(SOMA) > 0) {
        rating += 2 ;
      }
    }
    if (venue instanceof Holding) {
      return 2.0f + ((Holding) venue).upgradeLevel() ;
    }
    if (venue instanceof Bastion) {
      final Bastion b = (Bastion) venue ;
      return 3.0f + (b.structure.upgradeLevel(Bastion.NOBLE_QUARTERS) * 2) ;
    }
    return rating ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (priorityFor(actor) <= 0) return null ;
    final Action relax = new Action(
      actor, venue,
      this, "actionRelax",
      Action.TALK_LONG, "Relaxing at "+venue
    ) ;
    return relax ;
  }
  
  
  public boolean actionRelax(Actor actor, Venue venue) {
    final float interval = 1f / World.STANDARD_DAY_LENGTH ;
    float comfort = rateVenue(venue, actor) ;
    if (venue.stocks.amountOf(SOMA) > 0) {
      //  TODO:  You need to pay for this.  Also, what about intoxication?
      venue.stocks.removeItem(Item.withAmount(SOMA, 1 * interval)) ;
    }
    //
    //  TODO:  Chat at random with other occupants.
    
    comfort *= interval ;
    actor.health.adjustMorale(comfort) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Relaxing at ") ;
    d.append(venue) ;
  }
}











