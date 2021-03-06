/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.graphics.sfx ;
import src.graphics.common.* ;
import src.util.* ;
import java.io.* ;
import org.lwjgl.opengl.GL11 ;


//
//  TODO:  Integrate with PlaneFX?


public class ShieldFX extends SFX {
  
  
  /**  Fields, constants, setup and save/load methods-
    */
  final public static Model
    SHIELD_MODEL = new Model("shield_fx_model", ShieldFX.class) {
      public Sprite makeSprite() { return new ShieldFX() ; }
      public Colour averageHue() { return Colour.NONE ; }
    } ;
  
  final public static Texture
    SHIELD_BURST_TEX = Texture.loadTexture("media/SFX/shield_burst.png"),
    SHIELD_HALO_TEX  = Texture.loadTexture("media/SFX/shield_halo.png" ) ;
  final public static float
    BURST_FADE_INC  = 0.033f,
    MIN_ALPHA_GLOW  = 0.00f,
    MAX_BURST_ALPHA = 0.99f ;
  
  
  class Burst { float angle, timer ; }
  //public float radius = 1.0f ;
  private Stack <Burst> bursts = new Stack <Burst> () ;
  private float glowAlpha = 0.0f ;
  private static Mat3D rotMat = new Mat3D() ;
  
  
  
  public Model model() { return SHIELD_MODEL ; }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeFloat(glowAlpha) ;
    out.writeInt(bursts.size()) ;
    for (Burst b : bursts) {
      out.writeFloat(b.angle) ;
      out.writeFloat(b.timer) ;
    }
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    glowAlpha = in.readFloat() ;
    for (int i = in.readInt() ; i-- > 0 ;) {
      final Burst b = new Burst() ;
      b.angle = in.readFloat() ;
      b.timer = in.readFloat() ;
      bursts.add(b) ;
    }
  }
  
  
  
  /**  Specialty methods for use by external clients-
    */
  public void attachBurstFromPoint(Vec3D point, boolean intense) {
    final Burst burst = new Burst() ;
    burst.angle = 270 - new Vec2D(
      position.x - point.x,
      position.y - point.y
    ).toAngle() ;
    //I.say("Angle is: "+burst.angle) ;
    burst.timer = intense ? 1 : 2 ;
    bursts.add(burst) ;
    glowAlpha = 1 ;
  }
  
  
  public Vec3D interceptPoint(Vec3D origin) {
    final Vec3D offset = new Vec3D().setTo(position).sub(origin) ;
    final float newLength = offset.length() - scale ;
    offset.scale(newLength / offset.length()) ;
    offset.add(origin) ;
    return offset ;
  }
  
  
  public void update() {
    super.update() ;
    glowAlpha -= BURST_FADE_INC ;
    if (glowAlpha < MIN_ALPHA_GLOW) glowAlpha = MIN_ALPHA_GLOW ;
    for (Burst burst : bursts) {
      burst.timer -= BURST_FADE_INC ;
      if (burst.timer <= 0) bursts.remove(burst) ;
    }
  }
  
  
  public boolean visible() {
    return glowAlpha > MIN_ALPHA_GLOW ;
  }
  
  
  
  /**  Actual rendering-
    */
  public void renderTo(Rendering rendering) {
    //
    //  First, establish coordinates for the halo corners-
    final Vec3D off = new Vec3D(1.0f, 1.0f, 0).scale(scale) ;
    rendering.port.viewInvert(off) ;
    verts[0].setTo(position).add(off) ;
    verts[2].setTo(position).sub(off) ;
    off.set(1.0f, -1.0f, 0).scale(scale) ;
    rendering.port.viewInvert(off) ;
    verts[1].setTo(position).add(off) ;
    verts[3].setTo(position).sub(off) ;
    //
    //  Render the halo itself-
    GL11.glColor4f(1, 1, 1, glowAlpha) ;
    renderTex(verts, SHIELD_HALO_TEX) ;
    GL11.glColor4f(1, 1, 1, 1) ;
    //
    //  Then render each burst-
    for (Burst burst : bursts) renderBurst(burst) ;
  }
  
  
  private void renderBurst(Burst burst) {
    //I.say("Rendering burst from angle: "+burst.angle) ;
    final float s = burst.timer ;
    verts[0].set(-s, 2, -s) ;
    verts[1].set( s, 2, -s) ;
    verts[2].set( s, 2,  s) ;
    verts[3].set(-s, 2,  s) ;
    rotMat.setIdentity().rotateZ((float) Math.toRadians(burst.angle)) ;
    for (Vec3D v : verts) {
      rotMat.trans(v) ;
      v.scale(scale / 2f) ;
      v.add(position) ;
    }
    GL11.glColor4f(1, 1, 1, Math.min(1, burst.timer * MAX_BURST_ALPHA)) ;
    renderTex(verts, SHIELD_BURST_TEX) ;
    GL11.glColor4f(1, 1, 1, 1) ;
  }
}






