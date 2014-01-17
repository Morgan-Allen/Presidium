

package src.game.wild ;
import src.game.actors.Actor;
import src.game.actors.Background;
import src.game.actors.Behaviour;
import src.game.actors.Choice;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.widgets.HUD;
import src.user.Composite;
import src.util.* ;


//
//  TODO:  This is used to establish appropriate distance from other native
//  huts.


public class NativeHall extends NativeHut {
  
  
  final List <NativeHut> children = new List <NativeHut> () ;
  private float idealPopEstimate = -1 ;
  
  
  protected NativeHall(int tribeID, Base base) {
    super(4, 2, TYPE_HALL, tribeID, base) ;
  }


  public NativeHall(Session s) throws Exception {
    super(s) ;
    s.loadObjects(children) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(children) ;
  }
  
  
  
  
  /**  Updates and behavioural functions-
    */
  public Background[] careers() {
    return super.careers() ;
    //  Cargo Cultist, Marked One, Medicine Man, Chieftain.
  }
  
  
  public Behaviour jobFor(Actor actor) {
    //  Well, firstly determine if any more huts should be placed or repaired.
    
    return super.jobFor(actor) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    if (numUpdates % 10 == 0) {
      float estimate = Nest.idealNestPop(Species.HUMAN, this, world, false) ;
      if (idealPopEstimate == -1) idealPopEstimate = estimate ;
      else {
        final float inc = 10f / World.STANDARD_DAY_LENGTH ;
        idealPopEstimate *= 1 - inc ;
        idealPopEstimate += estimate * inc ;
      }
    }
    
    final int idealNumHuts = (int) (idealPopEstimate / 2) ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Chief's Hall" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return super.portrait(UI) ;
  }
  
  
  public String helpInfo() {
    return
      "Native settlements will often have a central meeting place where "+
      "the tribe's leadership and elders will gather to make decisions, "+
      "such as arranging marriage, arbitrating dispute or mounting raids." ;
  }
}






