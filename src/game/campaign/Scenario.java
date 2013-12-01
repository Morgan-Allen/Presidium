

package src.game.campaign ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.graphics.widgets.* ;
import src.util.* ;
import java.io.* ;




public class Scenario implements Session.Saveable {
  
  
  private World world ;
  private Base base ;
  final boolean isDebug ;
  
  private BaseUI UI ;
  private List <String> timeStamps = new List <String> () ;
  private String savesPrefix ;
  
  
  
  public Scenario(World world, Base base, String saveFile) {
    this.world = world ;
    this.base = base ;
    this.savesPrefix = saveFile ;
    this.isDebug = false ;
    
    UI = createUI(base, PlayLoop.rendering()) ;
  }
  
  
  public Scenario(String saveFile, boolean isDebug) {
    this.savesPrefix = saveFile ;
    this.isDebug = isDebug ;
    setupScenario() ;
  }
  
  
  private void setupScenario() {
    this.world = createWorld() ;
    this.base = createBase(world) ;
    UI = createUI(base, PlayLoop.rendering()) ;
    configureScenario(world, base, UI) ;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.world() ;
    base = (Base) s.loadObject() ;
    savesPrefix = s.loadString() ;
    isDebug = s.loadBool() ;
    for (int i = s.loadInt() ; i-- > 0 ;) timeStamps.add(s.loadString()) ;
    
    UI = createUI(base, PlayLoop.rendering()) ;
    UI.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveString(savesPrefix) ;
    s.saveBool(isDebug) ;
    s.saveInt(timeStamps.size()) ;
    for (String stamp : timeStamps) s.saveString(stamp) ;
    
    UI.saveState(s) ;
  }
  
  
  public World world() { return world ; }
  public Base base() { return base ; }
  public BaseUI UI() { return UI ; }
  
  
  
  /**  Default methods for creating a new world, base, and user interface.
    */
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
  
  
  protected void resetScenario() {
    this.world = null ;
    this.base = null ;
    this.UI = null ;
    PlayLoop.gameStateWipe() ;
    setupScenario() ;
    PlayLoop.setupAndLoop(UI, this) ;
  }
  
  
  
  /**  Methods for keeping track of saved and loaded state-
    */
  //
  //  Saves must be tagged by ruler, day and time.  There can't be more than
  //  three for a given ruler at once- and if there would be, the least
  //  recently saved is deleted.  So you have the following array of files-
  //
  //  saves/<ruler>-current.rep     _ONLY THIS CAN BE LOADED FROM MENU_
  //  saves/<ruler><timestamp A>.rep
  //  saves/<ruler><timestamp B>.rep
  //  saves/<ruler><timestamp C>.rep
  //
  //  When and if you go back in the timeline, subsequent saves are deleted.
  //  So there's always a strictly ascending chronological order.
  
  final public static String
    CURRENT_SAVE = "-current" ;
  final public static int
    MAX_SAVES = 3 ;
  
  
  public String savesPrefix() {
    return savesPrefix ;
  }
  
  
  public void saveProgress(boolean overwrite) {
    //
    //  In the case of an overwrite, just save under the current file-
    if (overwrite) {
      PlayLoop.saveGame(fullSavePath(savesPrefix, CURRENT_SAVE)) ;
      return ;
    }
    //
    //  If necessary, delete the least recent save.
    if (timeStamps.size() >= MAX_SAVES) {
      final String oldStamp = timeStamps.removeFirst() ;
      final File f = new File(fullSavePath(savesPrefix, oldStamp)) ;
      if (f.exists()) f.delete() ;
    }
    //
    //  Create a new save.
    final float time = world.currentTime() / World.STANDARD_DAY_LENGTH ;
    String
      day = "Day "+(int) time,
      hour = ""+(int) (24 * (time % 1)),
      minute = ""+(int) (((24 * (time % 1)) % 1) * 60) ;
    while (hour.length() < 2) hour = "0"+hour ;
    while (minute.length() < 2) minute = "0"+minute ;
    
    final String newStamp = day+", "+hour+minute+" Hours" ;
    timeStamps.addLast(newStamp) ;
    PlayLoop.saveGame(fullSavePath(savesPrefix, newStamp)) ;
  }
  
  
  public String[] loadOptions() {
    //
    //  Strip away any missing entries-
    for (String stamp : timeStamps) {
      final File f = new File(fullSavePath(savesPrefix, stamp)) ;
      if (! f.exists()) timeStamps.remove(stamp) ;
    }
    return timeStamps.toArray(String.class) ;
  }
  
  
  public void wipeSavesAfter(String option) {
    //
    //  Delete any subsequent time-stamp entries.
    boolean matched = false ; for (String stamp : timeStamps) {
      if (matched) {
        timeStamps.remove(stamp) ;
        final File f = new File(fullSavePath(savesPrefix, stamp)) ;
        if (f.exists()) f.delete() ;
      }
      if (stamp.equals(option)) matched = true ;
    }
    if (! matched) I.complain("NO SUCH TIME STAMP!") ;
  }
  
  
  public static String fullSavePath(String prefix, String suffix) {
    if (suffix == null) return "saves"+prefix+".rep" ;
    return "saves/"+prefix+suffix+".rep" ;
  }
  
  
  public static List <String> savedFiles(String prefix) {
    final List <String> allSaved = new List <String> () ;
    final File savesDir = new File("saves/") ;
    
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName() ;
      if (! name.endsWith(".rep")) continue ;
      if (prefix == null) {
        if (! name.endsWith(CURRENT_SAVE+".rep")) continue ;
      }
      else if (! name.startsWith(prefix)) continue ;
      allSaved.add(name) ;
    }
    
    return allSaved ;
  }
  
  
  public static boolean loadedFrom(String prefix) {
    final String fullPath = fullSavePath(prefix, CURRENT_SAVE) ;
    final File file = new File(fullPath) ;
    if (! file.exists()) return false ;
    try {
      PlayLoop.loadGame(fullPath, true) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; }
    return false ;
  }
  
  
  
  
  /**  Methods for override by subclasses-
    */
  public boolean shouldExitLoop() {
    if (isDebug) {
      if (KeyInput.wasKeyPressed('r')) {
        I.say("RESET MISSION?") ;
        resetScenario() ;
        return false ;
      }
      if (KeyInput.wasKeyPressed('f')) {
        I.say("Paused? "+PlayLoop.paused()) ;
        PlayLoop.setPaused(! PlayLoop.paused()) ;
      }
      if (KeyInput.wasKeyPressed('s')) {
        I.say("SAVING GAME...") ;
        PlayLoop.saveGame(fullSavePath(savesPrefix, CURRENT_SAVE)) ;
        return false ;
      }
      if (KeyInput.wasKeyPressed('l')) {
        I.say("LOADING GAME...") ;
        PlayLoop.loadGame(fullSavePath(savesPrefix, CURRENT_SAVE), true) ;
        return true ;
      }
    }
    return false ;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if (PlayLoop.gameSpeed() != 1) {
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
    if (PlayLoop.gameSpeed() > 1) {
      Power.applyResting(PlayLoop.gameSpeed(), this) ;
    }
    world.updateWorld() ;
  }
  
  
  public void afterSaving() {
    world.ephemera.applyFadeColour(Colour.GREY) ;
    Power.applyWalkPath(this) ;
  }
  
  
  public void afterLoading(boolean fromMenu) {
    world.ephemera.applyFadeColour(Colour.BLACK) ;
    if (! fromMenu) Power.applyDenyVision(this) ;
  }
  
  

  
  /**  Helper/Utility methods-
    */
  //
  //  TODO:  This method needs to search for the *closest available* spot at
  //  which the given venue can be established (hell, it could be in the middle
  //  of an ocean.)  Use a Tilespread for the purpose(?)
  public static Venue establishVenue(
    final Venue v, int atX, int atY, boolean intact, final World world,
    Actor... employed
  ) {
    
    //
    //  First, you need to find a suitable entry point.  Try points close to
    //  the starting location, and perform tilespreads from there.
    //
    //  ...I think a pathing map might be needed for this purpose.
    
    Tile init = world.tileAt(atX, atY) ;
    init = Spacing.nearestOpenTile(init, init) ;
    if (init == null) return null ;
    
    final TileSpread search = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        v.setPosition(t.x, t.y, world) ;
        return v.canPlace() ;
      }
    } ;
    search.doSearch() ;
    
    if (! search.success()) return null ;//I.complain("NO STARTING POSITION FOUND!") ;
    else v.doPlace(v.origin(), null) ;
    
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








