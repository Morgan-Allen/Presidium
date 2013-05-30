

package src.user ;
import src.game.actors.* ;



public class ActorPanel extends InfoPanel {
  
  
  Composite portrait ;
  
  
  public ActorPanel(BaseUI UI, Actor actor) {
    super(UI, actor, 100) ;
    portrait = actor.portrait(UI) ;
    portrait.relBound.set(0, 1, 0, 0) ;
    portrait.absBound.set(10, -90, 80, 80) ;
    portrait.attachTo(this) ;
  }
}
