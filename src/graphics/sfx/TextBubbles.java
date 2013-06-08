

package src.graphics.sfx ;
import src.graphics.common.Texture ;
import src.graphics.widgets.* ;
import src.util.* ;



public class TextBubbles extends UIGroup {
  
  
  final static Texture
    BUBBLE_TEX = Texture.loadTexture("media/GUI/textBubble.png") ;
  
  int maxWidth = 160, lineHeight = 40 ;
  
  Vec2D anchorPoint ;
  Stack <Bubble> bubbles ;
  
  
  public TextBubbles(HUD myHUD) {
    super(myHUD) ;
  }
  
  
  static class Bubble extends Bordering {
    
    final String phrase ;
    Text text ;

    public Bubble(HUD myHUD, String phrase) {
      super(myHUD, BUBBLE_TEX) ;
      this.phrase = phrase ;
    }
  }
  
  
  public void setPhrases(String... phrases) {
    for (Bubble b : bubbles) b.detach() ;
    bubbles.clear() ;
    //
    //  TODO:  Apply an alpha effect to fade out older lines.
    int down = 0 ; for (String p : phrases) {
      final Bubble b = new Bubble(myHUD, p) ;
      
      b.text.absBound.set(0, 0, maxWidth, lineHeight) ;
      b.text.setText(p) ;
      b.text.setToPreferredSize() ;
      b.absBound.setTo(b.text.absBound) ;
      b.absBound.ypos(down) ;
      down += b.absBound.ydim() ;
      
      bubbles.add(b) ;
      b.attachTo(this) ;
    }
    for (Bubble b : bubbles) b.absBound.ypos(b.absBound.ypos() - down) ;
  }
}










