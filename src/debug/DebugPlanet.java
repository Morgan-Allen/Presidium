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
    //if (Scenario.loadedFrom("test_planet")) return ;
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
    //PlayLoop.setGameSpeed(0.25f) ;
    //PlayLoop.setGameSpeed(5.0f) ;
    //PlayLoop.setGameSpeed(25.0f) ;
    //  100 x 25 = 2500.  You're within an order of magnitude, certainly.
  }
  
  
  
  protected World createWorld() {
    GameSettings.fogFree   = true ;
    GameSettings.hireFree  = true ;
    GameSettings.buildFree = true ;
    
    final TerrainGen TG = new TerrainGen(
      64, 0.33f,
      //Habitat.OCEAN  , 0.33f,
      Habitat.ESTUARY, 0.25f,
      Habitat.MEADOW , 0.5f,
      Habitat.BARRENS, 0.3f,
      Habitat.DESERT , 0.2f
    ) ;
    /*
    final TerrainGen TG = new TerrainGen(
      64, 0,
      Habitat.OCEAN       , 0.5f,
      Habitat.ESTUARY     , 0.2f,
      Habitat.MEADOW      , 1f,
      Habitat.BARRENS     , 2f,
      Habitat.DESERT      , 3f,
      Habitat.CURSED_EARTH, 2f
    ) ;
    //*/
    final World world = new World(TG.generateTerrain()) ;
    
    TG.setupMinerals(world, 0, 0, 0) ;
    //TG.setupOutcrops(world) ;
    //TG.presentMineralMap(world, world.terrain()) ;
    final EcologyGen EG = new EcologyGen(world, TG) ;
    EG.populateFlora() ;
    EG.populateFauna(Species.VAREEN, Species.QUUD, Species.MICOVORE) ;
    
    return world ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    
    /*
    I.say("Adding nest...") ;
    final Nest nest = Species.QUUD.createNest() ;
    nest.structure.setState(Structure.STATE_INTACT, 0.05f) ;
    nest.enterWorldAt(5, 5, world) ;
    //*/
    
    /*
    Fauna hunter = Species.VAREEN.newSpecimen() ;
    hunter.enterWorldAt(10, 10, world) ;
    hunter.health.setupHealth(0.5f, 1, 0) ;
    hunter.health.loseSustenance(hunter.health.energyLevel() * 0.8f) ;
    
    Fauna prey = Species.QUUD.newSpecimen() ;
    prey.health.setupHealth(0.5f, 1, 0) ;
    prey.enterWorldAt(15, 15, world) ;
    prey.health.takeInjury(prey.health.maxHealth() + 1) ;
    prey.health.setState(ActorHealth.STATE_DYING) ;
    
    //final Hunting hunt = Hunting.asFeeding(hunter, prey) ;
    //hunt.priorityMod = Plan.PARAMOUNT ;
    //hunter.mind.assignBehaviour(hunt) ;
    UI.selection.pushSelection(hunter, true) ;
    //*/
    
    /*
    Tile middle = world.tileAt(world.size / 2, world.size / 2) ;
    for (int i = 10 ; i-- > 0 ;) {
      final Fauna f = i == 0 ? Species.MICOVORE.newSpecimen() :
        Rand.yes() ?
        Species.VAREEN.newSpecimen() :
        Species.QUUD.newSpecimen() ;
        I.say("I is: "+i+", adding: "+f) ;
      f.health.setupHealth(0.5f, 1, 0) ;
      Tile free = Spacing.pickRandomTile(middle, world.size, world) ;
      free = Spacing.nearestOpenTile(free, free) ;
      if (free == null) continue ;
      f.enterWorldAt(free, world) ;
    }
    //*/
  }
}




