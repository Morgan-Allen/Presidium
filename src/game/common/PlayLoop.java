/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import org.lwjgl.opengl.Display ;
import src.game.campaign.* ;
import src.graphics.widgets.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.user.* ;
//import jlibs.core.lang.RuntimeUtil ;  //  TODO:  RESTORE THIS?



public final class PlayLoop {
  
  
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

  private static boolean verbose = false ;
  
  
  private static Rendering rendering ;
  private static HUD UI ;
  private static Scenario scenario ;
  
  private static long lastFrame, lastUpdate, sT, rT ;
  private static float frameTime ;
  
  private static boolean
    paused  = false,
    started = true,
    loop    = false,
    noInput = false ;
  
  private static int frameRate = FRAMES_PER_SECOND ;
  private static float speedMultiple = 1.0f ;
  private static long lastSaveTime = -1 ;
  
  
  
  /**  Returns the components of the current game state-
    */
  public static HUD currentUI() {
    return UI ;
  }
  
  public static Rendering rendering() {
    if (rendering == null) rendering = new Rendering(
      DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_HERTZ, false
    ) ;
    return rendering ;
  }
  
  
  public static Scenario currentScenario() {
    return scenario ;
  }
  
  
  
  /**  Save and load functionality-
    */
  public static void saveGame(String saveFile) {
    if (scenario == null) return ;
    KeyInput.clearInputs() ;
    try {
      Session.saveSession(scenario.world(), scenario, saveFile) ;
      lastSaveTime = lastUpdate ;
      scenario.afterSaving() ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static void loadGame(String saveFile, boolean fromMenu) {
    try {
      gameStateWipe() ;
      final Session s = Session.loadSession(saveFile) ;
      scenario = s.scenario() ;
      scenario.afterLoading(fromMenu) ;
      setupAndLoop(scenario.UI(), scenario) ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static void gameStateWipe() {
    KeyInput.clearInputs() ;
    Spacing.wipeTempArrays() ;
    I.talkAbout = null ;
    
    scenario = null ;
    UI       = null ;
    if (rendering != null) rendering.clearAll() ;
    lastSaveTime = -1 ;
    //RuntimeUtil.gc() ;  //  TODO:  RESTORE THIS?
  }
  
  
  public static long timeSinceLastSave() {
    if (scenario == null || lastSaveTime == -1) return -1 ;
    return (lastUpdate - lastSaveTime) / UPDATES_PER_SECOND ;
  }
  
  
  
  /**  The big static setup, run and exit methods-
    */
  public static void setupAndLoop(HUD UI, Scenario scenario) {
    PlayLoop.UI = UI ;
    PlayLoop.scenario = scenario ;
    runLoop() ;
  }
  
  
  public static void exitLoop() {
    loop = false ;
    PlayLoop.UI = null ;
    PlayLoop.scenario = null ;
  }
  
  
  protected static void runLoop() {
    if (loop) return ;
    LoadService.loadPackage("src") ;
    rendering() ;
    
    if (scenario != null) {
      scenario.updateGameState() ;
    }
    
    lastUpdate = lastFrame = timeMS() ;
    loop = true ;
    long simTime = 0, renderTime = 0, loopNum = 0 ;
    while (loop) {
      final long time = timeMS() ;
      loopCycle() ;
      //
      //  Go to sleep until the next round of updates are called for:
      try {
        final int
          frameInterval = (1000 / frameRate),
          taken = (int) (timeMS() - time),
          sleeps = Math.max(frameInterval + (SLEEP_MARGIN - taken), 0) ;
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

    Display.destroy() ;
    java.lang.System.exit(0) ;
  }
  
  
  protected static void loopCycle() {
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
      
      if (UI != null && ! noInput) {
        KeyInput.updateKeyboard() ;
        UI.updateMouse() ;
      }
      
      if (scenario != null && scenario.shouldExitLoop()) return ;
      if (scenario != null) scenario.renderVisuals(rendering) ;
      
      if (UI != null) {
        UI.renderWorldFX() ;
        rendering.assignHUD(UI) ;
      }
      
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
      
      if (scenario != null) for (int n = numUpdates ; n-- > 0 ;) {
        scenario.updateGameState() ;
      }
      //
      //  Now we essentially 'pretend' that updates were occurring once every
      //  UPDATE_INTERVAL milliseconds:
      lastUpdate += numUpdates * UPDATE_INTERVAL ;
      sT = timeMS() - checkTime ;
    }
  }
  
  private static long timeMS() {
    return java.lang.System.nanoTime() / 1000000 ;
  }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    return frameTime ;
  }
  
  
  public static boolean paused() {
    return paused ;
  }
  
  
  public static float gameSpeed() {
    return speedMultiple ;
  }
  
  
  public static void setGameSpeed(float mult) {
    ///I.say("Setting game speed: "+mult) ;
    speedMultiple = Math.max(0, mult) ;
  }
  
  
  public static void setFrameRate(int rate) {
    frameRate = Math.max(0, rate) ;
  }
  
  
  public static void setPaused(boolean p) {
    if (paused && ! p) started = true ;
    paused = p ;
  }
  
  
  public static void setNoInput(boolean n) {
    noInput = n ;
  }
}


