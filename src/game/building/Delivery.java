/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.social.Auditing;
import src.game.actors.* ;
import src.game.base.* ;
import src.util.* ;
import src.user.* ;
import src.game.building.Inventory.Owner ;


//
//  TODO:  Barges need to be made more persistent.


public class Delivery extends Plan implements BuildConstants {
  
  final public static int
    TYPE_SHOPPING  = 0,
    TYPE_DELIVERS  = 1,
    TYPE_STRETCHER = 2,
    TYPE_DRIVEN    = 3 ;
  
  final static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_DROPOFF =  1,
    STAGE_RETURN  =  2,
    STAGE_DONE    =  3 ;
  final static int
    MIN_BULK = 5 ;
  
  private static boolean verbose = false ;
  
  
  final public Owner origin, destination ;
  final public Item items[] ;
  final Actor passenger ;
  
  private byte stage = STAGE_INIT ;
  private Suspensor suspensor ;
  public Vehicle driven ;
  
  
  
  public Delivery(Item item, Owner origin, Owner destination) {
    this(new Item[] { item }, origin, destination) ;
  }
  
  
  public Delivery(Item items[], Owner origin, Owner destination) {
    super(null, origin, destination) ;
    this.origin = origin ;
    this.destination = destination ;
    this.items = items ;
    this.passenger = null ;
  }
  
  
  public Delivery(Actor passenger, Venue destination) {
    super(null, passenger, destination) ;
    this.origin = null ;
    this.destination = destination ;
    this.items = new Item[0] ;
    this.passenger = passenger ;
  }
  
  
  public Delivery(Session s) throws Exception {
    super(s) ;
    items = new Item[s.loadInt()] ;
    for (int n = 0 ; n < items.length ;) items[n++] = Item.loadFrom(s) ;
    passenger = (Actor) s.loadObject() ;
    origin = (Owner) s.loadObject() ;
    destination = (Owner) s.loadObject() ;
    stage = (byte) s.loadInt() ;
    suspensor = (Suspensor) s.loadObject() ;
    driven = (Vehicle) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(items.length) ;
    for (Item i : items) Item.saveTo(s, i) ;
    s.saveObject(passenger) ;
    s.saveObject((Session.Saveable) origin) ;
    s.saveObject((Session.Saveable) destination) ;
    s.saveInt(stage) ;
    s.saveObject(suspensor) ;
    s.saveObject(driven) ;
  }
  
  
  int stage() { return stage ; }
  
  
  public boolean matchesPlan(Plan plan) {
    if (! super.matchesPlan(plan)) return false ;
    final Delivery d = (Delivery) plan ;
    if (d.origin != origin || d.destination != destination) return false ;
    boolean overlap = false;
    for (Item i : items) {
      for (Item dI : d.items) if (i.type == dI.type) overlap = true ;
    }
    return overlap ;
  }
  
  
  
  /**  Assessing targets and priorities-
    */
  private boolean isShopping() {
    return
      (destination instanceof Venue) &&
      ((Venue) destination).privateProperty() ;
  }
  
  
  public static float purchasePrice(Item item, Actor actor, Owner origin) {
    float TP = origin.priceFor(item.type) ;
    if (actor != null && actor.vocation().guild == Background.GUILD_MILITANT) {
      TP -= Auditing.MILITANT_RATION ;
      if (TP <= 0) return 0 ;
    }
    return item.amount * TP ;
  }
  
  
  private Item[] available(Actor actor) {
    final Batch <Item> available = new Batch <Item> () ;
    if (actor == null) {
      return items ;
    }
    
    final boolean shopping = isShopping() ;
    final Owner carrier = driven == null ? actor : driven ;
    
    if (stage <= STAGE_PICKUP) {
      float sumPrice = 0 ;
      for (Item i : items) {
        final float amount = origin.inventory().amountOf(i) ;
        if (amount <= 0) continue ;
        if (shopping) {
          sumPrice += purchasePrice(i, actor, origin) ;
          if (sumPrice > actor.gear.credits() / 2f) break ;
        }
        available.add(i) ;
      }
    }
    else {
      for (Item i : items) {
        if (! carrier.inventory().hasItem(i)) {
          final float amount = carrier.inventory().amountOf(i) ;
          if (amount > 0) available.add(Item.withAmount(i, amount)) ;
          continue ;
        }
        else available.add(i) ;
      }
    }
    return available.toArray(Item.class) ;
  }
  
  
  public float priorityFor(Actor actor) {
    final Item[] available = available(this.actor) ;
    if (available.length == 0) return 0 ;
    
    final float rangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(actor, destination) +
      Plan.rangePenalty(origin, destination)
    ) / (driven == null ? 2f : 10f) ;

    float costVal = 0 ;
    if (isShopping() && stage <= STAGE_PICKUP) {
      int price = 0 ;
      float foodVal = 0 ;
      for (Item i : available) {
        price += purchasePrice(i, actor, origin) ;
        if (Visit.arrayIncludes(ALL_FOOD_TYPES, i.type)) foodVal += i.amount ;
      }
      if (price > actor.gear.credits()) return 0 ;
      costVal = actor.AI.greedFor(price) * CASUAL ;
      costVal -= actor.health.hungerLevel() * CASUAL * foodVal ;
    }
    return Visit.clamp(
      ROUTINE + priorityMod - (costVal + rangePenalty), 0, URGENT
    ) ;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (driven != null) {
      if (driven.destroyed()) return false ;
      if (! driven.canPilot(actor)) return false ;
    }
    //
    //  TODO:  Put the passenger-delivery schtick into a different class.  It's
    //  making a mess of things here.
    if (passenger != null) return true ;
    if (stage < STAGE_RETURN && available(actor).length == 0) {
      if (driven != null) { stage = STAGE_RETURN ; return true ; }
      return false ;
    }
    return true ;
  }
  
  
  public boolean finished() {
    return stage == STAGE_DONE ;
  }
  
  
  public void onSuspend() {
    if (driven != null) driven.setPilot(null) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    
    if (stage == STAGE_INIT) {
      if (driven != null) {
        final Action boarding = new Action(
          actor, driven,
          this, "actionBoardVehicle",
          Action.STAND, "Boarding vehicle"
        ) ;
        return boarding ;
      }
      else stage = STAGE_PICKUP ;
    }
    if (stage == STAGE_PICKUP) {
      final Action pickup = new Action(
        actor, (passenger == null) ? origin : passenger,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      ) ;
      if (driven != null) pickup.setMoveTarget(driven) ;
      return pickup ;
    }
    if (stage == STAGE_DROPOFF) {
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      ) ;
      if (driven != null) dropoff.setMoveTarget(driven) ;
      else dropoff.setProperties(Action.CARRIES) ;
      return dropoff ;
    }
    if (stage == STAGE_RETURN && driven != null && driven.hangar() != null) {
      final Action returns = new Action(
        actor, driven.hangar(),
        this, "actionReturn",
        Action.REACH_DOWN, "Returning in vehicle"
      ) ;
      returns.setMoveTarget(driven) ;
      return returns ;
    }
    return null ;
  }
  
  
  public boolean actionBoardVehicle(Actor actor, Vehicle driven) {
    actor.goAboard(driven, actor.world()) ;
    if (! driven.setPilot(actor)) abortBehaviour() ;
    stage = STAGE_PICKUP ;
    return true ;
  }
  
  
  private float transferGoods(Owner a, Owner b) {
    if (a == null || b == null) return 0 ;
    float sumItems = 0 ;
    float totalPrice = 0 ;
    for (Item i : available(actor)) {
      final float TA = a.inventory().transfer(i, b) ;
      totalPrice += TA * purchasePrice(i, actor, origin) / i.amount ;
      sumItems += TA ;
    }
    origin.inventory().incCredits(totalPrice) ;
    if (isShopping()) actor.gear.incCredits(0 - totalPrice) ;
    else destination.inventory().incCredits(0 - totalPrice) ;
    return sumItems ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    //
    //  Vehicles get special treatment-
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour() ;
      driven.pathing.updateTarget(target) ;
      if (driven.aboard() == target) {
        transferGoods(origin, driven) ;
        stage = STAGE_DROPOFF ;
        return true ;
      }
      return false ;
    }
    //
    //  Perform the actual transfer of goods, make the payment required, and
    //  see if a suspensor is needed-
    final float sum = transferGoods(origin, actor) ;
    final boolean bulky = sum >= 5 || passenger != null ;
    if (verbose) I.sayAbout(actor, "Performing pickup!") ;
    //
    //  Passengers always require a suspensor.
    if (bulky) {
      final Suspensor suspensor = new Suspensor(actor, this) ;
      final Tile o = actor.origin() ;
      suspensor.enterWorldAt(o.x, o.y, o.world) ;
      this.suspensor = suspensor ;
    }
    if (target == passenger) suspensor.passenger = passenger ;
    stage = STAGE_DROPOFF ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Owner target) {
    if (stage != STAGE_DROPOFF) return false ;
    
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour() ;
      driven.pathing.updateTarget(target) ;
      if (driven.aboard() == target) {
        for (Service t : ALL_COMMODITIES) {
          driven.cargo.transfer(t, target) ;
        }
        //for (Item i : items) driven.cargo.transfer(i.type, target) ;
        stage = STAGE_RETURN ;
        return true ;
      }
      return false ;
    }
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i.type, target) ;
    if (passenger != null) {
      passenger.goAboard((Boardable) target, actor.world()) ;
    }
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionReturn(Actor actor, Venue target) {
    if (! driven.setPilot(actor)) abortBehaviour() ;
    driven.pathing.updateTarget(target) ;
    if (driven.aboard() == target) {
      driven.setPilot(null) ;
      actor.goAboard(target, actor.world()) ;
      stage = STAGE_DONE ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN) {
      d.append("Returning to ") ;
      d.append(origin) ;
      return ;
    }
    
    d.append("Delivering ") ;
    final Item available[] = available(actor) ;
    final Batch <Service> types = new Batch <Service> () ;
    for (Item i : available) types.add(i.type) ;
    d.appendList("", types) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}


