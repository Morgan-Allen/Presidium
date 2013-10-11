


package src.debug ;
import src.graphics.common.* ;
import src.graphics.space.* ;
import src.util.* ;



public class DebugStarcharts extends ViewLoop {
  
  final static int
    TINY_CLUSTER_COUNT   = 25,
    SMALL_CLUSTER_COUNT  = 40,
    MEDIUM_CLUSTER_COUNT = 100,
    LARGE_CLUSTER_COUNT  = 250,
    VAST_CLUSTER_COUNT   = 450 ;
  
  
  public static void main(String s[]) {
    final DebugStarcharts dS = new DebugStarcharts() ;
    dS.run() ;
  }
  
  
  Starfield starfield ;
  
  
  protected void setup() {
    final int
      NUM_STARS = MEDIUM_CLUSTER_COUNT,
      MAX_DIST = (int) (15 * (float) Math.pow(NUM_STARS / 100f, 0.25f)),
      RIM_PART = 4 ;
    starfield = new Starfield(NUM_STARS, MAX_DIST) ;
    //
    //  Assign a bunch of random stars...
    final Quat q = new Quat() ;
    final Mat3D m = new Mat3D() ;
    final Vec3D v = new Vec3D() ;
    for (int n = NUM_STARS ; n-- > 0 ;) {
      //
      //  I'm using some obscure black magic for uniformly random quaternions
      //  here- reference at:  http://planning.cs.uiuc.edu/node198.html
      final float
        u1 = Rand.num(), u2 = Rand.num(), u3 = Rand.num() ;
      q.set(
        (float) (Math.sqrt(1 - u1) * Math.sin(Math.PI * u2 * 2)),
        (float) (Math.sqrt(1 - u1) * Math.cos(Math.PI * u2 * 2)),
        (float) (Math.sqrt(u1    ) * Math.sin(Math.PI * u3 * 2)),
        (float) (Math.sqrt(u1    ) * Math.cos(Math.PI * u3 * 2))
      ) ;
      q.setUnit() ;
      q.putMatrixForm(m) ;
      final boolean rim = Rand.index(RIM_PART) == 0 ;
      v.set(0, 0, MAX_DIST * (rim ?
        (1 - (Rand.num() / RIM_PART)) :
        (Rand.num() * Rand.num())
      )) ;
      m.trans(v) ;
      starfield.addStar(v.x, v.y, v.z, null, 0.5f + Rand.avgNums(2)) ;
    }
  }
  
  
  protected void update() {
    rendering.port.cameraRotation += Math.PI / 180 ;
    
    starfield.starPort.viewBounds.setTo(rendering.port.viewBounds) ;
    starfield.fieldElevate = rendering.port.cameraElevated ;
    starfield.fieldRotate  = rendering.port.cameraRotation ;
    
    starfield.starPort.cameraElevated = 0 ;
    starfield.starPort.cameraRotation = 0 ;
    starfield.starPort.cameraZoom = 0.5f ;
    
    starfield.starPort.updateView() ;
    starfield.starPort.applyView() ;
    starfield.starPort.setIsoMode() ;
    
    rendering.addClient(starfield) ;
  }
}




