/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.wild.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



//
//  You also need to try scenarios between multiple actors, some of them
//  hostile, and see how they respond.  Ideally, you don't want actors
//  willingly running into situations that they then run away from.
//
//  Add hunting at the redoubt, and update farming/the vats a bit.  Spontaneous
//  missions, and a clearer factoring out of venue/actor batches in the AI.
//  Squalor maps and pollution FX, integrated with the Ecology class.  Impact
//  health/morale/disease/life-support.
//  
//  The Supply Depot.  That's the next thing you need, so you can perform
//  offworld trade.  And make sure the stock exchange is working.
//  
//  Re-introduce external FX for items at venues.  Those were cool.
/*
Diplomatic conversion.  (Good relations are way too easy/quick at the moment.)

Simplify the user interface, implement Powers, and add a Main Menu.  That's it.

Walls/Roads and Power/Life Support are the next items, but those might require
a bigger game.  Maybe *just* power.  Keep it simple.  Condensors for water, and
from the Vault System.  Share with whole settlement.
//*/



public class DebugBehaviour extends PlayLoop {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugBehaviour test = new DebugBehaviour() ;
    test.runLoop() ;
  }
  
  
  protected DebugBehaviour() {
    super(true) ;
  }
  
  
  public DebugBehaviour(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Setup and updates-
    */
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
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      32, 0.2f,
      Habitat.MEADOW , 0.7f,
      Habitat.BARRENS, 0.3f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    TG.setupMinerals(world, 0, 0, 0) ;
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
    
    I.say(" "+(String.class.isAssignableFrom(Object.class))) ;
    //natureScenario(world, base, HUD) ;
    //baseScenario(world, base, HUD) ;
    //missionScenario(world, base, HUD) ;
    socialScenario(world, base, HUD) ;
  }
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;
    if (((BaseUI) currentUI()).currentTask() == null) {
      //DebugPathing.highlightPlace() ;
      //DebugPathing.highlightPath() ;
    }
  }
  
  

  /**  Testing out interactions between alien creatures or primitive humanoids.
    */
  private void natureScenario(World world, Base base, HUD UI) {
    GameSettings.noFog = true ;
    
    /*
    Actor prey = new Vareen() ;
    prey.health.setupHealth(0.5f, 1, 0) ;
    prey.enterWorldAt(12, 12, world) ;
    ((BaseUI) UI).selection.pushSelection(prey, true) ;
    //*/
    
    Actor hunter = new Micovore() ;
    hunter.health.setupHealth(0.5f, 1, 0) ;
    hunter.enterWorldAt(6, 6, world) ;
    ((BaseUI) UI).selection.pushSelection(hunter, true) ;
    //hunter.AI.assignBehaviour(new Hunting(hunter, prey, Hunting.TYPE_FEEDS)) ;
    
    ///PlayLoop.setGameSpeed(10.0f) ;
    /*
    for (int n = 16 ; n-- > 0 ;) {
      final Actor prey = Rand.yes() ? new Quud() : new Vareen() ;
      final Tile e = Spacing.pickRandomTile(world.tileAt(16, 16), 8, world) ;
      prey.health.setupHealth(Rand.num(), 1, 0) ;
      prey.enterWorldAt(e.x, e.y, world) ;
    }
    
    final Actor hunter = new Micovore() ;
    hunter.health.setupHealth(Rand.num(), 1, 0) ;
    hunter.enterWorldAt(4, 4, world) ;
    ((BaseUI) UI).selection.pushSelection(hunter, true) ;
    //*/
  }
  
  
  
  /**  These are scenarios associated with upkeep, maintenance and
    *  construction of the settlement-
    */
  private void baseScenario(World world, Base base, HUD UI) {
    
    final Foundry foundry = new Foundry(base) ;
    this.establishVenue(foundry, 8, 8, true) ;
    base.intelMap.liftFogAround(foundry, 5) ;
    ((BaseUI) UI).selection.pushSelection(foundry, true) ;
    
    final Actor client = new Human(Vocation.VETERAN, base) ;
    client.gear.incCredits(500) ;
    client.enterWorldAt(4, 4, world) ;
    establishVenue(new Garrison(base), 2, 6, true, client) ;
  }
  
  
  
  /**  Testing out directed behaviour like combat, exploration, security or
    *  contact missions.
    */
  private void missionScenario(World world, Base base, HUD UI) {
    
    final Actor actorA = new Human(Vocation.RUNNER, base) ;
    actorA.enterWorldAt(15, 15, world) ;
    final Actor actorB = new Human(Vocation.VETERAN, base) ;
    actorB.enterWorldAt(15, 3, world) ;
    
    final Actor target = new Quud() ;
    target.health.setupHealth(0.5f, 1, 0) ;
    target.enterWorldAt(5, 5, world) ;
    
    final Base otherBase = new Base(world) ;
    world.registerBase(otherBase, true) ;
    base.setRelation(otherBase, -1) ;
    otherBase.setRelation(base, -1) ;
    
    final Venue garrison = new Garrison(otherBase) ;
    establishVenue(garrison, 8, 8, true) ;
    actorA.AI.assignBehaviour(new Combat(actorA, garrison)) ;
    actorB.AI.assignBehaviour(new Combat(actorA, garrison)) ;
    ((BaseUI) UI).selection.pushSelection(actorA, true) ;
    
    /*
    final Mission mission = new ReconMission(base, world.tileAt(20, 20)) ;
    base.addMission(mission) ;
    ((BaseUI) UI).selection.setSelected(mission) ;
    ((BaseUI) UI).camera.zoomNow(mission.subject()) ;
    //*/
    /*
    final Mission mission = new StrikeMission(base, garrison) ;
    mission.setApplicant(assails, true) ;
    base.addMission(mission) ;
    ((BaseUI) UI).selection.setSelected(mission) ;
    //*/
  }
  
  
  
  /**  Testing out pro-social behaviour like dialogue, recreation and medical
    *  treatment.
    */
  private void socialScenario(final World world, Base base, HUD UI) {
    base.incCredits(1000) ;
    GameSettings.noFog = true ;
    
    final Actor actor = new Human(Vocation.PHYSICIAN, base) ;
    final Actor other = new Human(Vocation.VETERAN, base) ;
    actor.enterWorldAt(4, 4, world) ;
    other.enterWorldAt(6, 6, world) ;
    establishVenue(new Sickbay(base), 9, 2, true, actor) ;
    establishVenue(new Garrison(base), 2, 9, true, other) ;
    establishVenue(new Cantina(base), 9, 9, true) ;
    
    final EcologyGen EG = new EcologyGen() ;
    EG.populateFlora(world) ;
    ///EG.populateFauna(world, Species.VAREEN) ;
  }
  
  
  private Venue establishVenue(
    Venue v, int atX, int atY, boolean intact,
    Actor... employed
  ) {
    v.enterWorldAt(atX, atY, v.base().world) ;
    if (intact) {
      v.structure.setState(VenueStructure.STATE_INTACT, 1.0f) ;
      v.onCompletion() ;
    }
    else {
      v.structure.setState(VenueStructure.STATE_INSTALL, 0.0f) ;
    }
    for (Actor a : employed) a.AI.setEmployer(v) ;
    VenuePersonnel.fillVacancies(v) ;
    v.setAsEstablished(true) ;
    return v ;
  }
}




