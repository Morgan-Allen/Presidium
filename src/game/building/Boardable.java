/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.util.* ;



public interface Boardable extends Target {
  
  
  void setInside(Mobile m, boolean is) ;
  Series <Mobile> inside() ;
  
  Box2D area(Box2D put) ;
  Boardable[] canBoard(Boardable batch[]) ;
  
  int pathType() ;
}