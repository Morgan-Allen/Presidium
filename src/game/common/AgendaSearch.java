/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.util.* ;
import java.lang.reflect.Array ;



//
//  TODO:  Don't rely on targets.  All you really need here is flagging.  Maybe
//  move to the .util package?

//  PROBLEM:  You were trying to remove insertion references that had already
//  been deleted!  Ha!  Gotcha now, sucker!


public abstract class AgendaSearch <T extends Target> {
  
  
  /**  Fields and constructors-
    */
  final SortTree <T> agenda = new SortTree <T> () {
    public int compare(T a, T b) {
      if (a == b) return 0 ;
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ? 1 : -1 ;
    }
  } ;
  
  
  private class Entry {
    float priorCost, futureEstimate, total ;
    T refers ;
    Entry prior ;
    private Object agendaRef ;
  }
  

  final T init ;
  Batch <T> flagged = new Batch <T> () ;
  
  private float totalCost = -1 ;
  private boolean success = false ;
  private Entry bestEntry = null ;
  
  public boolean verbose = false ;
  

  public AgendaSearch(T init) {
    if (init == null) I.complain("INITIAL AGENDA ENTRY CANNOT BE NULL!") ;
    this.init = init ;
  }
  
  
  /**  Performs the actual search algorithm.
    */
  public AgendaSearch <T> doSearch() {
    if (verbose) I.say("   ...searching ") ;
    if (! canEnter(init)) return this ;
    tryEntry(init, null, 0) ;
    
    while (agenda.size() > 0) {
      final Object nextRef = agenda.leastRef() ;
      final T next = agenda.refValue(nextRef) ;
      agenda.deleteRef(nextRef) ;
      if (endSearch(next)) {
        success = true ;
        totalCost = bestEntry.total ;
        if (verbose) I.say("  ...search complete, total cost: "+totalCost) ;
        break ;
      }
      for (T near : adjacent(next)) if (near != null) {
        //if (near == null || ! canEnter(near)) continue ;
        tryEntry(near, next, cost(next, near)) ;
      }
    }
    
    for (T t : flagged) setEntry(t, null) ;
    return this ;
  }
  
  
  protected void tryEntry(T spot, T prior, float cost) {
    if (cost < 0) return ;
    final Entry
      oldEntry = entryFor(spot),
      priorEntry = (prior == null) ? null : entryFor(prior) ;
    //
    //  If a pre-existing entry for this spot already exists and is at least as
    //  efficient, ignore it.  Otherwise replace it.
    final float priorCost = cost + (prior == null ? 0 : priorEntry.priorCost) ;
    if (oldEntry != null) {
      if (oldEntry.priorCost <= priorCost) return ;
      final Object oldRef = oldEntry.agendaRef ;
      if (agenda.containsRef(oldRef)) agenda.deleteRef(oldRef) ;
    }
    else if (! canEnter(spot)) return ;
    //
    //  Create the new entry-
    final Entry newEntry = new Entry() ;
    newEntry.priorCost = priorCost ;
    newEntry.futureEstimate = estimate(spot) ;
    newEntry.total = newEntry.priorCost + newEntry.futureEstimate ;
    newEntry.refers = spot ;
    newEntry.prior = priorEntry ;
    //
    //  Finally, flag the tile as assessed-
    setEntry(spot, newEntry) ;
    newEntry.agendaRef = agenda.insert(spot) ;
    if (oldEntry == null) flagged.add(spot) ;
    if (bestEntry == null || bestEntry.futureEstimate > newEntry.futureEstimate) {
      bestEntry = newEntry ;
    }
    if (verbose) I.add("|") ;
  }
  
  
  protected abstract T[] adjacent(T spot) ;
  protected boolean canEnter(T spot) { return true ; }
  protected abstract boolean endSearch(T best) ;
  
  protected abstract float cost(T prior, T spot) ;
  protected abstract float estimate(T spot) ;
  
  
  protected void setEntry(T spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  protected Entry entryFor(T spot) {
    return (Entry) spot.flaggedWith() ;
  }
  
  
  /**  Returns a list of all the nodes along the path back from the last node
    *  searched (which is presumed to be the 'best' result found.)
    */
  public T[] bestPath(Class pathClass) {
    if (bestEntry == null) return null ;
    final Batch <T> pathTiles = new Batch <T> () ;
    for (Entry next = bestEntry ; next != null ; next = next.prior) {
      pathTiles.add(next.refers) ;
    }
    if (verbose) I.say("Path size: "+pathTiles.size()) ;
    int len = pathTiles.size() ;
    T path[] = (T[]) Array.newInstance(pathClass, len) ;
    for (T t : pathTiles) path[--len] = t ;
    return path ;
  }
  
  
  public T[] fullPath(Class pathClass) {
    if (! success) return null ;
    return bestPath(pathClass) ;
  }
  
  
  public T[] allSearched(Class pathClass) {
    int len = flagged.size() ;
    T searched[] = (T[]) Array.newInstance(pathClass, len) ;
    for (T t : flagged) searched[--len] = t ;
    return searched ;
  }
  
  
  public float totalCost() {
    return totalCost ;
  }
}







