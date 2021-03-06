

package src.user ;
import src.graphics.common.* ;
import src.graphics.widgets.Image;
import src.graphics.widgets.Text.Clickable ;
import src.util.Series;



public class StringDescription implements Description {
  
  
  final StringBuffer buffer = new StringBuffer() ;
  

  public void append(String s, Clickable link, Colour c) {
    if (s != null) buffer.append(s) ;
    else if (link != null) buffer.append(link.fullName()) ;
    else buffer.append("(none)") ;
  }
  
  public String toString() {
    return buffer.toString() ;
  }
  
  public void append(Object o) {
    if (o == null) append("(none)") ;
    else append(o.toString()) ;
  }
  
  
  public void append(Clickable l, Colour c) { append(null, l, c) ; }
  public void append(Clickable l) { append(null, l, null) ; }
  public void append(String s, Clickable l) { append(s, l, null) ; }
  public void append(String s, Colour c) { append(s, null, c) ; }
  public void append(String s) { append(s, null, null) ; }

  public boolean insert(Texture graphic, int maxSize) { return false ; }
  public boolean insert(Image graphic, int maxSize) { return false ; }
  
  
  public void appendList(String s, Object... l) {
    if (l.length == 0) return ;
    append(s) ;
    int i = 0 ; for (Object o : l) {
      append(o) ;
      if (++i < l.length) append(", ") ;
    }
  }
  
  
  public void appendList(String s, Series l) {
    appendList(s, l.toArray()) ;
  }
}
