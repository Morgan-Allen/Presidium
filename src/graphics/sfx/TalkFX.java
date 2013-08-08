


package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;
import java.io.* ;
import org.lwjgl.opengl.GL11 ;




public class TalkFX extends SFX {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final static Model TALK_MODEL = new Model("talk_fx_model", TalkFX.class) {
    public Sprite makeSprite() { return new TalkFX() ; }
  } ;
  
  final static Texture
    BUBBLE_TEX = Texture.loadTexture("media/GUI/textBubble.png") ;
  final static Alphabet
    FONT = Text.INFO_FONT ;
  final static int
    MAX_LINES = 2 ;
  final static float
    LINE_HIGH  = FONT.map[' '].height,
    LINE_SPACE = LINE_HIGH + 10,
    FADE_RATE  = 1f / 25 ;
  
  final Stack <Bubble> toShow = new Stack <Bubble> () ;
  final Stack <Bubble> showing = new Stack <Bubble> () ;
  
  
  public TalkFX() {
  }
  
  
  public Model model() {
    return TALK_MODEL ;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeInt(toShow .size()) ;
    out.writeInt(showing.size()) ;
    final Batch <Bubble> all = new Batch <Bubble> () ;
    for (Bubble b : toShow ) all.add(b) ;
    for (Bubble b : showing) all.add(b) ;
    for (Bubble b : all) {
      LoadService.writeString(out, b.phrase) ;
      out.writeBoolean(b.spoken) ;
      out.writeFloat(b.width) ;
      out.writeFloat(b.xoff ) ;
      out.writeFloat(b.yoff ) ;
      out.writeFloat(b.alpha) ;
    }
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    final int numT = in.readInt(), numS = in.readInt() ;
    for (int n = 0 ; n < numT + numS ; n++) {
      final Bubble b = new Bubble() ;
      b.phrase = LoadService.readString(in) ;
      b.spoken = in.readBoolean() ;
      b.width = in.readFloat() ;
      b.xoff  = in.readFloat() ;
      b.yoff  = in.readFloat() ;
      b.alpha = in.readFloat() ;
      if (n < numT) toShow.add(b) ;
      else showing.add(b) ;
    }
  }
  
  
  
  /**  Updates and modifications-
    */
  static class Bubble {
    
    String phrase ;
    boolean spoken ;
    
    float width ;
    float xoff, yoff ;
    float alpha ;
  }
  
  
  public void update() {
    float spacing = Float.POSITIVE_INFINITY ;
    for (Bubble b : showing) {
      b.yoff += FADE_RATE * LINE_SPACE * (1 + numPhrases()) / 2f ;
      b.alpha -= FADE_RATE / MAX_LINES ;
      if (b.alpha <= 0) showing.remove(b) ;
      if (b.yoff < spacing) spacing = b.yoff ;
    }
    if (spacing >= LINE_SPACE && toShow.size() > 0) {
      showBubble(toShow.removeFirst()) ;
    }
  }
  
  
  public void addPhrase(String phrase, boolean spoken) {
    final Bubble b = new Bubble() ;
    b.phrase = phrase ;
    b.spoken = spoken ;
    toShow.add(b) ;
  }
  
  
  private void showBubble(Bubble b) {
    final float fontScale = LINE_HIGH / FONT.map[' '].height ;
    float width = 0 ;
    for (char c : b.phrase.toCharArray()) {
      Alphabet.Letter l = FONT.map[c] ;
      if (l == null) l = FONT.map[' '] ;
      width += l.width * fontScale ;
    }
    b.width = width ;
    b.yoff = 5 ;
    b.xoff = -40 ;
    b.alpha = 1 ;
    showing.add(b) ;
  }
  
  
  public int numPhrases() {
    return showing.size() + toShow.size() ;
  }
  
  
  
  /**  Rendering methods-
    */
  public void renderTo(Rendering rendering) {
    final Vec3D flatPoint = new Vec3D(position) ;
    rendering.port.isoToScreen(flatPoint) ;
    float fontScale = LINE_HIGH / FONT.map[' '].height ;
    
    GL11.glDepthMask(false) ;
    rendering.port.setScreenMode() ;
    
    BUBBLE_TEX.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    for (Bubble bubble : showing) if (bubble.spoken) {
      renderBubble(rendering, bubble, flatPoint, fontScale) ;
    }
    GL11.glEnd() ;

    FONT.fontTex.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    for (Bubble bubble : showing) {
      renderPhrase(rendering, bubble, flatPoint, fontScale) ;
    }
    GL11.glEnd() ;
    
    rendering.port.setIsoMode() ;
    GL11.glDepthMask(true) ;
    GL11.glColor4f(1, 1, 1, 1) ;
  }
  
  //  TODO:  use the SFX class' renderTex method instead?
  
  
  private void renderBubble(
    Rendering rendering, Bubble bubble,
    Vec3D flatPoint, float fontScale
  ) {
    //
    //  Some of this could be moved to the constants section-
    final float
      x = flatPoint.x + bubble.xoff,
      y = flatPoint.y + bubble.yoff,
      //TW = BUBBLE_TEX.xdim(),
      //TH = BUBBLE_TEX.ydim(),
      
      CAP_LU = 0.25f,
      CAP_RU = 0.75f,
      BOT_V = 0,
      TOP_V = BUBBLE_TEX.maxV(),
      
      //pad = 5,
      texHigh = (LINE_HIGH + 10) * 1.5f,
      minY = y - (5 + (texHigh / 3)),
      maxY = minY + texHigh,
      
      texWide = 128 * 40f / texHigh,  //True width/height for the texture.
      minX = x - 10,
      maxX = x + bubble.width + 10,
      capXL = minX + (texWide * 0.25f),
      capXR = maxX - (texWide * 0.25f) ;
    ///I.say("Tex dims are: "+TW+" "+TH) ;
    //
    //  Render the three segments of the bubble-
    GL11.glColor4f(1, 1, 1, bubble.alpha / 1f) ;
    UINode.drawQuad(
      minX , minY,
      capXL, maxY,
      0, BOT_V, CAP_LU, TOP_V,
      flatPoint.z + bubble.yoff
    ) ;
    UINode.drawQuad(
      capXL, minY,
      capXR, maxY,
      CAP_LU, BOT_V, CAP_RU, TOP_V,
      flatPoint.z + bubble.yoff
    ) ;
    UINode.drawQuad(
      capXR, minY,
      maxX , maxY,
      CAP_RU, BOT_V, 1, TOP_V,
      flatPoint.z + bubble.yoff
    ) ;
  }
  
  
  private void renderPhrase(
    Rendering rendering, Bubble bubble,
    Vec3D flatPoint, float fontScale
  ) {
    float
      scanW = 0,
      x = flatPoint.x + bubble.xoff,
      y = flatPoint.y + bubble.yoff ;
    
    GL11.glColor4f(1, 1, 1, bubble.alpha) ;
    for (char c : bubble.phrase.toCharArray()) {
      Alphabet.Letter l = FONT.map[c] ;
      if (l == null) l = FONT.map[' '] ;
      UINode.drawQuad(
        x + scanW, y,
        x + scanW + (l.width * fontScale),
        y + (l.height * fontScale),
        l.umin, l.vmin, l.umax, l.vmax,
        flatPoint.z + bubble.yoff
      ) ;
      scanW += l.width * fontScale ;
    }
  }
}










