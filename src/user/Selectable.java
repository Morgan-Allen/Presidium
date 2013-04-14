/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.util.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;



public interface Selectable extends Text.Clickable, Target {
  
  String fullName() ;
  Texture portrait() ;
  String helpInfo() ;
  
  String[] infoCategories() ;
  void writeInformation(Description description, int categoryID) ;
  
  void whenClicked() ;
}