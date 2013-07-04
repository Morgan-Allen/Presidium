


package src.user ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/" ;
  
  final public static Texture
    ICON_LIT_TEX  = Texture.loadTexture("media/GUI/iconLit.gif"),
    SELECT_CIRCLE = Texture.loadTexture("media/GUI/selectCircle.png"),
    SELECT_SQUARE = Texture.loadTexture("media/GUI/selectSquare.png"),
    TIPS_TEX = Texture.loadTexture("media/GUI/tips_frame.png") ;
  
  final public static Alphabet
    INFO_FONT = new Alphabet(
      "media/GUI/", "FontVerdana.gif", "FontVerdana.gif",
      "FontVerdana.map", 8, 16
    ) ;
  
  
  final public static int
    NUM_TABS = 3,
    NUM_QUICK_SLOTS = 10,
    MAX_TIP_WIDTH = 200 ;
  //  Move the tooltips class over to this package.
  
  //  ...Include insets as well?
  final public static Box2D
    MAIN_BOUNDS = new Box2D().set(0, 1, 0.66f, 1.0f),
    
    MINI_BOUNDS = new Box2D().set(0, 1, 0, 0),
    MINI_INSETS = new Box2D().set(10, -210, 200, 200),
    
    
    INFO_BOUNDS = new Box2D().set(0.66f, 0, 0.33f, 1.0f),
    PANE_BOUNDS = new Box2D().set(0, 0, 1.0f, 0.9f),
    TABS_BOUNDS = new Box2D().set(0, 0.9f, 1.0f, 0.1f),
    
    TIPS_INSETS = new Box2D().set(-10, -10, 20, 20) ;
  
  
  //  The size of the tooltip text.
  //  The size of the info header and label/category text.
  //  The area allocated to buttons for the guilds/missions/powers tabs.
  
  //  The size of the minimap.
  //  The size of the credits readout.
  //  The area allocated to buttons for the charts/career/logs tabs.
  //  The size of the quickbar.
  
}




