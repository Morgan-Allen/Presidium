


package src.graphics.widgets ;
import src.graphics.common.* ;
import src.util.* ;



public class Tooltips extends UIGroup {
  
  
  Bordering bordering ;
  Text infoText ;
  
  
  public Tooltips(HUD UI, Alphabet font, Texture border, Box2D inset) {
    super(UI) ;
    bordering = new Bordering(UI, border) ;
    bordering.relBound.set(0, 0, 1, 1) ;
    bordering.drawInset.setTo(inset) ;
    bordering.attachTo(this) ;
    infoText = new Text(UI, font) ;
    infoText.attachTo(this) ;
  }
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    return null ;
  }
  
  
  public void render() {
    super.render() ;
    //if (! hidden) I.say("rendering tooltip...") ;
  }


  protected void updateState() {
    //  It would also be nice to have the alpha fade in gradually.
    final float HOVER_TIME = 0.75f, HOVER_FADE = 0.25f ;
    final int MAX_TIPS_WIDTH = 200 ;
    hidden = true ;
    final HUD UI = myHUD ;
    if (
      UI.selected() != null &&
      UI.isMouseState(HUD.HOVERED) &&
      UI.timeHovered() > HOVER_TIME
    ) {
      final String info = UI.selected().info() ;
      if (info != null) {
        final float alpha = Visit.clamp(
          (UI.timeHovered() - HOVER_TIME) / HOVER_FADE, 0, 1
        ) ;
        hidden = false ;
        infoText.alpha = alpha ;
        bordering.alpha = alpha ;
        infoText.setText(info) ;
        infoText.setToPreferredSize(MAX_TIPS_WIDTH) ;
        //
        //  You need to constrain your bounds to fit within the visible area of
        //  the screen, but still accomodate visible text.
        final Box2D
          TB = infoText.preferredSize(),
          SB = UI.screenBounds(),
          BI = bordering.drawInset ;
        final float wide = TB.xdim(), high = TB.ydim() ;
        absBound.xdim(wide) ;
        absBound.ydim(high) ;
        absBound.xpos(Visit.clamp(
          UI.mousePos().x, 0 - BI.xpos(),
          SB.xdim() - (wide + BI.xmax())
        )) ;
        absBound.ypos(Visit.clamp(
          UI.mousePos().y, 0 - BI.ypos(),
          SB.ydim() - (high + BI.ymax())
        )) ;
      }
    }
    super.updateState() ;
  }
}





/*
public class Tooltips extends UIGroup {
  
  
  final public static Texture BORDER_TEX = Texture.loadTexture(
    "media/GUI/", "Frame.gif", "FrameAlpha.gif"
  ) ;
  final public Box2D BORDER_INSET = new Box2D().set(0.2f, 0.2f, 0.6f, 0.6f) ;
  
  
  public Tooltips(HUD myHUD) {
    super(myHUD) ;
  }
  
  /*
  final InsetBorder border ;
  Text infoText ;
  
  
  public static void attachTooltips() {
    final Tooltips tooltips = new Tooltips() ;
    HUD.addUIGroup(tooltips) ;
  }
  
  private Tooltips() {
    this.border = new InsetBorder(BORDER_TEX) ;
    border.relBound.set(0, 0, 1, 1) ;
    border.absBound.set(-5, -5, 10, 10) ;
    border.shade = 0.5f ;
    border.attachTo(this) ;
    infoText = new Text(PlayerUI.INFO_FONT) ;
    //absBound.set(0, 0, 100, 100) ;
    infoText.attachTo(this) ;
    //infoText.relBound.set(0, 0, 1, 1) ;
    infoText.colour.set(1, 1, 1, 1) ;
  }
  
  
  protected UINode selectionAt(Vec2D mousePos) { return null ; }
  
  
  protected void updateState() {
    //I.say("Updating help info.") ;
    super.updateState() ;
    hidden = true ;
    if (HUD.selected() != null && HUD.isMouseState(HUD.HOVERED)) {
      //I.say("Selected item is: "+HUD.selected().info()) ;
      final String info = HUD.selected().info() ;
      if (info != null) {
        hidden = false ;
        infoText.setText(info) ;
        infoText.setToPreferredSize() ;
        absBound.xdim(infoText.xdim()) ;
        absBound.ydim(infoText.ydim()) ;
        absBound.xpos(HUD.mousePos().x) ;
        absBound.ypos(HUD.mousePos().y + 20) ;
        //I.say("Absolute bound is: "+absBound) ;
      }
    }
  }
  //*/
//}