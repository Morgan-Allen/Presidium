

package src.game.building ;
import src.game.common.* ;
import src.util.* ;



/**  This class is intended specifically to work with the BuildingsTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Placement {
  
  boolean pointsOkay(Tile points[]) ;
  void doPlace(Tile points[]) ;
}

//  TODO:  You also need to render a preview of the structure(s) being placed,
//  along with other interface methods, such as the help info and portrait,
//  in line with the Selectable interface.