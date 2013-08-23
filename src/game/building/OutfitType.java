

package src.game.building ;
import src.graphics.common.* ;
import java.io.* ;



public class OutfitType extends Service implements BuildConstants {
  
  
  //final public static String
    //SHIELD_ATTACH_POINT = "root" ;
  
  final public float
    defence,
    shieldBonus ;
  final public Conversion materials ;
  final public Texture skin ;
  
  
  public OutfitType(
    Class baseClass, String name, int defence, int basePrice,
    Conversion conversion
  ) {
    super(baseClass, OUTFIT, name, basePrice) ;
    this.defence = defence ;
    this.shieldBonus = defence ;
    this.materials = conversion ;
    final String imagePath = ITEM_PATH+name+"_skin.gif" ;
    if (new File(imagePath).exists())
      this.skin = Texture.loadTexture(imagePath) ;
    else
      this.skin = null ;
  }
  
  
  public Conversion materials() {
    return materials ;
  }
}



