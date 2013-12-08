/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.sfx.PlaneFX ;
import src.user.* ;
import src.util.* ;



public abstract class Mobile extends Element
  implements Schedule.Updates
{
  
  final public static PlaneFX.Model SHADOW_MODEL = new PlaneFX.Model(
    "ground_shadow_model", Mobile.class,
    "media/SFX/ground_shadow.png", 1, 0, 0, false
  ) ;
  
  final static int
    MAX_PATH_SCAN = World.SECTOR_SIZE ;
  
  private static boolean verbose = false ;
  
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
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    (aboard = origin()).setInside(this, true) ;
    world().schedule.scheduleForUpdates(this) ;
    world().toggleActive(this, true) ;
    return true ;
  }
  
  
  public void exitWorld() {
    world.toggleActive(this, false) ;
    world().schedule.unschedule(this) ;
    //
    //  To be on the safe side, unregister from both current and next tiles-
    final Vec3D nP = nextPosition ;
    final Tile nT = world.tileAt(nP.x, nP.y) ;
    world.presences.togglePresence(this, nT, false) ;
    if (aboard != null) aboard.setInside(this, false) ;
    //
    //  And update position, so you don't get odd jitter effects if selected-
    position.setTo(nextPosition) ;
    rotation = nextRotation ;
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
    if (verbose) I.sayAbout(this, "NOW aboard: "+aboard) ;
  }
  

  public boolean setPosition(float xp, float yp, World world) {
    if (verbose) I.sayAbout(this, "SETTING POSITION") ;
    nextPosition.set(xp, yp, aboveGroundHeight()) ;
    return setHeading(nextPosition, nextRotation, true, world) ;
  }
  
  
  public boolean setHeading(
    Vec3D pos, float rotation, boolean instant, World world
  ) {
    final Tile
      oldTile = origin(),
      newTile = world.tileAt(pos.x, pos.y) ;
    if (! super.setPosition(pos.x, pos.y, world)) return false ;
    if (pos != null) nextPosition.setTo(pos) ;
    if (rotation != -1) nextRotation = rotation ;
    
    if (aboard == null || ! aboard.area(null).contains(newTile.x, newTile.y)) {
      if (aboard != null) aboard.setInside(this, false) ;
      (aboard = newTile).setInside(this, true) ;
      if (verbose) I.sayAbout(this, "FORCED aboard: "+aboard) ;
    }
    if (instant) {
      this.position.setTo(pos) ;
      this.rotation = rotation ;
      if (inWorld() && oldTile != newTile) {
        onTileChange(oldTile, newTile) ;
      }
    }
    return true ;
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    world.presences.togglePresence(this, oldTile, false) ;
    world.presences.togglePresence(this, newTile, true ) ;
  }
  
  
  public boolean indoors() {
    return aboard != null && ! aboard.openPlan() ;
  }
  
  
  protected void updateAsMobile() {
    //
    //  If your current location is blocked, you need to escape to a free tile-
    if (blockedBy(aboard)) {
      final Tile blocked = origin() ;
      final Tile free = Spacing.nearestOpenTile(blocked, this) ;
      if (free == null) I.complain("NO FREE TILE AVAILABLE!") ;
      ///if (BaseUI.isPicked(this)) I.say("Escaping to free tile: "+free) ;
      setPosition(free.x, free.y, world) ;
      onMotionBlock(blocked) ;
      return ;
    }
    final Boardable next = pathing == null ? null : pathing.nextStep() ;
    final Tile oldTile = origin() ;
    final Vec3D p = nextPosition ;
    final boolean outOfBounds =
      (! aboard.area(null).contains(p.x, p.y)) ||
      (aboard.destroyed()) ;
    //
    //  We allow mobiles to 'jump' between dissimilar objects, or track the
    //  sudden motions of mobile boardables (i.e, vehicles)-
    if (aboard instanceof Mobile && outOfBounds) {
      aboard.position(nextPosition) ;
    }
    else if (next != null && next.getClass() != aboard.getClass()) {
      if (verbose) I.sayAbout(this, "Jumping to: "+next) ;
      aboard.setInside(this, false) ;
      (aboard = next).setInside(this, true) ;
      next.position(nextPosition) ;
    }
    //
    //  If you're not in either your current 'aboard' object, or the area
    //  corresponding to the next step in pathing, you need to default to the
    //  nearest clear tile.
    final Tile newTile = world().tileAt(nextPosition.x, nextPosition.y) ;
    if (oldTile != newTile || outOfBounds) {
      if (oldTile != newTile) onTileChange(oldTile, newTile) ;
      final boolean awry = next != null && Spacing.distance(next, this) > 1 ;
      
      if (next != null && next.area(null).contains(p.x, p.y)) {
        aboard.setInside(this, false) ;
        (aboard = next).setInside(this, true) ;
      }
      else if (outOfBounds) {
        if (awry) onMotionBlock(newTile) ;
        if (verbose) I.sayAbout(this, "Entering tile: "+newTile) ;
        aboard.setInside(this, false) ;
        (aboard = newTile).setInside(this, true) ;
      }
    }
    //
    //  Either way, update current position-
    position.setTo(nextPosition) ;
    rotation = nextRotation ;
    super.setPosition(position.x, position.y, world) ;
    /*
    if (verbose && BaseUI.isPicked(this)) {
      I.say("Aboard: "+aboard) ;
      I.say("Position "+nextPosition) ;
      I.say("Next step: "+next) ;
    }
    //*/
  }
  
  
  //  TODO:  Make this specific to tiles?  Might be simpler.
  public boolean blockedBy(Boardable b) {
    if (b instanceof Tile) {
      final Tile t = (Tile) b ;
      if (t.blocked()) return true ;
      //if (t.inside().size() > 2) return true ;
    }
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
  public boolean visibleTo(Base base) {
    if (indoors()) return false ;
    return super.visibleTo(base) ;
  }
  
  
  public Vec3D viewPosition(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    final float alpha = PlayLoop.frameTime() ;
    v.setTo(position).scale(1 - alpha) ;
    v.add(nextPosition, alpha, v) ;
    return v ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float scale = spriteScale() ;
    s.scale = scale ;
    //
    //  Render your shadow, either on the ground or on top of occupants-
    final float R2 = (float) Math.sqrt(2) ;
    final PlaneFX shadow = (PlaneFX) SHADOW_MODEL.makeSprite() ;
    shadow.scale = radius() * scale * R2 ;
    final Vec3D p = s.position ;
    shadow.position.setTo(p) ;
    shadow.position.z = shadowHeight(p) ;
    rendering.addClient(shadow) ;
    
    this.viewPosition(s.position) ;
    final float alpha = PlayLoop.frameTime() ;
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation) ;
    s.rotation = (rotation + (rotateChange * alpha) + 360) % 360 ;
    rendering.addClient(s) ;
  }
  
  
  protected float shadowHeight(Vec3D p) {
    return world.terrain().trueHeight(p.x, p.y) ;
  }
  
  
  protected float spriteScale() {
    return 1 ;
  }
  
  
  public abstract void describeStatus(Description d) ;
}







