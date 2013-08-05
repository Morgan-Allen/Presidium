/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.user ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;
import org.lwjgl.input.* ;



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
  
  final Rendering rendering ;
  final public Camera camera ;
  
  Minimap minimap ;
  //Button charts, policies, household, futures ;  -for Later.
  UIGroup quickBar, readout ;
  
  UIGroup helpText ;
  UIGroup infoArea ;
  MainPanel mainPanel ;
  
  private UIGroup currentPanel, newPanel ;  //TODO:  DO A TRANSITION!
  private long panelInceptTime = -1 ;
  
  
  
  public BaseUI(World world, Rendering rendering) {
    this.world = world ;
    this.rendering = rendering ;
    this.camera = new Camera((BaseUI) (Object) this, rendering.port) ;
    configLayout() ;
  }
  
  
  public void assignBaseSetup(Base played, Vec3D homePos) {
    this.played = played ;
    if (homePos != null) rendering.port.cameraPosition.setTo(homePos) ;
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
  private void configLayout() {
    
    this.minimap = new Minimap(this, world, null) ;
    minimap.relBound.setTo(MINI_BOUNDS) ;
    minimap.absBound.setTo(MINI_INSETS) ;
    minimap.attachTo(this) ;
    
    infoArea = new UIGroup(this) ;
    infoArea.relBound.setTo(INFO_BOUNDS) ;
    infoArea.absBound.setTo(INFO_INSETS) ;
    infoArea.attachTo(this) ;
    
    mainPanel = new MainPanel(this) ;
    mainPanel.attachTo(infoArea) ;
    currentPanel = newPanel = mainPanel ;
    
    helpText = new Tooltips(this, INFO_FONT, TIPS_TEX, TIPS_INSETS) ;
    helpText.attachTo(this) ;
  }
  
  
  
  /**  Modifying the interface layout-
    */
  public void setInfoPanel(UIGroup infoPanel) {
    if (newPanel != currentPanel) return ;  //Not during a transition...
    if (infoPanel == currentPanel) return ;
    newPanel = infoPanel ;
    if (newPanel == null) {
      newPanel = mainPanel ;
    }
    if (newPanel != null) {
      newPanel.attachTo(infoArea) ;
      newPanel.relAlpha = 0 ;
      panelInceptTime = System.currentTimeMillis() ;
    }
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateInput() {
    super.updateInput() ;
    if (newPanel != currentPanel) {
      final float TRANSITION_TIME = 0.5f ;
      float fade = System.currentTimeMillis() - panelInceptTime ;
      fade = (fade / 1000f) / TRANSITION_TIME ;
      final float newFade = Visit.clamp((fade - 0.5f) * 2, 0, 1) ;
      final float oldFade = Visit.clamp((0.5f - fade) * 2, 0, 1) ;
      if (currentPanel != null) {
        currentPanel.relAlpha = oldFade ;
        if (fade >= 1) currentPanel.detach() ;
      }
      if (newPanel != null) newPanel.relAlpha = newFade ;
      if (fade >= 1) currentPanel = newPanel ;
    }
    if (selection.updateSelection(world, rendering.port, infoArea)) {
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
      camera.setLockOffset(infoArea.xdim() / -2, 0) ;
    }
    else {
      camera.setLockOffset(0, 0) ;
    }
    camera.updateCamera() ;
  }
  
  
  
  /**  Handling task execution (Outsource this to the HUD class?)-
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






