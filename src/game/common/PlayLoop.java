/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.graphics.widgets.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.user.* ;
//import jlibs.core.lang.RuntimeUtil ;  //  TODO:  RESTORE THIS.



public abstract class PlayLoop implements Session.Saveable {
  
  
  /**  Fields and constant definitions-
    */
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 25,
    
    DEFAULT_WIDTH  = 1000,
    DEFAULT_HEIGHT = 600,
    DEFAULT_HERTZ  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2 ;
  //final public static String SAVE_PATH = "saves/test_session.rep" ;

  private static boolean verbose = false ;
  
  
  private static PlayLoop currentGame ;
  private static Rendering rendering ;
  private static long lastFrame, lastUpdate, sT, rT ;
  private static float frameTime ;
  
  
  private HUD UI ;
  private World world ;
  private Base played ;

  private boolean
    paused  = false,
    started = true,
    loop    = true ;
  
  private int frameRate = FRAMES_PER_SECOND ;
  private float speedMultiple = 1.0f ;
  private static long lastSaveTime = -1 ;
  
  
  
  /**  Returns the components of the current game state-
    */
  public static HUD currentUI() {
    if (currentGame == null) return null ;
    return currentGame.UI ;
  }
  
  public static Rendering rendering() {
    return rendering ;
  }
  
  public static World world() {
    if (currentGame == null) return null ;
    return currentGame.world ;
  }
  
  public static Base played() {
    if (currentGame == null) return null ;
    return currentGame.played ;
  }
  
  
  
  /**  Overall loop setup, control, and execution.
    */
  protected PlayLoop() {
  }
  
  
  protected void gameStateWipe() {
    world = null ;
    played = null ;
    UI = null ;
    rendering.clearAll() ;
    lastSaveTime = -1 ;
    //RuntimeUtil.gc() ;  //  TODO:  RESTORE THIS.
  }
  
  
  protected boolean loadedAtStartup() {
    return false ;
  }
  
  
  protected void resetGame() {
    I.say("_____RESETING_WORLD______") ;
    gameStateWipe() ;
    world = createWorld() ;
    played = createBase(world) ;
    world.registerBase(played, true) ;
    UI = createUI(played, rendering) ;
    configureScenario(world, played, UI) ;
  }
  
  
  protected abstract World createWorld() ;
  protected abstract Base createBase(World world) ;
  protected abstract HUD createUI(Base base, Rendering rendering) ;
  protected abstract void configureScenario(World world, Base base, HUD HUD) ;
  
  
  
  /**  Save and load functionality-
    */
  public PlayLoop(Session s) throws Exception {
    this() ;
    s.cacheInstance(this) ;
    world = s.world() ;
    played = (Base) s.loadObject() ;
    UI = createUI(played, rendering) ;
    if (UI instanceof BaseUI) ((BaseUI) UI).loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(played) ;
    if (UI instanceof BaseUI) ((BaseUI) UI).saveState(s) ;
  }
  
  
  public static void saveGame(String saveFile) {
    if (currentGame == null) return ;
    KeyInput.clearInputs() ;
    try {
      Session.saveSession(world(), currentGame, saveFile) ;
      lastSaveTime = lastUpdate ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static void loadGame(String saveFile) {
    try {
      KeyInput.clearInputs() ;
      currentGame.gameStateWipe() ;
      currentGame = null ;
      final Session s = Session.loadSession(saveFile) ;
      currentGame = s.loop() ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static long timeSinceLastSave() {
    if (currentGame == null || lastSaveTime == -1) return -1 ;
    return
      (lastUpdate - lastSaveTime) / UPDATES_PER_SECOND ;
  }
  
  
  
  /**  Methods for override by subclasses-
    */
  protected void updateGameInputs() {
    UI.updateMouse() ;
  }
  
  
  protected void renderGameGraphics() {
    world.renderFor(rendering, played) ;
    played.renderFor(rendering) ;
    UI.renderWorldFX() ;
  }
  
  
  protected void updateGameState() {
    world.updateWorld() ;
  }
  
  
  protected boolean shouldExitLoop() {
    return false ;
  }
  
  
  
  /**  The big static runLoop method-
    */
  public static void runLoop(PlayLoop toLoop) {
    if (rendering == null) rendering = new Rendering(
      DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_HERTZ, false
    ) ;
    LoadService.loadClassesInDir("src/", "src") ;
    
    currentGame = toLoop ;
    if (currentGame != null) {
      if (! currentGame.loadedAtStartup()) currentGame.resetGame() ;
      currentGame.updateGameState() ;
    }

    lastUpdate = lastFrame = timeMS() ;
    long simTime = 0, renderTime = 0, loopNum = 0 ;
    while (currentGame == null || currentGame.loop) {
      final long time = timeMS() ;
      if (currentGame != null) {
        currentGame.playLoop() ;
      }
      //
      //  Go to sleep until the next round of updates are called for:
      try {
        final int FRAME_INTERVAL = currentGame == null ?
          MIN_SLEEP : (1000 / currentGame.frameRate) ;
        final int
          taken = (int) (timeMS() - time),
          sleeps = Math.max(FRAME_INTERVAL + (SLEEP_MARGIN - taken), 0) ;
        if (sleeps > 0) Thread.sleep(sleeps) ;
        simTime += sT ;
        renderTime += rT ;
        //
        //  Print out a report on rendering/update times- if desired.
        if (++loopNum >= 100) {
          final long totalTime = simTime + renderTime ;
          if (verbose) I.say(
            "Time spent on graphics/sim/total: " +
             (renderTime / loopNum) + " / " +
             (simTime    / loopNum) + " / " +
            (totalTime / loopNum)
          ) ;
          simTime = renderTime = loopNum = 0 ;
        }
      }
      catch (InterruptedException e) {}
    }
  }
  
  
  protected void playLoop() {
    //
    //  If you've just come out of pausing, update for the last frame.
    if (started) {
      lastUpdate = lastFrame ;
      started = false ;
    }
    //
    //  Calculate the time gaps for screen and world updates:
    long checkTime = 0 ;
    final long
      time = timeMS(),
      frameGap  = time - lastFrame,
      updateGap = time - lastUpdate ;
    final int FRAME_INTERVAL  = 1000 / frameRate ;
    final int UPDATE_INTERVAL = (int) (
      1000 / (UPDATES_PER_SECOND * speedMultiple)
    ) ;
    //
    //  Update rendering on-screen.  (The relative frame-time between update
    //  gaps must be provided to ensure smooth interpolation of sprites.)
    if (frameGap > FRAME_INTERVAL) {
      checkTime = timeMS() ;
      if (paused) {
        frameTime = 1.0f ;
      }
      else {
        frameTime = (updateGap - FRAME_INTERVAL) * 1.0f / UPDATE_INTERVAL ;
        frameTime = Math.max(0, Math.min(1, frameTime)) ;
      }
      rendering.updateViews() ;
      
      KeyInput.updateKeyboard() ;
      updateGameInputs() ;
      if (shouldExitLoop()) return ;
      
      renderGameGraphics() ;
      rendering.assignHUD(UI) ;
      rendering.renderDisplay() ;
      lastFrame = time ;
      rT = timeMS() - checkTime ;
    }
    //
    //  And perform any necessary world updates that would have occurred in
    //  this time:
    if (! paused) {
      checkTime = timeMS() ;
      final int maxUpdates = 1 + (FRAME_INTERVAL / UPDATE_INTERVAL) ;
      int numUpdates = (int) (updateGap / UPDATE_INTERVAL) ;
      numUpdates = Math.min(numUpdates, maxUpdates) ;
      
      for (int n = numUpdates ; n-- > 0 ;) {
        updateGameState() ;
      }
      //
      //  Now we essentially 'pretend' that updates were occurring once every
      //  UPDATE_INTERVAL milliseconds:
      lastUpdate += numUpdates * UPDATE_INTERVAL ;
      sT = timeMS() - checkTime ;
    }
  }
  
  private static long timeMS() { return System.nanoTime() / 1000000 ; }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    if (currentGame == null) return 0 ;
    return frameTime ;
  }
  
  
  public static boolean paused() {
    if (currentGame == null) return false ;
    return currentGame.paused ;
  }
  
  
  public static float gameSpeed() {
    if (currentGame == null) return -1 ;
    return currentGame.speedMultiple ;
  }
  
  
  public static void setGameSpeed(float mult) {
    if (currentGame == null) return ;
    currentGame.speedMultiple = Math.max(0, mult) ;
  }
  
  
  public static void setFrameRate(int rate) {
    if (currentGame == null) return ;
    currentGame.frameRate = Math.max(0, rate) ;
  }
  
  
  public static void setPaused(boolean p) {
    if (currentGame == null) return ;
    if (currentGame.paused && ! p) currentGame.started = true ;
    currentGame.paused = p ;
  }
}



