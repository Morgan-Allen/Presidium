/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.Selectable ;
import src.util.* ;



public abstract class Mobile extends Element
  implements WorldSchedule.Updates, Selectable
{
  
  protected float
    rotation,
    nextRotation ;
  protected Vec3D
    facing = new Vec3D(1, 0, 0) ;
  protected final Vec3D
    position = new Vec3D(),
    nextPosition = new Vec3D() ;
  
  private ListEntry <Mobile> entry = null ;
  private Boardable aboard = null ;
  
  
  /**  Basic constructors and save/load functionality-
    */
  public Mobile() {
  }
  
  
  public Mobile(Session s) throws Exception {
    super(s) ;
    this.rotation     = s.loadFloat() ;
    this.nextRotation = s.loadFloat() ;
    position.    loadFrom(s.input()) ;
    nextPosition.loadFrom(s.input()) ;
    facing. loadFrom(s.input()) ;
    //base = (Base) s.loadObject() ;
    aboard = (Boardable) s.loadTarget() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(rotation    ) ;
    s.saveFloat(nextRotation) ;
    position    .saveTo(s.output()) ;
    nextPosition.saveTo(s.output()) ;
    facing .saveTo(s.output()) ;
    //s.saveObject(base) ;
    s.saveTarget(aboard) ;
  }
  
  //public void setBase(Base realm) { this.base = realm ; }
  //public Base realm() { return base ; }
  
  
  /**  Again, more data-definition methods subclasses might well override.
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.setTo(position) ;
  }
  
  public float radius() { return 0.25f ; }
  public int pathType() { return Tile.PATH_CLEAR ; }
  public int owningType() { return NOTHING_OWNS ; }
  
  
  
  /**  Called whenever the actor enters/exits the world...
   */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    I.say("mobile entering world at: "+x+" "+y) ;
    world().schedule.scheduleForUpdates(this) ;
    origin().setInside(this, true) ;
    world().toggleActive(this, true) ;
  }
  
  void setEntry(ListEntry <Mobile> e) { entry = e ; }
  
  
  public void exitWorld() {
    world().toggleActive(this, false) ;
    origin().setInside(this, false) ;
    world().schedule.unschedule(this) ;
    super.exitWorld() ;
  }
  
  ListEntry <Mobile> entry() { return entry ; }
  
  
  public void setPosition(float xp, float yp, World world) {
    final Tile oldTile = origin() ;
    super.setPosition(xp, yp, world) ;
    nextPosition.setTo(position.set(xp, yp, 0)) ;
    final Tile newTile = origin() ;
    if (inWorld() && oldTile != newTile) {
      if (oldTile != null) oldTile.setInside(this, false) ;
      if (newTile != null) newTile.setInside(this, true) ;
      onTileChange(oldTile, newTile) ;
    }
  }
  
  
  public void setHeading(Vec3D nextPos, float nextRot) {
    nextPosition.setTo(nextPos) ;
    nextRotation = nextRot ;
  }
  
  
  public void projectHeading(Vec3D t, float speed) {
    Vec2D disp = new Vec2D() ;
    disp.set(
      t.x - position.x,
      t.y - position.y
    ) ;
    disp.normalise() ;
    facing.setTo(disp) ;
    final float
      angle = disp.toAngle(),
      dist = disp.length() ;
    if (dist < this.radius()) {
      return ;
    }
    //
    //  Calculate how far forward one can safely move in this interval-
    float moveRate = 0 ; if (speed > 0) {
      moveRate = speed / PlayLoop.UPDATES_PER_SECOND ;
      if (dist < moveRate) moveRate = dist ;
    }
    disp.scale(moveRate) ;
    disp.x += position.x ;
    disp.y += position.y ;
    //
    //  -I suspect you may need a more thorough system for dealing with
    //  blockage.
    final Tile comingTile = world().tileAt(disp.x, disp.y) ;
    if ((speed <= 0) || checkTileClear(comingTile)) {
      nextPosition.setTo(disp) ;
      nextRotation = angle ;
    }
    else onMotionBlock(comingTile) ;
  }
  
  
  protected void updateAsMobile() {
    final Tile
      oldTile = origin(),
      newTile = world().tileAt(nextPosition.x, nextPosition.y) ;
    if (checkTileClear(newTile)) {
      position.setTo(nextPosition) ;
      rotation = nextRotation ;
      super.setPosition(nextPosition.x, nextPosition.y, world()) ;
      if (newTile != oldTile) {
        setAboard(oldTile, false) ;
        setAboard(newTile, true ) ;
        onTileChange(oldTile, newTile) ;
      }
    }
    else {
      nextRotation = rotation ;
      nextPosition.setTo(position) ;
      onMotionBlock(newTile) ;
    }
  }
  
  
  protected boolean checkTileClear(Tile t) { return true ; }
  protected void onMotionBlock(Tile t) {}
  protected void onTileChange(Tile oldTile, Tile newTile) {}
  
  public float scheduledInterval() { return 1.0f ; }
  public void updateAsScheduled() {}
  
  
  
  /**  Dealing with venues, vehicles, security checks, etc-
    */
  public Boardable aboard() {
    return aboard ;
  }
  
  protected void setAboard(Tile tile, boolean is) {
    if (tile == null) {
      if (is) aboard = null ;
      return ;
    }
    if (tile.owner() instanceof Boardable) {
      if (is) aboard = (Boardable) tile.owner() ;
      ((Boardable) tile.owner()).setInside(this, is) ;
    }
    else {
      if (is) aboard = null ;
      tile.setInside(this, is) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float alpha = PlayLoop.frameTime() ;
    s.position.setTo(position).scale(1 - alpha) ;
    s.position.add(nextPosition, alpha, s.position) ;
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation) ;
    s.rotation = (rotation + (rotateChange *  alpha) + 360) % 360 ;
    rendering.addClient(s) ;
    //super.renderFor(rendering, base) ;
  }
}


