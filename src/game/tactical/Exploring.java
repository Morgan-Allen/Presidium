


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.planet.* ;
import src.user.* ;
import src.util.* ;
import src.game.common.WorldSections.Section ;
import src.game.building.TileSpread ;


//
//  Alright.  In the case of exploring a definite area, you grab every reachable
//  tile- and either all of that is accessible to a given actor, or none of it
//  is.  Then you pick tiles from within that general area, based on proximity
//  to the actor.

//  Use the TODO system.  Then they can go back a mission they dropped!

//  You also need to merge Exploring with Recon, and Combat with Strike.



public class Exploring extends Plan implements ActorConstants {
  
  
  Base base ;
  Tile lookedAt ;
  
  
  public Exploring(Actor actor, Base base, Tile lookedAt) {
    super(actor) ;
    this.base = base ;
    this.lookedAt = lookedAt ;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s) ;
    base = (Base) s.loadObject() ;
    lookedAt = (Tile) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(base) ;
    s.saveTarget(lookedAt) ;
  }
  
  
  protected Behaviour getNextStep() {
    //
    //  TODO:  Consider grabbing another nearby spot.
    if (actor.assignedBase().intelMap.fogAt(lookedAt) == 1) return null ;
    final Action looking = new Action(
      actor, lookedAt,
      this, "actionLook",
      Action.LOOK, "Looking at "+lookedAt.habitat()
    ) ;
    //looking.setProperties(Action.RANGED) ;
    return looking ;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    //  TODO:  Check for mission-completion here?
    final IntelMap map = base.intelMap ;
    map.liftFogAround(point, actor.health.sightRange() * 1.414f) ;
    return true ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    return rateExplorePoint(actor, lookedAt, 0) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Exploring") ;
    d.append(" "+lookedAt.habitat().name) ;
  }
  
  
  
  /**  Utility methods for grabbing working areas-
    */
  public static float rateExplorePoint(
    Actor actor, Tile point, float winReward
  ) {
    winReward += actor.traits.trueLevel(INQUISITIVE) / 2f ;
    winReward -= actor.traits.trueLevel(INDOLENT) / 2f ;
    
    //
    //  Subtract the dangers of the journey.  Divide by time taken/distance.
    float distance = Spacing.distance(actor, point) ;
    distance = (distance + Terrain.SECTOR_SIZE) / Terrain.SECTOR_SIZE ;
    
    return winReward / distance ;
  }
  
  
  static Tile[] grabExploreArea(
    final IntelMap intelMap, final Tile point, final float radius
  ) {
    //
    //  Firstly, we grab all contiguous nearby tiles.
    final TileSpread spread = new TileSpread(point) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(t,  point) < radius ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        return false ;
      }
    } ;
    spread.doSearch() ;
    //
    //  As a final touch, we sort and return these tiles in random order.
    final List <Tile> sorting = new List <Tile> () {
      protected float queuePriority(Tile r) {
        return (Float) r.flaggedWith() ;
      }
    } ;
    for (Tile t : spread.allSearched(Tile.class)) {
      t.flagWith(Rand.num()) ;
      sorting.add(t) ;
    }
    sorting.queueSort() ;
    for (Tile t : sorting) t.flagWith(null) ;
    return (Tile[]) sorting.toArray(Tile.class) ;
  }
  
  
  
  /**  This method is used by independent actors during spontaneous missions-
    */
  public static Tile getUnexplored(
    IntelMap intelMap, Target target
  ) {
    final Vec3D pos = target.position(null) ;
    final MipMap map = intelMap.fogMap() ;
    int high = map.high(), x = 0, y = 0, kX, kY ;
    Coord kids[] = new Coord[] {
      new Coord(0, 0), new Coord(0, 1),
      new Coord(1, 0), new Coord(1, 1)
    } ;
    float mX, mY, rating = 0 ;
    Float ratings[] = new Float[4] ;
    //
    //  Work your way down from the topmost sections, picking the most
    //  appealing child at each point.
    while (high-- > 1) {
      final float s = 1 << high ;
      //Coord picked = null ;
      //float bestRating = Float.NEGATIVE_INFINITY ;
      for (int i = 4 ; i-- > 0 ;) {
        //
        //  We calculate the coordinates for each child-section, both within
        //  the mip-map, and in terms of world-coordinates midpoint, and skip
        //  over anything outside the supplied bounds.
        final Coord c = kids[i] ;
        kX = (x * 2) + c.x ;
        kY = (y * 2) + c.y ;
        mX = (kX + 0.5f) * s ;
        mY = (kY + 0.5f) * s ;
        //
        //  Otherwise, favour closer areas that are partially unexplored.
        final float level = map.getAvgAt(kX, kY, high - 1) < 1 ? 1 : 0 ;
        final float distance = pos.distance(mX, mY, 0) ;
        rating = level * World.SECTION_RESOLUTION ;
        rating /= distance + World.SECTION_RESOLUTION ;
        //if (rating > bestRating) { picked = c ; bestRating = rating ; }
        ratings[i] = rating ;
        //sumRating += rating ;
      }
      final Coord picked = (Coord) Rand.pickFrom(kids, ratings) ;
      if (picked == null) return null ;
      x = (x * 2) + picked.x ;
      y = (y * 2) + picked.y ;
    }
    return intelMap.world().tileAt(x, y) ;
  }
}



/**  Acquires the next unblocked, unexplored tile within the designated area
  *  for exploration.
public static Tile getUnexplored(
  final Target point,
  final Realm realm,
  final Box2D area
) {
  final Vars.Ref <Tile> ref = new Vars.Ref <Tile> () ;
  final Vars.Int
    count = new Vars.Int(),
    total = new Vars.Int() ;
  final Vec2D workerPos = new Vec2D(point.targX(), point.targY()) ;
  final NearInsertion insert = new NearInsertion(realm.world, workerPos, area) {
    
    protected float quadRating(QuadChild quad) {
      total.val++ ;
      //final int s = 1 << quad.quadHeight() ;
      final float rating =
        (1 - quad.blockLevel()) *
        (1 - realm.fogMap.fogMipAt(
          quad.quadX(),
          quad.quadY(),
          quad.quadHeight()
        )) ;
      if (rating > 0) count.val++ ;
      return rating ;
    }
    
    protected boolean checkForFreeArea(Tile tile) { return true ; }
    
    protected boolean doPlacementAt(Tile t) {
      ref.value = t ;
      return true ;
    }
  } ;
  insert.perform() ;
  return ref.value ;
}
}
//*/


