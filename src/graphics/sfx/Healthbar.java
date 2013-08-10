

package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.*;
import java.io.* ;
import org.lwjgl.opengl.* ;




public class Healthbar extends Sprite {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static Model
    BAR_MODEL = new Model("health_bar_model", Healthbar.class) {
      public Sprite makeSprite() { return new Healthbar() ; }
    } ;
    
  final static int
    BAR_HEIGHT = 3,
    DEFAULT_WIDTH = 40 ;
  final static Texture BAR_TEX = Texture.loadTexture(
    "media/SFX/laser_beam.gif"
  ) ;
  
  
  public float level = 0.5f ;
  public float size = DEFAULT_WIDTH ;
  public Colour full = Colour.BLUE, empty = Colour.RED ;
  
  
  public Healthbar() {}
  public Model model() { return BAR_MODEL ; }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeFloat(level) ;
    out.writeFloat(size) ;
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    level = in.readFloat() ;
    size = in.readFloat() ;
  }
  
  
  
  /**  Updates and rendering-
    */
  public void update() {
  }
  
  
  public void setAnimation(String animName, float progress) {}
  public int[] GL_disables() { return null ; }
  
  
  public void renderTo(Rendering rendering) {
    //
    //  First, establish screen coordinates for the bottom-left corner.
    final Vec3D base = new Vec3D().setTo(position) ;
    rendering.port.isoToScreen(base) ;
    final int
      x = (int) (base.x - (size / 2)),
      y = (int) (base.y - (BAR_HEIGHT / 2)) ;
    //
    //  Begin actual rendering-
    //GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    rendering.port.setScreenMode() ;
    Texture.WHITE_TEX.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    //
    //  First, do the background-
    final Colour a = full, b = empty ;
    final Colour c = colour == null ? Colour.WHITE : colour ;
    final float s = 1 - level, f = fog ;
    GL11.glColor4f(
      0.5f * f * c.r,
      0.5f * f * c.g,
      0.5f * f * c.b,
      1 * c.a
    ) ;
    UINode.drawQuad(
      x, y, x + (int) size, y + BAR_HEIGHT,
      0, 0, 1, 1, base.z
    ) ;
    //
    //  Then, the filled section-
    final Colour mix = new Colour().set(
      (a.r * level) + (b.r * s),
      (a.g * level) + (b.g * s),
      (a.b * level) + (b.b * s),
      1
    ) ;
    mix.setValue(1) ;
    GL11.glColor4f(
      mix.r * c.r * f,
      mix.g * c.g * f,
      mix.b * c.b * f,
      1 * c.a
    ) ;
    UINode.drawQuad(
      x, y, x + (int) (size * level), y + BAR_HEIGHT,
      0, 0, 1, 1, base.z
    ) ;
    //
    //  And finish off-
    GL11.glEnd() ;
    rendering.port.setIsoMode() ;
    //GL11.glEnable(GL11.GL_DEPTH_TEST) ;
  }
}



