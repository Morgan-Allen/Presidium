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
import src.game.building.Citizen;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;
import src.util.* ;





public class TestGame extends PlayLoop {
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    TestGame test = new TestGame() ;
    test.runLoop() ;
  }
  
  protected TestGame() {
    super(true) ;
  }
  
  public TestGame(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  /**  Setup and initialisation-
    */
  protected World createWorld() {
    Terrain terrain = new Terrain(
      16, 0.5f, 0.75f,  //map size, relative elevation, and amount of land
      7, 6, 5  //insolation, moisture and radiation
    ) ;
    World world = new World(terrain) ;
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
    
    /*
    final Citizen c = new Citizen(Vocation.ARTIFICER) ;
    c.enterWorldAt(free.x, free.y, world) ;
    //c.assignBehaviour(new Patrolling(c, c, 10)) ;
    ((BaseUI) HUD).setSelection(c) ;
    //*/
  }
  
  
  
  /**  Updates and monitoring-
    */
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










