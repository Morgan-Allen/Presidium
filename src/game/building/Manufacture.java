/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;




public class Manufacture extends Plan implements Behaviour {
  
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TIME_PER_UNIT = 30 ;
  
  final public Venue venue ;
  final public Conversion conversion ;
  
  private Item made, needed[] ;
  public int checkBonus = 0, timeMult = 1 ;
  private boolean pastFive = false ;
  
  
  
  public Manufacture(
    Actor actor, Venue venue, Conversion conversion, Item made
  ) {
    super(actor, venue) ;
    this.venue = venue ;
    this.made = made == null ? conversion.out : made ;
    this.conversion = conversion ;
    this.needed = conversion.raw ;
  }
  
  
  public Manufacture(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
    conversion = Conversion.loadFrom(s) ;
    made = Item.loadFrom(s) ;
    //timeTaken = s.loadFloat() ;
    this.needed = conversion.raw ;
    checkBonus = s.loadInt() ;
    timeMult   = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    Conversion.saveTo(s, conversion) ;
    Item.saveTo(s, made) ;
    //s.saveFloat(timeTaken) ;
    s.saveInt(checkBonus) ;
    s.saveInt(timeMult  ) ;
  }
  
  
  public Item made() {
    return made ;
  }
  
  
  public Item[] needed() {
    return needed ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false ;
    final Manufacture m = (Manufacture) p ;
    if (m.conversion != conversion) return false ;
    if (! m.made().matches(made)) return false ;
    return true ;
  }
  
  
  
  /**  Vary this based on delay since inception and demand at the venue-
    */
  public float priorityFor(Actor actor) {
    if (GameSettings.hardCore && ! hasNeeded()) return 0 ;
    //
    //  Don't work on this outside your shift (or at least make it more
    //  casual.)  TODO:  Implement 'secondary shifts' or overtime?
    final Venue venue = (Venue) actor.AI.work() ;
    if (! venue.personnel.onShift(actor)) {
      return 0 ;
    }
    //final List <Manufacture> orders = venue.stocks.specialOrders() ;
    //if (orders.size() > 0 && ! orders.contains(this)) return 0 ;
    final boolean hasNeeded = hasNeeded() ;
    float competition = begun() ? 0 : venue.personnel.assignedTo(this) ;
    //
    //  Vary priority based on how qualified to perform the task you are.
    final Conversion c = conversion ;
    float chance = 1.0f ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      chance *= actor.traits.chance(
        c.skills[i], c.skillDCs[i] - checkBonus
      ) ;
    }
    chance = (chance + 1) / 2f ;
    ///I.sayAbout(actor, "Chance: "+chance+" for "+this) ;
    float impetus = (URGENT * chance) + priorityMod - competition ;
    if (! hasNeeded) impetus /= 2 ;
    return Visit.clamp(impetus, IDLE, URGENT) ;
  }
  
  
  public boolean valid() {
    if (GameSettings.hardCore && ! hasNeeded()) return false ;
    return super.valid() ;
  }
  
  
  private boolean hasNeeded() {
    for (Item need : needed) {
      if (! venue.stocks.hasItem(need)) return false ;
    }
    return true ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished()) return true ;
    return pastFive || venue.stocks.hasItem(made) ;
  }
  
  
  public Behaviour getNextStep() {
    final float demand = venue.stocks.demandFor(made.type) ;
    if (demand > 0) made = Item.withAmount(made, demand + 5) ;
    if (venue.stocks.hasItem(made)) {
      return null ;
    }
    if (GameSettings.hardCore && ! hasNeeded()) return null ;
    return new Action(
      actor, venue,
      this, "actionMake",
      Action.REACH_DOWN, "Working"
    ) ;
  }
  
  
  public boolean actionMake(Actor actor, Venue venue) {
    //
    //  First, check to make sure you have adequate raw materials.  (In hard-
    //  core mode, raw materials are strictly essential, and will be depleted
    //  regardless of success.)
    ///I.say(actor+" making "+made) ;
    final boolean hasNeeded = hasNeeded() ;
    if (GameSettings.hardCore && ! hasNeeded) {
      abortBehaviour() ;
      return false ;
    }
    final Conversion c = conversion ;
    final int checkMod = (hasNeeded ? 0 : 5) - checkBonus ;
    final float timeTaken = made.amount * TIME_PER_UNIT * timeMult ;
    final float progInc = (hasNeeded ? 1 : 0.5f) / timeTaken ;
    //
    //  Secondly, make sure the skill tests all check out, and deplete any raw
    //  materials used up.
    boolean success = true ;
    for (int i = c.skills.length ; i-- > 0 ;) {
      success &= actor.traits.test(c.skills[i], c.skillDCs[i] + checkMod, 1) ;
    }
    if (success || GameSettings.hardCore && progInc > 0) {
      for (Item r : c.raw) {
        final Item used = Item.withAmount(r, r.amount * progInc) ;
        venue.inventory().removeItem(used) ;
      }
    }
    //
    //  Advance progress, and check if you're done yet.
    final float oldAmount = venue.stocks.amountOf(made) ;
    float progress = (success ? progInc : (progInc / 10f)) * made.amount ;
    if (progress + oldAmount > made.amount) progress = made.amount - oldAmount ;
    if (progress > 0) {
      venue.stocks.addItem(Item.withAmount(made, progress)) ;
    }
    ///I.say("EXACT AMOUNT OF "+made.type+" IS: "+venue.stocks.amountOf(made)) ;
    final float newAmount = venue.stocks.amountOf(made) ;
    pastFive = ((int) (oldAmount / 5) > (int) (newAmount / 5)) ;
    return venue.stocks.hasItem(made) ;
  }
  
  
  
  /**  Rendering and interface behaviour-
    */
  public void describeBehaviour(Description d) {
    d.append("Manufacturing "+made) ;
    //final float progress = venue.stocks.amountOf(made) / made.amount ;
    //d.append(" ("+((int) (progress * 100))+"%)") ;
  }
}








