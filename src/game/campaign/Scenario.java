

package src.game.campaign ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD;
import src.graphics.widgets.KeyInput;
import src.user.* ;
import src.util.* ;



public class Scenario implements Session.Saveable {
  
  
  final public World world ;
  final public Base base ;
  
  private String saveFile ;
  final public BaseUI UI ;
  
  
  
  public Scenario(World world, Base base, String saveFile) {
    this.world = world ;
    this.base = base ;
    this.saveFile = saveFile ;
    
    UI = createUI(base, PlayLoop.rendering()) ;
  }
  
  
  public Scenario(String saveFile) {
    this.world = createWorld() ;
    this.base = createBase(world) ;
    this.saveFile = saveFile ;
    
    UI = createUI(base, PlayLoop.rendering()) ;
    configureScenario(world, base, UI) ;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.world() ;
    base = (Base) s.loadObject() ;
    saveFile = s.loadString() ;
    
    UI = createUI(base, PlayLoop.rendering()) ;
    UI.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveString(saveFile) ;
    
    UI.saveState(s) ;
  }
  
  
  public String saveFile() {
    return saveFile ;
  }
  
  
  protected BaseUI createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  

  protected World createWorld() {
    final World world = new World(64) ;
    return world ;
  }
  
  
  protected Base createBase(World world) {
    return Base.createFor(world) ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
  }
  
  /*
  public boolean loadedAtStartup() {
    try {
      PlayLoop.loadGame(saveFile) ;
      final Base base = PlayLoop.played() ;
      if (base.credits() < 2000) base.incCredits(2000) ;
      PlayLoop.setGameSpeed(1.0f) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  //*/
  
  
  
  /**  Methods for override by subclasses-
    */
  public boolean shouldExitLoop() {
    if (KeyInput.wasKeyPressed('r')) {
      I.say("RESET MISSION?") ;
      //resetGame() ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('f')) {
      I.say("Paused? "+PlayLoop.paused()) ;
      PlayLoop.setPaused(! PlayLoop.paused()) ;
    }
    if (KeyInput.wasKeyPressed('s')) {
      I.say("SAVING GAME...") ;
      PlayLoop.saveGame(saveFile) ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      PlayLoop.loadGame(saveFile) ;
      return true ;
    }
    return false ;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if (PlayLoop.gameSpeed() < 1) {
      final Colour blur = new Colour().set(0.5f, 0.5f, 0.1f, 0.4f) ;
      world.ephemera.applyFadeColour(blur) ;
    }
    world.renderFor(rendering, base) ;
    base.renderFor(rendering) ;
  }
  
  
  public void updateGameState() {
    if (PlayLoop.gameSpeed() < 1) {
      Power.applyTimeDilation(PlayLoop.gameSpeed(), this) ;
    }
    world.updateWorld() ;
  }
  
  
  public void afterSaving() {
    world.ephemera.applyFadeColour(Colour.GREY) ;
    Power.applyWalkPath(this) ;
  }
  
  
  public void afterLoading() {
    world.ephemera.applyFadeColour(Colour.BLACK) ;
    Power.applyDenyVision(this) ;
  }
  
  

  
  /**  Helper/Utility methods-
    */
  public static Venue establishVenue(
    Venue v, int atX, int atY, boolean intact, World world,
    Actor... employed
  ) {
    v.doPlace(world.tileAt(atX, atY), null) ;
    if (intact) {
      v.structure.setState(Structure.STATE_INTACT, 1.0f) ;
      v.onCompletion() ;
    }
    else {
      v.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
    }
    final Tile e = world.tileAt(v) ;
    for (Actor a : employed) {
      a.mind.setEmployer(v) ;
      if (! a.inWorld()) {
        a.assignBase(v.base()) ;
        a.enterWorldAt(e.x, e.y, world) ;
        a.goAboard(v, world) ;
      }
    }
    if (GameSettings.hireFree) VenuePersonnel.fillVacancies(v) ;
    v.setAsEstablished(true) ;
    return v ;
  }
}