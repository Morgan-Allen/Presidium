


package src.game.campaign ;
import src.game.common.* ;



public class TutorialScenario extends Scenario {
  
  
  
  public TutorialScenario(String saveFile) {
    super(saveFile) ;
  }
  
  
  public TutorialScenario(World world, Base base, String saveFile) {
    super(world, base, saveFile) ;
  }
  
  
  public TutorialScenario(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  //
  //  TODO:  Guide the player through the basics of setting up a settlement,
  //  using missions to explore, defend, and either attack or contact natives
  //  and other local threats.
  //
  //  TODO:  Take this functionality out of the MainMenu class and into this
  //  class.  (Likewise for the campaign-style scenario.)
  
}






