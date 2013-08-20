


package src.game.social ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;
import src.user.* ;




//  Okay.  This is the next item to implement.
//  *  See if anyone's tending shop.
//  *  If so, place an order for an item.
//  *  The most qualified employee at the shop undertakes the job, if they're
//     not already too busy.  Less skilled employees get less demanding jobs,
//     but not fewer of them.  (Have the manufacturing class assess the chance
//     of successful completion.)
//     ...These assignments are recalculated every once in a while, if not
//     already undertaken, in case the roster or relative priorities change.
//  *  The client heads away and does other stuff.
//  *  The assigned employee finishes the manufacturing process.
//  *  If anyone is tending shop, the client comes to collect the item.
//  *  The client pays for the item, where the price difference is split
//     between taxes to the state and commission to the worker.


public class Purchase extends Plan {
  
  
  final Item item ;
  final Venue shop ;
  private Manufacture order = null ;
  
  
  public Purchase(Actor actor, Item item, Venue shop) {
    super(actor, item.type, shop) ;
    this.item = item ;
    this.shop = shop ;
  }
  
  
  public Purchase(Session s) throws Exception {
    super(s) ;
    item = Item.loadFrom(s) ;
    shop = (Venue) s.loadObject() ;
    order = (Manufacture) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    Item.saveTo(s, item) ;
    s.saveObject(shop) ;
    s.saveObject(order) ;
  }
  
  
  
  
  public void writeInformation(Description d) {
    d.append("purchasing "+item+" at ") ;
    d.append(shop) ;
  }
  
  
  public float priorityFor(Actor actor) {
    return ROUTINE ;
    /*
    if (order != null && ! order.complete()) {
      if (! order.facility.inWorld()) endActions() ;
      return 0 ;
    }
    return 4 ;
    //*/
  }
  
  
  public boolean complete() {
    //  If you've been waiting too long, return null ;
    return order != null && order.complete() ;
  }


  protected Behaviour getNextStep() {
    //
    //  TODO:  Ensure someone is attending the shop.
    
    if (order != null && order.complete()) {
      final Action pickup = new Action(
        actor, shop,
        this, "actionPickupItem",
        "Collecting", "talk"
      ) ;
      return pickup ;
    }
    
    if (order == null) {
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        "Placing Order", "talk"
      ) ;
      return placeOrder ;
    }
    return null ;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    //order = new Manufacture(item, shop) ;
    //shop.stocks.assignOrder(order) ;
    return true ;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    //  TODO:  Derive this from the item, not the item type.
    final int price = item.type.basePrice ;
    shop.inventory().incCredits(price) ;
    actor.inventory().incCredits(0 - price) ;
    
    shop.inventory().removeItem(item) ;
    actor.inventory().addItem(item) ;
    return true ;
  }
}





//
//  This plan allows actors to 'upgrade' their weapons, armour, outfits or
//  devices.  Apply to the device and weapon type suited to their vocation.
//  TODO:  Use this to extend/replace the Delivery/BulkDelivery classes?



/*
public String description() { return "purchase of "+item ; }

//  This plan is not forgotten when the actor switches to a different
//  behaviour- only when the plan itself decides that it's complete.
protected boolean persistant() { return true ; }

protected float priority() {
  if (waiting) return 0 ;
  final Actor actor = boundActor() ;
  return value * actor.world.planet.dayValue() ;
}

protected float calcLoss() {
  return 0 ;// - boundActor().AI.valueOfCredits(0 - price) ;
}


public Actionable nextStep() {
  final Actor citizen = boundActor() ;
  final Venue shop = boundTarget() ;
  waiting = false ;
  ///final Venue home = citizen.AI.home() ;
  //  If YOU have the item already, pay the wage required and you're done-
  if (citizen.inventory().hasItem(item)) {
    citizen.inventory().incCredits(0 - price) ;
    shop.inventory.incCredits(price) ;
    if (item.type instanceof ImplementType) {
      citizen.inventory().equipImplement(item) ;
    }
    else if (item.type instanceof OutfitType) {
      citizen.inventory().equipOutfit(item) ;
    }
    cancel() ;
    return null ;
  }
  //  If the venue has the item ready, go buy it.
  if (shop.inventory.hasItem(item)) {
    return new TransactAction(
      citizen, shop, "collecting "+item+" from "+shop,
      false, item, citizen, true
    ) ;
  }
  //  If the venue has an order for the item, wait.
  final Manufacture manfOrder = new Manufacture(item, shop) ;
  if (shop.state.orderAssigned(manfOrder)) {
    waiting = true ;
    return null ;
  }
  //  ...Otherwise, place an order.
  return new PlaceOrderAction(citizen, shop, item) ;
}
//*/
//
//  Purchase commodities for home.
//  Purchase a device/weapon, or outfit/armour.
//  Purchase rations, fuel cells or a medkit.





