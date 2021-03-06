/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.graphics.widgets.Alphabet.Letter ;
import src.user.Description ;  //  TODO:  Remove out-of-package references?
import src.util.* ;
import src.graphics.common.* ;
import org.lwjgl.opengl.* ;
import java.awt.event.KeyEvent ;



/**  A text object that wraps will continue onto subsequent lines when a given
  *  line is filled.  Non-wrapping text will adjust width to match current
  *  entries.
  */
public class Text extends UINode implements Description {
  
  
  final public static Alphabet INFO_FONT = new Alphabet(
    "media/GUI/", "FontVerdana.gif", "FontVerdana.gif",
    "FontVerdana.map", 8, 16
  ) ;
  final public static Colour LINK_COLOUR = new Colour().set(
    0.2f, 0.6f, 0.8f, 1
  ) ;
  
  
  public float scale = 1.0f ;
  
  final protected Alphabet alphabet ;
  private boolean needsFormat = false ;
  private Scrollbar scrollbar ;
  
  protected List <Box2D> allEntries = new List <Box2D> () ;
  private Box2D fullSize = new Box2D() ;
  private float oldWide, oldHigh = 0 ;
  
  
  /**  Assorted constructors.  If 'grow' is true, the text will expand to fit
    *  the total size of the String given.  If 'wrap' is true, width will
    *  remain fixed but text will wrap around from side to side.  If 'roll'
    *  is true, then text beyond size limits will be hidden, but the last
    *  entries will remain visible.
    */
  public Text(HUD myHUD, Alphabet a) {
    this(myHUD, a, "") ;
  }
  
  
  public Text(HUD myHUD, Alphabet a, String s) {
    super(myHUD) ;
    alphabet = a ;
    setText(s) ;
  }
  
  //
  //  TODO:  Scrollbars should be possible to associate with arbitrary UI
  //  groups.  In fact, they should probably be a group type unto themselves.
  public Scrollbar getScrollBar() {
    final Scrollbar bar = new Scrollbar(
      myHUD, Scrollbar.SCROLL_TEX, fullSize, true
    ) ;
    final float W = Scrollbar.DEFAULT_SCROLL_WIDTH ;
    bar.relBound.set(relBound.xmax(), relBound.ypos(), 0, relBound.ydim()) ;
    bar.absBound.set(absBound.xmax(), absBound.ypos(), W, absBound.ydim()) ;
    return this.scrollbar = bar ;
  }
  
  
  
  /**  Essential component classes and interfaces- clickables can be used to
    *  navigate to other objects, with image and text entries provide
    *  information or emphasis-
    */
  public static interface Clickable {
    String fullName() ;
    void whenClicked() ;
  }
  
  
  static class ImageEntry extends Box2D {
    Image graphic ;
    boolean visible ;
    int wide, high ;
  }
  
  
  static class TextEntry extends Box2D {
    char key ;
    Letter letter ;
    boolean visible ;
    Colour colour = null ;
    Clickable link = null ;
  }
  
  
  
  /**  Various overrides of UINode functionality-
    */
  protected void updateAbsoluteBounds() {
    super.updateAbsoluteBounds() ;
    if ((oldWide != xdim()) || (oldHigh != ydim())) {
      needsFormat = true ;
      oldWide = xdim() ;
      oldHigh = ydim() ;
    }
    if (needsFormat && (allEntries.size() > 0)) format(xdim()) ;
    //needsFormat = false ;
  }
  
  
  
  /**  Adds the given String to this text object in association with
    *  the specified selectable.
    */
  public void append(String s, Clickable link, Colour c) {
    if (s == null) s = "(none)" ;
    for (int n = 0, l = s.length() ; n < l ; n++)
      addEntry(s.charAt(n), link, c)  ;
  }
  

  public void append(Clickable l, Colour c) {
    if (l == null) append("(none)") ;
    else append(l.fullName(), l, c) ;
  }
  
  
  public void append(Clickable l) {
    if (l == null) append("(none)") ;
    else append(l.fullName(), l, LINK_COLOUR) ;
  }
  
  
  public void append(Object o) {
    if (o instanceof Clickable) append((Clickable) o) ;
    else if (o != null) append(o.toString()) ;
    else append("(none)") ;
  }
  
  
  public void append(String s, Clickable l) { append(s, l, LINK_COLOUR) ; }
  public void append(String s, Colour c) { append(s, null, c) ; }
  public void append(String s) { append(s, null, null) ; }
  
  
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
  
  
  
  /**  Adds a single image entry to this text object.  Images are used as
    *  'bullets' to indent and separate text, and this needsFormat is retained until
    *  the next carriage return or another image is inserted.
    */
  public boolean insert(Texture texGraphic, int maxSize) {
    return insert(new Image(myHUD, texGraphic), maxSize) ;
  }
  
  
  public boolean insert(Image graphic, int maxSize) {
    if (graphic == null) return false ;
    graphic.absBound.set(0, 0, maxSize, maxSize) ;
    graphic.relBound.set(0, 0, 0, 0) ;
    graphic.updateRelativeParent() ;
    graphic.updateAbsoluteBounds() ;
    final ImageEntry entry = new ImageEntry() ;
    entry.graphic = graphic ;
    entry.wide = (int) graphic.xdim() ;
    entry.high = (int) graphic.ydim() ;
    allEntries.add(entry) ;
    needsFormat = true ;
    return true ;
  }
  
  
  public void cancelBullet() {
    //  TODO:  Get rid of the indent effect associated with the last image?
  }
  
  
  
  /**  Adds a single letter entry to this text object.
    */
  boolean addEntry(char k, Clickable links, Colour c) {
    Letter l = null ;
    if (((l = alphabet.map[k]) == null) && (k != '\n')) return false ;
    final TextEntry entry = new TextEntry() ;
    entry.key = k ;
    entry.letter = l ;
    entry.colour = c ;
    entry.link = links ;
    allEntries.addLast(entry) ;
    needsFormat = true ;
    return true ;
  }
  
  
  
  /**  Sets this text object to the given string.
    */
  public void setText(String s) {
    allEntries.clear() ;
    append(s, null, null) ;
    needsFormat = true ;
  }
  
  
  
  /**  Gets the string this text object contains.
    */
  public String getText() {
    int n = 0 ;
    char charS[] = new char[allEntries.size()] ;
    for (Box2D entry : allEntries) {
      if (entry instanceof TextEntry)
        charS[n++] = ((TextEntry) entry).key ;
      else
        charS[n++] = '*' ;
    }
    return new String(charS) ;
  }
  
  
  
  /**  Handles any key press this text is registered to listen for.
    */
  void keyPress(char k) {
    switch(k) {
      case(KeyEvent.VK_BACK_SPACE) :
      case(KeyEvent.VK_DELETE)     :
        if (allEntries.size() > 0) {
          allEntries.removeLast() ;  //delete last letter.
          needsFormat = true ;
        }
        break ;
      case(KeyEvent.VK_ESCAPE) :
      case(KeyEvent.VK_ENTER)  :
      case(KeyEvent.VK_ACCEPT) :
        escaped() ;
        break ;
      default :
        addEntry(k, null, null) ;
    }
  }
  
  
  
  /**  Called when edit mode is escaped.
    */
  void escaped() {
    I.say("exiting edit mode...") ;
  }
  
  
  
  /**  Returns the selectable associated with the currently hovered unit of
    *  text.
    */
  protected Clickable getTextLink(Vec2D mousePos, Box2D textArea) {
    if (myHUD.selected() != this) return null ;
    final float
      mX = mousePos.x + textArea.xpos() - xpos(),
      mY = mousePos.y + textArea.ypos() - ypos() ;
    if (! textArea.contains(mX, mY)) return null ;
    Box2D box = new Box2D() ;
    for (Box2D entry : allEntries) {
      box.set(
        entry.xpos() - 1, entry.ypos() - 1,
        entry.xdim() + 2, entry.ydim() + 2
      ) ;
      if (box.contains(mX, mY) && entry.containedBy(textArea)) {
        if (entry instanceof TextEntry) return ((TextEntry) entry).link ;
        return null ;
      }
    }
    return null ;
  }
  
  
  
  /**  Draws this text object.
    */
  protected void render() {
    if (allEntries.size() == 0) return ;
    //
    //  First, determine a 'viewing window' for the text, if larger than the
    //  visible pane-
    final Box2D textArea = new Box2D().set(0, 0, xdim(), ydim()) ;
    if (scrollbar != null) {
      final float lineHigh = 0 ;// alphabet.map[' '].height * scale ;
      textArea.ypos(
        (0 - lineHigh) -
        (fullSize.ydim() - ydim()) * (1 - scrollbar.scrollPos())
      ) ;
    }
    //
    //  See what was clicked, if anything-
    //  TODO:  Move this to the selection method?
    final Clickable link = getTextLink(myHUD.mousePos(), textArea) ;
    final Batch <ImageEntry> bullets = new Batch <ImageEntry> () ;
    //
    //  Begin the rendering pass...
    alphabet.fontTex.bindTex() ;
    GL11.glEnable(GL11.GL_SCISSOR_TEST) ;
    GL11.glScissor((int) xpos(), (int) ypos(), (int) xdim(), (int) ydim()) ;
    GL11.glBegin(GL11.GL_QUADS) ;
    for (Box2D entry : allEntries) {
      if (entry instanceof TextEntry) {
        renderText(textArea, (TextEntry) entry, link) ;
      }
      else bullets.add((ImageEntry) entry) ;
    }
    GL11.glEnd() ;
    for (ImageEntry entry : bullets) {
      renderImage(textArea, entry) ;
    }
    GL11.glDisable(GL11.GL_SCISSOR_TEST) ;
    //  TODO:  Move this to the selection method?
    if (myHUD.mouseClicked() && link != null) link.whenClicked() ;
  }
  
  
  
  /**  Renders an image embedded within the text.
    */
  protected void renderImage(Box2D bounds, ImageEntry entry) {
    if (! entry.intersects(bounds)) return ;
    final Box2D b = entry.graphic.absBound ;
    entry.graphic.relAlpha = this.absAlpha ;
    b.xpos(entry.xpos() + xpos() - bounds.xpos()) ;
    b.ypos(entry.ypos() + ypos() - bounds.ypos()) ;
    entry.graphic.updateState() ;
    entry.graphic.updateRelativeParent() ;
    entry.graphic.updateAbsoluteBounds() ;
    
    entry.graphic.render() ;
  }
  
  
  
  /**  Renders a single character within the text field, if visible.
    */
  protected boolean renderText(Box2D area, TextEntry entry, Clickable link) {
    if (entry.letter == null || ! entry.intersects(area)) return false ;
    //
    //  If this text links to something, we may need to colour the text (and
    //  possibly select it's link target if clicked.)
    if (link != null && entry.link == link) {
      GL11.glColor4f(1, 1, 0, absAlpha) ;
    }
    else {
      final Colour c = entry.colour != null ? entry.colour : Colour.WHITE ;
      GL11.glColor4f(c.r, c.g, c.b, c.a * absAlpha) ;
    }
    final float xoff = xpos() - area.xpos(), yoff = ypos() - area.ypos() ;
    //
    //  Draw the text entry-
    drawQuad(
      entry.xpos() + xoff, entry.ypos() + yoff,
      entry.xmax() + xoff, entry.ymax() + yoff,
      entry.letter.umin, entry.letter.vmin,
      entry.letter.umax, entry.letter.vmax,
      absDepth
    ) ;
    return true ;
  }
  
  
  
  /**  Sets this text object to the size it would ideally prefer in order to
    *  accomodate it's text.
    */
  public void setToPreferredSize(float maxWidth) {
    format(maxWidth) ;
    relBound.xdim(0) ;
    relBound.ydim(0) ;
    absBound.xdim(fullSize.xdim()) ;
    absBound.ydim(fullSize.ydim()) ;
  }
  
  
  public Box2D preferredSize() {
    return fullSize ;
  }
  
  
  
  /**  Puts all letters in their proper place, allowing for roll/wrap/grow
    *  effects, and, if neccesary, adjusts the bounds of this UIObject
    *  accordingly.
    */
  protected void format(float maxWidth) {
    ListEntry <Box2D>
      open = allEntries,
      wordOpen = open,
      lineOpen = open ;
    ImageEntry
      lastBullet = null ;
    boolean
      newWord,
      newLine ;
    float
      xpos = 0,
      ypos = 0 ;
    final float
      lineHigh = alphabet.map[' '].height * scale,
      charWide = alphabet.map[' '].width  * scale ;
    //
    //  Here's the main loop for determining entry positions...
    while ((open = open.nextEntry()) != allEntries) {
      newLine = newWord = false ;
      //
      //  In the case of an image entry...
      if (open.refers instanceof ImageEntry) {
        if (lastBullet != null) {
          final float minY = lastBullet.ypos() - (lineHigh * 1.5f) ;
          if (ypos > minY) ypos = minY ;
        }
        final ImageEntry entry = (ImageEntry) open.refers ;
        entry.visible = true ;
        entry.set(
          0, ypos + lineHigh - (entry.high),
          entry.wide * scale, entry.high
        ) ;
        xpos = entry.wide ;
        lastBullet = entry ;
      }
      //
      //  In the case of a text entry...
      else {
        final TextEntry entry = (TextEntry) open.refers ;
        entry.visible = true ;
        switch (entry.key) {
          //
          //  In the case of a return character, you definitely need a new line,
          //  and you automatically escape from the last bullet.
          //  Either that or a space means a new word.
          case('\n'):
            newLine = newWord = true ;
            entry.visible = false ;
          break ;
          case(' '):
            newWord = true ;
          default:
            entry.set(
              xpos, ypos,
              entry.letter.width  * scale,
              entry.letter.height * scale
            ) ;
            xpos += entry.xdim() ;
            //
            //  If the width of the current line exceeds the space allowed, you
            //  need to go back to that start of the current word and jump to
            //  the next line.
            if ((maxWidth > 0) && (xpos > maxWidth) && (wordOpen != lineOpen)) {
              newLine = true ;
            }
        }
      }
      //
      //  Mark the first character of a new word so you can escape back to it.
      //  If a new line is called for, flow around the current bullet.
      if (newWord) wordOpen = open ;
      if (newLine) {
        xpos = (lastBullet == null) ? 0 : (lastBullet.wide + charWide) ;
        ypos -= lineHigh ;
        open = lineOpen = wordOpen ;
      }
    }
    //
    //  We now reposition entries to fit the window, and update the full bounds.
    fullSize.set(0, 0, 0, lineHigh) ;
    final float heightAdjust = ydim() - lineHigh ;
    for (Box2D entry : allEntries) {
      fullSize.include(entry) ;
      entry.ypos(entry.ypos() + heightAdjust) ;
    }
    needsFormat = false ;
  }
}




