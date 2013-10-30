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
import src.game.campaign.* ;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.user.* ;
import src.util.* ;



public class DebugSituation extends Scenario implements Economy {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugSituation test = new DebugSituation() ;
    PlayLoop.setupAndLoop(test.UI, test) ;
  }
  
  
  private Human citizen ;
  private Action lastAction = null ;
  
  
  protected DebugSituation() {
    super("saves/test_pathing.rep") ;
  }
  
  public DebugSituation(Session s) throws Exception {
    super(s) ;
    citizen = (Human) s.loadObject() ;
    lastAction = (Action) s.loadObject() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(citizen) ;
    s.saveObject(lastAction) ;
  }
  
  
  
  /**  Setup and initialisation-
    */
  protected boolean loadedAtStartup() {
    //if (true) return false ;
    try {
      PlayLoop.loadGame("saves/test_pathing.rep") ;
      final Base base = PlayLoop.currentScenario().base ;
      if (base.credits() < 500) base.incCredits(500 - base.credits()) ;
      PlayLoop.setGameSpeed(1.0f) ;
      GameSettings.psyFree = true ;
      //GameSettings.pathFree = true ;
      final World world = PlayLoop.currentScenario().world ;
      world.ephemera.applyFadeColour(Colour.GREY) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      32, 0.2f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    //
    //  TODO:  Put in an EcologyGen as well
    return world ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    I.say("Configuring world...") ;
    
    base.incCredits(10000) ;
    //GameSettings.fogFree = true ;
    GameSettings.buildFree = true ;
    //GameSettings.hireFree = true ;
    GameSettings.psyFree = true ;
    PlayLoop.setGameSpeed(1.0f) ;

    final Actor actor = new Human(Background.PHYSICIAN, base) ;
    final Actor other = new Human(Background.VETERAN  , base) ;
    actor.enterWorldAt(3, 3, world) ;
    other.enterWorldAt(5, 5, world) ;
    
    final Outcrop o = new Outcrop(2, 2, 0) ;
    o.enterWorldAt(4, 4, world) ;
    o.setAsEstablished(true) ;
    
    Scenario.establishVenue(new Sickbay(base), 9, 2, true, world) ;
    Scenario.establishVenue(new Garrison(base), 9, 8, true, world) ;
    
    //other.traits.incLevel(KINESTHESIA_EFFECT, 1) ;
    ((BaseUI) UI).selection.pushSelection(other, true) ;
  }
  
  
  
  /**  Updates and monitoring-
    */
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering) ;
    if (UI.currentTask() == null) {
      //highlightPlace() ;
      //highlightPath() ;
    }
  }
  
  /*
  public boolean shouldExitLoop() {
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
      final World world = PlayLoop.world() ;
      world.ephemera.applyFadeColour(Colour.BLACK) ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      //GameSettings.frozen = true ;
      PlayLoop.loadGame("saves/test_pathing.rep") ;
      final World world = PlayLoop.world() ;
      world.ephemera.applyFadeColour(Colour.GREY) ;
      return true ;
    }
    return false ;
  }
  //*/
  
  
  
  /**  Debugging actor and mobile pathing behaviour-
    */
  private void introduceCitizen(Tile free) {
    if (free == null) return ;
    citizen = new Human(Background.ARTIFICER, base) ;
    citizen.enterWorldAt(free.x, free.y, world) ;
    UI.selection.pushSelection(citizen, true) ;
  }
  
  
  private void assignRandomTarget(Human c) {
    if (c == null) return ;
    if (c.mind.topBehaviour() == lastAction) return ;
    final int size = world.size ;
    Tile dest = world.tileAt(Rand.index(size), Rand.index(size)) ;
    dest = Spacing.nearestOpenTile(dest, dest) ;
    if (dest == null) return ;
    I.say("SENDING ACTOR TO: "+dest) ;
    c.mind.assignBehaviour(lastAction = new Action(
      c, dest, this, "actionGo",
      Action.LOOK, "going to "+dest
    )) ;
  }
  
  
  public boolean actionGo(Actor actor, Tile dest) {
    return true ;
  }
  
  
  
  /**  Debugging pathfinding and region-caching-
    */
  public static void highlightPlace(
    Rendering rendering, World world, BaseUI UI
  ) {
    final Tile t = UI.selection.pickedTile() ;
    if (t == null) return ;
    
    final Tile placeTiles[] = world.pathingCache.placeTiles(t) ;
    final Tile placeRoutes[][] = world.pathingCache.placeRoutes(t) ;
    
    if (placeTiles == null || placeTiles.length < 1) return ;
    final TerrainMesh placeMesh = world.terrain().createOverlay(
      world, placeTiles, false, Texture.WHITE_TEX
    ) ;
    placeMesh.colour = Colour.SOFT_YELLOW ;
    rendering.addClient(placeMesh) ;
    
    if (KeyInput.wasKeyPressed('p')) {
      I.say(t.x+" "+t.y+" has "+placeRoutes.length+" routes") ;
      I.say("  road mask: "+world.terrain().roadMask(t)) ;
    }
    for (Tile route[] : placeRoutes) {
      if (placeRoutes == null || placeRoutes.length < 1) return ;
      final TerrainMesh routeMesh = world.terrain().createOverlay(
        world, route, false, Texture.WHITE_TEX
      ) ;
      routeMesh.colour = Colour.SOFT_CYAN ;
      rendering.addClient(routeMesh) ;
    }
  }

  
  private static Boardable picked = null ;
  private static Boardable lastFailAll[], lastFailPath[] ;
  private static Boardable lastSuccessPath[] ;
  
  
  public static void highlightPath(
    Rendering rendering, World world, BaseUI UI
  ) {
    renderOverlay(rendering, world, lastFailAll, Colour.SOFT_MAGENTA) ;
    renderOverlay(rendering, world, lastFailPath, Colour.SOFT_RED) ;
    if (UI.mouseClicked()) {
      if (picked != null) picked = null ;
      else picked = hovered(UI) ;
    }
    final Boardable hovered = hovered(UI) ;
    if (picked != null && hovered != null) {
      
      final PathingSearch search = (! GameSettings.pathFree) ?
        world.pathingCache.fullPathSearch(picked, hovered, null, 16) :
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
          renderOverlay(rendering, world, allSearched, Colour.SOFT_GREEN) ;
          renderOverlay(rendering, world, bestPath, Colour.SOFT_BLUE) ;
        }
      }
    }
    else if (hovered instanceof Tile) {
      renderOverlay(
        rendering, world, new Tile[] { (Tile) hovered }, Colour.SOFT_GREEN
      ) ;
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
  
  
  private static void renderOverlay(
    Rendering rendering, World world, Boardable path[], Colour c
  ) {
    final Batch <Tile> tiles = new Batch <Tile> () ;
    if (path != null) for (Boardable b : path) {
      if (b instanceof Tile) tiles.add((Tile) b) ;
    }
    if (tiles.size() > 0) {
      final TerrainMesh overlay = world.terrain().createOverlay(
        world, tiles.toArray(Tile.class), false, Texture.WHITE_TEX
      ) ;
      overlay.colour = c ;
      rendering.addClient(overlay) ;
    }
  }
}




