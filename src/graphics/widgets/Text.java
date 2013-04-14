/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.graphics.widgets.Alphabet.Letter ;
import src.user.Description;
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
  

  public Colour
    colour = (new Colour()).set(1, 1, 1, 1) ;
  public float
    scale = 1.0f ;
  
  final protected Alphabet alphabet ;
  private boolean roll ;
  private boolean format = false ;
  private Scrollbar scrollbar ;
  
  protected List <Box2D> allEntries = new List () ;
  private Box2D fullSize = new Box2D() ;
  private float oldWide, oldHigh = 0 ;
  
  
  /**  Assorted constructors.  If 'grow' is true, the text will expand to fit
    *  the total size of the String given.  If 'wrap' is true, width will
    *  remain fixed but text will wrap around from side to side.  If 'roll'
    *  is true, then text beyond size limits will be hidden, but the last
    *  entries will remain visible.
    */
  public Text(HUD myHUD, Alphabet a) { this(myHUD, a, "", false) ; }
  public Text(HUD myHUD, Alphabet a, String s) { this(myHUD, a, s, false) ; }
  
  protected Text(HUD myHUD, Alphabet a, String t, boolean roll) {
    super(myHUD) ;
    alphabet = a ;
    this.roll = roll ;
    this.shade = -1 ;
    setText(t) ;
  }
  
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
    Texture graphic ;
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
      format = true ;
      oldWide = xdim() ;
      oldHigh = ydim() ;
    }
    if (format && (allEntries.size() > 0)) format() ;
    format = false ;
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
  
  public void append(String s, Clickable l) { append(s, l, null) ; }
  public void append(String s, Colour c) { append(s, null, c) ; }
  public void append(String s) { append(s, null, null) ; }
  
  
  /**  Adds a single image entry to this text object.  Images are used as
    *  'bullets' to indent and separate text, and this format is retained until
    *  the next carriage return or another image is inserted.
    */
  public boolean insert(Texture graphic, int maxSize) {
    if (graphic == null) return false ;
    float maxDim = Math.max(graphic.maxU(), graphic.maxV()) ;
    final ImageEntry entry = new ImageEntry() ;
    entry.graphic = graphic ;
    entry.wide = (int) (graphic.maxU() * maxSize / maxDim) ;
    entry.high = (int) (graphic.maxV() * maxSize / maxDim) ;
    allEntries.add(entry) ;
    format = true ;
    return true ;
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
    format = true ;
    return true ;
  }
  
  
  /**  Sets this text object to the given string.
    */
  public void setText(String s) {
    allEntries.clear() ;
    append(s, null, null) ;
    format = true ;
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
          format = true ;
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
    //
    //  If the text is meant to roll, shift the text area to fit over the last
    //  characters.  Otherwise, check on what our scrollbar says.
    if (roll && fullSize.xdim() > xdim()) {
      textArea.xpos(fullSize.xdim() - xdim()) ;
    }
    if (scrollbar != null && ! roll) {
      textArea.ypos(
        0 - (fullSize.ydim() - ydim()) *
        (1 - scrollbar.scrollPos())
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
    colour.bindColour() ;
    GL11.glEnable(GL11.GL_SCISSOR_TEST) ;
    GL11.glScissor((int) xpos(), (int) ypos(), (int) xdim(), (int) ydim()) ;
    GL11.glBegin(GL11.GL_QUADS) ;
    for (Box2D entry : allEntries) {
      if (entry instanceof TextEntry)
        renderText(textArea, (TextEntry) entry, link) ;
      else bullets.add((ImageEntry) entry) ;
    }
    GL11.glEnd() ;
    GL11.glColor4f(1, 1, 1, 1) ;
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
    entry.graphic.bindTex() ;
    final float xoff = xpos() - bounds.xpos(), yoff = ypos() - bounds.ypos() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    drawQuad(
      entry.xpos() + xoff, entry.ypos() + yoff,
      entry.xmax() + xoff, entry.ymax() + yoff,
      0, 0, entry.graphic.maxU(), entry.graphic.maxV()
    ) ;
    GL11.glEnd() ;
  }
  
  
  /**  Renders a single character within the text field, if visible.
    */
  protected boolean renderText(Box2D area, TextEntry entry, Clickable link) {
    if (entry.letter == null || ! entry.intersects(area)) return false ;
    //
    //  If this text links to something, we may need to colour the text (and
    //  possibly select it's link target if clicked.)
    if (link != null && entry.link == link) {
      GL11.glColor3f(1, 1, 0) ;
    }
    else {
      final Colour c = entry.colour != null ? entry.colour : this.colour ;
      GL11.glColor4f(c.r, c.g, c.b, c.a) ;
    }
    final float xoff = xpos() - area.xpos(), yoff = ypos() - area.ypos() ;
    //
    //  Draw the text entry-
    drawQuad(
      entry.xpos() + xoff, entry.ypos() + yoff,
      entry.xmax() + xoff, entry.ymax() + yoff,
      entry.letter.umin, entry.letter.vmin,
      entry.letter.umax, entry.letter.vmax
    ) ;
    return true ;
  }
  
  
  /**  Sets this text object to the size it would ideally prefer in order to
    *  accomodate it's text.
    */
  public void setToPreferredSize() {
    roll = true ;
    format() ;
    relBound.xdim(0) ;
    relBound.ydim(0) ;
    absBound.xdim(fullSize.xdim()) ;
    absBound.ydim(fullSize.ydim()) ;
  }
  
  
  /**  Puts all letters in their proper place, allowing for roll/wrap/grow
    *  effects, and, if neccesary, adjusts the bounds of this UIObject
    *  accordingly.
    *  TODO:  Fix wrapping problems.
    */
  protected void format() {
    ListEntry <Box2D>
      open = allEntries,
      wordOpen = open,
      lineOpen = open ;
    ImageEntry
      lastBullet = null ;
    boolean
      newWord,
      newLine,
      newBullet ;
    float
      xpos = 0, ypos = 0,
      entryW = 0, entryH = 0 ;
    final float
      lineHigh = alphabet.map[' '].height * scale,
      charWide = alphabet.map[' '].width  * scale,
      boundW = xdim(),
      boundH = ydim() ;
    //
    //  Here's the main loop for determining entry positions...
    while ((open = open.nextEntry()) != allEntries) {
      newLine = newWord = newBullet = false ;
      
      if (open.refers instanceof ImageEntry) {
        final ImageEntry entry = (ImageEntry) open.refers ;
        entry.visible = true ;
        entry.set(
          0, ypos + lineHigh - (entry.high * scale),
          entry.wide * scale, entry.high * scale
        ) ;
        xpos = entry.wide ;
        lastBullet = entry ;
      }
      else {
        final TextEntry entry = (TextEntry) open.refers ;
        entry.visible = true ;
        switch (entry.key) {
          //
          //  In the case of a return character, you definitely need a new line,
          //  and you automatically escape from the last bullet.
          //  Either that or a space means a new word.
          case('\n'):
            newLine = newWord = newBullet = true ;
            entry.visible = false ;
          break ;
          case(' '):
            newWord = true ;
          default:
            entry.set(
              xpos, ypos,
              entry.letter.width * scale,
              entry.letter.height * scale
            ) ;
            xpos += entry.xdim() ;
            //
            //  If the width of the current line exceeds the space allowed, you
            //  need to go back to that start of the current word and jump to
            //  the next line.
            if ((! roll) && (xpos > boundW) && (wordOpen != lineOpen))
              newLine = true ;
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
      else if (xpos > entryW)
        entryW = xpos ;
    }
    //
    //  We now reposition entries to fit the window, and cache the string.
    entryH = (0 - ypos) + lineHigh ;
    xpos = ypos = 0 ;
    ypos = boundH - lineHigh ;
    if (roll) {
      xpos = (entryW > boundW) ? (boundW - entryW) : xpos ;
      ypos = (entryH > boundH) ? (boundH - entryH) : ypos ;
    }
    //
    //  Move characters up to the top of the virtual area...
    fullSize.set(0, 0, boundW, lineHigh) ;
    for (Box2D entry : allEntries) {
      fullSize.include(entry) ;
      entry.ypos(entry.ypos() + boundH - lineHigh) ;
      //if (entry == allEntries.last().refers) I.say("Last entry: "+entry) ;
    }
    //I.say("Full size is: "+fullSize) ;
    format = false ;
  }
}

