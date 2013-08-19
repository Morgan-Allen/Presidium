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



public class Tile implements Target, TileConstants, Boardable {
  
  
  final public static int
    PATH_ROAD  = 0,
    PATH_CLEAR = 1,
    PATH_HINDERS = 2,
    PATH_BLOCKS = 3 ;
  private static Stack <Mobile>
    NONE_INSIDE = new Stack <Mobile> () ;
  
  
  final public World world ;
  final public int x, y ;
  private Object flagged ;
  
  private float elevation = Float.NEGATIVE_INFINITY ;
  private Habitat habitat = null ;
  private Element owner ;
  private Stack <Mobile> inside = NONE_INSIDE ;
  
  
  
  /**  Basic constructor and save/load functionality-
    */
  Tile(World world, int x, int y) {
    this.world = world ;
    this.x = x ;
    this.y = y ;
  }
  
  
  void loadState(Session s) throws Exception {
    elevation = s.loadFloat() ;
    habitat = Habitat.ALL_HABITATS[s.loadInt()] ;
    owner = (Element) s.loadObject() ;
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ()) ;
    else inside = NONE_INSIDE ;
  }
  
  
  void saveState(Session s) throws Exception {
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
    /*
    if (PlayLoop.currentUI() instanceof BaseUI) {
      ((BaseUI) PlayLoop.currentUI()).minimap.updateAt(this) ;
    }
    //*/
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
  
  
  public void flagWith(Object f) {
    /*
    if (f != null && flagged != null) {
      I.complain("PREVIOUS FLAGGING WAS NOT CLEARED- "+f+" "+flagged) ;
    }
    if (f == null && flagged == null) {
      I.complain("PREVIOUS FLAGGING ALREADY CLEARED!") ;
    }
    //*/
    flagged = f ;
  }
  
  
  public Object flaggedWith() {
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
    if (batch == null) batch = new Boardable[8] ;
    if (blocked() && owner() instanceof Boardable) {
      return ((Boardable) owner()).canBoard(batch) ;
    }
    for (int n : N_INDEX) {
      batch[n] = null ;
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t == null || t.blocked()) continue ;
      batch[n] = t ;
    }
    for (int i : Tile.N_DIAGONAL) if (batch[i] != null) {
      if (batch[(i + 7) % 8] == null) batch[i] = null ;
      if (batch[(i + 1) % 8] == null) batch[i] = null ;
    }
    for (int n : N_ADJACENT) if (batch[n] == null) {
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t == null || ! (t.owner() instanceof Boardable)) continue ;
      final Boardable v = (Boardable) t.owner() ;
      if (v.isEntrance(this)) batch[n] = v ;
    }
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable b) {
    if (b instanceof Tile) {
      final Tile t = (Tile) b ;
      if (t.blocked()) return false ;
      return Spacing.axisDist(this, t) < 2 ;
    }
    return (b == null) ? false : b.isEntrance(this) ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return ! blocked() ;
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
  public String toString() {
    return "Tile at "+x+" "+y ;
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







