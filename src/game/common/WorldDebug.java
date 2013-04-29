/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.PathingCache.Place ;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.user.* ;
import src.util.* ;





public class WorldDebug extends PlayLoop {
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    WorldDebug test = new WorldDebug() ;
    test.runLoop() ;
  }
  

  Citizen citizen ;
  Action lastAction = null ;
  Tile picked = null ;
  
  
  protected WorldDebug() {
    super(true) ;
  }
  
  public WorldDebug(Session s) throws Exception {
    super(s) ;
    citizen = (Citizen) s.loadObject() ;
    lastAction = (Action) s.loadObject() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(citizen) ;
    s.saveObject(lastAction) ;
  }
  
  
  /**  Setup and initialisation-
    */
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    World world = new World(TG.generateTerrain()) ;
    return world ;
  }
  
  
  protected Base createBase(World world) {
    Base base = new Base(world) ;
    return base ;
  }
  
  
  protected HUD createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.setupUI(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  
  
  protected void configureScenario(World world, Base base, HUD HUD) {
    //
    //  Now, just plonk down buildings instantly, deduct credits, and add
    //  workers!
    I.say("Configuring world...") ;
    Tile free = null ;
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      Flora.tryGrowthAt(c.x, c.y, world, true) ;
      final Tile t = world.tileAt(c.x, c.y) ;
      if (! t.blocked()) free = t ;
    }
    GameSettings.freePath = true ;
  }
  
  
  
  /**  Updates and monitoring-
    */
  protected void updateGameState() {
    super.updateGameState() ;
  }
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;
    this.highlightPath() ;
  }
  

  protected boolean shouldExitLoop() {
    if (KeyInput.wasKeyPressed('r')) {
      resetGame() ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('s')) {
      I.say("SAVING GAME...") ;
      PlayLoop.saveGame("saves/test_session.rep") ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      PlayLoop.loadGame("saves/test_session.rep") ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Debugging actor behaviour-
    */
  private void introduceCitizen(Tile free) {
    if (free != null) {
      citizen = new Citizen(Vocation.ARTIFICER, played()) ;
      citizen.enterWorldAt(free.x, free.y, world()) ;
      ((BaseUI) currentUI()).setSelection(citizen) ;
    }
  }
  
  private void assignRandomTarget(Citizen c) {
    final int size = world().size ;
    Tile dest = world().tileAt(Rand.index(size), Rand.index(size)) ;
    dest = Spacing.nearestOpenTile(dest, dest) ;
    if (dest == null) return ;
    I.say("SENDING ACTOR TO: "+dest) ;
    c.assignAction(lastAction = new Action(
      c, dest, this, "actionGo",
      Model.AnimNames.LOOK, "going..."
    )) ;
  }
  
  public boolean actionGo(Actor actor, Tile dest) {
    return true ;
  }
  
  
  /**  Debugging pathfinding and region-caching-
    */
  private void highlightRegion() {
    final Tile t = ((BaseUI) currentUI()).pickedTile() ;
    if (t == null) return ;
    final Place p = played().pathingCache.tilePlaces[t.x][t.y] ;
    if (p == null) return ;
    final TerrainMesh previewMesh = world().terrain().createOverlay(
      p.tiles, false, Texture.WHITE_TEX
    ) ;
    //previewMesh.colour = Colour.GREEN ;
    rendering().addClient(previewMesh) ;
  }
  
  
  private void highlightPath() {
    final BaseUI UI = (BaseUI) currentUI() ;
    if (UI.mouseClicked()) {
      if (picked != null) picked = null ;
      else {
        picked = UI.pickedTile() ;
        if (picked != null && picked.owner() instanceof Venue) {
          picked = ((Venue) picked.owner()).entrance() ;
        }
      }
    }
    Tile hovered = UI.pickedTile() ;
    if (hovered != null && hovered.owner() instanceof Venue) {
      hovered = ((Venue) hovered.owner()).entrance() ;
    }
    if (picked != null && hovered != null) {
      final PathingSearch search = new PathingSearch(picked, hovered) ;
      search.doSearch() ;
      final Tile path[] = (Tile[]) search.fullPath(Tile.class) ;
      if (path != null && path.length > 0) {
        final TerrainMesh overlay = world().terrain().createOverlay(
          path, false, Texture.WHITE_TEX
        ) ;
        rendering().addClient(overlay) ;
      }
    }
  }
}


//
//  TODO:  Create internal options for the following.

//
//  This enabled highlighting of paths between different tiles-
/*
    final BaseUI UI = (BaseUI) currentUI() ;
    if (UI.mouseClicked()) {
      if (picked != null) picked = null ;
      else {
        picked = UI.pickedTile() ;
        if (picked != null && picked.owner() instanceof Venue) {
          picked = ((Venue) picked.owner()).entrance() ;
        }
      }
    }
    Tile hovered = UI.pickedTile() ;
    if (hovered != null && hovered.owner() instanceof Venue) {
      hovered = ((Venue) hovered.owner()).entrance() ;
    }
    
    if (picked != null && hovered != null) {
      final PathingSearch search = new PathingSearch(picked, hovered) ;
      search.doSearch() ;
      final Tile path[] = (Tile[]) search.fullPath(Tile.class) ;
      if (path != null && path.length > 0) {
        final TerrainMesh overlay = world().terrain().createOverlay(
          path, Texture.WHITE_TEX
        ) ;
        rendering().addClient(overlay) ;
      }
    }
//*/

//
//  This enabled highlighting of regions associated with particular tiles-
/*
//*/


/*
final Citizen c = new Citizen(Vocation.ARTIFICER) ;
c.enterWorldAt(free.x, free.y, world) ;
//c.assignBehaviour(new Patrolling(c, c, 10)) ;
((BaseUI) HUD).setSelection(c) ;
//*/

/*
Terrain terrain = new Terrain(
  32, 0.5f, 0.75f,  //map size, relative elevation, and amount of land
  7, 6, 2  //insolation, moisture and radiation
) ;
//*/

