/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;



public interface Selectable extends Text.Clickable, Session.Saveable {
  
  String fullName() ;
  String helpInfo() ;
  
  String[] infoCategories() ;
  Composite portrait(HUD UI) ;
  void writeInformation(Description description, int categoryID, HUD UI) ;
  
  void whenClicked() ;
  InfoPanel createPanel(BaseUI UI) ;
  Target subject() ;
  void renderSelection(Rendering rendering, boolean hovered) ;
}