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
  private Mobile client = null ;
  
  private Boardable closest ;
  private float closestDist ;
  private Boardable batch[] = new Boardable[8] ;
  private Tile tileB[] = new Tile[8] ;
  
  //
  //  TODO:  Incorporate the Places-search constraint code here?  Maybe.
  //  Alternatively, you could discover those places as you go, allowing places
  //  to be intermixed with tiles.  And fogged tiles can be travelled through
  //  as standard.  That will solve the destination problem.
  
  
  
  public PathingSearch(Boardable init, Boardable dest, int limit) {
    super(init, (limit > 0) ? ((limit + 2) * 8) : -1) ;
    if (dest == null) {
      I.complain("NO DESTINATION!") ;
    }
    this.destination = dest ;
    this.closest = init ;
    this.closestDist = Spacing.distance(init, dest) ;
    if (destination instanceof Venue) {
      aimPoint = ((Venue) destination).mainEntrance() ;
    }
    if (aimPoint == null) aimPoint = destination ;
  }
  
  
  public PathingSearch(Boardable init, Boardable dest) {
    this(init, dest, Spacing.outerDistance(init, dest)) ;
  }
  
  
  public PathingSearch doSearch() {
    if (verbose) I.say(
      "Searching for path between "+init+" and "+destination+
      ", distance: "+Spacing.outerDistance(init, destination)
    ) ;
    super.doSearch() ;
    if (verbose) {
      if (success()) I.say("  Success!") ;
      else I.say("  Failed.") ;
    }
    return this ;
  }
  
  
  protected void tryEntry(Boardable spot, Boardable prior, float cost) {
    final float spotDist = Spacing.distance(spot, destination) ;
    if (spotDist < closestDist) {
      closest = spot ;
      closestDist = spotDist ;
    }
    super.tryEntry(spot, prior, cost) ;
  }
  
  
  protected void setEntry(Boardable spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  
  protected Entry entryFor(Boardable spot) {
    return (Entry) spot.flaggedWith() ;
  }
  
  
  
  /**  Actual search-execution methods-
    */
  private boolean fogged(Boardable spot) {
    if (client == null || ! (spot instanceof Tile)) return false ;
    return client.base().intelMap.fogAt((Tile) spot) == 0 ;
  }
  
  
  protected Boardable[] adjacent(Boardable spot) {
    if (fogged(spot)) {
      ((Tile) spot).allAdjacent(tileB) ;
      for (int i : Tile.N_INDEX) batch[i] = tileB[i] ;
      return batch ;
    }
    return spot.canBoard(batch) ;
  }
  
  
  protected float cost(Boardable prior, Boardable spot) {
    if (spot == null) return -1 ;
    float mods = 0 ;
    //
    //  TODO:  Incorporate sector-based danger values, and stay out of hostile
    //         bases' line of sight when sneaking.
    if (client != null && client.base() != null && spot instanceof Tile) {
      final float presence = client.base().dangerMap.valAt((Tile) spot) ;
      if (presence < 0) mods += presence * -5 ;
    }
    //
    //  TODO:  If the area or tile has other actors in it, increase the
    //         perceived cost.
    mods += spot.inside().size() / 2f ;
    //
    //  Finally, return a value based on pathing difficulties in the terrain-
    final float baseCost = Spacing.distance(prior, spot) ;
    if (fogged(spot)) return (2.0f * baseCost) + mods ;
    switch (spot.pathType()) {
      case (Tile.PATH_CLEAR  ) : return (1.0f * baseCost) + mods ;
      case (Tile.PATH_ROAD   ) : return (0.5f * baseCost) + mods ;
      case (Tile.PATH_HINDERS) : return (2.0f * baseCost) + mods ;
      default : return baseCost ;
    }
  }
  
  
  protected boolean canEnter(Boardable spot) {
    return (client == null) ? true : spot.allowsEntry(client) ;
  }
  
  
  protected float estimate(Boardable spot) {
    float dist = Spacing.distance(spot, aimPoint) ;
    dist += Spacing.distance(closest, spot) / 3.0f ;
    return dist * 1.1f ;
  }
  
  
  protected boolean endSearch(Boardable best) {
    return best == destination ;
  }
}


