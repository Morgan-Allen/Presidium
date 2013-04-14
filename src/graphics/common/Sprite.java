/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.common ;
import src.util.* ;



public abstract class Sprite implements Rendering.Client {
  
  
  final public Vec3D position = new Vec3D() ;
  public float scale = 1, rotation = 0 ;
  public float depth ;
  public Colour colour = null ;
  
  public abstract Model model() ;
  public abstract void setAnimation(String animName, float progress) ;
  
  
  protected Model.AnimRange rangeFor(String animName) {
    for (Model.AnimRange range : this.model().animRanges()) {
      if (range.name == animName) {
        return range ;
      }
    }
    return null ;
  }
}
