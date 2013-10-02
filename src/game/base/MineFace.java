/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Extend Segment instead?


public class MineFace extends Element implements Boardable, TileConstants {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  private static Stack <Mobile> NONE_INSIDE = new Stack <Mobile> () ;
  
  ExcavationSite shaft ;
  Stack <Mobile> inside = NONE_INSIDE ;
  
  protected float
    promise = 0,
    workDone = 0 ;
  
  
  
  public MineFace(ExcavationSite parent) {
    this.shaft = parent ;
  }
  
  
  public MineFace(Session s) throws Exception {
    super(s) ;
    shaft = (ExcavationSite) s.loadObject() ;
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ()) ;
    else inside = NONE_INSIDE ;
    promise = s.loadFloat() ;
    workDone = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(shaft) ;
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
      final MineFace near = shaft.faceAt(tN) ;
      if (near == null) continue ;
      batch[i++] = near ;
    }
    if (shaft.firstFace == this) batch[i++] = shaft ;
    while (i < batch.length) batch[i++] = null ;
    return batch ;
  }
  
  
  public boolean inWorld() {
    return shaft.faceAt(origin()) == this ;
  }
  
  
  public boolean isEntrance(Boardable b) {
    if (b == shaft) {
      return shaft.firstFace == this ;
    }
    if (b instanceof MineFace) {
      final MineFace m = (MineFace) b ;
      if (m.shaft != shaft) return false ;
      return Spacing.edgeAdjacent(m.origin(), origin()) ;
    }
    return false ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.base() == shaft.base() ;
  }
  
  
  public boolean openPlan() {
    return false ;
  }
  
  
  public String toString() {
    final Tile o = origin() ;
    return "Mine Face at: "+o.x+" "+o.y+" ("+promise+")" ;
  }
}




