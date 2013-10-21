

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;



//
//  TODO:  I need more SFX for the various powers.
//  Fade in/out for the save/load functions.
//  Swirly FX for remote vision.  Similar for kinesthesia.
//  Shield FX are done, they just need to be restored.

//  Save    (screen fade black)
//  Load    (screen fade grey)
//  Game speed  (screen overlay brown)
//  Far sight  (swirly FX)
//  Pushing   (swirly tilted FX)
//  Shields   (tilted FX)
//  Freezing  (Skin effect)
//  Reflex buff  (swirly tilted FX)
//  Telling  (tilted one-off FX)
/*
        WALK_THE_PATH, DENY_THE_VISION,
        TIME_DILATION, REMOTE_VIEWING,
        TELEKINESIS, FORCEFIELD,
        SUSPENSION,
        KINESTHESIA,
        VOICE_OF_COMMAND
//*/



public class Quickbar extends UIGroup implements UIConstants {
  
  final static int
    BUT_SIZE = 40 ;
  
  final BaseUI UI ;
  final Button slots[] = new Button[NUM_QUICK_SLOTS] ;
  private UIGroup optionList ;
  
  
  
  public Quickbar(BaseUI UI) {
    super(UI) ;
    this.UI = UI ;
  }
  
  
  class PowerTask implements UITask {
    
    final Power power ;
    final String option ;
    final Actor caster ;
    
    PowerTask(Power p, String o, Actor c) {
      power = p ;
      option = o ;
      caster = c ;
    }
    
    
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
  
  
  private UIGroup constructOptionList(final Power power, String options[]) {
    final UIGroup list = new UIGroup(UI) ;
    int i = 0 ; for (final String option : options) {
      final Text text = new Text(UI, Text.INFO_FONT) ;
      text.append(new Description.Link(option) {
        public void whenClicked() {
          final Actor caster = UI.played().ruler() ;
          final PowerTask task = new PowerTask(power, option, caster) ;
          UI.beginTask(task) ;
          optionList.detach() ;
        }
      }, Colour.GREY) ;
      text.absBound.set(0, i++ * 20, 66, 16) ;
      text.attachTo(list) ;
    }
    optionList = list ;
    return list ;
  }
  
  
  protected void setupPowers() {
    final Quickbar bar = this ;
    
    int i = 0 ; for (final Power power : Power.BASIC_POWERS) {
      final Button button = new Button(
        UI, power.buttonTex, Button.ICON_LIT_TEX,
        power.name.toUpperCase()+"\n  "+power.helpInfo
      ) {
        protected void whenClicked() {
          ///I.say(power.name+" CLICKED") ;
          final Actor caster = UI.played().ruler() ;
          if (optionList != null) optionList.detach() ;
          //
          //  If there are options, display them instead.
          final String options[] = power.options() ;
          if (options != null) {
            constructOptionList(power, options) ;
            optionList.absBound.setTo(this.absBound) ;
            optionList.absBound.ypos(BUT_SIZE + 2) ;
            optionList.attachTo(bar) ;
            return ;
          }
          else if (
            power.finishedWith(caster, null, null, true)
          ) return ;
          else {
            final PowerTask task = new PowerTask(power, null, caster) ;
            UI.beginTask(task) ;
          }
        }
      } ;
      button.absBound.set(i++ * (BUT_SIZE + 2), 0, BUT_SIZE, BUT_SIZE) ;
      button.attachTo(this) ;
    }
  }
  
  
  //
  //  TODO:  Use the task construction functions from theMissionsTab class.
  protected void setupMissionButtons() {
  }
}







