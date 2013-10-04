/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.util.* ;




public class PathingSearch extends Search <Boardable> {
  
  
  
  /**  Field definitions and constructors-
    */
  final protected Boardable destination ;
  public Mobile client = null ;
  private Boardable aimPoint = null ;
  
  private Boardable closest ;
  private float closestDist ;
  private Boardable batch[] = new Boardable[8] ;
  ///private Tile tileB[] = new Tile[8] ;
  //
  //  TODO:  Incorporate the Places-search constraint code here.
  //  TODO:  Allow for airborne pathing.
  //  TODO:  Allow for larger actors.
  //  TODO:  In the case of tiles, perform diagonals-culling here.
  //  private Place[] placesPath ;
  
  
  
  public PathingSearch(Boardable init, Boardable dest, int limit) {
    super(init, (limit > 0) ? ((limit + 2) * 8) : -1) ;
    if (dest == null) {
      I.complain("NO DESTINATION!") ;
    }
    this.destination = dest ;
    this.closest = init ;
    this.closestDist = Spacing.distance(init, dest) ;
    if (destination instanceof Venue) {
      final Venue venue = (Venue) destination ;
      aimPoint = venue.mainEntrance() ;
      if (aimPoint != null) {
        if (! venue.isEntrance(aimPoint)) {
          I.complain("DESTINATION CANNOT ACCESS AIM POINT: "+aimPoint) ;
        }
        if (! Visit.arrayIncludes(aimPoint.canBoard(null), destination)) {
          I.complain("AIM POINT CANNOT ACCESS DESTINATION: "+destination) ;
        }
      }
      else aimPoint = venue ;
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
      if (success()) I.say("\n  Success!") ;
      else {
        I.say("\n  Failed.") ;
        if (client != null) {
          I.say("Origin      blocked? "+client.blockedBy(init       )) ;
          I.say("Destination blocked? "+client.blockedBy(destination)) ;
        }
      }
      I.say("  Closest approach: "+closest+", aimed for "+aimPoint) ;
      I.say("  Total searched: "+flagged.size()+"/"+maxSearched) ;
      I.say("") ;
    }
    return this ;
  }
  
  
  protected void tryEntry(Boardable spot, Boardable prior, float cost) {
    final float spotDist = Spacing.distance(spot, aimPoint) ;
    if (spot == aimPoint) {
      if (verbose) I.say("\nMET AIM POINT: "+aimPoint) ;
      closest = spot ;
      closestDist = spotDist ;
    }
    else if (spotDist < closestDist) {
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
  /*
  private boolean fogged(Boardable spot) {
    if (client == null || ! (spot instanceof Tile)) return false ;
    if (client.base() == null) return false ;
    return client.base().intelMap.fogAt((Tile) spot) == 0 ;
  }
  //*/
  
  
  protected Boardable[] adjacent(Boardable spot) {
    return spot.canBoard(batch) ;
  }
  
  
  protected float cost(Boardable prior, Boardable spot) {
    if (spot == null) return -1 ;
    float mods = 0 ;
    //
    //  TODO:  Incorporate these checks only at the level of Place-routes, not
    //  individual tiles!
    
    //
    //  TODO:  Incorporate sector-based danger values, and stay out of hostile
    //         bases' line of sight when sneaking.
    /*
    if (client != null && client.base() != null && spot instanceof Tile) {
      final float presence = client.base().dangerMap.valAt((Tile) spot) ;
      if (presence < 0) mods += presence * -5 ;
    }
    //*/
    //
    //  TODO:  If the area or tile has other actors in it, increase the
    //         perceived cost.
    mods += spot.inside().size() ;
    //
    //  Finally, return a value based on pathing difficulties in the terrain-
    final float baseCost = Spacing.distance(prior, spot) ;
    ///if (fogged(spot)) return (2.0f * baseCost) + mods ;
    switch (spot.pathType()) {
      case (Tile.PATH_CLEAR  ) : return (1.0f * baseCost) + mods ;
      case (Tile.PATH_ROAD   ) : return (0.5f * baseCost) + mods ;
      case (Tile.PATH_HINDERS) : return (2.0f * baseCost) + mods ;
      default : return baseCost ;
    }
  }
  
  
  protected boolean canEnter(Boardable spot) {
    return (client == null) ? true :
      spot.allowsEntry(client) && ! client.blockedBy(spot) ;
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





