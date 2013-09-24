

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD;
import src.user.* ;



/**  This class is intended specifically to work with the InstallTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Structural extends Inventory.Owner {
  
  int buildCost() ;
  boolean pointsOkay(Tile from, Tile to) ;
  void doPlace(Tile from, Tile to) ;
  void preview(boolean canPlace, Rendering rendering, Tile from, Tile to) ;
  
  String fullName() ;
  Composite portrait(HUD UI) ;
  String helpInfo() ;
  String buildCategory() ;
  
  //
  //  TODO:  Have this return a VenueStructure object!  So vehicles can use it!
}















