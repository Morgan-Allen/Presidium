/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public abstract class Mobile extends Element
  implements Schedule.Updates
{
  
  final static int
    MAX_PATH_SCAN = World.DEFAULT_SECTOR_SIZE ;
  
  protected float
    rotation,
    nextRotation ;
  protected final Vec3D
    position = new Vec3D(),
    nextPosition = new Vec3D() ;
  
  protected Boardable aboard ;
  private ListEntry <Mobile> entry = null ;
  final public MobilePathing pathing = initPathing() ;
  
  
  
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
    aboard = (Boardable) s.loadTarget() ;
    //boarding = (Boardable) s.loadTarget() ;
    if (pathing != null) pathing.loadState(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(rotation    ) ;
    s.saveFloat(nextRotation) ;
    position    .saveTo(s.output()) ;
    nextPosition.saveTo(s.output()) ;
    s.saveTarget(aboard) ;
    //s.saveTarget(boarding) ;
    if (pathing != null) pathing.saveState(s) ;
  }
  
  
  public abstract Base base() ;
  protected MobilePathing initPathing() { return null ; }
  
  
  
  /**  Again, more data-definition methods subclasses might well override.
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.setTo(position) ;
  }
  
  public float rotation() {
    return rotation ;
  }
  
  public float radius() { return 0.25f ; }
  public int pathType() { return Tile.PATH_CLEAR ; }
  public int owningType() { return NOTHING_OWNS ; }
  public boolean isMobile() { return true ; }
  
  
  /**  Called whenever the mobile enters/exits the world...
   */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    (aboard = origin()).setInside(this, true) ;
    world().schedule.scheduleForUpdates(this) ;
    world().toggleActive(this, true) ;
  }
  
  
  public void exitWorld() {
    world.toggleActive(this, false) ;
    world().schedule.unschedule(this) ;
    if (aboard != null) aboard.setInside(this, false) ;
    super.exitWorld() ;
  }
  
  
  void setEntry(ListEntry <Mobile> e) { entry = e ; }
  ListEntry <Mobile> entry() { return entry ; }
  public float scheduledInterval() { return 1.0f ; }
  
  
  
  /**  Dealing with pathing-
    */
  public Boardable aboard() {
    return aboard ;
  }
  
  
  public void goAboard(Boardable toBoard, World world) {
    if (aboard != null) aboard.setInside(this, false) ;
    aboard = toBoard ;
    if (aboard != null) aboard.setInside(this, true) ;
    
    final Vec3D p = this.nextPosition ;
    if (! aboard.area(null).contains(p.x, p.y)) {
      final Vec3D pos = toBoard.position(null) ;
      pos.z += aboveGroundHeight() ;
      setHeading(pos, nextRotation, true, world) ;
    }
  }
  

  public void setPosition(float xp, float yp, World world) {
    nextPosition.set(xp, yp, aboveGroundHeight()) ;
    setHeading(nextPosition, nextRotation, true, world) ;
  }
  
  
  public void setHeading(
    Vec3D pos, float rotation, boolean instant, World world
  ) {
    final Tile
      oldTile = origin(),
      newTile = world.tileAt(pos.x, pos.y) ;
    super.setPosition(pos.x, pos.y, world) ;
    nextPosition.setTo(pos) ;
    nextRotation = rotation ;
    
    if (aboard != newTile) {
      if (aboard != null) aboard.setInside(this, false) ;
      (aboard = newTile).setInside(this, true) ;
    }
    if (instant) {
      this.position.setTo(pos) ;
      this.rotation = rotation ;
      if (inWorld() && oldTile != newTile) {
        onTileChange(oldTile, newTile) ;
      }
    }
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    world.presences.togglePresence(this, oldTile, false) ;
    world.presences.togglePresence(this, newTile, true ) ;
  }
  
  
  public boolean indoors() {
    return aboard != null && ! (aboard instanceof Tile) ;
  }
  
  
  protected void updateAsMobile() {
    //
    //  If your current location is blocked, you need to escape to a free tile-
    if (blocksMotion(aboard)) {
      final Tile blocked = origin() ;
      final Tile free = Spacing.nearestOpenTile(blocked, this) ;
      if (free == null) I.complain("NO FREE TILE AVAILABLE!") ;
      if (BaseUI.isPicked(this)) I.say("Escaping to free tile: "+free) ;
      setPosition(free.x, free.y, world) ;
      onMotionBlock(blocked) ;
      return ;
    }
    final Boardable next = pathing == null ? null : pathing.nextStep() ;
    final Tile
      oldTile = origin(),
      newTile = world().tileAt(nextPosition.x, nextPosition.y) ;
    //
    //  We allow mobiles to 'jump' between dissimilar objects.
    if (next != null && next.getClass() != aboard.getClass()) {
      aboard.setInside(this, false) ;
      (aboard = next).setInside(this, true) ;
      next.position(nextPosition) ;
    }
    //
    //  If you're not in either your current 'aboard' object, or the area
    //  corresponding to the next step in pathing, you need to default to the
    //  nearest clear tile.
    if (oldTile != newTile || ! aboard.inWorld()) {
      onTileChange(oldTile, newTile) ;
      final Box2D area = new Box2D() ;
      final boolean awry = next != null && Spacing.distance(next, this) > 1 ;
      final Vec3D p = nextPosition ;
      if (next != null && next.area(area).contains(p.x, p.y)) {
        ///I.say("Moving to next aboard: "+next) ;
        aboard.setInside(this, false) ;
        (aboard = next).setInside(this, true) ;
      }
      else {
        ///I.say("Moving to next tile: "+newTile) ;
        if (awry) onMotionBlock(newTile) ;
        aboard.setInside(this, false) ;
        (aboard = newTile).setInside(this, true) ;
      }
    }
    //
    //  Either way, update position and check for tile changes-
    ///I.say("Distance is: "+position.distance(nextPosition)) ;
    position.setTo(nextPosition) ;
    rotation = nextRotation ;
    super.setPosition(position.x, position.y, world) ;
  }
  
  
  //  TODO:  Make this specific to tiles?  Might be simpler.
  public boolean blocksMotion(Boardable t) {
    if (t instanceof Tile) return ((Tile) t).blocked() ;
    return false ;
  }
  
  
  //  TODO:  Make this abstract?
  protected void pathingAbort() {
  }
  
  
  protected void onMotionBlock(Tile t) {
    ///if (BaseUI.isPicked(this)) I.say("...MOTION BLOCKED") ;
    final boolean canRoute = pathing != null && pathing.refreshPath() ;
    if (! canRoute) pathingAbort() ;
  }
  
  
  protected float aboveGroundHeight() {
    return 0 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    final float alpha = PlayLoop.frameTime() ;
    v.setTo(position).scale(1 - alpha) ;
    v.add(nextPosition, alpha, v) ;
    return v ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    this.viewPosition(s.position) ;
    final float alpha = PlayLoop.frameTime() ;
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation) ;
    s.rotation = (rotation + (rotateChange * alpha) + 360) % 360 ;
    rendering.addClient(s) ;
  }
  
  
  public void describeStatus(Description d) {}
}







