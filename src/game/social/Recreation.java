


package src.game.social ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;



public class Recreation extends Plan implements EconomyConstants {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean verbose = false ;
  
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
    if (actor.mind.work() instanceof Venue) {
      final Venue v = (Venue) actor.mind.work() ;
      if (v.personnel.onShift(actor)) return IDLE ;
    }
    float priority = (ROUTINE * (1 - actor.health.moraleLevel())) + IDLE ;
    priority -= Plan.rangePenalty(actor, venue) ;
    priority *= rateVenue(venue, actor) / 10 ;
    //
    //  TODO:  Vary based on debauched/indolent traits, et cetera-
    
    if (verbose) I.sayAbout(actor, "Relax priority for "+venue+": "+priority) ;
    return priority ;
  }
  
  
  public static Recreation findRecreation(Actor actor) {
    final Batch <Venue> venues = new Batch <Venue> () ;
    actor.world().presences.sampleFromKey(
      actor, actor.world(), 5, venues,
      Cantina.class
    ) ;
    if (actor.mind.home() != null) venues.add(actor.mind.home()) ;
    
    final Choice choice = new Choice(actor) ;
    for (Venue venue : venues) {
      choice.add(new Recreation(actor, venue)) ;
    }
    return (Recreation) choice.weightedPick(actor.mind.whimsy()) ;
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
    
    if (
      actor.traits.traitLevel(SOMA_HAZE) <= 0 &&
      venue.stocks.amountOf(SOMA) > 0.1f
      && (somaPrice(venue) < actor.gear.credits() / 2)
    ) {
      final Action dropSoma = new Action(
        actor, venue,
        this, "actionDropSoma",
        Action.FALL, "Dropping soma"
      ) ;
      return dropSoma ;
    }
    
    if (venue instanceof Cantina) {
      final Cantina c = (Cantina) venue ;
      final Action gamble = c.nextGambleFor(actor) ;
      if (gamble != null && Rand.index(10) < gamble.priorityFor(actor)) {
        return gamble ;
      }
    }
    
    final Action relax = new Action(
      actor, venue,
      this, "actionRelax",
      Action.TALK_LONG, "Relaxing"
    ) ;
    
    return relax ;
  }
  
  
  private float somaPrice(Venue venue) {
    if (venue instanceof Cantina) {
      final Cantina c = (Cantina) venue ;
      return c.priceSoma() * 0.1f ;
    }
    return 0 ;
  }
  
  
  public boolean actionDropSoma(Actor actor, Venue venue) {
    final float price = somaPrice(venue) ;
    if (price > actor.gear.credits() / 2) return false ;
    venue.stocks.incCredits(price) ;
    actor.gear.incCredits(-price) ;
    venue.stocks.removeItem(Item.withAmount(SOMA, 0.1f)) ;
    actor.traits.incLevel(SOMA_HAZE, 0.1f) ;
    return true ;
  }
  
  
  public boolean actionRelax(Actor actor, Venue venue) {
    final float interval = 1f / World.STANDARD_DAY_LENGTH ;
    float comfort = rateVenue(venue, actor) ;
    if (actor.traits.traitLevel(SOMA_HAZE) > 0) {
      comfort++ ;
    }
    //
    //  TODO:  Chat at random with other occupants (using the Dialogue class.)
    //
    //  TODO:  Have morale converge to a particular level based on surroundings,
    //  rather than gaining a continual increase!
    comfort *= interval ;
    actor.health.adjustMorale(comfort) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! describedByStep(d)) d.append("Relaxing") ;
    d.append(" at ") ;
    d.append(venue) ;
  }
}











