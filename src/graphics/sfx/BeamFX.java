


package src.graphics.sfx ;
import src.graphics.common.* ;
import java.io.* ;
import src.util.* ;
import org.lwjgl.opengl.GL11 ;




/**  Renders a particle beam between two chosen points-
  */
public class BeamFX extends SFX {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final public static Model BEAM_MODEL = new Model("beam_model", BeamFX.class) {
    public Sprite makeSprite() { return new BeamFX() ; }
  } ;
  
  
  private Texture tex ;
  private float width ;
  //private float glowAlpha = 1.0f ;
  
  final public Vec3D
    origin = new Vec3D(),
    target = new Vec3D() ;
  private static Vec3D perp = new Vec3D() ;
  
  
  public BeamFX(Texture tex, float width) {
    this.tex = tex ;
    this.width = width ;
  }
  
  
  private BeamFX() {
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    Texture.saveTexture(tex, out) ;
    out.writeFloat(width) ;
    origin.saveTo(out) ;
    target.saveTo(out) ;
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    tex = Texture.loadTexture(in) ;
    width = in.readFloat() ;
    origin.loadFrom(in) ;
    target.loadFrom(in) ;
  }
  
  
  public Model model() {
    return BEAM_MODEL ;
  }
  
  
  
  /**  Updates and modifications-
    */
  public void update() {
    super.update() ;
    final Vec2D line = new Vec2D() ;
    line.x = target.x - origin.x ;
    line.y = target.y - origin.y ;
    //this.radius = line.length() * 0.5f ;
    this.position.setTo(origin).add(target).scale(0.5f) ;
    //glowAlpha -= 0.33f ;
    //if (glowAlpha < 0) glowAlpha = 0 ;
  }
  
  
  
  public void refreshBurst(Vec3D targPos, ShieldFX shield) {
    if (shield == null) target.setTo(targPos) ;
    else target.setTo(shield.interceptPoint(origin)) ;
    update() ;
    //glowAlpha = 1.0f ;
  }
  
  
  
  public void renderTo(Rendering rendering) {
    //
    //  First, we need to determine what the 'perp' angle should be (as in,
    //  perpendicular to the line of the beam, as perceived by the viewer.)
    perp.setTo(target).sub(origin) ;
    rendering.port.viewMatrix(perp) ;
    perp.set(perp.y, -perp.x, 0) ;
    rendering.port.viewInvert(perp) ;
    perp.normalise().scale(width) ;
    //
    //  Now render the beam itself-
    verts[0].setTo(origin).add(perp) ;
    verts[1].setTo(target).add(perp) ;
    verts[2].setTo(target).sub(perp) ;
    verts[3].setTo(origin).sub(perp) ;
    final Colour c = this.colour ;
    final float f = this.fog ;
    GL11.glColor4f(c.r * f, c.g * f, c.b * f, c.a) ;
    renderTex(verts, tex) ;
    GL11.glColor4f(1, 1, 1, 1) ;
  }
}




