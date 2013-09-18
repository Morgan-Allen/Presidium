

package src.game.building ;
import src.graphics.common.* ;
import java.io.* ;



public class OutfitType extends Service implements BuildConstants {
  
  
  final public float
    defence,
    shieldBonus ;
  final public Conversion materials ;
  final public Texture skin ;
  
  
  //
  //  TODO:  You'll have to supply a skin explicitly.
  public OutfitType(
    Class baseClass, String name, int defence, int basePrice,
    Conversion materials
  ) {
    super(
      baseClass, FORM_OUTFIT, name,
      basePrice + (materials == null ? 0 : materials.rawPriceValue())
    ) ;
    this.defence = defence ;
    this.shieldBonus = defence ;
    this.materials = materials ;
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



