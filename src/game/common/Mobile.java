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
  implements Schedule.Updates, Selectable
{
  
  protected float
    rotation,
    nextRotation ;
  //protected Vec3D
    //facing = new Vec3D(1, 0, 0) ;
  protected final Vec3D
    position = new Vec3D(),
    nextPosition = new Vec3D() ;
  private Boardable
    aboard = null,
    boarding = null ;
  
  private ListEntry <Mobile> entry = null ;
  
  
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
    //facing.loadFrom(s.input()) ;
    aboard = (Boardable) s.loadTarget() ;
    boarding = (Boardable) s.loadTarget() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(rotation    ) ;
    s.saveFloat(nextRotation) ;
    position    .saveTo(s.output()) ;
    nextPosition.saveTo(s.output()) ;
    //facing.saveTo(s.output()) ;
    s.saveTarget(aboard) ;
    s.saveTarget(boarding) ;
  }
  
  
  
  /**  Again, more data-definition methods subclasses might well override.
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.setTo(position) ;
  }
  
  public float radius() { return 0.25f ; }
  public int pathType() { return Tile.PATH_CLEAR ; }
  public int owningType() { return NOTHING_OWNS ; }
  
  public abstract Base assignedBase() ;
  
  
  
  /**  Called whenever the mobile enters/exits the world...
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
    world.toggleActive(this, false) ;
    if (aboard != null) aboard.setInside(this, false) ;
    world().schedule.unschedule(this) ;
    super.exitWorld() ;
  }
  
  ListEntry <Mobile> entry() { return entry ; }
  
  
  
  /**  Dealing with position and motion-
    */
  public void setPosition(float xp, float yp, World world) {
    final Tile
      oldTile = origin(),
      newTile = world.tileAt(xp, yp) ;
    super.setPosition(xp, yp, world) ;
    ///I.say("Setting mobile position") ;
    nextPosition.setTo(position.set(xp, yp, 0)) ;
    
    if (inWorld()) {
      if (oldTile != newTile) onTileChange(oldTile, newTile) ;
      final Boardable toBoard = (newTile.owner() instanceof Boardable) ?
        (Boardable) newTile.owner() :
        newTile ;
      if (aboard != toBoard) {
        if (aboard != null) oldTile.setInside(this, false) ;
        if (toBoard != null) toBoard.setInside(this, true) ;
        aboard = toBoard ;
      }
    }
  }
  
  
  public Boardable aboard() {
    return aboard ;
  }
  
  
  public void setHeading(Target target, float speed) {
    ///I.say("Projecting motion toward: "+target) ;
    //
    //  Determine the appropriate offset and angle for this target-
    final Vec3D p = target.position(null) ;
    final Vec2D disp = new Vec2D(
      p.x - position.x,
      p.y - position.y
    ) ;
    final float dist = disp.length() ;
    final float angle = dist == 0 ? 0 : disp.normalise().toAngle() ;
    //facing.setTo(disp) ;
    //
    //  Determine how far one can move this update.  (Later on, this might need
    //  doing for rotation as well?)
    final float moveRate = speed / PlayLoop.UPDATES_PER_SECOND ;
    disp.scale(Math.min(moveRate, dist)) ;
    disp.x += position.x ;
    disp.y += position.y ;
    //
    //  Determine whether the upcoming target (or, failing that, next tile,) is
    //  a suitable target for boarding.
    final Tile comingTile = world().tileAt(disp.x, disp.y) ;
    boarding = null ;
    if (target instanceof Boardable) {
      final Boardable bT = (Boardable) target ;
      if (bT.area(null).contains(disp.x, disp.y) && bT.isEntrance(aboard)) {
        boarding = bT ;
      }
    }
    if (boarding == null && aboard != null) {
      if (aboard.area(null).contains(disp.x, disp.y)) {
        boarding = aboard ;
      }
    }
    if (boarding == null) {
      if ((speed <= 0) || checkTileClear(comingTile)) {
        boarding = comingTile ;
      }
    }
    //
    //  If it's possible to board the upcoming tile/target, update heading.
    //  Otherwise, stay put and raise an alert-
    if (boarding == null) {
      onMotionBlock(comingTile) ;
      nextPosition.setTo(position) ;
      nextRotation = rotation ;
    }
    else {
      nextPosition.setTo(disp) ;
      nextRotation = angle ;
    }
    if (boarding == null || boarding instanceof Tile) {
      nextPosition.z = world.terrain().trueHeight(disp.x, disp.y) ;
    }
    else nextPosition.z = boarding.position(p).z ;
    //*/
  }
  
  
  protected void updateAsMobile() {
    
    final Tile
      oldTile = origin(),
      newTile = world().tileAt(nextPosition.x, nextPosition.y) ;
    if (oldTile != newTile) onTileChange(oldTile, newTile) ;
    //
    //  ...There's a problem here.  You need to have some kind of emergency
    //  fallback in the event that you can't follow your specified path.
    /*
    if ((! checkTileClear(newTile)) && newTile.owner() instanceof Boardable) {
      boarding = (Boardable) newTile.owner() ;
    }
    //*/
    if (boarding != aboard) {
      if (aboard != null) aboard.setInside(this, false) ;
      if (boarding != null) boarding.setInside(this, true) ;
      aboard = boarding ;
    }
    position.setTo(nextPosition) ;
    rotation = nextRotation ;
  }
  
  
  protected boolean checkTileClear(Tile t) {
    return ! t.blocked() ;
  }
  
  protected void onMotionBlock(Tile t) {}
  
  protected void onTileChange(Tile oldTile, Tile newTile) {}
  
  public abstract void pathingAbort() ;
  
  
  public float scheduledInterval() { return 1.0f ; }
  
  
  public boolean indoors() {
    return aboard != null && ! (aboard instanceof Tile) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    final float alpha = PlayLoop.frameTime() ;
    v.setTo(position).scale(1 - alpha) ;
    v.add(nextPosition, alpha, v) ;
    v.z += height() ;
    return v ;
  }
  
  
  protected void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float alpha = PlayLoop.frameTime() ;
    s.position.setTo(position).scale(1 - alpha) ;
    s.position.add(nextPosition, alpha, s.position) ;
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation) ;
    s.rotation = (rotation + (rotateChange * alpha) + 360) % 360 ;
    ///I.say("sprite position/rotation: "+s.position+" "+s.rotation) ;
    rendering.addClient(s) ;
  }
}






