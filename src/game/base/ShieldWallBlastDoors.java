

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class ShieldWallBlastDoors extends Venue implements TileConstants {
  
  
  final static String
    IMG_DIR = "media/Buildings/military aura/" ;
  final static ImageModel
    DOORS_MODEL_LEFT = ImageModel.asIsometricModel(
      ShieldWallBlastDoors.class, IMG_DIR+"wall_gate_left.png" , 2.5f, 1.5f
    ),
    DOORS_MODEL_RIGHT = ImageModel.asIsometricModel(
      ShieldWallBlastDoors.class, IMG_DIR+"wall_gate_right.png", 2.5f, 1.5f
    ) ;
  
  
  private int facing ;
  private Tile entrances[] = null ;
  
  
  
  public ShieldWallBlastDoors(Base base, int facing) {
    super(3, 2, Venue.ENTRANCE_NONE, base) ;
    this.facing = facing ;
    if (facing == X_AXIS)
      attachSprite(DOORS_MODEL_LEFT.makeSprite()) ;
    if (facing == Y_AXIS)
      attachSprite(DOORS_MODEL_RIGHT.makeSprite()) ;
  }
  
  
  public ShieldWallBlastDoors(Session s) throws Exception {
    super(s) ;
    facing = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(facing) ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] {} ;
  }
  
  
  protected Item.Type[] itemsMade() {
    return new Item.Type[] {} ;
  }
  
  
  
  /**  Life cycle and placement-
    */
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    entrances = null ;
  }
  
  
  public Tile[] entrances() {
    if (entrances != null) return entrances ;
    final Tile o = origin() ;
    if (o == null) return null ;
    if (facing == X_AXIS) {
      return entrances = new Tile[] {
        o.world.tileAt(o.x + 1, o.y - 1),
        o.world.tileAt(o.x + 1, o.y + 3)
      } ;
    }
    if (facing == Y_AXIS) {
      return entrances = new Tile[] {
        o.world.tileAt(o.x - 1, o.y + 1),
        o.world.tileAt(o.x + 3, o.y + 1)
      } ;
    }
    return null ;
  }
  
  
  public boolean isEntrance(Tile t) {
    entrances() ;
    return (entrances[0] == t) || (entrances[1] == t) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */

  public String fullName() {
    return "Blast Doors" ;
  }


  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/shield_wall_button.gif") ;
  }


  public String helpInfo() {
    return
      "Blast Doors grant your citizens access to enclosed sector of your "+
      "base." ;
  }
}









