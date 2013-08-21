/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;



public class Vareen extends Fauna {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private float flyHeight = 2.5f ;
  private Lair nest = null ;
  
  
  
  public Vareen() {
    super(Species.VAREEN) ;
  }
  
  
  public Vareen(Session s) throws Exception {
    super(s) ;
    flyHeight = s.loadFloat() ;
    nest = (Lair) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(flyHeight) ;
    s.saveObject(nest) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(10, 20, 3) ;
    health.initStats(
      5,    //lifespan
      0.5f, //bulk bonus
      1.0f, //sight range
      1.5f  //speed rate
    ) ;
    gear.setDamage(4) ;
    gear.setArmour(2) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  //
  //  TODO:  It might be an idea for the world to have a trees-map, so that you
  //  can look up their location more easily, including for forestry purposes.
  
  protected Behaviour nextFeeding() {
    //
    //  Pick several random nearby tiles, and see if one has flora.  If so,
    //  feed from it.
    Tile pick = null ;
    for (int numTries = 3 ; numTries-- > 0 ;) {
      final Tile seen = randomTileInRange(health.sightRange()) ;
      if (seen == null || ! (seen.owner() instanceof Flora)) continue ;
      pick = seen ;
      break ;
    }
    //
    //  If you can't find other food, just bask in the sun to gain energy-
    if (pick == null && ! origin().blocked()) {
      final Action basking = new Action(
        this, origin(),
        this, "actionBask",
        Action.MOVE, "Basking"
      ) ;
      basking.setProperties(Action.CAREFUL) ;
      ///basking.setDuration(2.0f) ;
      return basking ;
    }
    //
    //  Otherwise, go pluck some fruit or whatever-
    if (pick == null) return null ;
    final Action foraging = new Action(
      this, pick,
      this, "actionForage",
      Action.STRIKE, "Foraging"
    ) ;
    foraging.setMoveTarget(Spacing.nearestOpenTile(pick, this)) ;
    return foraging ;
  }
  
  
  public boolean actionBask(Vareen actor, Tile location) {
    //
    //  Adjust this based on night/day values?
    health.takeSustenance(location.habitat().insolation() / 100f, 1.0f) ;
    return true ;
  }
  
  
  public boolean actionForage(Vareen actor, Tile location) {
    if (! (location.owner() instanceof Flora)) return false ;
    final Flora f = (Flora) location.owner() ;
    health.takeSustenance(f.growStage() * 0.25f / Flora.MAX_GROWTH, 1.0f) ;
    f.incGrowth(-0.25f / 4, world, false) ;
    return true ;
  }
  
  
  protected Target findRestPoint() {
    //
    //  Return to your nest if possible.  Otherwise, roost in the trees.
    return origin() ;
    //return null ;
  }
  
  
  
  /**  Pathing modifications-
    */
  //*
  protected MobilePathing initPathing() {
    //
    //  We use a modified form of pathing search that can bypass most
    //  tiles.
    //final Vareen actor = this ;
    return new MobilePathing(this) {
      protected Boardable[] refreshPath(Boardable initB, Boardable destB) {
        final PathingSearch flightPS = new PathingSearch(initB, destB) {
          final Tile tileB[] = new Tile[8] ;
          
          protected Boardable[] adjacent(Boardable spot) {
            if (spot instanceof Tile) {
              ((Tile) spot).allAdjacent(tileB) ;
              for (int i : Tile.N_INDEX) {
                if (blocksMotion(tileB[i])) tileB[i] = null ;
              }
              Spacing.cullDiagonals(tileB) ;
              return tileB ;
            }
            return super.adjacent(spot) ;
          }
          /*
          protected boolean canEnter(Boardable spot) {
            return ! actor.blocksMotion(spot) ;
          }
          //*/
        } ;
        flightPS.doSearch() ;
        return flightPS.fullPath(Boardable.class) ;
      }
    } ;
  }
  
  
  public boolean blocksMotion(Boardable t) {
    if (t instanceof Tile) {
      final Element owner = ((Tile) t).owner() ;
      return owner != null && owner.height() > 2.5f ;
    }
    return false ;
  }
  
  
  protected void updateAsMobile() {
    float idealHeight = 2.5f ;
    if (currentAction() != null) {
      final String actName = currentAction().toString() ;
      if (actName.equals("Basking")) idealHeight = 0 ;
      /*
      else  {
        final Target t = currentAction().target() ;
        if (t instanceof Tile) idealHeight = ((Tile) t).elevation() + 2.5f ;
        else idealHeight = t.height() ;
      }
      //*/
    }
    if (! health.conscious()) idealHeight = 0 ;
    flyHeight = Visit.clamp(idealHeight, flyHeight - 0.1f, flyHeight + 0.1f) ;
    super.updateAsMobile() ;
    nextPosition.z = origin().elevation() + flyHeight ;
  }
  
  
  protected float aboveGroundHeight() {
    return flyHeight - 0.3f ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float shadowHeight(Vec3D p) {
    //
    //  Sample the height of the 4 nearby tiles, and interpolate between them.
    final Tile o = world.tileAt((int) p.x, (int) p.y) ;
    final float
      xA = p.x - o.x, yA = p.y - o.y,
      TSW = heightFor(o.x    , o.y    ),
      TSE = heightFor(o.x + 1, o.y    ),
      TNW = heightFor(o.x    , o.y + 1),
      TNE = heightFor(o.x + 1, o.y + 1) ;
    return
      (((TSW * (1 - xA)) + (TSE * xA)) * (1 - yA)) +
      (((TNW * (1 - xA)) + (TNE * xA)) *      yA ) ;
  }
  
  
  private float heightFor(int tX, int tY) {
    final Tile t = world.tileAt(
      Visit.clamp(tX, world.size),
      Visit.clamp(tY, world.size)
    ) ;
    if (t.owner() == null) return t.elevation() ;
    return t.owner().height() ;
  }
  
  
  protected float moveAnimStride() { return 1.0f ; }
}







