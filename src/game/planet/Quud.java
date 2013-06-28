/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;



public class Quud extends Organism {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  //  TODO:  Implement herd behaviour, warning calls, and 'lockdown' (to
  //         confer, say, a +10 armour bonus.)
  
  
  public Quud() {
    super(Species.QUUD) ;
  }
  
  
  public Quud(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(5, 2, 1) ;
    health.initStats(
      5,    //lifespan
      1,     //bulk bonus
      0.35f, //sight range
      0.15f  //speed rate
    ) ;
    gear.setDamage(0) ;
    gear.setArmour(15) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Behaviour implementations.
    */
  protected Behaviour nextFeeding() {
    //
    //  Pick a random tile to assess, along with any current neighbours-
    final Batch <Tile> around = new Batch <Tile> () ;
    final float range = health.sightRange() ;
    final Tile o = origin(), randTile = world().tileAt(
      o.x + Rand.range(-range, range),
      o.y + Rand.range(-range, range)
    ) ;
    if (randTile != null) around.add(randTile) ;
    for (Tile t : o.allAdjacent(null)) if (t != null) around.add(t) ;
    //
    //  Having obtained a list of possible tiles, rate the attractiveness of
    //  each-
    final Batch <Float> weights = new Batch <Float> () ;
    for (Tile t : around) weights.add(rateFeedSite(t)) ;
    final Tile pick = (Tile) Rand.pickFrom(around, weights) ;
    if (pick == null) return null ;
    //
    //  If so, move to the nearest open tile and crop the foliage-
    final Action browsing = new Action(
      this, pick,
      this, "actionBrowse",
      Action.STRIKE, "Browsing"
    ) ;
    browsing.setMoveTarget(Spacing.nearestOpenTile(pick, this)) ;
    return browsing ;
  }
  
  
  private float rateFeedSite(Tile pick) {
    if (pick == null || pick.pathType() == Tile.PATH_ROAD) return 0 ;
    else if (pick.owner() instanceof Flora) return 1 ;
    else if (pick.blocked()) return 0 ;
    return 1f / 4 ;
  }
  
  
  public boolean actionBrowse(Quud actor, Tile t) {
    float eaten = 0 ;
    if (t.owner() instanceof Flora) {
      final Flora f = (Flora) t.owner() ;
      eaten = 1 ;
      f.exitWorld() ;
    }
    else {
      eaten = t.habitat().moisture() / 4 ;
    }
    health.takeSustenance(eaten / 4, 1) ;
    return true ;
  }
  
  
  protected Target findRestPoint() {
    return origin() ;
  }


  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return 4.0f ; }
}






