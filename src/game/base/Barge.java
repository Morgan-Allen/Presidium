


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



//
//  Use for the bulk transport of goods from place to place?  Oh- have the
//  actor board a vehicle for the purpose!
public class Barge extends Mobile {
  
  
  
  final static Model BARGE_MODEL = MS3DModel.loadMS3D(
    Barge.class, "media/Vehicles/", "Barge.ms3d", 0.015f
  ) ;
  
  final Actor followed ;
  final float followDist ;
  
  private Actor passenger = null ;
  private Item cargo = null ;
  
  
  
  public Barge(Actor followed) {
    super() ;
    this.followed = followed ;
    this.followDist = followed.radius() + this.radius() ;
    attachSprite(BARGE_MODEL.makeSprite()) ;
  }
  
  
  public Barge(Session s) throws Exception {
    super(s) ;
    followed = (Actor) s.loadObject() ;
    followDist = s.loadFloat() ;
    passenger = (Actor) s.loadObject() ;
    cargo = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(followed) ;
    s.saveFloat(followDist) ;
    s.saveObject(passenger) ;
    Item.saveTo(s, cargo) ;
  }
  
  
  public float radius() { return 0.33f ; }
  
  
  
  /**  Speciality methods for dealing with cargo-
    */
  /*
  public void enterWorld() {
    super.enterWorld() ;
  }
  
  
  public void exitWorld() {
    if (! inWorld()) return ;
    dropCargoAndExitAt(null) ;
  }
  
  
  public boolean addItemFrom(Item item, Inventory.Owner owner) {
    if (item.refers instanceof Mobile) {
      ((Mobile) item.refers).goAboard(this) ;
      return true ;
    }
    else {
      final float amount = Math.min(
        owner.inventory().amountOf(item), item.amount
      ) ;
      if (amount == 0) return false ;
      item = new Item(item.type, amount) ;
      cargo.add(item) ;
      return true ;
    }
  }
  
  
  public void dropCargoAndExitAt(Inventory.Owner target) {
    for (Item carried : cargo) {
      if (carried.refers instanceof Mobile && target instanceof Boardable) {
        ((Mobile) carried.refers).goAboard((Boardable) target) ;
        continue ;
      }
      target.inventory().addItem(carried) ;
    }
    cargo.clear() ;
    super.exitWorld() ;
  }
  //*/
  
  
  
  /**  Satisfying the contract of the Boardable interface-
    */
  /*
  public Tile[] entrances() {
    return new Tile[] { this.location } ;
  }
  
  
  public Series <Mobile> occupants() {
    final Batch <Mobile> inside = new Batch <Mobile> () ;
    return inside ;
  }
  
  
  public boolean allowsEntry(Actor actor) {
    return false ;
  }
  
  
  public void mobileEntry(Mobile entered) {
    cargo.add(new Item(Economy.CARRIED, 1, entered)) ;
  }
  
  
  public void mobileExits(Mobile exiting) {
    for (Item i : cargo) if (i.refers == exiting) cargo.remove(i) ;
  }
  //*/
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates) {
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    float distance = Spacing.distance(this, followed) ;
    if (distance > 0) headTowards(followed, distance) ;
    goAboard(followed.aboard(), world) ;
    if (passenger != null) {
      passenger.setHeading(nextPosition, nextRotation, false, world) ;
      passenger.goAboard(followed.aboard(), world) ;
    }
  }
  
  
  protected float aboveGroundHeight() {
    //  Vary this based on the hands-height of what you follow!
    return 0.33f ;
  }


  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return ;
    super.renderFor(rendering, base) ;
  }
  
  
  public String fullName() {
    return "Barge" ;
  }


  public String helpInfo() {
    return "Barges help carry goods or persons from place to place" ;
  }


  public Composite portrait(BaseUI UI) {
    final Composite c = new Composite(UI) ;
    return c ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
  }
  
  
  public Base assignedBase() {
    return null ;
  }
}






/*
public static Barge bargeFor(Actor actor) {
  if (actor == null) return null ;
  for (Item item : actor.inventory().matches(Economy.CARRIED)) 
    return (Barge) item.refers ;
  return null ;
}

public static Barge attachTo(Actor actor, Item carried) {
  Barge barge = bargeFor(actor) ;
  if (barge != null) return barge ;
  else {
    barge = new Barge(actor, actor.targRadius(), carried) ;
    final Item record = new Item(Economy.CARRIED, 1, barge) ;
    actor.inventory().addItem(record) ;
    barge.enterWorldAt(actor.cornerX(), actor.cornerY()) ;
    return barge ;
  }
}

public static void detachFrom(Actor actor) {
  final Barge barge = bargeFor(actor) ;
  if (barge == null) return ;
  actor.inventory().removeItem(new Item(Economy.CARRIED, 1, barge)) ;
  barge.exitWorld() ;
}
//*/







/*
public float priceFor(ItemType item) { return 0 ; }
public int elementType() { return TYPE_VEHICLE ; }
public float targRadius() { return 0.33f ; }
//*/