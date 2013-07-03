


package src.user ;
import org.lwjgl.input.Keyboard;

import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;


/*
 *  Main View Screen:
 *    Minimap.  Starcharts.  Career & Legislation.
 *    Stardate, Credits, Reputation and Psych Readout.
 *    Quickbar (for Powers, Mission Types, and hotkeyed Actors or Venues.)
 *    Logs and Active Missions.
 *    
 *  Information Panels:
 *    Info on Actors/Missions/Venues (Selectables.)
 *    Guilds/Construction.
 *    Missions/Personnel.
 *    Game Powers/Settings/Seat of Power.
 */



public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private World world ;
  private Base played ;
  
  private UITask currentTask ;
  final public Selection selection = new Selection(this) ;
  private InfoPanel selectPanel, newPanel ;
  
  final Rendering rendering ;
  final Camera camera ;
  
  
  
  public BaseUI(World world, Rendering rendering) {
    this.world = world ;
    this.rendering = rendering ;
    this.camera = new Camera((BaseUI) (Object) this, rendering.port) ;
    configLayout() ;
  }
  
  
  public void assignBaseSetup(Base played, Vec3D homePos) {
    this.played = played ;
    rendering.port.cameraPosition.setTo(homePos) ;
    minimap.setBase(played) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    final Base played = (Base) s.loadObject() ;
    assignBaseSetup(played, null) ;
    camera.loadState(s) ;
    selection.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played) ;
    camera.saveState(s) ;
    selection.saveState(s) ;
  }
  
  
  public Base played() { return played ; }
  public World world() { return world  ; }
  
  
  
  /**  Construction of the default interface layout-
    */
  Minimap minimap ;
  Button chartsButton, careerButton, logsButton ;
  UIGroup activeMissions, quickBar, readout ;
  
  //  TODO:  Solve this first.
  UIGroup helpText ;
  UIGroup infoPanel ;
  Button installButton, missionsButton, powersButton ;
  
  
  private void configLayout() {
    
    this.minimap = new Minimap(this, world, null) ;
    minimap.relBound.setTo(MINI_BOUNDS) ;
    minimap.absBound.setTo(MINI_INSETS) ;
    minimap.attachTo(this) ;
    
    
    infoPanel = new UIGroup(this) ;
    infoPanel.relBound.setTo(INFO_BOUNDS) ;
    infoPanel.attachTo(this) ;
    
    installButton = buttonFor(
      TABS_PATH+"install_button.gif",
      "Open the installations tab",
      new InstallTab(this), 0
    ) ;
    /*
    missionsButton = buttonFor(
      TABS_PATH+"missions_button.gif",
      "Open the missions tab",
      new MissionsTab(this), 1
    ) ;
    powersButton = buttonFor(
      TABS_PATH+"powers_button.gif",
      "Open the powers tab",
      new PowersTab(this), 2
    ) ;
    //*/
    
    helpText = new Tooltips(this, INFO_FONT, TIPS_TEX, TIPS_INSETS) ;
    helpText.attachTo(this) ;
  }
  
  
  private Button buttonFor(
    String img, String help, final InfoPanel panel, final int index
  ) {
    final BaseUI UI = this ;
    final Button button = new Button(this, img, help) {
      protected void whenClicked() {
        if (UI.selectPanel == panel) {
          UI.setPanel(null) ;
        }
        else {
          UI.selection.setSelected(null) ;
          UI.setPanel(panel) ;
        }
      }
    } ;
    
    button.relBound.setTo(TABS_BOUNDS) ;
    final float step = TABS_BOUNDS.xdim() / NUM_TABS ;
    button.relBound.xpos(step * index) ;
    button.relBound.xdim(step) ;

    button.stretch = false ;
    button.attachTo(infoPanel) ;
    return button ;
  }
  
  
  
  /**  Modifying the interface layout-
    */
  public void setPanel(InfoPanel panel) {
    newPanel = panel ;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput() ;
    if (newPanel != selectPanel) {
      if (selectPanel != null) selectPanel.detach() ;
      if (newPanel   != null) newPanel.attachTo(infoPanel) ;
      selectPanel = newPanel ;
    }
    if (selection.updateSelection(world, rendering.port, infoPanel)) {
      if (mouseClicked() && currentTask == null) {
        selection.setSelected(selection.hovered()) ;
      }
    }
  }
  
  
  public void renderWorldFX() {
    selection.renderWorldFX(rendering) ;
    if (currentTask != null) {
      if (KeyInput.isKeyDown(Keyboard.KEY_ESCAPE)) currentTask.cancelTask() ;
      else currentTask.doTask() ;
    }
  }
  
  
  public void renderHUD(Box2D bounds) {
    super.renderHUD(bounds) ;
    if (selection.selected() != null) {
      camera.setLockOffset(infoPanel.xdim() / -2, 0) ;
    }
    else {
      camera.setLockOffset(0, 0) ;
    }
    camera.updateCamera() ;
  }
  
  
  
  /**  Handling task execution (Outsource this to the HUD class.)-
    *  TODO:  That
    */
  public UITask currentTask() {
    return currentTask ;
  }
  
  
  public void beginTask(UITask task) {
    currentTask = task ;
  }
  
  
  public void endCurrentTask() {
    currentTask = null ;
  }
  

  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI() ;
    if (! (hud instanceof BaseUI)) return false ;
    return (o == null) || ((BaseUI) hud).selection.selected() == o ;
  }
  
  
  public static void logFor(Object o, String log) {
    if (isPicked(o)) I.say(System.currentTimeMillis()+": "+log) ;
  }
}






/*
public class BaseUI extends HUD implements UIConstants {

  
  /**  This class implements rendering of a selection-overlay for currently
    *  hovered and picked objects.
  final World world ;
  final Rendering rendering ;
  
  private Base played ;
  private Vec3D homePos = new Vec3D() ;
  final public Camera camera ;
  
  final public Minimap minimap ;
  private Button buildingsButton ;
  
  
  private Tile pickTile ;
  private Fixture pickFixture ;
  private Mobile pickMobile ;
  private Selectable hovered, picked ;
  
  private Text infoText ;
  private UITask task ;
  private UIGroup selectInfo, newPanel ;
  
  
  
  
  public BaseUI(World world, Rendering rendering) {
    this.world = world ;
    this.rendering = rendering ;
    this.camera = new Camera(this, rendering.port) ;
    this.relBound.set(0, 0, 1, 1) ;
    
    final BaseUI UI = this ;
    this.buildingsButton = new Button(
      this, "media/GUI/Tabs/install_button.gif",
      "Open the installations panel"
    ) {
      protected void whenClicked() {
        if (UI.currentPanel() instanceof InstallTab) {
          UI.setPanel(null) ;
        }
        else {
          final InstallTab tab = new InstallTab(UI) ;
          UI.setPanel(tab) ;
        }
      }
    } ;
    buildingsButton.stretch = false ;
    buildingsButton.absBound.set(10, 10, 40, 40) ;
    buildingsButton.relBound.set(0, 0, 0, 0) ;
    buildingsButton.attachTo(this) ;

    this.minimap = new Minimap(this, world, null) ;
    minimap.relBound.set(1, 1, 0, 0) ;
    minimap.absBound.set(-210, -210, 200, 200) ;
    minimap.attachTo(this) ;
    
    this.infoText = new Text(this, INFO_FONT) ;
    infoText.relBound.set(0.3f, 1, 0.3f, 0) ;
    infoText.absBound.set(0, -40, 0, 40) ;
    infoText.attachTo(this) ;
  }
  
  
  public void assignBaseSetup(Base played, Vec3D homePos) {
    this.played = played ;
    this.homePos.setTo(homePos) ;
    rendering.port.cameraPosition.setTo(homePos) ;
    minimap.setBase(played) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    final Base played = (Base) s.loadObject() ;
    homePos.loadFrom(s.input()) ;
    assignBaseSetup(played, homePos) ;
    camera.loadState(s) ;
    final Target lastSelect = s.loadTarget() ;
    setSelection((Selectable) lastSelect) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played) ;
    homePos.saveTo(s.output()) ;
    camera.saveState(s) ;
    s.saveTarget((Target) picked) ;
  }
  
  
  public Base played() { return played ; }
  

  /**  This should be called every frame immediately prior to world rendering.
  public void updateInput() {
    super.updateInput() ;
    //if (true) return ;
    if (KeyInput.isKeyDown(Keyboard.KEY_Q)) {
      //  Quit the game-
      System.exit(0) ;
    }
    updateSelection() ;
  }
  
  
  /**  This should be called every frame immediately following world rendering.
  public void renderWorldFX() {
    if (task != null) {
      if (KeyInput.isKeyDown(Keyboard.KEY_ESCAPE)) task.cancelTask() ;
      else task.doTask() ;
    }
    //
    //  You need to add the selection overlays as clients here.
    if (hovered instanceof Element && hovered != picked) {
      renderSelectFX((Element) hovered, Colour.transparency(0.5f)) ;
    }
    if (picked instanceof Element) {
      renderSelectFX((Element) picked, Colour.WHITE) ;
    }
  }
  
  
  private void renderSelectFX(Element element, Colour c) {
    if (element.sprite() == null) return ;
    final Texture ringTex = (element instanceof Fixture) ?
      SELECT_SQUARE :
      SELECT_CIRCLE ;
    final PlaneFX hoverRing = new PlaneFX(ringTex, element.radius() * 2) ;
    hoverRing.colour = c ;
    hoverRing.position.setTo(element.sprite().position) ;
    rendering.addClient(hoverRing) ;
  }
  
  
  
  /**  Whereas this is called to render the HUD itself, which occurs once all
    *  terrain, sprites, SFX etc. have been rendered-
  public void renderHUD(Box2D bounds) {
    super.renderHUD(bounds) ;
    //
    //  NOTE:  We have to wait until after HUD rendering for this, to ensure
    //  that the info-panel's bounds have been correctly updated.
    infoText.setText("Days: "+(world.currentTime() / World.DEFAULT_DAY_LENGTH)) ;
    
    if (selectInfo != null) camera.setLockOffset(selectInfo.xdim() / 2, 0) ;
    else camera.setLockOffset(0, 0) ;
    camera.updateCamera() ;
  }
  
  
  public void setTask(UITask task) {
    this.task = task ;
  }
  
  public void endTask() {
    task = null ;
  }
  
  public UITask currentTask() {
    return task ;
  }
  
  public void pushMessage(String message) {
    //if (messages == null) return ;
    //messages.addMessage(message) ;
  }

  
  
  /**  Here, we put various utility methods for selection of world elements-
  public Selectable playerHovered() { return hovered  ; }
  public Selectable playerSelection() { return picked ; }
  public Tile    pickedTile   () { return pickTile    ; }
  public Fixture pickedFixture() { return pickFixture ; }
  public Mobile  pickedMobile () { return pickMobile  ; }
  
  
  
  /**  Updates the current selection of items in the world-
  void updateSelection() {
    //
    //  If the new selection differs from the old, we can think about adding or
    //  removing it's associated information panel-
    if (newPanel != selectInfo) {
      if (selectInfo != null) selectInfo.detach() ;
      if (newPanel   != null) newPanel.attachTo(this) ;
      selectInfo = newPanel ;
    }
    //
    //  If a UI element is selected, don't pick anything else-
    if (selected() != null) {
      pickTile = null ;
      pickMobile = null ;
      pickFixture = null ;
      hovered = null ;
      return ;
    }
    //
    //  Our first task to see what the currently selected object is.  Start with
    //  tiles and actors-
    hovered = null ;
    pickTile = world.pickedTile(this, rendering.port) ;
    pickMobile = world.pickedMobile(this, rendering.port) ;
    ///if (pickMobile != null) I.say("Picked mobile: "+pickMobile) ;
    pickFixture = world.pickedFixture(this, rendering.port) ;
    
    if (pickMobile != null) hovered = pickMobile ;
    else if (pickFixture instanceof Selectable) {
      hovered = (Selectable) pickFixture ;
    }
    else hovered = null ;
    //
    //  Having done so, we can check whether the selection is active or not-
    if (mouseClicked() && task == null) {
      setSelection(hovered) ;
    }
  }
  
  
  
  /**  Sets the current selection to the given argument.
  public void setSelection(Selectable s) {
    if (s != null) {
      if ((s instanceof Element) && ((Element) s).inWorld()) {
        picked = s ;
        camera.lockOn(picked) ;
      }
      newPanel = s.createPanel(this) ;
      //if (s instanceof Actor) newPanel = new ActorPanel(this, (Actor) s) ;
      //else newPanel = new InfoPanel(this, s, InfoPanel.DEFAULT_TOP_MARGIN) ;
    }
    else if (picked != null) {
      camera.lockOn(picked = null) ;
      newPanel = null ;
    }
  }
  
  
  /**  Tells the UI to present the given info panel.
  public void setPanel(InfoPanel panel) {
    //I.say("Setting panel: "+panel) ;
    newPanel = panel ;
    picked = null ;
  }
  
  
  /**  Ensures that nothing is selected at the moment-
  void voidSelection() {
    pickTile = null ;
    pickMobile = null ;
    pickFixture = null ;
    hovered = null ;
    if (selectInfo != null) {
      selectInfo.detach() ;
      selectInfo = null ;
    }
  }
  
  
  /**  Returns the current information panel.
  UIGroup currentPanel() {
    return selectInfo ;
  }
  
  
  public static boolean isPicked(Object o) {
    final HUD hud = PlayLoop.currentUI() ;
    if (! (hud instanceof BaseUI)) return false ;
    return (o == null) || ((BaseUI) hud).picked == o ;
  }
  
  
  public static void logFor(Object o, String log) {
    if (isPicked(o)) I.say(System.currentTimeMillis()+": "+log) ;
  }
}

//*/


