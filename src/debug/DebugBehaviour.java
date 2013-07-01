/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.Hunting;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



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
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
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
    natureScenario(world, base, HUD) ;
    //
    //  Create two actors and have them talk to eachother...
    //socialScenario(world, base, HUD) ;
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
  
  
  
  /**  Various scenarios to execute:
    */
  private void natureScenario(World world, Base base, HUD HUD) {
    final Actor
      hunter = new Micovore(),
      prey = new Quud() ;
    
    hunter.health.setupHealth(Rand.num(), 1, 0) ;
    hunter.enterWorldAt(5, 5, world) ;
    prey.health.setupHealth(Rand.num(), 1, 0) ;
    prey.enterWorldAt(8, 8, world) ;
    
    hunter.psyche.assignBehaviour(
      new Hunting(hunter, prey, Hunting.TYPE_FEEDS)
    ) ;
    hunter.assignAction(null) ;
    ((BaseUI) HUD).setSelection(hunter) ;
  }
  
  
  private void socialScenario(World world, Base base, HUD HUD) {
    final Actor
      actor = new Human(Vocation.PHYSICIAN, base),
      other = new Human(Vocation.MILITANT , base) ;
    actor.enterWorldAt(5, 5, world) ;
    other.enterWorldAt(8, 8, world) ;
    ((BaseUI) HUD).setSelection(actor) ;
  }
}












