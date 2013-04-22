/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.game.common.* ;
import src.util.* ;
import java.lang.reflect.Array ;
import java.util.* ;



//
//  TODO:  Don't rely on targets.  All you really need here is flagging.

/**  Provides an A* search implementation for pathfinding.
  */
public abstract class AgendaSearch <T extends Target> {
  
  
  /**  Fields and constructors-
    */
  final Comparator <T> compares = new Comparator <T> () {
    public int compare(T a, T b) {
      if (a == b) return 0 ;
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ? 1 : -1 ;
      //return aT > bT ? -1 : 1 ;//  This causes a near-instant crash...
    }
  } ;
  final TreeSet <T> agenda = new TreeSet <T> (compares) ;
  
  //  TODO:  Consider re-instating SortTree?  None of the recent malfunctions
  //  were specifically it's fault.
  /*
  final SortTree <T> agenda = new SortTree <T> () {
    protected boolean greater(T a, T b) {
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ;
    }
    protected boolean match(T a, T b) { return a == b ; }
  } ;
  //*/
  
  
  protected class Entry {
    protected float cost, estimate, total ;
    protected T refers, prior ;
    //private Object agendaNode ;
    //private AgendaSearch search ;
  }
  
  
  Batch <T> flagged = new Batch <T> () ;
  final T init ;
  private float totalCost = -1 ;
  private T last ;
  private Batch <T> pathTiles ;
  

  public AgendaSearch(T init) {
    this.init = init ;
    if (init == null) I.complain("INITIAL AGENDA ENTRY CANNOT BE NULL!") ;
  }
  
  
  /**  Performs the actual search algorithm.
    */
  public AgendaSearch <T> doSearch() {
    //I.say("   ...searching from: "+init) ;
    I.say("   ...searching ") ;
    tryEntry(init, null, 0) ;
    //
    //  Begin the search proper-
    while (agenda.size() > 0) {
      final T best = agenda.first() ;
      //final Object bestNode = agenda.least() ;
      //final T best = agenda.valueFor(bestNode) ;
      ///I.say("Best is: "+best) ;
      agenda.remove(best) ;
      //agenda.delete(bestNode) ;
      if (endSearch(best)) {
        last = best ;
        totalCost = entryFor(best).total ;
        break ;
      }
      tryEntriesFor(best) ;
    }
    //
    //  Now, we generate the path toward the best entry found.
    if (last != null) {
      //
      //  Follow the chain of entries back to their source.
      pathTiles = new Batch <T> () ;
      T next = last ; while (true) {
        pathTiles.add(next) ;
        final Entry entry = entryFor(next) ;
        if (entry.prior == null) break ;
        next = entry.prior ;
      }
    }
    else {
      //I.say("Unable to find path!") ;
      pathTiles = null ;
    }
    //
    //  Cleanup and report-
    for (T t : flagged) setEntry(t, null) ;
    return this ;
  }
  
  
  /**  Inserts entries appropriate to nodes adjacent to the argument.
    */
  protected void tryEntriesFor(T spot) {
    for (T near : adjacent(spot)) {
      if (near == null || ! canEnter(near)) continue ;
      tryEntry(near, spot, cost(spot, near)) ;
    }
  }
  
  
  /**  Places a new Entry on the agenda.
    */
  protected void tryEntry(T spot, T prior, float cost) {
    if (cost < 0) return ;
    final Entry oldEntry = entryFor(spot) ;
    cost += (prior == null) ? 0 : entryFor(prior).cost ;
    //I.say("Agenda size: "+agenda.size()) ;
    if (oldEntry != null) {
      //if (oldEntry.search != this) I.complain("CONFLICT BETWEEN SEARCHES!") ;
      if (oldEntry.cost <= cost) return ;
      else agenda.remove(oldEntry.refers) ;
      //else agenda.delete(oldEntry.agendaNode) ;
    }
    //
    //  Firstly, we estimate the cost of reaching our destination.  (We
    //  increase the estimate slightly as this increases overall performance
    //  at a slight cost to accuracy.)
    final Entry entry = new Entry() ;
    entry.cost = cost ;
    entry.estimate = estimate(spot) ;
    entry.total = entry.cost + entry.estimate ;
    entry.refers = spot ;
    //entry.search = this ;
    //
    //  Finally, flag the tile as assessed-
    entry.prior = prior ;
    setEntry(spot, entry) ;
    flagged.add(spot) ;
    
    I.add("|") ;
    final int oldSize = agenda.size() ;
    agenda.add(spot) ;
    if (agenda.size() - oldSize != 1) I.complain("Inconsistent comparator?") ;
    //entry.agendaNode = agenda.insert(spot) ;
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
  public T[] getPath(Class pathClass) {
    if (pathTiles == null) return null ;
    int len = pathTiles.size() ;
    T path[] = (T[]) Array.newInstance(pathClass, len) ;
    for (T t : pathTiles) path[--len] = t ;
    return path ;
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







