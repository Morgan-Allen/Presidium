/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class Vehicle extends Mobile implements Boardable {
  
  
  List <Mobile> inside = new List <Mobile> () ;
  DropPoint dropPoint ;
  
  
  
  
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
    }
  }
  
  public List <Mobile> inside() {
    return inside ;
  }

  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[1] ;
    else for (int i = batch.length ; i-- > 0 ;) batch[i] = null ;
    batch[0] = dropPoint ;
    return batch ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Vec3D p = position ;
    final float r = radius() ;
    put.set(p.x - (r / 2), p.y - (r / 2), r, r) ;
    return put ;
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return null ;
  }
  
  public Texture portrait() {
    return null ;
  }
  
  public String helpInfo() {
    return null ;
  }
  
  public String[] infoCategories() {
    return null ;
  }
  
  public void writeInformation(Description description, int categoryID) {
  }

  public void whenClicked() {
  }
  
}










