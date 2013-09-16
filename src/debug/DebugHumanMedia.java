


package src.debug ;
import src.game.actors.* ;
import src.game.base.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



public class DebugHumanMedia extends ViewLoop {
  
  
  static int numHumans = 16 ;
  static Background vocations[] = Background.ALL_BACKGROUNDS ;
  static String animName = Action.FIRE ;
  
  Batch <Human> generated = new Batch <Human> () ;
  float animProgress = 0 ;
  Composite currentPortrait ;
  
  
  
  protected void setup() {
    final int gridSize = (int) Math.ceil(Math.sqrt(numHumans)) ;
    final float HG = gridSize / 2f ;
    int i = 0 ;
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      final Background v = (Background) Rand.pickFrom(vocations) ;
      final Human human = new Human(v, null) ;
      generated.add(human) ;
      final JointSprite s = (JointSprite) human.sprite() ;
      s.position.set(c.x - HG, c.y - HG, 0) ;
      s.rotation = -45 ;
      if (++i > numHumans) break ;
    }
    rendering.port.cameraZoom *= 2 ;
  }
  
  
  protected void update() {
    rendering.lighting.direct(
      rendering.port.viewInvert(new Vec3D(0, 0, 1))
    ) ;
    Human selected = null ;
    //
    //  Render the generated humanoid sprites-
    for (Human human : generated) {
      final JointSprite s = (JointSprite) human.sprite() ;
      s.toggleGroup("pistol", true) ;
      s.setAnimation(animName, animProgress) ;
      if (rendering.port.mouseIntersects(s.position, 0.5f, HUD)) {
        selected = human ;
      }
      rendering.addClient(s) ;
    }
    animProgress += 0.04f ;
    //
    //  ...You should also be able to preview portraits here.
    if (currentPortrait != null) {
      currentPortrait.detach() ;
      currentPortrait = null ;
    }
    if (selected != null) {
      Selection.renderPlane(
        rendering, selected.sprite().position, 0.5f,
        Colour.WHITE, Selection.SELECT_CIRCLE
      ) ;
      currentPortrait = selected.portrait(HUD) ;
      currentPortrait.absBound.set(10, 10, 100, 100) ;
      currentPortrait.attachTo(HUD) ;
    }
  }
  
  
  public static void main(String a[]) {
    final DebugHumanMedia test = new DebugHumanMedia() ;
    test.run() ;
  }
}





