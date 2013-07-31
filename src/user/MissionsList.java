

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;



public class MissionsList extends UIGroup {
  
  
  
  final BaseUI UI ;
  List <Button> listing ;
  
  
  public MissionsList(BaseUI UI) {
    super(UI) ;
    this.UI = UI ;
  }
  
  
  
  protected void updateState() {
    
    //
    //  Only do this if the missions-list has changed?  Animate positional
    //  changes?
    
    for (Button button : listing) button.detach() ;
    
    float down = 0 ;
    for (final Mission mission : UI.played().allMissions()) {
      Texture t = mission.flagTex() ;
      final Button button = new Button(
        UI, t, Button.ICON_LIT_TEX, mission.fullName()
      ) {
        protected void whenHovered() {
        }
        protected void whenClicked() {
          UI.selection.setSelected(mission) ;
        }
      } ;
      
      button.absBound.set(0, down, 40, 40) ;
      button.relBound.set(0, 1, 0, 0) ;
      button.attachTo(this) ;
      down += 40 ;
    }
  }
}













