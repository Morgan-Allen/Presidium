/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.graphics.common.* ;
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
  
  
  public abstract Base assignedBase() ;
  protected MobilePathing initPathing() { return null ; }
  
  
  
  /**  Again, more data-definition methods subclasses might well override.
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.setTo(position) ;
  }
  
  public float radius() { return 0.25f ; }
  public int pathType() { return Tile.PATH_CLEAR ; }
  public int owningType() { return NOTHING_OWNS ; }
  
  
  
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
  protected void pathingAbort() {
  }
  
  
  protected void onMotionBlock(Tile t) {
    final boolean canRoute = pathing != null && pathing.refreshPath() ;
    if (! canRoute) pathingAbort() ;
  }
  
  
  public Boardable aboard() {
    return aboard ;
  }
  
  
  public void goAboard(Boardable toBoard, World world) {
    if (aboard != null) aboard.setInside(this, false) ;
    aboard = toBoard ;
    if (aboard != null) aboard.setInside(this, true) ;
    
    final Vec3D p = this.nextPosition ;
    if (! aboard.area(null).contains(p.x, p.y)) {
      setHeading(toBoard.position(null), nextRotation, true, world) ;
    }
  }
  

  public void setPosition(float xp, float yp, World world) {
    nextPosition.set(xp, yp, 0) ;
    setHeading(nextPosition, nextRotation, true, world) ;
  }
  
  
  public void setHeading(
    Vec3D pos, float rotation, boolean instant, World world
  ) {
    final Tile oldTile = origin(), newTile = world.tileAt(pos.x, pos.y) ;
    super.setPosition(pos.x, pos.y, world) ;
    nextPosition.setTo(pos) ;
    nextRotation = rotation ;
    if (instant) {
      this.position.setTo(pos) ;
      this.rotation = rotation ;
      if (inWorld() && oldTile != newTile) {
        onTileChange(oldTile, newTile) ;
      }
    }
  }
  
  
  public boolean indoors() {
    return aboard != null && ! (aboard instanceof Tile) ;
  }
  
  
  
  protected void updateAsMobile() {
    final Tile
      oldTile = origin(),
      newTile = world().tileAt(nextPosition.x, nextPosition.y) ;
    //
    //  If you're not in either your current 'aboard' object, or the area
    //  corresponding to the next step in pathing, you need to default to the
    //  nearest clear tile.
    if (oldTile != newTile || ! aboard.inWorld()) {
      onTileChange(oldTile, newTile) ;
      final Box2D area = new Box2D() ;
      final Boardable next = pathing == null ? null : pathing.nextStep() ;
      final boolean awry = next != null && Spacing.distance(next, this) > 1 ;
      final Vec3D p = nextPosition ;
      if (aboard.area(area).contains(p.x, p.y) && aboard.inWorld()) {
        //  In this case, you're fine.  Just carry on.
      }
      else if (next != null && next.area(area).contains(p.x, p.y)) {
        aboard.setInside(this, false) ;
        (aboard = next).setInside(this, true) ;
      }
      else if (newTile.blocked()) {
        onMotionBlock(newTile) ;
        final Tile free = Spacing.nearestOpenTile(newTile, this) ;
        if (free == null) I.complain("NO FREE TILE AVAILABLE!") ;
        setPosition(free.x, free.y, world) ;
        return ;
      }
      else {
        if (awry) onMotionBlock(newTile) ;
        aboard.setInside(this, false) ;
        (aboard = newTile).setInside(this, true) ;
      }
    }
    //
    //  Either way, update position and check for tile changes-
    position.setTo(nextPosition) ;
    rotation = nextRotation ;
    super.setPosition(position.x, position.y, world) ;
    ///if (oldTile != newTile) onTileChange(oldTile, newTile) ;
  }
  
  
  public boolean canEnter(Boardable t) {
    if (t instanceof Tile) return ! ((Tile) t).blocked() ;
    return true ;
  }
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    world.presences.togglePresence(this, oldTile, false) ;
    world.presences.togglePresence(this, newTile, true ) ;
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
    ///v.z += height() + moveAnimHeight() ;
    return v ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float alpha = PlayLoop.frameTime() ;
    s.position.setTo(position).scale(1 - alpha) ;
    s.position.add(nextPosition, alpha, s.position) ;
    ///s.position.z += moveAnimHeight() ;
    final float rotateChange = Vec2D.degreeDif(nextRotation, rotation) ;
    s.rotation = (rotation + (rotateChange * alpha) + 360) % 360 ;
    ///I.say("sprite position/rotation: "+s.position+" "+s.rotation) ;
    rendering.addClient(s) ;
  }
}





/*
private Boardable
  aboard = null,
  boarding = null ;
//*/



/*
if (toBoard == boarding) return ;
if (aboard != null) aboard.setInside(this, false) ;
aboard = boarding = toBoard ;
if (aboard != null) aboard.setInside(this, true) ;
setHeading(toBoard.position(null), nextRotation, true, world) ;
//*/



/*
//*/

/*
if (BaseUI.isPicked(this)) {
  I.say("Updating mobile- "+this) ;
  I.say("  Aboard: "+aboard) ;
  I.say("  Boarding: "+boarding) ;
  I.say("  Old/new position: "+position+"/"+nextPosition) ;
  I.say("  Origin: "+origin()) ;
}
//*/
//
//  ...There's a problem here.  You need to have some kind of emergency
//  fallback in the event that you can't follow your specified path.
/*
if ((! checkTileClear(newTile)) && newTile.owner() instanceof Boardable) {
  boarding = (Boardable) newTile.owner() ;
}
//*/



/*
private Boardable location(Target t) {
  if (t instanceof Boardable) return (Boardable) t ;
  if (t instanceof Mobile) {
    final Mobile a = (Mobile) t ;
    if (a.aboard() != null) return a.aboard() ;
    return a.origin() ;
  }
  if (t instanceof Element) {
    return Spacing.nearestOpenTile((Element) t, this, world) ;
  }
  I.complain("CANNOT GET LOCATION FOR: "+t) ;
  return null ;
}


public boolean refreshPath() {
  path = refreshPath(location(this), location(target)) ;
  stepIndex = 0 ;
  return path != null ;
}


protected Boardable[] refreshPath(Boardable initB, Boardable destB) {
  if (GameSettings.freePath) {
    final PathingSearch search = new PathingSearch(initB, destB) ;
    search.doSearch() ;
    return search.fullPath(Boardable.class) ;
  }
  else {
    return world.pathingCache.getLocalPath(
      initB, destB, MAX_PATH_SCAN * 2
    ) ;
  }
}


public void updateWithTarget(Target moveTarget, float minDist) {
  //
  //  Firstly, check to see if the actual path target has been changed-
  this.target = moveTarget ;
  if (
    (! (moveTarget instanceof Venue)) &&
    (Spacing.distance(this, moveTarget) <= minDist)
  ) {
    closeEnough = true ;
    return ;
  }
  final Boardable location = location(this), dest = location(moveTarget) ;
  ///I.say(mobile+" location: "+location+", dest: "+dest+" ("+target+")") ;
  if (location == dest) {
    closeEnough = true ;
    return ;
  }
  else closeEnough = false ;
  //
  //  Check to ensure that subsequent steps along this path are not blocked-
  boolean blocked = false, nearTarget = false, doRefresh = false ;
  if (nextStep() != null) for (int i = 0 ; i < MAX_PATH_SCAN ; i++) {
    final int index = stepIndex + i ;
    if (index >= path.length) break ;
    final Boardable t = path[index] ;
    if (! canEnter(t)) blocked = true ;
    else if (! t.inWorld()) blocked = true ;
    if (t == dest) nearTarget = true ;
  }
  doRefresh = blocked || path == null || pathTarget != dest ;
  //
  //  In the case that the path we're following is only partial, update once
  //  we're approaching the terminus-
  if (path != null && ! nearTarget) {
    final Target last = path[path.length - 1] ;
    final int dist = Spacing.outerDistance(location, last) ;
    if (dist < World.SECTION_RESOLUTION / 2) {
      doRefresh = true ;
    }
  }
  //
  //  If the path needs refreshment, do so-
  if (doRefresh) {
    pathTarget = dest ;
    refreshPath() ;
    if (path == null) {
      I.say("COULDN'T FIND PATH TO: "+pathTarget) ;
      pathingAbort() ;
      stepIndex = -1 ;
      return ;
    }
  }
  if (location == path[stepIndex]) {
    stepIndex = Visit.clamp(stepIndex + 1, path.length) ;
  }
}


Target nextStep() {
  if (stepIndex == -1 || path == null) return null ;
  return path[stepIndex] ;
}


protected void onMotionBlock(Tile t) {
  pathingAbort() ;
}
//*/


/*
public boolean closeEnough() {
  return closeEnough ;
}


public boolean facingTarget() {
  return facingTarget ;
}
//*/


