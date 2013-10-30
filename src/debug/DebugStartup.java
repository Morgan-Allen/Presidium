

package src.debug ;
import src.game.campaign.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class DebugStartup {
  public static void main(String a[]) {
    
    final HUD UI = new HUD() ;
    final MainMenu mainMenu = new MainMenu(UI) ;
    mainMenu.relBound.set(0.5f, 0, 0, 1) ;
    mainMenu.absBound.set(-200, 0, 400, 0) ;
    mainMenu.attachTo(UI) ;
    
    PlayLoop.setupAndLoop(UI, null) ;
  }
}

