/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.lang.reflect.Array ;



/**  A genericised search algorithm suitable for A*, Djikstra, or other forms
  *  of pathfinding and graph navigation.
  */
public abstract class Search <T> {
  
  
  /**  Fields and constructors-
    */
  final Sorting <T> agenda = new Sorting <T> () {
    public int compare(T a, T b) {
      if (a == b) return 0 ;
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ? 1 : -1 ;
    }
  } ;
  
  
  protected class Entry {
    float priorCost, futureEstimate, total ;
    T refers ;
    Entry prior ;
    private Object agendaRef ;
  }
  

  final protected T init ;
  final int maxSearched ;
  Batch <T> flagged = new Batch <T> () ;
  
  private float totalCost = -1 ;
  private boolean success = false ;
  private Entry bestEntry = null ;
  
  public boolean verbose = false ;
  

  public Search(T init, int maxPathLength) {
    if (init == null) I.complain("INITIAL AGENDA ENTRY CANNOT BE NULL!") ;
    this.init = init ;
    this.maxSearched = (maxPathLength < 0) ? -1 : (maxPathLength * 1) ;
  }
  
  
  /**  Performs the actual search algorithm.
    */
  public Search <T> doSearch() {
    if (verbose) I.say("   ...searching ") ;
    if (! canEnter(init)) return this ;
    tryEntry(init, null, 0) ;
    
    while (agenda.size() > 0) {
      if (maxSearched > 0 && flagged.size() > maxSearched) {
        if (verbose) I.say("Reached maximum search size ("+maxSearched+")") ;
        break ;
      }
      final Object nextRef = agenda.leastRef() ;
      final T next = agenda.refValue(nextRef) ;
      agenda.deleteRef(nextRef) ;
      if (endSearch(next)) {
        success = true ;
        bestEntry = entryFor(next) ;
        totalCost = bestEntry.total ;
        if (verbose) I.say(
          "  ...search complete at "+next+", total cost: "+totalCost+
          " all searched: "+flagged.size()
        ) ;
        break ;
      }
      for (T near : adjacent(next)) if (near != null) {
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
  
  protected abstract void setEntry(T spot, Entry flag) ;
  protected abstract Entry entryFor(T spot) ;
  
  
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
  
  
  public boolean success() {
    return success ;
  }
}







