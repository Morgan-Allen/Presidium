/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.campaign ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.util.* ;


//
//  TODO:  Consider merging this with/extending the Vocation/Background class?

public class System extends Background {
  
  
  
  final Texture image ;
  final Vec2D starCoords ;
  
  final Service goodsMade[], goodsNeeded[] ;
  final public Trait climate ;
  final public int gravity ;
  
  
  
  public System(
    String name, String imgName, float starX, float starY,
    Trait climate, int gravity, Object... args
  ) {
    super(name, null, null, -1, args) ;
    this.image = imgName == null ? null : Texture.loadTexture(imgName) ;
    this.starCoords = new Vec2D(starX, starY) ;
    
    this.climate = climate ;
    this.gravity = gravity ;
    
    final Batch <Service> madeB = new Batch(), needB = new Batch() ;
    Object tag = null ;
    for (Object arg : args) {
      if (arg == MAKES || arg == NEEDS) tag = arg ;
      if (arg instanceof Service) {
        if (tag == MAKES) madeB.add((Service) arg) ;
        if (tag == NEEDS) needB.add((Service) arg) ;
      }
    }
    
    goodsMade   = madeB.toArray(Service.class) ;
    goodsNeeded = needB.toArray(Service.class) ;
  }
}










