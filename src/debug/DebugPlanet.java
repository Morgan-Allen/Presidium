/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.debug ;
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
    PlayLoop.runLoop(test) ;
  }
  
  
  protected DebugPlanet() {
    super() ;
  }
  
  
  public DebugPlanet(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Setup and updates-
    */
  protected boolean loadedAtStartup() {
    try {
      GameSettings.noFog = true ;
      PlayLoop.loadGame("saves/test_planet.rep") ;
      PlayLoop.setGameSpeed(1.0f) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  
  
  protected World createWorld() {
    GameSettings.noFog = true ;
    PlayLoop.setGameSpeed(5.0f) ;
    
    final TerrainGen TG = new TerrainGen(
      64, 0.33f,
      Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    
    //*
    TG.setupMinerals(world, 0, 0, 0) ;
    TG.setupOutcrops(world) ;
    ///TG.presentMineralMap(world, world.terrain()) ;
    
    GameSettings.noFog = true ;
    final EcologyGen EG = new EcologyGen() ;
    EG.populateFlora(world) ;
    
    EG.populateFauna(world, Species.VAREEN, Species.QUUD) ;
    EG.populateFauna(world, Species.MICOVORE) ;
    //*/
    
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
  
  
  protected void updateGameState() {
    super.updateGameState() ;
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
      I.say("\nSAVING GAME...") ;
      PlayLoop.saveGame("saves/test_planet.rep") ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("\nLOADING GAME...") ;
      //GameSettings.frozen = true ;
      PlayLoop.loadGame("saves/test_planet.rep") ;
      return true ;
    }
    return false ;
  }
}




