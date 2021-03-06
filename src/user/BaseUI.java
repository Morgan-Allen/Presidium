/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.user ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;
import src.game.actors.ActorHealth ;

import org.lwjgl.input.* ;
import org.lwjgl.opengl.GL11 ;
import org.lwjgl.* ;
import java.nio.* ;




public class BaseUI extends HUD implements UIConstants {
  
  
  
  /**  Core field definitions, constructors, and save/load methods-
    */
  private World world ;
  private Base played ;
  
  private UITask currentTask ;
  final public Selection selection = new Selection(this) ;
  
  final Rendering rendering ;
  final public Camera camera ;
  
  UIGroup helpText ;
  Minimap minimap ;
  Text readout ;
  UIGroup infoArea ;
  //MainPanel mainPanel ;
  Quickbar quickbar ;
  
  private ByteBuffer panelFade ;
  private UIGroup currentPanel, newPanel ;
  private long panelInceptTime = -1 ;
  private boolean capturePanel = false ;
  
  
  
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
    
    this.readout = new Text(this, INFO_FONT) ;
    readout.relBound.set(0, 1, 1, 0) ;
    readout.absBound.set(200, -50, -300, 40) ;
    readout.attachTo(this) ;
    
    this.infoArea = new UIGroup(this) ;
    infoArea.relBound.setTo(INFO_BOUNDS) ;
    infoArea.absBound.setTo(INFO_INSETS) ;
    infoArea.attachTo(this) ;
    
    //this.mainPanel = new MainPanel(this) ;
    //mainPanel.attachTo(infoArea) ;
    currentPanel = newPanel = null ;
    
    this.quickbar = new Quickbar(this) ;
    quickbar.absBound.set(20, 20, -40, 0) ;
    quickbar.relBound.set(0, 0, 1, 0) ;
    quickbar.attachTo(this) ;
    quickbar.setupMissionButtons() ;
    quickbar.setupPowersButtons() ;
    quickbar.setupInstallButtons() ;
    
    //
    //  This *has* to be attached last, to ensure it displays on top of other
    //  components-
    this.helpText = new Tooltips(this, INFO_FONT, TIPS_TEX, TIPS_INSETS) ;
    helpText.attachTo(this) ;
  }
  
  
  
  /**  Modifying the interface layout-
    */
  public void setInfoPanel(UIGroup infoPanel) {
    //if (newPanel != currentPanel) return ;  //Not during a transition...
    if (infoPanel == currentPanel) return ;
    newPanel = infoPanel ;
    //if (newPanel == null) newPanel = mainPanel ;
  }
  
  
  
  /**  Core update and rendering methods, in order of execution per-frame.
    */
  public void updateMouse() {
    super.updateMouse() ;
    if (selection.updateSelection(world, rendering.port, infoArea)) {
      if (mouseClicked() && currentTask == null) {
        selection.pushSelection(selection.hovered(), true) ;
      }
    }
    I.talkAbout = selection.selected() ;
  }
  
  
  protected void updateState() {
    super.updateState() ;
    updateReadout() ;
  }
  
  
  protected void updateReadout() {
    //
    //  TODO:  Move these functions to a separate class.
    if (readout == null) return ;
    readout.setText("") ;
    //
    //  Credits first-
    final int credits = played.credits() ;
    if (credits >= 0) readout.append(credits+" Credits", Colour.WHITE) ;
    else readout.append((0 - credits)+" In Debt", Colour.YELLOW) ;
    readout.append("   ") ;
    //
    //  Then time and date-
    final float
      time = world.currentTime() / World.STANDARD_DAY_LENGTH ;
    final int
      days  = (int) time,
      hours = (int) ((time - days) * 24) ;
    String hS = hours+"00" ;
    while (hS.length() < 4) hS = "0"+hS ;
    String dS = "Day "+days+" "+hS+" Hours" ;
    readout.append(dS) ;
    //
    //  And finally current psy points-
    final boolean ruled = played.ruler() != null ;
    final ActorHealth RH = ruled ? played.ruler().health : null ;
    final int PS = ruled ? 2 * (int) RH.maxPsy() : 0 ;
    float psyPoints = 0 ;
    if (played.ruler() != null) {
      psyPoints += played.ruler().health.psyPoints() ;
      psyPoints *= PS / RH.maxPsy() ;
    }
    if (PS > 0 && psyPoints > 0) {
      readout.append("   Psy Points: ") ;
      float a = psyPoints / PS ;
      Colour tone = new Colour().set((1 - a) / 2, a, (1 - a), 1) ;
      while (--psyPoints > 0) {
        readout.append("|", tone) ;
        a = psyPoints / PS ;
        tone = new Colour().set((1 - a) / 2, a, (1 - a), 1) ;
        tone.setValue(1) ;
      }
      if ((psyPoints + 1) > 0) {
        tone.a = psyPoints + 1 ;
        readout.append("|", tone) ;
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
    
    if (currentPanel != newPanel) {
      beginPanelFade() ;
      if (currentPanel != null) currentPanel.detach() ;
      if (newPanel != null) newPanel.attachTo(infoArea) ;
      currentPanel = newPanel ;
    }
    if (capturePanel) {
      panelFade = UINode.copyPixels(infoArea.trueBounds(), panelFade) ;
      capturePanel = false ;
    }
    
    final float TRANSITION_TIME = 0.33f ;
    float fade = System.currentTimeMillis() - panelInceptTime ;
    fade = (fade / 1000f) / TRANSITION_TIME ;
    if (fade <= 1) {
      GL11.glColor4f(1, 1, 1, 1 - fade) ;
      UINode.drawPixels(infoArea.trueBounds(), panelFade) ;
    }
  }
  
  
  protected void beginPanelFade() {
    panelInceptTime = System.currentTimeMillis() ;
    capturePanel = true ;
  }
  
  
  
  /**  Handling task execution (Outsource this to the HUD class?)-
    */
  public UITask currentTask() {
    return currentTask ;
  }
  
  
  public UIGroup currentPanel() {
    return this.currentPanel ;
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
}



