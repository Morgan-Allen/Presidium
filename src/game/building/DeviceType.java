

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.graphics.jointed.* ;
import src.util.* ;



public class DeviceType extends Item.Type implements BuildConstants {
  
  
  
  /**  Data fields, property accessors-
    */
  final static Texture
    LASER_TEX = Texture.loadTexture("media/SFX/laser_beam.gif") ;
  
  final public float baseDamage ;
  final public int properties ;
  final Conversion materials ;
  
  final public String groupName ;
  
  
  DeviceType(
    Class baseClass, String name,
    float baseDamage, int properties,
    int basePrice, Conversion conversion,
    String groupName
  ) {
    super(baseClass, DEVICE, name, basePrice) ;
    this.baseDamage = baseDamage ;
    this.properties = properties ;
    this.materials = conversion ;
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
      //  Put in a little 'splash' FX, in the direction of the arc.
    }
    else if (type.hasProperty(RANGED | PHYSICAL)) {
      //  You'll have to create a missile effect, with similar parameters.
    }
    else if (type.hasProperty(RANGED | ENERGY)) {
      final BeamFX beam = new BeamFX(LASER_TEX, 0.05f) ;
      
      uses.position(beam.origin) ;
      final JointSprite sprite = (JointSprite) uses.sprite() ;
      beam.origin.setTo(sprite.attachPoint("fire")) ;
      beam.target.setTo(hitPoint(applied, hits)) ;
      
      beam.position.setTo(beam.origin).add(beam.target).scale(0.5f) ;
      final Vec3D p = beam.position ;
      final Tile centre = world.tileAt(p.x, p.y) ;
      final float size = beam.origin.sub(beam.target, null).length() ;
      
      world.ephemera.addGhost(centre, size, beam, 0.33f) ;
    }
  }
}














