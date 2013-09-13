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





//Where do people earn money from?  Presumably, the sale of goods and
//services.  You tack something on top of average buying price to determine
//the selling price, and split the difference between the number of workers
//over a given time frame, plus taxes to the state.  That's fair, right?
//Okay.  Cool.


//
//  Implement offworld trade, and vehicle trades.  Plus, add upgrades for the
//  stock exchange.  Make it useful!
//
//  Update farming/the vats/mining a bit (including minimum spacing?)
//  A clearer factoring out of venue/actor batches in the AI.
//  Have pollution effects impact health/life-support, and possibly change the
//  landscape.
//  
//  Re-introduce external FX for items at venues.  Those were cool.
//
//  Actors need to be automatically aware of persons attacking them, should
//  call for help from allies, and need proper line of sight.
/*
Diplomacy and mood.  (Good relations are way too easy/quick at the moment.)

Simplify the user interface, implement Powers, and add a Main Menu.  That's it.

Walls/Roads and Power/Life Support are the next items, but those might require
a bigger game.  Maybe *just* power.  Keep it simple.  Condensors for water, and
from the Vault System.  Share with whole settlement.
//*/



public class DebugBehaviour extends PlayLoop implements BuildConstants {
  
  
  
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
    baseScenario(world, base, HUD) ;
    //missionScenario(world, base, HUD) ;
    //socialScenario(world, base, HUD) ;
  }
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;
    if (((BaseUI) currentUI()).currentTask() == null) {
      //DebugPathing.highlightPlace() ;
      //DebugPathing.highlightPath() ;
    }
  }
  

  protected void updateGameState() {
    super.updateGameState() ;
  }
  
  
  
  /**  Testing out interactions between alien creatures or primitive humanoids.
    */
  private void natureScenario(World world, Base base, HUD UI) {
    GameSettings.noFog = true ;
    PlayLoop.setGameSpeed(5.0f) ;
    
    final EcologyGen EG = new EcologyGen() ;
    EG.populateFlora(world) ;
    
    final Actor hunter = new Micovore() ;
    hunter.health.setupHealth(0.5f, 1, 0) ;
    hunter.enterWorldAt(6, 6, world) ;
    ((BaseUI) UI).selection.pushSelection(hunter, true) ;
    
    for (int n = 16 ; n-- > 0 ;) {
      final Actor prey = Rand.yes() ? new Quud() : new Vareen() ;
      final Tile e = Spacing.pickRandomTile(world.tileAt(16, 16), 8, world) ;
      prey.health.setupHealth(Rand.num(), 1, 0) ;
      prey.enterWorldAt(e.x, e.y, world) ;
    }
  }
  
  
  
  /**  These are scenarios associated with upkeep, maintenance and
    *  construction of the settlement-
    */
  private void baseScenario(World world, Base base, HUD UI) {
    GameSettings.noFog     = true ;
    GameSettings.hireFree  = true ;
    GameSettings.buildFree = true ;
    
    final Actor pilot = new Human(Vocation.SUPPLY_CORPS, base) ;
    pilot.enterWorldAt(2, 2, world) ;
    final Venue DA = establishVenue(new SupplyDepot(base), 4, 4 , true, pilot) ;
    final Venue DB = establishVenue(new SupplyDepot(base), 4, 20, true) ;
    
    //final CargoBarge barge = ((SupplyDepot) DA).cargoBarge() ;
    //barge.pathing.updateTarget(DB) ;
    //((BaseUI) UI).selection.pushSelection(barge, true) ;
    
    final Venue foundry = establishVenue(new Foundry(base), 4, 25, true) ;
    DA.stocks.addItem(Item.withAmount(METALS, 100)) ;

    ///((BaseUI) UI).selection.pushSelection(DA, true) ;
  }
  
  
  
  /**  Testing out directed behaviour like combat, exploration, security or
    *  contact missions.
    */
  private void missionScenario(World world, Base base, HUD UI) {
    
    GameSettings.noFog = true ;
    //GameSettings.hireFree = true ;
    PlayLoop.setGameSpeed(1.0f) ;
    
    final Base otherBase = new Base(world) ;
    world.registerBase(otherBase, true) ;
    base.setRelation(otherBase, -1) ;
    otherBase.setRelation(base, -1) ;
    otherBase.colour = Colour.CYAN ;
    
    //*
    final Batch <Actor> allies = new Batch <Actor> () ;
    float sumPower = 0 ;
    for (int n = 5 ; n-- > 0 ;) {
      final Actor actor = new Human(Vocation.SURVEYOR, base) ;
      actor.setPosition(
        24 + Rand.range(-4, 4),
        24 + Rand.range(-4,  4),
        world
      ) ;
      actor.enterWorld() ;
      allies.add(actor) ;
      sumPower += Combat.combatStrength(actor, null) ;
      ///establishVenue(new SurveyorRedoubt(base), 4, 4, true, actor) ;
    }
    I.say("TOTAL POWER OF ALLIES: "+sumPower) ;
    //*/
    
    //*
    final EcologyGen EG = new EcologyGen() ;
    final Batch <Ruins> ruins = EG.populateRuins(world.tileAt(8, 8), 16) ;
    EG.populateArtilects(ruins, world) ;
    EG.populateFlora(world) ;
    //*/
    
    /*
    final Actor enemy = new Tripod() ;
    //enemy.assignBase(otherBase) ;
    enemy.enterWorldAt(24, 24, world) ;
    ((BaseUI) UI).selection.pushSelection(enemy, true) ;
    
    I.say("POWER OF ENEMY: "+Combat.combatStrength(enemy, null)) ;
    //*/
    //((BaseUI) UI).selection.pushSelection(allies.atIndex(0), true) ;
  }
  
  
  
  /**  Testing out pro-social behaviour like dialogue, recreation and medical
    *  treatment.
    */
  private void socialScenario(final World world, Base base, HUD UI) {
    base.incCredits(1000) ;
    GameSettings.noFog = true ;
    GameSettings.buildFree = true ;
    
    final Actor actor = new Human(Vocation.PHYSICIAN, base) ;
    final Actor other = new Human(Vocation.VETERAN, base) ;
    other.health.takeInjury(other.health.maxHealth()) ;
    ((BaseUI) UI).selection.pushSelection(actor, true) ;
    
    actor.enterWorldAt(4, 4, world) ;
    other.enterWorldAt(6, 6, world) ;
    establishVenue(new Sickbay(base), 9, 2, true, actor) ;
    /*
    establishVenue(new Garrison(base), 2, 9, true, other) ;
    establishVenue(new Cantina(base), 9, 9, true) ;
    //*/
    
    final EcologyGen EG = new EcologyGen() ;
    EG.populateFlora(world) ;
    ///EG.populateFauna(world, Species.VAREEN) ;
  }
  
  
  private Venue establishVenue(
    Venue v, int atX, int atY, boolean intact,
    Actor... employed
  ) {
    v.enterWorldAt(atX, atY, PlayLoop.world()) ;
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





/*
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
//*/

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


