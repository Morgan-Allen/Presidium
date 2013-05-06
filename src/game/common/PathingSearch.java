/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
//import src.game.common.WorldSections.Section ;
import src.game.building.* ;
import src.util.* ;




public class PathingSearch extends Search <Boardable> {
  
  
  
  /**  Field definitions and constructors-
    */
  final protected Boardable destination ;
  private Target aimPoint = null ;
  private Boardable batch[] = new Boardable[8] ;
  
  
  public PathingSearch(Boardable init, Boardable dest, boolean safe) {
    super(init, safe ? ((Spacing.outerDistance(init, dest) * 10) + 10) : -1) ;
    if (dest == null) {
      I.complain("NO DESTINATION!") ;
    }
    this.destination = dest ;
    if (destination instanceof Venue) {
      final Tile e[] = ((Venue) destination).entrances() ;
      if (e.length == 1) aimPoint = e[0] ;
    }
    if (aimPoint == null) aimPoint = destination ;
  }
  
  
  public PathingSearch(Boardable init, Boardable dest) {
    this(init, dest, true) ;
  }
  
  
  public PathingSearch doSearch() {
    if (verbose) I.say(
      "Searching for path between "+init+" and "+destination+
      ", distance: "+Spacing.outerDistance(init, destination)
    ) ;
    super.doSearch() ;
    if (verbose) {
      if (success()) I.say("  Success!") ;
      else I.say("Failed.") ;
    }
    return this ;
  }
  
  
  protected void setEntry(Boardable spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  
  protected Entry entryFor(Boardable spot) {
    return (Entry) spot.flaggedWith() ;
  }


  /**  Actual search-execution methods-
    */
  protected Boardable[] adjacent(Boardable spot) {
    //  TODO:  If this spot is fogged, return all adjacent.
    return spot.canBoard(batch) ;
  }
  
  
  protected float cost(Boardable prior, Boardable spot) {
    if (spot == null) return -1 ;
    //  TODO:  If this spot is fogged, return a low nonzero value.
    //  TODO:  Incorporate sector-based danger values.
    float baseCost = Spacing.distance(prior, spot) ;
    switch (spot.pathType()) {
      case (Tile.PATH_CLEAR  ) : return 1.0f * baseCost ;
      case (Tile.PATH_ROAD   ) : return 0.5f * baseCost ;
      case (Tile.PATH_HINDERS) : return 2.0f * baseCost ;
      default : return baseCost ;
    }
  }
  
  
  protected float estimate(Boardable spot) {
    final float dist = Spacing.distance(spot, aimPoint) ;
    //if (init.pathType() == Tile.PATH_ROAD) return dist * 0.55f ;
    return dist * 1.1f ;
  }
  
  
  protected boolean endSearch(Boardable best) {
    return best == destination ;
  }
}


