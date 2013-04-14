/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.awt.* ;
import java.awt.image.* ;
import javax.swing.* ;



/**  This class is used to provide shorthand versions of various print output
  *  functions.
  *  (The name is intended to be as terse as possible.)
  *  TODO:  You need to have a logging system that allows various classes to
  *         be toggled on and off for reports.
  */
public class I {
  
  private static boolean mute = false ;
  
  
  public static final void add(String s) {
    if (! mute) {
      System.out.print(s) ;
      //if (GameLoop.currentUI() != null)
        //PlayerUI.pushMessage(s) ;
    }
  }
  
  public static final void say(String s) {
    if (! mute) {
      System.out.print("\n") ;
      System.out.print(s) ;
      //if (GameLoop.currentUI() != null)
        //GameLoop.currentUI().pushMessage(s) ;
    }
  }
  
  public static final void complain(String e) {
    say(e) ;
    throw new RuntimeException(e) ;
  }
  
  public static void report(Exception e) {
    if (! mute) {
      System.out.println("\nERROR:  "+e.getMessage()) ;
      e.printStackTrace() ;
    }
  }
  
  public static void amMute(boolean m) { mute = m ; }
  
  
  

  
  /*
  public static boolean amHeld(Object o) {
    final PlayerUI UI = GameLoop.currentUI() ;
    return UI.playerSelection() == o || UI.playerHovered() == o ;
  }

  
  public static void append(String prefix, Series s, Description d) {
    if (s == null) return ;
    append(prefix, s.toArray(), d) ;
  }
  
  public static void append(String prefix, Object[] s, Description d) {
    if (s.length == 0) return ;
    d.append(prefix) ;
    for (int i = 0 ; i < s.length ;) {
      d.append(s[i++]) ;
      if (i < s.length) d.append(", ") ;
    }
  }
  //*/
  
  
  
  
  
  
  private final static int
    MODE_GREY = 0 ;
  
  private static class Presentation extends JFrame {
    
    private Object data ;
    private int mode ;
    
    
    Presentation(String name, Object data, int mode) {
      super(name) ;
      this.data = data ;
      this.mode = mode ;
    }
    
    public void paint(Graphics g) {
      super.paint(g) ;
      final byte scale[] = new byte[256] ;
      for (int s = 256 ; s-- > 0 ;) {
        scale[s] = (byte) s ;
      }
      if (mode == MODE_GREY) {
        float vals[][] = (float[][]) data ;
        final int w = vals.length, h = vals[0].length ;
        BufferedImage image = new BufferedImage(
          w, h, BufferedImage.TYPE_BYTE_GRAY
        ) ;
        final byte byteData[] = new byte[w * h] ;
        for (int y = h ; y-- > 0 ;) for (int x = 0 ; x < w ; x++) {
          final int grey = (int) (vals[x][y] * 255) ;
          byteData[((h - (y + 1)) * w) + x] = scale[grey] ;
        }
        image.getRaster().setDataElements(0, 0, w, h, byteData) ;
        Container pane = this.getContentPane() ;
        g.drawImage(
          image,
          0, this.getHeight() - pane.getHeight(),
          pane.getWidth(), pane.getHeight(),
          null
        ) ;
      }
    }
  }
  
  
  private static Table <String, Presentation> windows = new Table() ;
  
  
  public static void present(
    float greyVals[][],
    String name, int w, int h
  ) {
    Presentation window = windows.get(name) ;
    if (window == null) {
      window = new Presentation(name, greyVals, MODE_GREY) ;
      window.getContentPane().setPreferredSize(new Dimension(w, h)) ;
      window.pack() ;
      window.setVisible(true) ;
      windows.put(name, window) ;
    }
    else {
      window.data = greyVals ;
      window.getContentPane().setPreferredSize(new Dimension(w, h)) ;
      window.pack() ;
      window.repaint() ;
    }
  }
}




















