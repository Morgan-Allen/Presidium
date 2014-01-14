/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.debug ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.campaign.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class DebugPlanet extends Scenario {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    if (Scenario.loadedFrom("test_planet")) return ;
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
  public void updateGameState() {
    super.updateGameState() ;
    PlayLoop.rendering().port.cameraZoom = 1.33f ;
    //PlayLoop.setGameSpeed(0.25f) ;
    //PlayLoop.setGameSpeed(5.0f) ;
    PlayLoop.setGameSpeed(25.0f) ;
  }
  
  
  
  protected World createWorld() {
    GameSettings.fogFree   = true ;
    GameSettings.hireFree  = true ;
    GameSettings.buildFree = true ;
    
    final TerrainGen TG = new TerrainGen(
      128, 0,
      Habitat.OCEAN       , 0.5f,
      Habitat.ESTUARY     , 0.2f,
      Habitat.MEADOW      , 1f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE      , 3f,
      Habitat.CURSED_EARTH, 2f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    
    TG.setupMinerals(world, 0, 0, 0) ;
    TG.setupOutcrops(world) ;
    final EcologyGen EG = new EcologyGen(world, TG) ;
    EG.populateFlora() ;
    EG.populateFauna(Species.VAREEN, Species.QUUD, Species.MICOVORE) ;
    
    return world ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
  }
}




