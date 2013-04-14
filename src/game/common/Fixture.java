/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.graphics.common.* ;
import src.user.Selectable ;
import src.util.* ;



public abstract class Fixture extends Element implements Selectable {
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public int size, high ;
  final Box2D area = new Box2D() ;
  
  
  public Fixture(int size, int high) {
    this.size = size ;
    this.high = high ;
  }
  
  
  public Fixture(Session s) throws Exception {
    super(s) ;
    size = s.loadInt() ;
    high = s.loadInt() ;
    area.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(size) ;
    s.saveInt(high) ;
    area.saveTo(s.output()) ;
  }
  
  
  
  /**  Life cycle, ownership and positioning-
    */
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    for (Tile t : origin().world.tilesIn(area, false)) {
      if (t == null || t.blocked()) return false ;
    }
    return true ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    for (Tile t : world.tilesIn(area, false)) {
      t.setOwner(this) ;
    }
  }
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    area.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
  }
  
  
  public void exitWorld() {
    for (Tile t : world().tilesIn(area, false)) {
      t.setOwner(null) ;
    }
    super.exitWorld() ;
  }


  public int xdim() { return size ; }
  public int ydim() { return size ; }
  public int zdim() { return high ; }
  public float radius() { return size / 2 ; }
  public Box2D area() { return area ; }
  
  
  public Vec3D position(Vec3D v) {
    final Tile o = origin() ;
    if (o == null) return null ;
    if (v == null) v = new Vec3D() ;
    v.set(
      o.x + (size / 2f) - 0.5f,
      o.y + (size / 2f) - 0.5f,
      o.elevation()
    ) ;
    return v ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
}







