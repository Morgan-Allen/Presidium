/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.user ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;




public class MainPanel extends UIGroup implements UIConstants {
  
  
  /**  Field definitions, constructors and initial setup-
    */
  final static int
    TAB_MISSIONS = 0,
    TAB_ADMIN    = 1,
    TAB_POWERS   = 2,
    TAB_COMM_LOG = 3,
    TAB_GUILDS   = 4 ;
  
  final public Texture BORDER_TEX = Texture.loadTexture(
    "media/GUI/main_pane_background.png"
  ) ;
  
  
  final static int
    BORDER_MARGIN = 10,
    PANEL_WIDE = INFO_AREA_WIDE - (BORDER_MARGIN * 2),
    PORTRAIT_DOWN = PANEL_WIDE / 3,
    
    PANES_TOP = PORTRAIT_DOWN ,
    PANE_B_WIDE = PANEL_WIDE / 2,
    PANE_B_HIGH = PANE_B_WIDE / 2,
    PANE_INSET_PERCENT = 10,
    PANES_HIGH = (PANE_B_HIGH * 2),
    PANES_BOTTOM = PANES_TOP + PANES_HIGH,
    
    GUILDS_MARGIN = 15,
    GUILDS_TOP = PANES_BOTTOM + 10,
    GUILDS_WIDE = PANEL_WIDE - (GUILDS_MARGIN * 2),
    GUILD_B_WIDE = GUILDS_WIDE / 3,
    GUILD_B_HIGH = (int) (GUILD_B_WIDE / 2.5f),
    GUILDS_HIGH = (GUILD_B_HIGH * 2),
    GUILDS_BOTTOM = GUILDS_TOP + GUILDS_HIGH,
    
    HEADER_TOP = 5,
    HEADER_HIGH = 20,
    TEXT_TOP = GUILDS_BOTTOM + 10 ;
  
  
  final BaseUI UI ;
  Bordering border ;
  Button adminButton, missionsButton, powersButton, commsButton ;
  Button guildButtons[] ;
  //  Add panes for Special Structures and a 'Blueprint mode'?  Maybe later.
  
  //  TODO:  You also need a portrait image for the top.
  Composite portraitImage ;
  Text mainText, headerText ;
  InfoPanel currentTab = null ;
  //InstallTab currentTab = null ;
  
  
  
  public MainPanel(BaseUI UI) {
    super(UI) ;
    this.UI = UI ;
    InstallTab.setupTypes() ;
    setupLayout() ;
  }
  
  
  void setupLayout() {
    final int m = BORDER_MARGIN ;
    this.relBound.set(0, 0, 1, 1) ;
    this.absBound.set(m, m, -(m * 2), -(m * 2)) ;
    
    border = new Bordering(UI, BORDER_TEX) ;
    border.drawInset.set(0, -PANES_HIGH, 0, PANES_HIGH) ;
    border.texInset.set(0, 0.2f, 1, 0.8f) ;
    border.relBound.set(0, 0, 1, 1) ;
    border.absBound.set(-m, -m, m * 2, (m * 2) - PANES_BOTTOM) ;
    border.attachTo(this) ;
    
    
    missionsButton = createPaneButton(
      TABS_PATH+"missions_tab.png",
      "Open the missions tab",
      TAB_MISSIONS, 0, 0
    ) ;
    missionsButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
    adminButton = createPaneButton(
      TABS_PATH+"install_tab.png",  //  CHANGE THIS
      "Open the administration tab",
      TAB_ADMIN, 0.5f, 0
    ) ;
    adminButton.setHighlight(Button.TRI_INV_LIT_TEX) ;
    powersButton = createPaneButton(
      TABS_PATH+"powers_tab.png",
      "Open the powers tab",
      TAB_POWERS, 1, 0
    ) ;
    powersButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
    commsButton = createPaneButton(
      TABS_PATH+"comms_tab.png",
      "Open the communications tab",
      TAB_COMM_LOG, 0.5f, 1
    ) ;
    commsButton.setHighlight(Button.TRIANGLE_LIT_TEX) ;
    
    
    createGuildButton(
      "militant_category_button", "Militant Structures",
      TAB_GUILDS + 0, 0, 0
    ) ;
    createGuildButton(
      "merchant_category_button", "Merchant Structures",
      TAB_GUILDS + 1, 1, 0
    ) ;
    createGuildButton(
      "aesthete_category_button", "Aesthete Structures",
      TAB_GUILDS + 2, 2, 0
    ) ;
    createGuildButton(
      "artificer_category_button", "Artificer Structures",
      TAB_GUILDS + 3, 0, 1
    ) ;
    createGuildButton(
      "ecologist_category_button", "Ecologist Structures",
      TAB_GUILDS + 4, 1, 1
    ) ;
    createGuildButton(
      "physician_category_button", "Physician Structures",
      TAB_GUILDS + 5, 2, 1
    ) ;
    
    portraitImage = new Composite(UI) ;
    portraitImage.relBound.set(0, 1, 1, 0) ;
    portraitImage.absBound.set(0, -PORTRAIT_DOWN, 0, PORTRAIT_DOWN) ;
    portraitImage.attachTo(this) ;
    portraitImage.stretch = true ;
    
    headerText = new Text(UI, BaseUI.INFO_FONT) ;
    headerText.relBound.set(0, 1, 1, 0) ;
    headerText.absBound.set(0, -(HEADER_TOP + HEADER_HIGH), 0, HEADER_HIGH) ;
    headerText.attachTo(this) ;
    
    mainText = new Text(UI, BaseUI.INFO_FONT) ;
    mainText.relBound.set(0, 0, 1, 1) ;
    mainText.absBound.set(GUILDS_MARGIN, 0, -(GUILDS_MARGIN * 2), -TEXT_TOP) ;
    mainText.attachTo(this) ;
    mainText.getScrollBar().attachTo(this) ;
  }
  
  
  private Button createPaneButton(
    String img, String help, final int buttonID, float a, float d
  ) {
    final Button button = new Button(UI, img, help) {
      protected void whenClicked() { switchToPane(buttonID) ; }
    } ;
    button.stretch = true ;
    button.selectMode = Button.MODE_ALPHA ;
    button.relBound.set(0, 1, 0, 0) ;

    final float
      m = PANE_INSET_PERCENT / 100f,
      xM = PANE_B_WIDE * m,
      yM = PANE_B_HIGH * m ;
    button.absBound.set(
      xM + (a * PANE_B_WIDE),
      yM - (((d + 1) * PANE_B_HIGH) + PANES_TOP),
      PANE_B_WIDE - (xM * 2), PANE_B_HIGH - (yM * 2)
    ) ;
    button.attachTo(this) ;
    return button ;
  }
  
  
  private Button createGuildButton(
    String img, String help, final int buttonID, float a, float d
  ) {
    final Button button = new Button(
      UI, BUTTONS_PATH+img+".png", help
    ) {
      protected void whenClicked() {
        switchToPane(buttonID) ;
      }
    } ;
    button.relBound.set(0, 1, 0, 0) ;
    button.absBound.set(
      GUILDS_MARGIN + (a * GUILD_B_WIDE),
      0 - (((d + 1) * GUILD_B_HIGH) + GUILDS_TOP),
      GUILD_B_WIDE, GUILD_B_HIGH
    ) ;
    button.attachTo(this) ;
    button.stretch = true ;
    return button ;
  }
  
  
  
  /**  Subsequent UI responses and content production-
    */
  private void switchToPane(int buttonID) {
    if (buttonID == TAB_MISSIONS) {
      currentTab = new MissionsTab(UI) ;
    }
    if (buttonID >= TAB_GUILDS) {
      final String catName = INSTALL_CATEGORIES[buttonID - TAB_GUILDS] ;
      currentTab = new InstallTab(this, catName) ;
    }
  }
  
  
  protected void updateState() {
    if (currentTab != null) {
      currentTab.updateText(UI, headerText, mainText) ;
    }
    super.updateState() ;
  }
}





///protected void whenHovered() { I.say("Hovering over pane "+buttonID) ; }




