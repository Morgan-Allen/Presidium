/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.util.* ;
import src.game.planet.* ;
import src.game.building.* ;



public class Tile implements Target, TileConstants, Boardable {
  
  
  final public static int
    PATH_ROAD  = 0,
    PATH_CLEAR = 1,
    PATH_HINDERS = 2,
    PATH_BLOCKS = 3 ;
  
  
  final public World world ;
  final public int x, y ;
  private Object flagged ;
  
  private float elevation = Float.NEGATIVE_INFINITY ;
  private Habitat habitat = null ;
  private Element owner ;
  private Stack <Mobile> inside = null ;
  
  
  
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
    else inside = null ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveFloat(elevation) ;
    s.saveInt(habitat().ID) ;
    s.saveObject(owner) ;
    if (inside == null) s.saveBool(false) ;
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
  
  public boolean inWorld() {
    return true ;
  }
  
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    return v.set(x, y, elevation()) ;
  }
  
  public float radius() {
    return 0.0f ;
  }
  
  public float height() {
    return 0 ;
  }
  
  
  /**  Setting path type and occupation-
    */
  public Element owner() {
    return owner ;
  }
  
  
  public void setOwner(Element e) {
    if (e == owner) return ;
    this.owner = e ;
    world.sections.flagBoundsUpdate(x, y) ;
  }
  
  
  public Habitat habitat() {
    if (habitat != null) return habitat ;
    return habitat = world.terrain().habitatAt(x, y) ;
  }
  
  
  public boolean blocked() {
    return
      (owner != null && owner.pathType() == PATH_BLOCKS) ||
      (! habitat().pathClear) ;
  }
  
  
  public int pathType() {
    if (owner != null) return owner.pathType() ;
    if (world.terrain().isRoad(this)) return PATH_ROAD ;
    return habitat().pathClear ? PATH_CLEAR : PATH_BLOCKS ;
  }
  
  
  public int owningType() {
    if (owner == null) return Element.NOTHING_OWNS ;
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
    
    if (owner() instanceof Venue) {
      final Tile e = ((Venue) owner()).entrance() ;
      for (int n : N_INDEX) {
        batch[n] = null ;
        final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
        if (t.owner() != this.owner && t != e) continue ;
        batch[n] = t ;
      }
      return batch ;
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
    
    for (int n : N_ADJACENT) {
      final Tile t = world.tileAt(x + N_X[n], y + N_Y[n]) ;
      if (t == null || ! (t.owner() instanceof Venue)) continue ;
      final Venue v = (Venue) t.owner() ;
      if (v.entrance() == this) batch[n] = v ;
    }
    
    return batch ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    put.set(x - 0.5f, y - 0.5f, 1, 1) ;
    return put ;
  }
  
  
  public void setInside(Mobile m, boolean is) {
    ///I.say(is+" for presence at "+this) ;
    if (is) {
      if (inside == null) inside = new Stack <Mobile> () ;
      inside.include(m) ;
    }
    else if (inside != null) {
      inside.remove(m) ;
      if (inside.size() == 0) inside = null ;
    }
  }
  
  
  public Stack <Mobile> inside() {
    if (inside == null) return new Stack <Mobile> () ;
    return inside ;
  }
  
  
  /**  Interface and media-
    */
  public String toString() {
    return "Tile at "+x+" "+y ;
  }
}




