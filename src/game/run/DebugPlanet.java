/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.run ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class DebugPlanet extends PlayLoop {
  

  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugPlanet test = new DebugPlanet() ;
    test.runLoop() ;
  }
  
  
  protected DebugPlanet() {
    super(true) ;
  }
  
  
  public DebugPlanet(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Setup and updates-
    */
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.33f,
      Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    TG.setupMinerals(world, 0, 0, 0) ;
    TG.setupOutcrops(world) ;
    TG.presentMineralMap(world, world.terrain()) ;
    
    final EcologyGen EG = new EcologyGen(
      Species.QUUD,
      Species.VAREEN
    ) ;
    EG.populateFlora(world) ;
    EG.populateFauna(world) ;
    
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
  }
  
  
  protected boolean shouldExitLoop() {
    if (KeyInput.wasKeyPressed('r')) {
      resetGame() ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('f')) {
      GameSettings.frozen = ! GameSettings.frozen ;
    }
    if (KeyInput.wasKeyPressed('s')) {
      I.say("SAVING GAME...") ;
      PlayLoop.saveGame("saves/test_session.rep") ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      //GameSettings.frozen = true ;
      PlayLoop.loadGame("saves/test_session.rep") ;
      return true ;
    }
    return false ;
  }
}




/*
public static void main(String args[]) {
  DebugPlanet test = new DebugPlanet() ;
  test.run() ;
}

private Terrain terrain ;
private List <ImageSprite> sprites ;


public void setup() {
  final int size = 64 ;
  final TerrainGen TG = new TerrainGen(
    64, 0.33f,
    Habitat.OCEAN  , 0.33f,
    Habitat.ESTUARY, 0.25f,
    Habitat.MEADOW , 0.5f,
    Habitat.BARRENS, 0.3f,
    Habitat.DESERT , 0.2f
  ) ;
  
  terrain = TG.generateTerrain() ;
  terrain.initPatchGrid(16) ;
  rendering.port.cameraPosition.set(size / 2, size / 2, 0) ;
  
  sprites = new List <ImageSprite> () {
    protected float queuePriority(ImageSprite r) {
      return r.depth ;
    }
  } ;
  
  //rendering.lighting.direct(new Vec3D(-1, -1, 1)) ;
  /*
  for (Coord c : Visit.grid(0, 0, size, size, 1)) {
    final Habitat h = terrain.habitatAt(c.x, c.y) ;
    if (h.floraModels == null) continue ;
    final float height = terrain.trueHeight(c.x, c.y) ;
    final float growChance = h.moisture / 10 ;
    final ImageModel model = h.floraModels[Rand.index(4)][Rand.index(4)] ;
    if (Rand.num() < (growChance * growChance)) {
      final ImageSprite sprite = new ImageSprite(model) ;
      sprite.position.set(c.x, c.y, height) ;
      sprites.add(sprite) ;
    }
  }
  //*/
/*
}



protected void update() {
  final int size = terrain.mapSize ;
  final float time = System.currentTimeMillis() / 1000f ;
  terrain.renderFor(new Box2D().set(0, 0, size, size), rendering, time) ;
  Vec3D deep = new Vec3D() ;
  for (Sprite sprite : sprites) {
    Viewport.DEFAULT_VIEW.viewMatrix(deep.setTo(sprite.position)) ;
    sprite.depth = 0 - deep.z ;
  }
  sprites.queueSort() ;
  //rendering.clearDepth() ;
  for (ImageSprite sprite : sprites) {
    rendering.addClient(sprite) ;
  }
}
//*/







