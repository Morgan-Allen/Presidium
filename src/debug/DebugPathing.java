/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.user.* ;
import src.util.* ;



public class DebugPathing extends PlayLoop implements BuildConstants {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugPathing test = new DebugPathing() ;
    PlayLoop.runLoop(test) ;
  }
  
  
  private Human citizen ;
  private Action lastAction = null ;
  
  
  protected DebugPathing() {
    super() ;
  }
  
  public DebugPathing(Session s) throws Exception {
    super(s) ;
    //if (true) return ;
    citizen = (Human) s.loadObject() ;
    lastAction = (Action) s.loadObject() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    //if (true) return ;
    s.saveObject(citizen) ;
    s.saveObject(lastAction) ;
  }
  
  
  
  /**  Setup and initialisation-
    */
  protected boolean loadedAtStartup() {
    ///if (true) return false ;
    try {
      PlayLoop.loadGame("saves/test_pathing.rep") ;
      final Base base = PlayLoop.played() ;
      if (base.credits() < 500) base.incCredits(500 - base.credits()) ;
      PlayLoop.setGameSpeed(0.5f) ;
      ///GameSettings.pathFree = true ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      128, 0.2f,
      //Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    //TG.setupMinerals(world, 0.55f, 0.3f, 0.15f) ;
    //TG.setupOutcrops(world) ;
    return world ;
  }
  
  
  protected Base createBase(World world) {
    Base base = new Base(world) ;
    return base ;
  }
  
  
  protected HUD createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  
  
  protected void configureScenario(World world, Base base, HUD HUD) {
    I.say("Configuring world...") ;
    
    GameSettings.noFog = true ;
    GameSettings.hireFree = true ;
    GameSettings.buildFree = true ;
    
    final StockExchange EA = new StockExchange(base) ;
    DebugBehaviour.establishVenue(EA, 5, 5, true) ;
    EA.stocks.bumpItem(DECOR, 50) ;
    
    final StockExchange EB = new StockExchange(base) ;
    DebugBehaviour.establishVenue(EB, 15, 5, true) ;
    EB.stocks.forceDemand(DECOR, 5000, 0) ;
    
    final BotanicalStation BS = new BotanicalStation(base) ;
    DebugBehaviour.establishVenue(BS, 10, 15, true) ;
    
    ((BaseUI) HUD).selection.pushSelection(BS, true) ;
  }
  
  
  
  /**  Updates and monitoring-
    */
  protected void updateGameState() {
    super.updateGameState() ;
    assignRandomTarget(citizen) ;
  }
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;
    if (((BaseUI) currentUI()).currentTask() == null) {
      //highlightPlace() ;
      //highlightPath() ;
    }
  }
  
  
  protected boolean shouldExitLoop() {
    if (KeyInput.wasKeyPressed('r')) {
      resetGame() ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('f')) {
      PlayLoop.setPaused(! PlayLoop.paused()) ;
    }
    if (KeyInput.wasKeyPressed('s')) {
      I.say("SAVING GAME...") ;
      PlayLoop.saveGame("saves/test_pathing.rep") ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      //GameSettings.frozen = true ;
      PlayLoop.loadGame("saves/test_pathing.rep") ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Debugging actor and mobile pathing behaviour-
    */
  private void introduceCitizen(Tile free) {
    if (free == null) return ;
    citizen = new Human(Background.ARTIFICER, played()) ;
    citizen.enterWorldAt(free.x, free.y, world()) ;
    ((BaseUI) currentUI()).selection.pushSelection(citizen, true) ;
  }
  
  
  private void assignRandomTarget(Human c) {
    if (c == null) return ;
    if (c.AI.topBehaviour() == lastAction) return ;
    final int size = world().size ;
    Tile dest = world().tileAt(Rand.index(size), Rand.index(size)) ;
    dest = Spacing.nearestOpenTile(dest, dest) ;
    if (dest == null) return ;
    I.say("SENDING ACTOR TO: "+dest) ;
    c.AI.assignBehaviour(lastAction = new Action(
      c, dest, this, "actionGo",
      Action.LOOK, "going to "+dest
    )) ;
  }
  
  
  public boolean actionGo(Actor actor, Tile dest) {
    return true ;
  }
  
  
  
  /**  Debugging pathfinding and region-caching-
    */
  public static void highlightPlace() {
    final Tile t = ((BaseUI) currentUI()).selection.pickedTile() ;
    ////I.say("Picked tile is: "+t) ;
    if (t == null) return ;
    
    final Tile placeTiles[] = world().pathingCache.placeTiles(t) ;
    final Tile placeRoutes[][] = world().pathingCache.placeRoutes(t) ;
    
    if (placeTiles == null || placeTiles.length < 1) return ;
    final TerrainMesh placeMesh = world().terrain().createOverlay(
      world(), placeTiles, false, Texture.WHITE_TEX
    ) ;
    placeMesh.colour = Colour.SOFT_YELLOW ;
    rendering().addClient(placeMesh) ;
    
    if (KeyInput.wasKeyPressed('p')) {
      I.say(placeRoutes.length+" routes to/from place at: "+t) ;
    }
    for (Tile route[] : placeRoutes) {
      if (placeRoutes == null || placeRoutes.length < 1) return ;
      final TerrainMesh routeMesh = world().terrain().createOverlay(
        world(), route, false, Texture.WHITE_TEX
      ) ;
      routeMesh.colour = Colour.SOFT_CYAN ;
      rendering().addClient(routeMesh) ;
    }
  }

  
  private static Boardable picked = null ;
  private static Boardable lastFailAll[], lastFailPath[] ;
  private static Boardable lastSuccessPath[] ;
  
  
  public static void highlightPath() {
    final BaseUI UI = (BaseUI) currentUI() ;
    renderOverlay(lastFailAll, Colour.SOFT_MAGENTA) ;
    renderOverlay(lastFailPath, Colour.SOFT_RED) ;
    if (UI.mouseClicked()) {
      if (picked != null) picked = null ;
      else picked = hovered(UI) ;
    }
    final Boardable hovered = hovered(UI) ;
    if (picked != null && hovered != null) {
      
      final PathingSearch search = (! GameSettings.pathFree) ?
        world().pathingCache.fullPathSearch(picked, hovered, null, 16) :
        new PathingSearch(picked, hovered) ;
      
      if (search != null) {
        if (KeyInput.wasKeyPressed('p')) search.verbose = true ;
        search.doSearch() ;
        final Boardable
          allSearched[] = search.allSearched(Boardable.class),
          bestPath[] = search.bestPath(Boardable.class) ;
        if (! search.success()) {
          I.say("TILE PATHING FAILED BETWEEN "+picked+" AND "+hovered) ;
          lastFailAll = allSearched ;
          lastFailPath = bestPath ;
        }
        else {
          lastSuccessPath = bestPath ;
          renderOverlay(allSearched, Colour.SOFT_GREEN) ;
          renderOverlay(bestPath, Colour.SOFT_BLUE) ;
        }
      }
    }
    else if (hovered instanceof Tile) {
      renderOverlay(new Tile[] { (Tile) hovered }, Colour.SOFT_GREEN) ;
      if (KeyInput.wasKeyPressed('p')) {
        final Tile tile = ((Tile) hovered) ;
        I.say("Current tile: "+tile) ;
        I.say("  OWNED BY: "+tile.owner()) ;
        I.say("  BLOCKED: "+tile.blocked()) ;
        //I.say("  OWNING TYPE: "+tile.owningType()) ;
        //I.say("  PATHING TYPE: "+tile.pathType()) ;
      }
    }
  }
  
  
  private static boolean checkForChange(Boardable newPath[]) {
    if (lastSuccessPath != null && newPath == null) return true ;
    if (newPath == null || lastSuccessPath == null) return false ;
    //
    //  If the path doesn't connect the same points, ignore it-
    if (Visit.last(newPath) != Visit.last(lastSuccessPath)) return false ;
    if (newPath[0] != lastSuccessPath[0]) return false ;
    //
    //  Otherwise, ensure the contents are identical-
    if (newPath.length != lastSuccessPath.length) return true ;
    for (int i = newPath.length ; i-- > 0 ;) {
      if (newPath[i] != lastSuccessPath[i]) return true ;
    }
    return false ;
  }
  
  
  private static Boardable hovered(BaseUI UI) {
    final Tile t = UI.selection.pickedTile() ;
    if (t != null && t.owner() instanceof Venue) {
      return (Venue) t.owner() ;
    }
    return t ;
  }
  
  
  private static void renderOverlay(Boardable path[], Colour c) {
    final Batch <Tile> tiles = new Batch <Tile> () ;
    if (path != null) for (Boardable b : path) {
      if (b instanceof Tile) tiles.add((Tile) b) ;
    }
    if (tiles.size() > 0) {
      final TerrainMesh overlay = world().terrain().createOverlay(
        world(), tiles.toArray(Tile.class), false, Texture.WHITE_TEX
      ) ;
      overlay.colour = c ;
      rendering().addClient(overlay) ;
    }
  }
}




