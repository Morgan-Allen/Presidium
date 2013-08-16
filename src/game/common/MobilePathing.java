/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.* ;
import src.game.building.* ;
import src.util.* ;



public class MobilePathing {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final int MAX_PATH_SCAN = 8 ;
  
  final Mobile mobile ;
  Target trueTarget ;
  
  Boardable path[] = null, pathTarget ;
  int stepIndex = -1 ;
  
  
  public MobilePathing(Mobile a) {
    this.mobile = a ;
  }
  
  
  void loadState(Session s) throws Exception {
    trueTarget = s.loadTarget() ;
    path = (Boardable[]) s.loadTargetArray(Boardable.class) ;
    pathTarget = (Boardable) s.loadTarget() ;
    stepIndex = s.loadInt() ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTarget(trueTarget) ;
    s.saveTargetArray(path) ;
    s.saveTarget(pathTarget) ;
    s.saveInt(stepIndex) ;
  }
  
  
  
  /**  Updating current heading-
    */
  protected Boardable location(Target t) {
    if (t instanceof Boardable) return (Boardable) t ;
    if (t instanceof Mobile) {
      final Mobile a = (Mobile) t ;
      if (a.aboard() != null) return a.aboard() ;
      return a.origin() ;
    }
    if (t instanceof Element) {
      return Spacing.nearestOpenTile((Element) t, mobile, mobile.world()) ;
    }
    I.complain("CANNOT GET LOCATION FOR: "+t) ;
    return null ;
  }
  
  
  protected boolean refreshPath() {
    path = refreshPath(location(mobile), location(trueTarget)) ;
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
      return mobile.world().pathingCache.getLocalPath(
        initB, destB, MAX_PATH_SCAN * 2
      ) ;
    }
  }
  
  
  public void updatePathing(Target moveTarget, float minDist) {
    this.trueTarget = moveTarget ;
    final Boardable location = location(mobile), dest = location(trueTarget) ;
    boolean blocked = false, nearTarget = false, doRefresh = false ;
    //
    //  Check to ensure that subsequent steps along this path are not blocked,
    //  and that the path target has not changed.
    if (nextStep() != null) for (int i = 0 ; i < MAX_PATH_SCAN ; i++) {
      final int index = stepIndex + i ;
      if (index >= path.length) break ;
      final Boardable t = path[index] ;
      if (! mobile.canEnter(t)) blocked = true ;
      else if (! t.inWorld()) blocked = true ;
      if (t == dest) nearTarget = true ;
    }
    doRefresh = blocked || path == null || pathTarget != dest ;
    //
    //  In the case that the path we're following is only partial, update once
    //  we're approaching the terminus-
    if (path != null && ! nearTarget && Visit.last(path) != dest) {
      final Target last = (Target) Visit.last(path) ;
      final int dist = Spacing.outerDistance(location, last) ;
      if (dist < World.SECTION_RESOLUTION / 2) doRefresh = true ;
    }
    //
    //  If the path needs refreshment, do so-
    if (doRefresh) {
      pathTarget = dest ;
      refreshPath() ;
      if (path == null) {
        I.say("COULDN'T FIND PATH TO: "+pathTarget) ;
        mobile.pathingAbort() ;
        stepIndex = -1 ;
        return ;
      }
    }
    //
    //  If you're close to the centre of your current step, advance one step.
    if (path != null && inLocus(path[stepIndex])) {
      stepIndex = Visit.clamp(stepIndex + 1, path.length) ;
    }
  }
  
  
  private boolean inLocus(Boardable b) {
    return Spacing.innerDistance(mobile, b) < 0.5f ;
  }
  
  
  public Boardable nextStep() {
    if (stepIndex == -1 || path == null) return null ;
    if (! path[stepIndex].inWorld()) return null ;
    return path[stepIndex] ;
  }
  
  
  
  /**  Specialty methods for modifying the position/facing of actors-
    */
  private Vec2D displacement(Target target) {
    final Vec3D p = target.position(null) ;
    final Vec2D disp = new Vec2D(
      p.x - mobile.position.x,
      p.y - mobile.position.y
    ) ;
    return disp ;
  }
  
  
  public void headTowards(
    Target target, float speed, boolean moves
  ) {
    //
    //  Determine the appropriate offset and angle for this target-
    if (target == null) return ;
    final Vec2D disp = displacement(target) ;
    final float dist = disp.length() ;
    float angle = dist == 0 ? 0 : disp.normalise().toAngle() ;
    float moveRate = moves ? (speed / PlayLoop.UPDATES_PER_SECOND) : 0 ;
    //
    //  Determine how far one can move this update, including limits on
    //  maximum rotation-
    final float maxRotate = speed * 90 / PlayLoop.UPDATES_PER_SECOND  ;
    final float
      angleDif = Vec2D.degreeDif(angle, mobile.rotation),
      absDif   = Math.abs(angleDif) ;
    if (absDif > maxRotate) {
      angle = mobile.rotation + (maxRotate * (angleDif > 0 ? 1 : -1)) ;
      angle = (angle + 360) % 360 ;
      moveRate *= (180 - absDif) / 180 ;
    }
    disp.scale(Math.min(moveRate, dist)) ;
    //
    //  Then apply the changes-
    disp.x += mobile.position.x ;
    disp.y += mobile.position.y ;
    mobile.nextPosition.setTo(disp) ;
    mobile.nextPosition.z = mobile.aboveGroundHeight() ;
    mobile.nextRotation = angle ;
  }
  
  
  public boolean closeEnough(Target target, float minDist) {
    ///if (target instanceof Boardable) return inLocus((Boardable) target) ;
    return Spacing.distance(mobile, target) <= minDist ;
  }
  
  
  public boolean facingTarget(Target target) {
    final Vec2D disp = displacement(target) ;
    if (disp.length() == 0) return true ;
    final float angleDif = Math.abs(Vec2D.degreeDif(
      disp.normalise().toAngle(), mobile.rotation
    )) ;
    return angleDif < 30 ;
  }
}



