


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
    bordering.absBound.setTo(inset) ;
    ///bordering.shade = 0.5f ;
    bordering.attachTo(this) ;
    infoText = new Text(UI, font) ;
    infoText.colour.set(1, 1, 1, 1) ;
    infoText.attachTo(this) ;
  }
  
  
  protected void updateState() {
    super.updateState() ;
    hidden = true ;
    if (myHUD.selected() != null && myHUD.isMouseState(HUD.HOVERED)) {
      final String info = myHUD.selected().info() ;
      if (info != null) {
        hidden = false ;
        infoText.setText(info) ;
        infoText.setToPreferredSize() ;
        absBound.xdim(infoText.xdim()) ;
        absBound.ydim(infoText.ydim()) ;
        absBound.xpos(myHUD.mousePos().x) ;
        absBound.ypos(myHUD.mousePos().y + 20) ;
      }
    }
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