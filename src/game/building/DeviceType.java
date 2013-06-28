

package src.game.building ;
import src.graphics.common.* ;


public class DeviceType extends Item.Type implements VenueConstants {
  
  
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
    //this.model = SubgroupModel.modelFor(Economy.IMPLEMENTS_FILE, groupName) ;
    this.groupName = groupName ;
    //this.animName = animName ;
    //this.attachJoint = attachJoint ;
  }
  
  public Conversion materials() { return materials ; }
  
  public boolean hasProperty(int p) {
    return (properties & p) == p ;
  }
}







