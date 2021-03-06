/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.util.* ;
import src.game.planet.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.graphics.widgets.Text.Clickable ;



public final class Tile implements
  Target, TileConstants, Boardable, Session.Saveable, Clickable
{
  
  
  final public static int
    PATH_ROAD    = 0,
    PATH_CLEAR   = 1,
    PATH_HINDERS = 2,
    PATH_BLOCKS  = 3 ;
  private static Stack <Mobile>
    NONE_INSIDE = new Stack <Mobile> () ;
  
  
  final public World world ;
  final public int x, y ;
  private Object flagged ;
  private Boardable boardingCache[] = null ;
  
  private float elevation = Float.NEGATIVE_INFINITY ;
  private Habitat habitat = null ;
  private Element owner ;
  private Stack <Mobile> inside = NONE_INSIDE ;
  
  
  
  /**  Basic constructor and save/load functionality-
    */
  protected Tile(World world, int x, int y) {
    this.world = world ;
    this.x = x ;
    this.y = y ;
  }
  
  
  public static Tile loadConstant(Session s) throws Exception {
    return s.world().tileAt(s.loadInt(), s.loadInt()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(x) ;
    s.saveInt(y) ;
  }
  
  
  protected void loadTileState(Session s) throws Exception {
    elevation = s.loadFloat() ;
    habitat = Habitat.ALL_HABITATS[s.loadInt()] ;
    owner = (Element) s.loadObject() ;
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ()) ;
    else inside = NONE_INSIDE ;
  }
  
  
  protected void saveTileState(Session s) throws Exception {
    s.saveFloat(elevation) ;
    s.saveInt(habitat().ID) ;
    s.saveObject(owner) ;
    if (inside == NONE_INSIDE) s.saveBool(false) ;
    else { s.saveBool(true) ; s.saveObjects(inside) ; }
  }
  
  
  
  /**  Satisfying the target contract and other similar interfaces.
    */
  public float elevation() {
    if (elevation == Float.NEGATIVE_INFINITY) {
      elevation = world.terrain().trueHeight(x, y) ;
    }
    return elevation ;
  }
  
  public boolean inWorld() { return true ; }
  public boolean destroyed() { return false ; }
  public World world() { return world ; }
  
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.set(x, y, elevation()) ;
  }
  
  public float radius() { return 0 ; }
  public float height() { return 0 ; }
  
  
  
  /**  Setting path type and occupation-
    */
  public Element owner() {
    return owner ;
  }
  
  
  public void setOwner(Element e) {
    if (e == owner) return ;
    this.owner = e ;
    
    world.sections.flagBoundsUpdate(x, y) ;
    boardingCache = null ;
    for (int n : N_INDEX) {
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t != null) t.boardingCache = null ;
    }
  }
  
  
  public Habitat habitat() {
    if (habitat != null) return habitat ;
    refreshHabitat() ;
    return habitat ;
  }
  
  
  public void refreshHabitat() {
    habitat = world.terrain().habitatAt(x, y) ;
  }
  
  
  public boolean blocked() {
    return pathType() >= PATH_BLOCKS ;
  }
  
  
  public int pathType() {
    if (owner != null) return owner.pathType() ;
    if (world.terrain().isRoad(this)) return PATH_ROAD ;
    return habitat().pathClear ? PATH_CLEAR : PATH_BLOCKS ;
  }
  
  
  public int owningType() {
    if (owner == null) {
      if (habitat().pathClear) return Element.NOTHING_OWNS ;
      else return Element.TERRAIN_OWNS ;
    }
    return owner.owningType() ;
  }
  
  
  public final void flagWith(final Object f) {
    flagged = f ;
  }
  
  
  public final Object flaggedWith() {
    return flagged ;
  }
  
  
  public Tile[] edgeAdjacent(Tile batch[]) {
    if (batch == null) batch = new Tile[N_ADJACENT.length] ;
    int i = 0 ; for (int n : N_ADJACENT) {
      batch[i++] = world.tileAt(x + N_X[n], y + N_Y[n]) ;
    }
    return batch ;
  }
  
  
  public Tile[] allAdjacent(Tile batch[]) {
    if (batch == null) batch = new Tile[N_INDEX.length] ;
    for (int n : N_INDEX) {
      batch[n] = world.tileAt(x + N_X[n], y + N_Y[n]) ;
    }
    return batch ;
  }
  
  
  public Tile[] vicinity(Tile batch[]) {
    if (batch == null) batch = new Tile[9] ;
    allAdjacent(batch) ;
    batch[8] = this ;
    return batch ;
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  public Boardable[] canBoard(Boardable batch[]) {
    //
    //  TODO:  See if this can't be made more efficient.  Try caching whenever
    //  ownership/occupants change?
    if (boardingCache != null) return boardingCache ;
    else batch = null ;
    
    if (batch == null) batch = new Boardable[8] ;
    
    //  TODO:  RESTORE THIS FOR VEHICLES LATER, IF REQUIRED?
    /*
    if (inside != null) {
      int numB = 0 ;
      for (Mobile m : inside) {
        if (m instanceof Boardable) numB++ ;
      }
      if (numB > 0) {
        batch = new Boardable[8 + numB] ;
        for (Mobile m : inside) {
          if (m instanceof Boardable) {
            batch[8 + --numB] = (Boardable) m ;
          }
        }
      }
    }
    //*/
    
    if (blocked() && owner() instanceof Boardable) {
      return ((Boardable) owner()).canBoard(batch) ;
    }
    for (int n : N_INDEX) {
      batch[n] = null ;
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t == null || t.blocked()) continue ;
      batch[n] = t ;
    }
    Spacing.cullDiagonals(batch) ;
    
    for (int n : N_ADJACENT) {
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t == null || ! (t.owner() instanceof Boardable)) continue ;
      final Boardable v = (Boardable) t.owner() ;
      if (v.isEntrance(this)) batch[n] = v ;
    }
    
    boardingCache = batch ;
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable b) {
    if (b instanceof Tile) {
      final Tile t = (Tile) b ;
      if (t.blocked()) return false ;
      return Spacing.maxAxisDist(this, t) < 2 ;
    }
    return (b == null) ? false : b.isEntrance(this) ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return true ;
  }
  
  
  public int boardableType() {
    return BOARDABLE_TILE ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    put.set(x - 0.5f, y - 0.5f, 1, 1) ;
    return put ;
  }
  
  
  public void setInside(Mobile m, boolean is) {
    if (is) {
      if (inside == NONE_INSIDE) inside = new Stack <Mobile> () ;
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
      if (inside.size() == 0) inside = NONE_INSIDE ;
    }
  }
  
  
  public Stack <Mobile> inside() {
    return inside ;
  }
  
  
  
  /**  Interface and media-
    */
  public String fullName() {
    return toString() ;
  }
  
  
  public void whenClicked() {
    //  TODO:  Open a simple display pane to give basic information on the
    //         habitat type.
    final BaseUI UI = (BaseUI) PlayLoop.currentUI() ;
    UI.selection.pushSelection(null, false) ;
    UI.camera.lockOn(this) ;
  }
  
  
  public String toString() {
    if (habitat == null) return "Tile at X"+x+" Y"+y ;
    return habitat.name+" at X"+x+" Y"+y ;
  }
  
  
  public Colour minimapHue() {
    /*
    if (owner != null && owner.sprite() != null) {
      return owner.sprite().averageHue() ;
    }
    if (world.terrain().isRoad(this)) return Habitat.ROAD_TEXTURE.averaged() ;
    //*/
    return habitat().baseTex.averaged() ;
  }
}







