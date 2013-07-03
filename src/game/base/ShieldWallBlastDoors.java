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
import src.user.* ;
import src.util.* ;



public class ShieldWallBlastDoors extends Venue implements TileConstants {
  
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
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
  
  
  
  /**  Employment methods-
    */
  protected Vocation[] careers() {
    return new Vocation[] {} ;
  }
  
  
  protected Item.Type[] goods() {
    return new Item.Type[] {} ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  
  /**  Life cycle and placement-
    */
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    entrances = null ;
  }
  
  
  public Tile mainEntrance() {
    return entrances()[0] ;
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
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[2] ;
    else for (int i = batch.length ; i-- > 2 ;) batch[i] = null ;
    entrances() ;
    batch[0] = entrances[0] ;
    batch[1] = entrances[1] ;
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable t) {
    entrances() ;
    return (entrances[0] == t) || (entrances[1] == t) ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    entrances() ;
    final Tile s[] = surrounds() ;
    if (inWorld) Paving.clearRoad(s) ;
    world.terrain().maskAsPaved(s, inWorld) ;
    for (Tile t : entrances()) {
      base().paving.updateJunction(t, inWorld) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Blast Doors" ;
  }


  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/shield_wall_button.gif") ;
  }


  public String helpInfo() {
    return
      "Blast Doors grant your citizens access to enclosed sector of your "+
      "base." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITARY ;
  }
}









