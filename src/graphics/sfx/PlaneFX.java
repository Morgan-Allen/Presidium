/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.sfx ;
import src.graphics.common.* ;
import src.util.* ;
import java.io.* ;
//import org.lwjgl.opengl.GL11 ;



public class PlaneFX extends SFX {
  
  
  /**  Model definitions, fields, constructors, and save/load methods-
    */
  public static class Model extends src.graphics.common.Model {
    
    final Texture image ;
    final float initSize, spin, growth ;
    final boolean tilted ;
    
    
    public Model(
      String modelName, Class modelClass,
      String image, float initSize, float spin, float growth, boolean tilted
    ) {
      super(modelName, modelClass) ;
      this.image = Texture.loadTexture(image) ;
      this.initSize = initSize ;
      this.spin = spin ;
      this.growth = growth ;
      this.tilted = tilted ;
    }
    
    public Sprite makeSprite() { return new PlaneFX(this) ; }
  }
  
  
  final Model model ;
  private float radius ;
  
  
  protected PlaneFX(Model model) {
    this.model = model ;
    this.radius = model.initSize ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  public void update() {
    super.update() ;
    rotation = (rotation + model.spin) % 360 ;
    radius += model.growth ;
  }
  
  
  /**  Actual rendering-
    */
  private static Mat3D trans = new Mat3D() ;
  
  public void renderTo(Rendering rendering) {
    final Vec3D p = this.position ;
    final float r = this.radius * scale ;
    
    trans.setIdentity() ;
    trans.rotateZ((float) (rotation * Math.PI / 180)) ;
    
    
    verts[0].set(0 - r, 0 - r, 0) ;
    verts[1].set(0 - r, 0 + r, 0) ;
    verts[2].set(0 + r, 0 + r, 0) ;
    verts[3].set(0 + r, 0 - r, 0) ;
    for (Vec3D v : verts) {
      trans.trans(v) ;
      if (model.tilted) rendering.port.viewInvert(v) ;
      v.add(p) ;
    }
    
    if (colour != null) colour.bindColour() ;
    renderTex(verts, model.image) ;
  }
}










