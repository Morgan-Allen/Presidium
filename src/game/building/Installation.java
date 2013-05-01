

package src.game.building ;
//import src.game.base.MagLineNode ;
import src.game.base.MagLineNode;
import src.game.base.ShieldWallSection;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.TerrainMesh ;
import src.util.* ;



/**  This class is intended specifically to work with the BuildingsTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Installation {
  
  
  boolean pointsOkay(Tile from, Tile to) ;
  void doPlace(Tile from, Tile to) ;
  void preview(boolean canPlace, Rendering rendering, Tile from, Tile to) ;
  
  String fullName() ;
  Texture portrait() ;
  String helpInfo() ;
}












