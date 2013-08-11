


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
  final Behaviour tracked ;
  
  public Actor passenger = null ;
  public Item cargo = null ;
  
  
  
  public Barge(Actor followed, Behaviour tracked) {
    super() ;
    this.followed = followed ;
    this.tracked = tracked ;
    attachSprite(BARGE_MODEL.makeSprite()) ;
  }
  
  
  public Barge(Session s) throws Exception {
    super(s) ;
    followed = (Actor) s.loadObject() ;
    tracked = (Behaviour) s.loadObject() ;
    passenger = (Actor) s.loadObject() ;
    cargo = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(followed) ;
    s.saveObject(passenger) ;
    Item.saveTo(s, cargo) ;
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates) {
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    if (
      (! followed.inWorld()) ||
      ! followed.psyche.agenda().includes(tracked)
    ) {
      exitWorld() ;
      return ;
    }
    followed.position(nextPosition) ;
    //
    //  Update your position so as to follow the actor-
    final Vec3D FP = followed.position(null) ;
    final Vec3D pos = new Vec3D(FP) ;
    pos.sub(this.position).normalise() ;
    final float idealDist = followed.radius() + this.radius() ;
    final float angle = new Vec2D().setTo(pos).toAngle() ;
    pos.scale(0 - idealDist).add(FP) ;
    super.setHeading(pos, angle, false, world) ;
    
    //goAboard(followed.aboard(), world) ;
    /*
    if (passenger != null) {
      passenger.setHeading(nextPosition, nextRotation, false, world) ;
      passenger.goAboard(followed.aboard(), world) ;
    }
    //*/
  }
  
  
  protected float aboveGroundHeight() { return 0.33f ; }
  public float radius() { return 0.0f ; }
  public Base assignedBase() { return null ; }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return ;
    super.renderFor(rendering, base) ;
  }
}






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