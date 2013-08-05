/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.util.* ;



//
//  You need to base proximity tests on spacing/distance, rather than location.

public class MobilePathing {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final int MAX_PATH_SCAN = 8 ;
  
  final Mobile mobile ;
  Target target ;
  
  Boardable path[] = null, pathTarget ;
  int stepIndex = -1 ;
  boolean closeEnough ;
  
  
  protected MobilePathing(Mobile a) {
    this.mobile = a ;
  }
  
  
  void loadState(Session s) throws Exception {
    target = s.loadTarget() ;
    path = (Boardable[]) s.loadTargetArray(Boardable.class) ;
    pathTarget = (Boardable) s.loadTarget() ;
    stepIndex = s.loadInt() ;
    closeEnough = s.loadBool() ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTarget(target) ;
    s.saveTargetArray(path) ;
    s.saveTarget(pathTarget) ;
    s.saveInt(stepIndex) ;
    s.saveBool(closeEnough) ;
  }
  
  
  
  
  /**  Updating current heading-
    */
  private Boardable location(Target t) {
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
  
  
  public boolean refreshPath() {
    path = refreshPath(location(mobile), location(target)) ;
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
  
  
  void updateWithTarget(Target target, float minDist) {
    //
    //  Firstly, check to see if the actual path target has been changed-
    this.target = target ;
    if (
      ((target instanceof Element) || (target instanceof Tile)) &&
      Spacing.distance(mobile, target) <= minDist
    ) {
      closeEnough = true ;
      return ;
    }
    final Boardable location = location(mobile), dest = location(target) ;
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
      if (! mobile.canEnter(t)) blocked = true ;
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
        ///I.say("COULDN'T FIND PATH TO: "+pathTarget) ;
        mobile.pathingAbort() ;
        stepIndex = -1 ;
        return ;
      }
    }
    if (location == path[stepIndex]) {
      stepIndex = Visit.clamp(stepIndex + 1, path.length) ;
    }
  }
  
  
  boolean closeEnough() {
    return closeEnough ;
  }
  
  
  Target nextStep() {
    if (stepIndex == -1) return null ;
    return path[stepIndex] ;
  }
}








