/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class MineShaft extends Venue implements TileConstants {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static int
    TYPE_UNDER_SHAFT = 0,
    TYPE_OPEN_SHAFT  = 1,
    TYPE_DRILL_SHAFT = 2 ;
  final static Stack <Mobile> NONE_INSIDE = new Stack <Mobile> () ;
  
  
  final ExcavationSite mainShaft ;
  final int type ;
  
  
  public MineShaft(ExcavationSite site, int type) {
    super(2, 0, Venue.ENTRANCE_NORTH, site.base()) ;
    this.mainShaft = site ;
    this.type = type ;
    attachModel(Smelter.SHAFT_MODELS[2]) ;
  }
  
  
  public MineShaft(Session s) throws Exception {
    super(s) ;
    mainShaft = (ExcavationSite) s.loadObject() ;
    type = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(mainShaft) ;
    s.saveInt(type) ;
  }
  
  
  public Behaviour jobFor(Actor actor) { return null ; }
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  
  
  
  
  /**  Implementing the boardable interface-
    */
  /*
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D() ;
    final Tile o = origin() ;
    v.set(o.x, o.y, -1) ;
    return v ;
  }
  //*/
  
  public Boardable[] canBoard(Boardable[] batch) {
    return super.canBoard(batch) ;
    /*
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
    //*/
  }
  
  /*
  public boolean inWorld() {
    return shaft.faceAt(origin()) == this ;
  }
  //*/


  public boolean isEntrance(Boardable b) {
    return super.isEntrance(b) ;
    /*
    if (b == shaft) {
      return shaft.firstFace == this ;
    }
    if (b instanceof MineFace) {
      final MineFace m = (MineFace) b ;
      if (m.shaft != shaft) return false ;
      return Spacing.edgeAdjacent(m.origin(), origin()) ;
    }
    return false ;
    //*/
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Mine Shaft" ;
  }


  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Mine Shafts allow your excavators to access more distant seams of "+
      "mineral deposits." ;
  }


  public String buildCategory() {
    return UIConstants.TYPE_ARTIFICER ;
  }
}



