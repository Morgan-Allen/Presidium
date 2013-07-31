/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.user ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
///import src.graphics.widgets.Text.Clickable ;



public class InfoPanel extends UIGroup implements UIConstants {
  
  
  final public Texture BORDER_TEX = Texture.loadTexture(
    "media/GUI/Panel.png"
  ) ;
  final public static int
    DEFAULT_TOP_MARGIN = 50,
    MARGIN_WIDTH = 10,
    HEADER_HEIGHT = 50 ;
  
  
  final protected BaseUI UI ;
  
  final Bordering border ;
  final Text headerText, detailText ;
  final protected Selectable selected ;
  private int categoryID ;
  
  
  
  public InfoPanel(BaseUI UI, Selectable selected, int topMargin) {
    super(UI) ;
    this.UI = UI ;
    this.relBound.setTo(PANE_BOUNDS) ;
    //  TODO:  Make the insets part of the constants as well?
    this.absBound.set(20, 30, -40, -50) ;
    final int MW = MARGIN_WIDTH, HH = HEADER_HEIGHT ;
    
    this.border = new Bordering(UI, BORDER_TEX) ;
    border.drawInset.set(-40, -40, 80, 80) ;
    border.absBound.set(MW, MW, -2 * MW, -2 * MW) ;
    border.relBound.set(0, 0, 1, 1) ;
    border.attachTo(this) ;
    
    headerText = new Text(UI, BaseUI.INFO_FONT) ;
    headerText.relBound.set(0, 1, 1, 0) ;
    headerText.absBound.set(MW, -MW - (topMargin + HH), -2 * MW, HH) ;
    headerText.attachTo(this) ;
    
    detailText = new Text(UI, BaseUI.INFO_FONT) ;
    detailText.relBound.set(0, 0, 1, 1) ;
    detailText.absBound.set(MW, MW, -2 * MW, (-2 * MW) - (topMargin + HH)) ;
    detailText.attachTo(this) ;
    detailText.getScrollBar().attachTo(this) ;

    this.selected = selected ;
    final String cats[] = (selected == null) ?
      null : selected.infoCategories() ;
    categoryID = 0 ;
  }
  
  
  protected void updateState() {
    /*
    if (selected != null && ! selected.inWorld()) {
      I.say(
        "SELECTION IS NO LONGER IN WORLD... "+
        selected+" "+selected.getClass().getName()
      ) ;
      UI.selection.setSelected(null) ;
      return ;
    }
    //*/
    updateText(UI, headerText, detailText) ;
    super.updateState() ;
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText
  ) {
    if (selected == null) return ;
    headerText.setText(selected.fullName()) ;
    final String cats[] = selected.infoCategories() ;
    if (cats != null) {
      headerText.append("\n") ;
      for (int i = 0 ; i < cats.length ; i++) {
        final int index = i ;
        headerText.append(new Text.Clickable() {
          public String fullName() { return "("+cats[index]+")" ; }
          public void whenClicked() { categoryID = index ; }
        }) ;
      }
    }
    detailText.setText("") ;
    selected.writeInformation(detailText, categoryID, UI) ;
  }
}











