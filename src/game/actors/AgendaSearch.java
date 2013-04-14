/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.actors ;
import src.game.common.* ;
import src.util.* ;
import java.lang.reflect.Array ;



/**  Provides an A* search implementation for pathfinding.
  */
public abstract class AgendaSearch <T extends Target> {
  
  
  /**  Fields and constructors-
    */
  final SortTree <T> agenda = new SortTree <T> () {
    protected boolean greater(T a, T b) {
      final float aT = entryFor(a).total, bT = entryFor(b).total ;
      return aT > bT ;
    }
    protected boolean match(T a, T b) { return a == b ; }
  } ;
  
  protected class Entry {
    protected float cost, estimate, total ;
    protected T prior ;
    private Object agendaNode ;
  }
  
  
  Batch <T> flagged = new Batch <T> () ;
  final T init ;
  private float totalCost = -1 ;
  private T last ;//, path[] ;
  private Batch <T> pathTiles ;
  

  public AgendaSearch(T init) {
    this.init = init ;
  }
  
  
  /**  Performs the actual search algorithm.
    */
  public AgendaSearch <T> doSearch() {
    doEntry(init, null, 0) ;
    //
    //  Begin the search proper-
    while (agenda.size() > 0) {
      final Object bestNode = agenda.least() ;
      final T best = agenda.valueFor(bestNode) ;
      ///I.say("Best is: "+best) ;
      agenda.delete(bestNode) ;
      if (endSearch(best)) {
        last = best ;
        totalCost = entryFor(best).total ;
        break ;
      }
      putAdjacentEntriesFor(best) ;
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
      //int len = pathTiles.size() ;
      //
      //  Allocate an array and fill it in backwards (i.e, from source to
      //  target.)
      //path = (T[]) Array.newInstance(last.getClass(), len) ;
      //for (T t : pathTiles) path[--len] = t ;
    }
    else {
      //I.say("Unable to find path!") ;
      pathTiles = null ;
    }
    //
    //  Cleanup and report-
    /*
    if (last instanceof Tile) {
      final int dist = (int) Spacing.distance((Tile) init, (Tile) last) + 1 ;
      I.say("DISTANCE WAS "+dist+" SEARCH RATIO: "+(flagged.size() / dist)) ;
      I.say(" PATH- ") ; for (T t : path) I.add(t+" ") ;
    }
    //*/
    for (T t : flagged) setEntry(t, null) ;
    return this ;
  }
  
  
  /**  Inserts entries appropriate to nodes adjacent to the argument.
    */
  protected void putAdjacentEntriesFor(T spot) {
    for (T near : adjacent(spot)) {
      if (near == null || (! canEnter(near)) || entryFor(near) != null)
        continue ;
      doEntry(near, spot, cost(spot, near)) ;
    }
  }
  
  
  /**  Places a new Entry on the agenda.
    */
  protected void doEntry(T spot, T prior, float cost) {
    final Entry oldEntry = entryFor(spot) ;
    cost += (prior == null) ? 0 : entryFor(prior).cost ;
    if (oldEntry != null) {
      if (oldEntry.cost <= cost) return ;
      else agenda.delete(oldEntry.agendaNode) ;
    }
    //
    //  Firstly, we estimate the cost of reaching our destination.  (We
    //  increase the estimate slightly as this increases overall performance
    //  at a slight cost to accuracy.)
    final Entry entry = new Entry() ;
    entry.cost = cost ;
    entry.estimate = estimate(spot) ;
    entry.total = entry.cost + entry.estimate ;
    //
    //  Finally, flag the tile as assessed-
    entry.prior = prior ;
    setEntry(spot, entry) ;
    flagged.add(spot) ;
    entry.agendaNode = agenda.insert(spot) ;
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
  
  public float totalCost() {
    return totalCost ;
  }
}


