/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.cutout ;
import src.graphics.common.* ;
import src.util.* ;
import java.util.Iterator ;
import java.io.* ;



public class GroupSprite extends Sprite {
  
  
  final static Model GROUP_MODEL = new Model("GROUP-MODEL", GroupSprite.class) {
    public Sprite makeSprite() { return new GroupSprite() ; }
    public Colour averageHue() { return Colour.GREY ; }
  } ;
  
  protected Stack <Sprite> modules = new Stack <Sprite> () ;
  protected Stack <Vec3D>  offsets = new Stack <Vec3D>  () ;
  
  
  /**  Basic constructor and save/load functionality.
    */
  public GroupSprite() {}
  
  public Model model() { return GROUP_MODEL ; }
  
  public Colour averageHue() {
    return modules.getFirst().averageHue() ;
  }
  
  
  /**  Creates a new sprite and attaches it to this group-sprite using the
    *  given offset.
    */
  public void attach(Sprite sprite, float xoff, float yoff, float zoff) {
    for (Sprite h : modules) if (h == sprite) return ;
    modules.addLast(sprite) ;
    offsets.addLast(new Vec3D(xoff, yoff, zoff)) ;
  }
  
  public void attach(Model model, float xoff, float yoff, float zoff) {
    attach(model.makeSprite(), xoff, yoff, zoff) ;
  }
  
  public int indexOf(Sprite sprite) {
    if (sprite == null) return -1 ;
    int i = 0 ;
    for (Sprite h : modules) {
      if (h == sprite) return i ; else i++ ;
    }
    return -1 ;
  }
  
  public Sprite atIndex(int n) {
    if (n == -1) return null ;
    return modules.atIndex(n) ;
  }
  
  public void clearAllAttachments() {
    modules.clear() ;
    offsets.clear() ;
  }
  
  
  
  /**  Regular updates and rendering-
    */
  public int[] GL_disables() {
    return null ;
  }
  
  
  public void setAnimation(String animName, float progress) {
    for (Sprite module : modules) module.setAnimation(animName, progress) ;
  }
  
  
  public void renderTo(Rendering r) {
    final Vec3D pos = position ;
    final Iterator <Vec3D> offs = offsets.iterator() ;
    for (Sprite module : modules) {
      final Vec3D off = offs.next() ;
      module.colour = colour ;
      module.scale = scale ;
      module.fog = fog ;
      module.position.setTo(off.scale(scale)).add(pos) ;
      module.renderTo(r) ;
    }
  }
}



