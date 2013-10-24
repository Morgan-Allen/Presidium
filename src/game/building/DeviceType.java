

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.graphics.sfx.PlaneFX.Model ;
import src.graphics.jointed.* ;
import src.util.* ;



public class DeviceType extends Service implements Economy {
  
  
  
  /**  Data fields, property accessors-
    */
  final static Texture
    LASER_TEX = Texture.loadTexture("media/SFX/laser_beam.gif") ;
  final static PlaneFX.Model
    SLASH_FX_MODEL = new PlaneFX.Model(
      "slash_fx", DeviceType.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false
    ) ;
  
  
  final public float baseDamage ;
  final public int properties ;
  final Conversion materials ;
  
  final public String groupName ;
  
  
  DeviceType(
    Class baseClass, String name,
    float baseDamage, int properties,
    int basePrice, Conversion materials,
    String groupName
  ) {
    super(
      baseClass, FORM_DEVICE, name,
      basePrice + (materials == null ? 0 : materials.rawPriceValue())
    ) ;
    this.baseDamage = baseDamage ;
    this.properties = properties ;
    this.materials = materials ;
    this.groupName = groupName ;
  }
  
  
  public Conversion materials() {
    return materials ;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p ;
  }
  
  
  
  /**  Rendering and interface-
    */
  static Vec3D hitPoint(Target applied, boolean hits) {
    final Vec3D HP = applied.position(null) ;
    final float r = applied.radius(), h = applied.height() / 2 ;
    HP.z += h ;
    if (hits) return HP ;
    HP.x += Rand.range(-r, r) ;
    HP.y += Rand.range(-r, r) ;
    HP.z += Rand.range(-h, h) ;
    return HP ;
  }
  
  
  public static void applyFX(
    DeviceType type, Mobile uses, Target applied, boolean hits
  ) {
    
    final World world = uses.world() ;
    if (type == null || type.hasProperty(MELEE)) {
      //
      //  Put in a little 'splash' FX, in the direction of the arc.
      final float r = uses.radius() ;
      final Sprite slashFX = SLASH_FX_MODEL.makeSprite() ;
      slashFX.scale = r * 2 ;
      world.ephemera.addGhost(uses, r, slashFX, 0.33f) ;
    }
    else if (type.hasProperty(RANGED | PHYSICAL)) {
      //  You'll have to create a missile effect, with similar parameters.
      //
      //  TODO:  IMPLEMENT THAT
    }
    else if (type.hasProperty(RANGED | ENERGY)) {
      //
      //  Otherwise, create an appropriate 'beam' FX-
      final BeamFX beam = new BeamFX(LASER_TEX, 0.05f) ;
      
      uses.position(beam.origin) ;
      final JointSprite sprite = (JointSprite) uses.sprite() ;
      beam.origin.setTo(sprite.attachPoint("fire")) ;
      beam.target.setTo(hitPoint(applied, hits)) ;
      
      beam.position.setTo(beam.origin).add(beam.target).scale(0.5f) ;
      final float size = beam.origin.sub(beam.target, null).length() / 2 ;
      world.ephemera.addGhost(null, size, beam, 0.33f) ;
    }
  }
}














