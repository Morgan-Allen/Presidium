/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.graphics.widgets.Text.Clickable ;
import src.graphics.common.* ;


public interface Description {
  
  
  public void append(String s, Clickable link, Colour c) ;
  public void append(Clickable link, Colour c) ;
  public void append(Clickable link) ;
  public void append(String s, Clickable link) ;
  public void append(String s, Colour c) ;
  public void append(String s) ;
  public void append(Object o) ;
  
  public boolean insert(Texture graphic, int maxSize) ;
}