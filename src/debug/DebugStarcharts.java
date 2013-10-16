/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.graphics.common.* ;
import src.graphics.space.* ;
import src.util.* ;



public class DebugStarcharts extends ViewLoop {
  
  final static int
    DEFAULT_LIGHT_YEARS = 15,
    DEFAULT_RIM_PORTION = 4,
    
    TINY_CLUSTER_COUNT   = 25,
    SMALL_CLUSTER_COUNT  = 45,
    MEDIUM_CLUSTER_COUNT = 100,
    LARGE_CLUSTER_COUNT  = 250,
    VAST_CLUSTER_COUNT   = 600 ;
  final static float
    LIGHT_YEAR_SCALE = 1.0f,
    STAR_MAG_SCALE   = 1.0f ;
  
  
  public static void main(String s[]) {
    final DebugStarcharts dS = new DebugStarcharts() ;
    dS.run() ;
  }
  
  
  Starfield starfield ;
  
  
  protected void setup() {
    final int
      NUM_STARS   = LARGE_CLUSTER_COUNT,
      RIM_PORTION = DEFAULT_RIM_PORTION,
      MAX_DIST = (int) (
          DEFAULT_LIGHT_YEARS * LIGHT_YEAR_SCALE *
          (float) Math.pow(NUM_STARS / 100f, 1f / RIM_PORTION)
      ) ;
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
      final boolean rim = Rand.index(RIM_PORTION) == 0 ;
      v.set(0, 0, MAX_DIST * (rim ?
        (1 - (Rand.num() / RIM_PORTION)) :
        (Rand.num() * Rand.num())
      )) ;
      m.trans(v) ;
      //final float magnitude = 0.5f + Rand.avgNums(2) ;
      starfield.addStar(
        v.x, v.y, v.z,
        Rand.num() * Rand.num(), Rand.num() * Rand.num()
      ) ;
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




