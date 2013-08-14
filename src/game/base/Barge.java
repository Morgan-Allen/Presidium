


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
    //
    //  Firstly, check whether you even need to exist any more-
    super.updateAsMobile() ;
    if (
      (! followed.inWorld()) ||
      ! followed.AI.agenda().includes(tracked)
    ) {
      exitWorld() ;
      return ;
    }
    //
    //  If so, update your position so as to follow behind the actor-
    final Vec3D FP = followed.position(null) ;
    final Vec2D FR = new Vec2D().setFromAngle(followed.rotation()) ;
    final float idealDist = followed.radius() + this.radius() ;
    FR.scale(0 - idealDist) ;
    FP.x += FR.x ;
    FP.y += FR.y ;
    nextPosition.setTo(FP) ;
    nextPosition.z = aboveGroundHeight() ;
    nextRotation = followed.rotation() ;
    //
    //  And if you have a passenger, update their position.
    if (passenger != null) {
      if (followed.indoors()) {
        final Boardable toBoard = followed.aboard() ;
        passenger.goAboard(followed.aboard(), world) ;
        passenger.setHeading(toBoard.position(null), 0, false, world) ;
      }
      else {
        final Vec3D raise = new Vec3D(nextPosition) ;
        raise.z += 0.15f ;
        passenger.setHeading(raise, nextRotation, false, world) ;
      }
    }
  }
  
  
  protected float aboveGroundHeight() { return 0.15f ; }
  public float radius() { return 0.0f ; }
  public Base base() { return null ; }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return ;
    super.renderFor(rendering, base) ;
  }
}





