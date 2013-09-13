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



public class BlastDoors extends ShieldWall implements TileConstants {
  
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  //private int facing ;
  private Tile entrances[] = null ;
  
  
  
  public BlastDoors(Base base, int facing) {
    super(TYPE_DOORS, 4, 2, base) ;
    //*
    this.facing = facing ;
    if (facing == X_AXIS)
      attachSprite(ShieldWall.DOORS_MODEL_LEFT.makeSprite()) ;
    if (facing == Y_AXIS)
      attachSprite(ShieldWall.DOORS_MODEL_RIGHT.makeSprite()) ;
    //*/
  }
  
  
  public BlastDoors(Session s) throws Exception {
    super(s) ;
    //facing = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    //s.saveInt(facing) ;
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
        o.world.tileAt(o.x + 1, o.y + 4)
      } ;
    }
    if (facing == Y_AXIS) {
      return entrances = new Tile[] {
        o.world.tileAt(o.x - 1, o.y + 1),
        o.world.tileAt(o.x + 4, o.y + 1)
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
    base().paving.updatePerimeter(this, inWorld) ;
    for (Tile t : entrances()) {
      base().paving.updateJunction(this, t, inWorld) ;
    }
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.terrain().maskAsPaved(Spacing.under(area(), world), true) ;
  }
  
  
  public void exitWorld() {
    world.terrain().maskAsPaved(Spacing.under(area(), world), false) ;
    super.exitWorld() ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Blast Doors" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/shield_wall_button.gif") ;
  }


  public String helpInfo() {
    return
      "Blast Doors grant your citizens access to enclosed sector of your "+
      "base." ;
  }
  
  
  //
  //  TODO:  Allow the player to control access to a given area by restricting
  //  what kind of personnel are allowed through given doors.
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
}









