/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.wild ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;



public class Vareen extends Fauna {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int FLY_PATH_LIMIT = 16 ;
  
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
      1.6f, //speed rate
      true  //organic
    ) ;
    gear.setDamage(4) ;
    gear.setArmour(2) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Behaviour modifications/implementation-
    */
  protected void updateAsMobile() {
    float idealHeight = 2.5f ;
    if (amDoing("actionRest") || amDoing("actionBrowse")) idealHeight = 1.0f ;
    if (! health.conscious()) idealHeight = 0 ;
    flyHeight = Visit.clamp(idealHeight, flyHeight - 0.1f, flyHeight + 0.1f) ;
    super.updateAsMobile() ;
    nextPosition.z = flyHeight + origin().elevation() ;
  }
  

  public void updateAsScheduled(int numUpdates) {
    //
    //  TODO:  Base nutritional value on daylight values...
    if (! indoors()) {
      final float value = 0.5f / World.DEFAULT_DAY_LENGTH ;
      health.takeSustenance(value, 1) ;
    }
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  
  protected float aboveGroundHeight() {
    return flyHeight ;
  }
  
  
  /*
  protected MobilePathing initPathing() {
    final Vareen actor = this ;
    //
    //  We use a modified form of pathing search that can bypass most
    //  tiles.
    return new MobilePathing(actor) {
      protected Boardable[] refreshPath(Boardable initB, Boardable destB) {
        final Lair lair = (Lair) actor.AI.home() ;
        
        final PathingSearch flightPS = new PathingSearch(
          initB, destB, FLY_PATH_LIMIT
        ) {
          final Boardable tileB[] = new Boardable[8] ;
          
          protected Boardable[] adjacent(Boardable spot) {
            //
            //  TODO:  There has got to be some way to factor this out into the
            //  Tile and PathingSearch classes.  This is recapitulating a lot
            //  of functionality AND IT'S DAMNED UGLY
            //  ...Also, it can't make use of the pathingCache class, though
            //  that's less vital here.
            if (spot instanceof Tile) {
              final Tile tile = ((Tile) spot) ;
              for (int i : Tile.N_DIAGONAL) {
                final Tile near = world.tileAt(
                  tile.x + Tile.N_X[i],
                  tile.y + Tile.N_Y[i]
                ) ;
                tileB[i] = blocksMotion(near) ? null : near ;
              }
              for (int i : Tile.N_ADJACENT) {
                final Tile near = world.tileAt(
                  tile.x + Tile.N_X[i],
                  tile.y + Tile.N_Y[i]
                ) ;
                if (
                  near != null && near.owner() == lair &&
                  lair != null && tile == lair.mainEntrance()
                ) tileB[i] = lair ;
                else tileB[i] = blocksMotion(near) ? null : near ;
              }
              Spacing.cullDiagonals(tileB) ;
              return tileB ;
            }
            return super.adjacent(spot) ;
          }
          
          protected boolean canEnter(Boardable spot) {
            return ! blocksMotion(spot) ;
          }
        } ;
        flightPS.doSearch() ;
        return flightPS.bestPath(Boardable.class) ;
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
  //*/
  
  
  
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




/*
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
//*/




