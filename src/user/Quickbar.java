

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;



public class Quickbar extends UIGroup implements UIConstants {
  
  final static int
    BUT_SIZE = 40 ;
  
  final BaseUI UI ;
  final Button slots[] = new Button[NUM_QUICK_SLOTS] ;
  
  
  
  public Quickbar(BaseUI UI) {
    super(UI) ;
    this.UI = UI ;
  }
  
  
  
  class PowerTask implements UITask {
    
    final Power power ;
    final Actor caster ;
    
    PowerTask(Power p, Actor c) { power = p ; caster = c ; }
    
    public void doTask() {
      final boolean clicked = UI.mouseClicked() ;
      Object hovered = UI.selection.hovered() ;
      if (hovered == null) hovered = UI.selection.pickedTile() ;
      
      if (hovered instanceof Target) {
        final Target picked = (Target) hovered ;
        if (power.finishedWith(caster, null, picked, clicked)) {
          cancelTask() ;
        }
      }
    }
    
    public void cancelTask() { UI.endCurrentTask() ; }
    public Texture cursorImage() { return power.buttonTex ; }
  }
  
  
  protected void setupPowers() {
    
    int i = 0 ; for (final Power power : Power.BASIC_POWERS) {
      final Button button = new Button(
        UI, power.buttonTex, Button.ICON_LIT_TEX,
        power.name.toUpperCase()+"\n  "+power.helpInfo
      ) {
        protected void whenClicked() {
          I.say(power.name+" CLICKED") ;
          final Actor caster = UI.played().ruler() ;
          //
          //  TODO:  You need to display options here...
          
          if (
            power.finishedWith(caster, null, null, true)
          ) return ;
          else {
            I.say(power.name+" needs more arguments...") ;
            final PowerTask task = new PowerTask(power, caster) ;
            UI.beginTask(task) ;
          }
        }
      } ;
      button.absBound.set(i++ * (BUT_SIZE + 2), 0, BUT_SIZE, BUT_SIZE) ;
      button.attachTo(this) ;
    }
  }
  
  //
  //  TODO:  Implement.
  protected void setupMissionButtons() {
    
  }
}



