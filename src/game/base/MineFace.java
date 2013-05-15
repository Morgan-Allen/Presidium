

package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;


//  Now... I'd like to extend this to cover underground tunnels too.

public class MineFace extends Element implements Boardable, TileConstants {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  private static Stack <Mobile> NONE_INSIDE = new Stack <Mobile> () ;
  
  MineShaft parent ;
  Stack <Mobile> inside = NONE_INSIDE ;
  
  protected float promise = 0 ;
  private float workDone = 0 ;
  
  
  
  public MineFace(MineShaft parent) {
    this.parent = parent ;
  }
  
  
  public MineFace(Session s) throws Exception {
    super(s) ;
    parent = (MineShaft) s.loadObject() ;
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ()) ;
    else inside = NONE_INSIDE ;
    promise = s.loadFloat() ;
    workDone = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(parent) ;
    if (inside == NONE_INSIDE) s.saveBool(false) ;
    else { s.saveBool(true) ; s.saveObjects(inside) ; }
    s.saveFloat(promise) ;
    s.saveFloat(workDone) ;
  }
  
  
  
  /**  Implementing the boardable interface-
    */
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    final Tile o = origin() ;
    v.set(o.x, o.y, -1) ;
    return v ;
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
  
  
  public Series <Mobile> inside() {
    return inside ;
  }
  
  
  public Boardable[] canBoard(Boardable[] batch) {
    if (batch == null) batch = new Boardable[5] ;
    final Tile o = origin() ;
    int i = 0 ; for (int n : N_ADJACENT) {
      final Tile tN = o.world.tileAt(o.x + N_X[n], o.y + N_Y[n]) ;
      final MineFace near = parent.openingAt(tN) ;
      if (near == null) continue ;
      batch[i++] = near ;
    }
    if (parent.firstFace == this) batch[i++] = parent ;
    while (i < batch.length) batch[i++] = null ;
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable b) {
    if (b == parent) {
      return parent.firstFace == this ;
    }
    if (b instanceof MineFace) {
      final MineFace m = (MineFace) b ;
      if (m.parent != parent) return false ;
      return Spacing.edgeAdjacent(m.origin(), origin()) ;
    }
    return false ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.assignedBase() == parent.base() ;
  }
  
}




/**  Rendering and interface methods-
  */
/*
public String fullName() {
  return "Shaft Opening" ;
}


public Texture portrait() {
  return null ;
}


public String helpInfo() {
  return
    "Shaft Openings allow your excavators to access subterranean deposits "+
    "further afield." ;
}


public String[] infoCategories() { return null ; }
public void writeInformation(Description description, int categoryID) {}
public void whenClicked() {}
//*/


/*
public boolean canPlace() {
  final Box2D surround = new Box2D().setTo(area()).expandBy(1) ;
  for (Tile t : origin().world.tilesIn(surround, false)) {
    if (t == null || ! t.habitat().pathClear) return false ;
    if (t.owningType() > Element.ENVIRONMENT_OWNS) return false ;
  }
  return true ;
}

public void enterWorldAt(int x, int y, World world) {
  final Box2D surround = new Box2D().setTo(area()).expandBy(1) ;
  for (Tile t : origin().world.tilesIn(surround, false)) {
    if (t.owner() != null) t.owner().exitWorld() ;
  }
  super.enterWorldAt(x, y, world) ;
}
//*/

