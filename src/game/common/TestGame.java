/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import src.game.planet.* ;
import src.game.tactical.Patrolling ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.Citizen ;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.terrain.* ;
import src.user.* ;
import src.util.* ;
import src.game.common.PathingCache.Region ;





public class TestGame extends PlayLoop {
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    TestGame test = new TestGame() ;
    test.runLoop() ;
  }
  

  Citizen citizen ;
  Action lastAction = null ;
  
  protected TestGame() {
    super(true) ;
  }
  
  public TestGame(Session s) throws Exception {
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
    final TerrainGen TG = new TerrainGen(32,
      Habitat.OCEAN, Habitat.ESTUARY,
      Habitat.MEADOW, Habitat.BARRENS
      //Habitat.DESERT, Habitat.MESA
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
    /*
    citizen = new Citizen(Vocation.ARTIFICER, base) ;
    citizen.enterWorldAt(free.x, free.y, world) ;
    ((BaseUI) HUD).setSelection(citizen) ;
    //*/
  }
  
  
  
  /**  Updates and monitoring-
    */
  protected void updateGameState() {
    super.updateGameState() ;
    //if (citizen.currentAction() != lastAction) assignRandomTarget(citizen) ;
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
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;

    final Tile t = ((BaseUI) currentUI()).pickedTile() ;
    if (t == null) return ;
    final Region r = played().pathingCache.tileRegions[t.x][t.y] ;
    if (r == null) return ;
    //I.say("Generating preview...") ;
    final TerrainMesh.Mask previewMask = new TerrainMesh.Mask() {
      protected boolean maskAt(int x, int y) {
        return played().pathingCache.tileRegions[x][y] == r ;
      }
    } ;
    final WorldSections.Section s = r.section ;
    final int SR = World.SECTION_RESOLUTION ;
    final int size = world().size ;
    final TerrainMesh previewMesh = TerrainMesh.genMesh(
      //s.x * SR, s.y * SR, (s.x * SR) + SR, (s.y * SR) + SR,
      0, 0, size, size,
      Texture.WHITE_TEX, new byte[size + 1][size + 1], previewMask
    ) ;
    //previewMesh.colour = Colour.GREEN ;
    rendering().addClient(previewMesh) ;
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
}




//
//  This enabled highlighting of regions associated with particular tiles-
//  TODO:  Create a separate debugging class for this purpose?
/*
final Tile t = ((BaseUI) currentUI()).pickedTile() ;
if (t == null) return ;
final Region r = played().pathingCache.tileRegions[t.x][t.y] ;
if (r == null) return ;
//I.say("Generating preview...") ;
final TerrainMesh.Mask previewMask = new TerrainMesh.Mask() {
  protected boolean maskAt(int x, int y) {
    return played().pathingCache.tileRegions[x][y] == r ;
  }
} ;
final WorldSections.Section s = r.section ;
final int SR = World.SECTION_RESOLUTION ;
final int size = world().size ;
final TerrainMesh previewMesh = TerrainMesh.genMesh(
  //s.x * SR, s.y * SR, (s.x * SR) + SR, (s.y * SR) + SR,
  0, 0, size, size,
  Texture.WHITE_TEX, new byte[size + 1][size + 1], previewMask
) ;
//previewMesh.colour = Colour.GREEN ;
rendering().addClient(previewMesh) ;
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

