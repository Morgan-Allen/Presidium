/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.sfx ;
import src.graphics.common.Colour;
import src.graphics.common.Model;
import src.graphics.common.Rendering;
import src.graphics.common.Sprite;
import src.graphics.common.Texture;
import src.util.* ;
import java.io.* ;

//import org.lwjgl.opengl.GL11;



public class PlaneFX extends SFX {
  
  
  /**  Model definitions, fields, constructors, and save/load methods-
    */
  final public static Model PLANE_FX_MODEL = new Model(
    "plane_fx_model", PlaneFX.class
  ) {
    public Sprite makeSprite() { return new PlaneFX() ; }
  } ;
  
  
  Texture image ;
  float radius ;
  
  
  public PlaneFX(Texture image, float radius) {
    this.image = image ;
    this.radius = radius ;
  }
  
  
  public Model model() {
    return PLANE_FX_MODEL ;
  }
  
  
  protected PlaneFX() {
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    LoadService.writeString(out, image.name()) ;
    out.writeFloat(radius) ;
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    final String imgName = LoadService.readString(in) ;
    image = Texture.loadTexture(imgName) ;
    radius = in.readFloat() ;
  }
  
  
  
  /**  Actual rendering-
    */
  public void renderTo(Rendering rendering) {
    final Vec3D p = this.position ;
    final float r = this.radius ;
    verts[0].set(p.x - r, p.y - r, p.z) ;
    verts[1].set(p.x - r, p.y + r, p.z) ;
    verts[2].set(p.x + r, p.y + r, p.z) ;
    verts[3].set(p.x + r, p.y - r, p.z) ;
    if (colour != null) colour.bindColour() ;
    renderTex(verts, image) ;
  }
}










