/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import jlibs.core.lang.RuntimeUtil ;
import src.graphics.widgets.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.user.BaseUI ;



//
//  TODO:  You'll have to wait until the loop is finished before you exit
//  and start the new loop.


public abstract class PlayLoop implements Session.Saveable {
  
  
  /**  Fields and constant definitions-
    */
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 25,
    SLEEP_MARGIN = 2 ;
  final public static String
    SAVE_PATH = "saves/test_session.rep" ;
  
  
  private static PlayLoop currentGame ;
  private static Rendering rendering ;
  //private static Operation operation ;
  
  private HUD UI ;
  private World world ;
  private Base played ;

  private boolean
    paused  = false,
    started = true,
    loop    = true ;
  
  private int frameRate = FRAMES_PER_SECOND ;
  private float speedMultiple = 1.0f ;
  private long lastFrame, lastUpdate ;
  private float frameTime ;
  
  
  
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
  protected PlayLoop(boolean setup) {
    if (rendering == null) rendering = new Rendering(1000, 500, 60, false) ;
    currentGame = this ;
    LoadService.loadClassesInDir("src/", "src") ;
    if (setup) gameSetup() ;
  }
  
  
  protected void gameSetup() {
    GameSettings.buildFree = false ;
    GameSettings.noFog = false ;
    resetGame() ;
  }
  
  
  protected void gameStateWipe() {
    world = null ;
    played = null ;
    UI = null ;
    rendering.clearAll() ;
    RuntimeUtil.gc() ;
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
  //  Okay.  I understand the problem now.  This is being initialised too early
  //  during the loading process.  And it's making indirect references.
  public PlayLoop(Session s) throws Exception {
    this(false) ;
    ///I.say("  ...CACHING PLAYLOOP INSTANCE: "+this) ;
    s.cacheInstance(this) ;
    world = s.world() ;
    ///I.say("FINISHED LOADING WORLD") ;
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
    try { Session.saveSession(world(), currentGame, saveFile) ; }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public static void loadGame(String saveFile) {
    try {
      currentGame.gameStateWipe() ;
      currentGame = null ;
      final Session s = Session.loadSession(saveFile) ;
      currentGame = s.loop() ;
      currentGame.runLoop() ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  
  /**  Methods for override by subclasses-
    */
  protected void updateGameInputs() {
    UI.updateInput() ;
  }
  
  
  protected void renderGameGraphics() {
    world.renderFor(rendering, played) ;
    UI.renderWorldFX() ;
  }
  
  
  protected void updateGameState() {
    world.updateWorld() ;
  }
  
  
  protected boolean shouldExitLoop() {
    return false ;
  }
  
  
  public void runLoop() {
    updateGameState() ;
    lastUpdate = lastFrame = timeMS() ;
    long simTime = 0, renderTime = 0, sT = 0, rT = 0, loopNum = 0 ;
    while (loop) {
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
        updateGameInputs() ;
        if (shouldExitLoop()) break ;
        renderGameGraphics() ;
        rendering.assignHUD(UI) ;
        rendering.renderDisplay() ;
        lastFrame = time ;
        rT = timeMS() - checkTime ;
      }
      //
      //  And perform any neccesary world updates that would have occured in
      //  this time:
      if (! paused) {
        checkTime = timeMS() ;
        final int numUpdates = (int) (updateGap / UPDATE_INTERVAL) ;
        ///I.say("Number of updates: "+numUpdates) ;
        for (int n = numUpdates ; n-- > 0 ;) {
          updateGameState() ;
        }
        //
        //  Now we essentially 'pretend' that updates were occurring once every
        //  UPDATE_INTERVAL milliseconds:
        lastUpdate += numUpdates * UPDATE_INTERVAL ;
        sT = timeMS() - checkTime ;
      }
      //
      //  Go to sleep until the next round of updates are called for:
      try {
        final int
          taken = (int) (timeMS() - time),
          sleeps = Math.max(FRAME_INTERVAL + SLEEP_MARGIN - taken, 0) ;
        Thread.sleep(sleeps) ;
        simTime += sT ;
        renderTime += rT ;
        //
        //  Print out a report on rendering/update times- if desired.
        final boolean verbose = false ;
        if (++loopNum >= 100) {
          if (verbose) I.say(
            "Time spent on graphics/sim/total: " +
             (renderTime / loopNum) + " / " +
             (simTime    / loopNum) + " / " +
            ((renderTime + simTime) / loopNum)
          ) ;
          simTime = renderTime = loopNum = 0 ;
        }
      }
      catch (InterruptedException e) {}
    }
  }
  
  private static long timeMS() { return System.nanoTime() / 1000000 ; }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    if (currentGame == null) return 0 ;
    return currentGame.frameTime ;
  }
  
  public static void setGameSpeed(float mult) {
    if (currentGame == null) return ;
    currentGame.speedMultiple = Math.max(0, mult) ;
  }
  
  public static void setFrameRate(int rate) {
    if (currentGame == null) return ;
    currentGame.frameRate = rate ;
  }
  
  public static void setPaused(boolean p) {
    if (currentGame == null) return ;
    if (currentGame.paused && ! p) currentGame.started = true ;
    currentGame.paused = p ;
  }
}



