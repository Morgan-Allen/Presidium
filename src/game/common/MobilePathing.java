/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class MobilePathing {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public static int MAX_PATH_SCAN = 8 ;
  private static boolean verbose = false ;
  
  final Mobile mobile ;
  Target trueTarget ;
  Boardable pathTarget ;
  
  Boardable path[] = null ;
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
  private boolean inLocus(Boardable b) {
    if (b == null) return false ;
    return Spacing.innerDistance(mobile, b) < 0.5f ;
  }
  
  
  public Target target() {
    return trueTarget ;
  }
  
  
  public Boardable nextStep() {
    if (stepIndex == -1 || path == null) return null ;
    if (! path[stepIndex].inWorld()) return null ;
    return path[stepIndex] ;
  }
  
  
  protected Boardable location(Target t) {
    if (t instanceof Boardable && t != mobile) return (Boardable) t ;
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
  
  
  public void updateTarget(Target moveTarget) {
    final Target oldTarget = trueTarget ;
    this.trueTarget = moveTarget ;
    if (trueTarget != oldTarget) {
      ///if (BaseUI.isPicked(mobile)) I.say("...TARGET HAS CHANGED") ;
      path = null ; stepIndex = -1 ; return ;
    }
    else if (inLocus(nextStep())) {
      stepIndex = Visit.clamp(stepIndex + 1, path.length) ;
    }
  }
  
  
  public boolean checkPathingOkay() {
    if (trueTarget == null) return true ;
    if (path == null) return false ;
    final Boardable dest = location(trueTarget) ;
    boolean blocked = false, nearTarget = false, validPath = true ;
    //
    //  Check to ensure that subsequent steps along this path are not blocked,
    //  and that the path target has not changed.
    validPath = nextStep() != null && pathTarget == dest ;
    ///if (BaseUI.isPicked(mobile)) I.say("NEW TARGET? "+(pathTarget != dest)) ;
    
    if (validPath) for (int i = 0 ; i < MAX_PATH_SCAN ; i++) {
      final int index = stepIndex + i ;
      if (index >= path.length) break ;
      final Boardable t = path[index] ;
      if (mobile.blocksMotion(t)) blocked = true ;
      else if (! t.inWorld()) blocked = true ;
      if (t == dest) nearTarget = true ;
    }
    if (blocked) {
      ///if (BaseUI.isPicked(mobile)) I.say("PATH IS BLOCKED") ;
      validPath = false ;
    }
    //
    //  In the case that the path we're following is only partial, update once
    //  we're approaching the terminus-
    if (validPath && (! nearTarget) && (Visit.last(path) != dest)) {
      final int dist = path.length - (stepIndex + 1) ;
      if (dist < World.SECTION_RESOLUTION / 2) {
        ///if (BaseUI.isPicked(mobile)) I.say("NEAR END OF PATH") ;
        validPath = false ;
      }
    }
    return validPath ;
  }
  
  
  public boolean refreshPath() {
    if (verbose && BaseUI.isPicked(mobile)) I.say("REFRESHING PATH") ;
    
    final Boardable origin = location(mobile) ;
    if (trueTarget == null) path = null ;
    else {
      pathTarget = location(trueTarget) ;
      path = refreshPath(origin, pathTarget) ;
    }
    if (path == null) {
      if (verbose && BaseUI.isPicked(mobile)) I.say(
        "COULDN'T PATH TO: "+pathTarget
      ) ;
      mobile.pathingAbort() ;
      stepIndex = -1 ;
      return false ;
    }
    else {
      if (verbose && BaseUI.isPicked(mobile)) {
        I.say("PATH IS: ") ;
        for (Boardable b : path) I.add(b+" ") ;//+mobile.blocksMotion(b)+" ") ;
      }
      int index = 0 ;
      while (index < path.length) if (path[index++] == origin) break ;
      stepIndex = Visit.clamp(index, path.length) ;
      return true ;
    }
  }
  
  
  protected Boardable[] refreshPath(Boardable initB, Boardable destB) {
    if (GameSettings.pathFree) {
      final PathingSearch search = new PathingSearch(initB, destB, -1) ;
      if (verbose && BaseUI.isPicked(mobile)) search.verbose = true ;
      search.client = mobile ;
      search.doSearch() ;
      return search.fullPath(Boardable.class) ;
    }
    else {
      return mobile.world().pathingCache.getLocalPath(
        initB, destB, MAX_PATH_SCAN * 2, mobile
      ) ;
    }
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
    ///if (BaseUI.isPicked(mobile)) I.say("Moving toward "+target) ;
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
    //  Then apply the changes in heading-
    disp.x += mobile.position.x ;
    disp.y += mobile.position.y ;
    mobile.nextPosition.setTo(disp) ;
    mobile.nextRotation = angle ;
    //
    //  And try to track the level of the underlying terrain-
    final Boardable aboard = mobile.aboard ;
    final float baseHigh ;
    if (aboard instanceof Tile) {
      baseHigh = mobile.world.terrain().trueHeight(disp.x, disp.y) ;
    }
    else {
      baseHigh = aboard.position(null).z ;
    }
    mobile.nextPosition.z = baseHigh + mobile.aboveGroundHeight() ;
    //*
    if (verbose && BaseUI.isPicked(mobile)) I.say(
      "OLD/NEW HEADING: "+mobile.position+"/"+mobile.nextPosition
    ) ;
    //*/
  }
  
  
  private static Boardable batch[] = new Boardable[8] ;
  
  public boolean closeEnough(Target target, float minDist) {
    if (target instanceof Boardable && minDist <= 0) {
      final Boardable b = (Boardable) target ;
      if (mobile.aboard() != b) {
        b.canBoard(batch) ;
        if (! Visit.arrayIncludes(batch, mobile.aboard())) return false ;
      }
      return inLocus(b) ;
    }
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



