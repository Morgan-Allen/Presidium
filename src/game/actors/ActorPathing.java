/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.building.* ;
import src.util.* ;

import src.game.base.* ;



public class ActorPathing {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final int MAX_PATH_SCAN = 8 ;
  
  final Actor actor ;
  Target target ;
  
  Boardable path[] = null, pathTarget ;
  int stepIndex = -1 ;
  boolean closeEnough ;
  
  
  ActorPathing(Actor a) {
    this.actor = a ;
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
    if (t instanceof Actor) {
      final Actor a = (Actor) t ;
      if (a.aboard() != null) return a.aboard() ;
      return a.origin() ;
    }
    if (t instanceof Element) return ((Element) t).origin() ;
    I.complain("CANNOT GET LOCATION FOR: "+t) ;
    return null ;
  }
  
  
  private void refreshPath() {
    if (actor.assignedBase() == null || GameSettings.freePath) {
      final PathingSearch search = new PathingSearch(
        location(actor), location(target)
      ) ;
      if (target instanceof DrillingMetals) {
        search.verbose = true ;
      }
      search.doSearch() ;
      //path = search.bestPath(Boardable.class) ;
      path = search.fullPath(Boardable.class) ;
      stepIndex = 0 ;
    }
    else {
      path = actor.assignedBase().pathingCache.getLocalPath(
        location(actor), location(target)
      ) ;
      stepIndex = 0 ;
    }
  }
  
  
  void updateWithTarget(Target target) {
    //
    //  Firstly, check to see if the actual path target has been changed-
    this.target = target ;
    final Boardable location = location(actor), dest = location(target) ;
    
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
      final Target t = path[index] ;
      if ((t instanceof Tile) && ((Tile) t).blocked()) blocked = true ;
      if (t == dest) nearTarget = true ;
    }
    doRefresh = blocked || path == null || pathTarget != dest ;
    //
    //  In the case that the path we're following is only partial, update once
    //  we're approaching the terminus-
    if (path != null && ! nearTarget) {
      final Target last = path[path.length - 1] ;
      final float dist = Spacing.distance(location, last) ;
      //  TODO:  Consider checking if you're in the same Region instead...
      if (dist < World.SECTION_RESOLUTION / 2) doRefresh = true ;
    }
    //
    //  If the path needs refreshment, do so-
    if (doRefresh) {
      pathTarget = dest ;
      refreshPath() ;
      if (path == null) {
        I.say("COULDN'T FIND PATH TO: "+pathTarget) ;
        actor.abortAction() ;
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








