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
import org.lwjgl.input.Keyboard ;
import java.io.* ;



/**  This class is intended to initialise GUI functions for the player once the
  *  world is initialised.
  */
public class BaseUI extends HUD {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/" ;
  final public static Texture
    ICON_LIT_TEX  = Texture.loadTexture("media/GUI/iconLit.gif"),
    SELECT_CIRCLE = Texture.loadTexture("media/GUI/selectCircle.png"),
    SELECT_SQUARE = Texture.loadTexture("media/GUI/selectSquare.png") ;
  final public static Alphabet INFO_FONT = new Alphabet(
    "media/GUI/", "FontVerdana.gif", "FontVerdana.gif",
    "FontVerdana.map", 8, 16
  ) ;

  
  /**  This class implements rendering of a selection-overlay for currently
    *  hovered and picked objects.
    */
  
  /*
  final SelectOverlay
    HOVER_OVERLAY  = new SelectOverlay(),
    PICKED_OVERLAY = new SelectOverlay() ;
  //*/
  
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
        if (UI.currentPanel() instanceof BuildingsTab) {
          UI.setPanel(null) ;
        }
        else {
          final BuildingsTab tab = new BuildingsTab(UI) ;
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
    */
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
    */
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
    */
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
    */
  public Selectable playerHovered() { return hovered  ; }
  public Selectable playerSelection() { return picked ; }
  public Tile    pickedTile   () { return pickTile    ; }
  public Fixture pickedFixture() { return pickFixture ; }
  public Mobile  pickedMobile () { return pickMobile  ; }
  
  
  
  /**  Updates the current selection of items in the world-
    */
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
    */
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
    */
  public void setPanel(InfoPanel panel) {
    //I.say("Setting panel: "+panel) ;
    newPanel = panel ;
    picked = null ;
  }
  
  
  /**  Ensures that nothing is selected at the moment-
    */
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
    */
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









/*
buildingsButton = new Button(
  this, "media/GUI/Tabs/install_button.gif",
  "Open the installations panel"
) {
  protected void whenClicked() {
    if (UI.currentPanel() instanceof BuildingsTab) {
      UI.setPanel(null) ;
      return ;
    }
    final BuildingsTab tab = new BuildingsTab(UI) ;
    UI.setPanel(tab) ;
  }
} ;
buildingsButton.stretch = false ;
buildingsButton.absBound.set(10, 10, 40, 40) ;
buildingsButton.relBound.set(0, 0, 0, 0) ;
buildingsButton.attachTo(this) ;

creditText = new Text(this, INFO_FONT, "", false) ;
creditText.absBound.set(10, -50, 100, 40) ;
creditText.relBound.set(0.33f, 1, 0, 0) ;
creditText.attachTo(this) ;

minimap = new Minimap(this, world, realm) ;
minimap.relBound.set(1, 1, 0, 0) ;
minimap.absBound.set(-210, -210, 200, 200) ;
minimap.attachTo(this) ;
//*/
/*
messages = new MessageFeed(this) ;
messages.relBound.set(0.33f, 0.9f, 0.33f, 0.1f) ;
messages.absBound.set(0, -10, 0, 0) ;
messages.attachTo(this) ;
//*/



/**  Used to distinguish the outline/radius/boundary of a given structure,
  *  element or actor currently under selection.
  */
//  TODO:  Just use a single texture, for fuck's sake.
/*
private class SelectOverlay {
  
  final Box2D area = new Box2D() ;
  
  //  These are used in the case of a ficture being selected-
  final Box2D fixtureArea = new Box2D() ;
  //final Vars.Ref <Fixture> fixRef = new Vars.Ref <Fixture> () ;
  
  
  final Texture
    SELECT_CIRCLE = Texture.loadTexture("media/GUI/selectCircle.gif") ;
  final SFX
    overMobile = new RingFX(SELECT_CIRCLE, 1) ;
  //
  //  TODO:  Move this to the view.terrain package?
  final TileOverlay
    overActor = new TileOverlay(SELECT_CIRCLE, area) {
      protected boolean maskedAt(Terrain t, int x, int y) {
        return true ;
      }
    },
    overBuilding = new TileOverlay(
      Texture.loadTexture("media/GUI/selection_tiles.gif"),
      TileOverlay.PATTERN_OUTER_FRINGE
    ) {
      protected boolean maskedAt(Terrain t, int x, int y) {
        if (! fixtureArea.contains(x, y)) return false ;
        if (world.tileAt(x, y) == null) return false ;
        return true ;
      } 
    }
  ;
  
  void draw(Selectable selects, Viewport view) {
    if (selects == null) {
      return ;
    }
    if (selects instanceof Fixture) {
      final Fixture fixture = (Fixture) selects ;
      ///I.say("Fixture selected- "+fixture) ;
      area.setTo(fixtureArea.setTo(fixture.area())) ;
      area.xpos(area.xpos() - 1) ;
      area.ypos(area.ypos() - 1) ;
      area.xmax(area.xmax() + 2) ;
      area.ymax(area.ymax() + 2) ;
      overBuilding.setArea(area) ;
      world.terrain().drawOverlay(overBuilding) ;
    }
    else if (selects instanceof Mobile) {
      if (selects instanceof Actor) {
        final Actor actor = (Actor) selects ;
        if(actor.indoors()) return ;
        final float r = actor.targRadius() + 0.25f ;
        final Vec3D p = actor.selectPos() ;
        overActor.setArea(area.set(p.x - r, p.y - r, r * 2, r * 2)) ;
        world.terrain().drawOverlay(overActor) ;
      }
    }
    else if (selects instanceof Flag) {
      final Flag flag = (Flag) selects ;
      final Target target = flag.mission.subject() ;
      if (target instanceof Selectable) draw((Selectable) target, view) ;
    }
  }
}
//*/