


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.planet.* ;
import src.user.Description;
import src.util.* ;




public class Exploring extends Plan {
  
  
  Box2D area = new Box2D() ;
  Tile nextLooked = null ;
  
  
  public Exploring(Actor actor, Box2D area) {
    super(actor) ;
    this.area.setTo(area) ;
    if (actor.assignedBase() == null) I.complain("EXPLORATION NEEDS A BASE!") ;
  }
  
  
  public Exploring(Session s) throws Exception {
    super(s) ;
    area.loadFrom(s.input()) ;
    nextLooked = (Tile) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    area.saveTo(s.output()) ;
    s.saveTarget(nextLooked) ;
  }
  
  
  protected Behaviour getNextStep() {
    //  TODO:  This should, perhaps, be moved to the ReconMission class?
    
    nextLooked = getUnexplored(actor.assignedBase().intelMap, actor, null) ;
    if (nextLooked == null) return null ;
    final Action looking = new Action(
      actor, nextLooked,
      this, "actionLook",
      Action.LOOK, "Looking at "+nextLooked.habitat()
    ) ;
    return looking ;
  }
  
  
  public boolean actionLook(Actor actor, Tile point) {
    final IntelMap map = actor.assignedBase().intelMap ;
    map.liftFogAround(point, actor.health.sightRange()) ;
    return true ;
  }
  
  
  
  public float priorityFor(Actor actor) {
    return super.priorityFor(actor) ;
    //
    //  Modify this based on the distance and average danger-value for the map.
  }
  
  
  
  public static float rateExplorePoint(Actor actor, Tile point) {
    return IDLE ;  //Include the restless and curious traits!
    //  Diminish with distance and danger.
  }
  
  
  
  
  
  //
  //  Shoot.  How do you ensure that the appropriate area of territory has been
  //  explored?  You can't include inaccessible tiles, but you have to include
  //  everything else.
  
  
  public void describeBehaviour(Description d) {
    d.append("Exploring") ;
    if (nextLooked != null) d.append(" "+nextLooked.habitat().name) ;
  }
  
  
  
  public static Tile getUnexplored(
    IntelMap intelMap, Target target, Box2D area
  ) {
    final Vec3D pos = target.position(null) ;
    final MipMap map = intelMap.fogMap() ;
    int high = map.high(), x = 0, y = 0, kX, kY ;
    Coord kids[] = new Coord[] {
      new Coord(0, 0), new Coord(0, 1),
      new Coord(1, 0), new Coord(1, 1)
    } ;
    float mX, mY, rating = 0, sumRating ;
    Float ratings[] = new Float[4] ;
    //
    //  Work your way down from the topmost sections, picking the most
    //  appealing child at each point.
    while (high-- > 1) {
      final float s = 1 << high ;
      sumRating = 0 ;
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
        if (area != null && area.distance(mX, mY) > s - 1) {
          ratings[i] = 0.0f ;
          continue ;
        }
        //
        //  Otherwise, favour closer areas that are partially explored.
        final float level = map.getAvgAt(kX, kY, high - 1) ;
        final float distance = pos.distance(mX, mY, 0) ;
        rating = (1 - level) * World.SECTION_RESOLUTION ;
        rating /= distance + World.SECTION_RESOLUTION ;
        ratings[i] = rating ;
        sumRating += rating ;
      }
      final Coord picked = (Coord) Rand.pickFrom(kids, ratings) ;
      if (picked == null) return null ;
      x = (x * 2) + picked.x ;
      y = (y * 2) + picked.y ;
    }
    return intelMap.world().tileAt(x, y) ;
  }
}





