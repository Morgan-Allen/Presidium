/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.debug ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.campaign.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class DebugPlanet extends Scenario {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugPlanet test = new DebugPlanet() ;
    PlayLoop.setupAndLoop(test.UI(), test) ;
  }
  
  
  protected DebugPlanet() {
    super("test_planet", true) ;
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
      GameSettings.fogFree = true ;
      PlayLoop.loadGame("saves/test_planet.rep", true) ;
      PlayLoop.setGameSpeed(1.0f) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  
  
  protected World createWorld() {
    GameSettings.fogFree = true ;
    PlayLoop.setGameSpeed(5.0f) ;
    
    /*
    final TerrainGen TG = new TerrainGen(
      64, 0.33f,
      Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    //*/
    //*
    final TerrainGen TG = new TerrainGen(
      128, 0,
      Habitat.OCEAN       , 0.5f,
      Habitat.ESTUARY     , 0.2f,
      Habitat.MEADOW      , 1f,
      Habitat.BARRENS     , 2f,
      Habitat.DESERT      , 3f,
      Habitat.CURSED_EARTH, 2f
    ) ;
    //*/
    /*
    final TerrainGen TG = new TerrainGen(
      32, 0,
      Habitat.OCEAN, 1.0f
    ) ;
    //*/
    final World world = new World(TG.generateTerrain()) ;
    
    //*
    TG.setupMinerals(world, 0, 0, 0) ;
    TG.setupOutcrops(world) ;
    ///TG.presentMineralMap(world, world.terrain()) ;
    
    GameSettings.fogFree = true ;
    GameSettings.hireFree = true ;
    GameSettings.buildFree = true ;
    final EcologyGen EG = new EcologyGen(world, TG) ;
    //EG.populateWithRuins() ;
    EG.populateWithNatives() ;
    
    EG.populateFlora() ;
    //
    //  TODO:  These all seem to be dying of disease...  Look into that.
    //EG.populateFauna(Species.VAREEN, Species.QUUD) ;
    //EG.populateFauna(Species.MICOVORE) ;
    
    return world ;
  }
  
  
  protected void configureScenario(World world, Base base, HUD HUD) {
  }
}




