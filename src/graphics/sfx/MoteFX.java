/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;
import org.lwjgl.opengl.* ;


//
//  This class should be a way of representing drifting smoke, 'spell'
//  indicators, and other similarly flat-on and/or randomly-drifting visual FX.


public class MoteFX extends SFX {
  
  
  final static Model MOTE_MODEL = new Model("mote_model", MoteFX.class) {
    public Sprite makeSprite() { return new MoteFX() ; }
  } ;
  
  public ImageSprite mote ;
  public float progress = 0, animTime = -1 ;
  
  
  
  public Model model() { return MOTE_MODEL ; }
  MoteFX() {}
  
  
  public MoteFX(Sprite mote) {
    this.mote = (ImageSprite) mote ;
  }
  
  
  public void update() {
    super.update() ;
    if (animTime != -1) {
      progress += 1f / (25 * animTime) ;
      if (progress > 1) progress = 1 ;
    }
  }
  
  
  //final static int DISABLES[] = { GL11.GL_DEPTH_TEST } ;
  //public int[] GL_disables() { return DISABLES ; }
  
  
  public void renderTo(Rendering rendering) {
    mote.matchTo(this) ;
    mote.setAnimation("animation", progress) ;
    mote.update() ;
    GL11.glDepthMask(false) ;
    GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    mote.renderTo(rendering) ;
    GL11.glEnable(GL11.GL_DEPTH_TEST) ;
    GL11.glDepthMask(true) ;
  }
}






