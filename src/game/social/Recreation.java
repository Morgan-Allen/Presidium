


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
    float priority = URGENT * (1 - actor.health.moraleLevel()) ;
    priority *= rateVenue(venue) / 10 ;
    priority -= Plan.rangePenalty(actor, venue) + 0.5f ;
    ///if (BaseUI.isPicked(actor)) I.say("  RECREATION PRIORITY: "+priority) ;
    return priority ;
  }
  
  
  public static Venue findRecreation(Actor actor) {
    return actor.world().presences.randomMatchNear(
      Cantina.class, actor, World.DEFAULT_SECTOR_SIZE
    ) ;
  }
  
  
  private static float rateVenue(Venue venue) {
    float rating = 0 ;
    if (venue instanceof Cantina) {
      rating += 4.0f + ((Cantina) venue).performValue() ;
      if (venue.stocks.amountOf(SOMA) > 0) {
        rating += 2 ;
      }
    }
    return rating ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (priorityFor(actor) <= 0) return null ;
    if (venue instanceof Cantina) {
      final Action relax = new Action(
        actor, venue,
        this, "actionCantinaRelax",
        Action.TALK_LONG, "Relaxing at "+venue
      ) ;
      return relax ;
    }
    return null ;
  }
  
  
  public boolean actionCantinaRelax(Actor actor, Venue venue) {
    final float interval = 1f / World.DEFAULT_DAY_LENGTH ;
    float comfort = rateVenue(venue) ;
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











