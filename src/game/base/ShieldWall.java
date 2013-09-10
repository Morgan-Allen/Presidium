/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class ShieldWall extends Segment {
  
  
  /**  Construction and save/load routines-
    */
  final static String
    IMG_DIR = "media/Buildings/military/" ;
  final static ImageModel
    SECTION_MODELS[] = ImageModel.loadModels(
      ShieldWall.class, 2, 4, IMG_DIR,
      "wall_segment_left.png",
      "wall_segment_right.png",
      "wall_corner.png",
      "wall_tower_left.png",
      "wall_tower_right.png"
    ),
    SECTION_MODEL_LEFT   = SECTION_MODELS[0],
    SECTION_MODEL_RIGHT  = SECTION_MODELS[1],
    SECTION_MODEL_CORNER = SECTION_MODELS[2],
    TOWER_MODEL_LEFT     = SECTION_MODELS[3],
    TOWER_MODEL_RIGHT    = SECTION_MODELS[4],
    DOORS_MODEL_LEFT = ImageModel.asIsometricModel(
      ShieldWall.class, IMG_DIR+"wall_gate_left.png" , 4, 2
    ),
    DOORS_MODEL_RIGHT = ImageModel.asIsometricModel(
      ShieldWall.class, IMG_DIR+"wall_gate_right.png", 4, 2
    ) ;
  
  final protected static int
    TYPE_SECTION = 0,
    TYPE_TOWER   = 1,
    TYPE_DOORS   = 2 ;
  
  
  
  public ShieldWall(Base base) {
    super(2, 3, base) ;
    structure.setupStats(75, 35, 40, 0, false) ;
  }
  
  
  protected ShieldWall(int type, int size, int high, Base base) {
    super(size, high, base) ;
    structure.setupStats(200, 35, 100, 0, false) ;
    type = TYPE_DOORS ;
  }
  
  
  public ShieldWall(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Configuring sections of the line-
    */
  protected void configFromAdjacent(boolean[] near, int numNear) {
    final Tile o = origin() ;
    if (numNear == 2) {
      type = TYPE_SECTION ;
      if (near[N] && near[S]) {
        facing = Y_AXIS ;
        if (o.y % 8 == 0) {
          type = TYPE_TOWER ;
          attachModel(TOWER_MODEL_RIGHT) ;
        }
        else attachModel(SECTION_MODEL_RIGHT) ;
        return ;
      }
      if (near[W] && near[E]) {
        facing = X_AXIS ;
        if (o.x % 8 == 0) {
          type = TYPE_TOWER ;
          attachModel(TOWER_MODEL_LEFT) ;
        }
        else attachModel(SECTION_MODEL_LEFT) ;
        return ;
      }
    }
    facing = CORNER ;
    attachModel(SECTION_MODEL_CORNER) ;
  }
  

  protected List <Segment> installedBetween(Tile start, Tile end) {
    final List <Segment> installed = super.installedBetween(start, end) ;
    if (installed == null || installed.size() < 4) return installed ;
    //
    //  If the stretch to install is long enough, we cut out the middle two
    //  segments and install a set of Blast Doors in their place-
    final World world = start.world ;
    final int cut = installed.size() / 2 ;
    final ShieldWall
      a = (ShieldWall) installed.atIndex(cut),
      b = (ShieldWall) installed.atIndex(cut - 1) ;
    if (a.facing != b.facing || a.facing == CORNER) return installed ;
    //
    //  The doors occupy the exact centre of this area, with the same facing-
    final Vec3D centre = a.position(null).add(b.position(null)).scale(0.5f) ;
    final BlastDoors doors = new BlastDoors(a.base(), a.facing) ;
    doors.setPosition(centre.x - 1.5f, centre.y - 1.5f, world) ;
    final Box2D bound = doors.area(null) ;
    for (Venue v : installed) {
      if (v == a || v == b) continue ;
      if (v.area(null).cropBy(bound).area() > 0) return installed ;
    }
    //
    //  Update and return the list-
    installed.remove(a) ;
    installed.remove(b) ;
    installed.add(doors) ;
    return installed ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Shield Wall" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/shield_wall_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Shield Walls are defensive emplacements that improve base security." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
}




