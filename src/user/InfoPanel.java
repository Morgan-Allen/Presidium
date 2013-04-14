/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.Bordering;
import src.graphics.widgets.Text;
import src.graphics.widgets.UIGroup;
import src.graphics.widgets.Text.Clickable;


/*   I'll also need multiple tabs for different forms of information.
 */
public class InfoPanel extends UIGroup {
  
  
  final public Texture BORDER_TEX = Texture.loadTexture(
      "media/GUI/Panel.png"
  ) ;
  
  
  final protected BaseUI UI ;
  
  final Bordering border ;
  final Text headerText, detailText ;
  final protected Selectable selected ;
  private int categoryID ;
  
  
  
  public InfoPanel(BaseUI UI, Selectable selected) {
    super(UI) ;
    this.UI = UI ;
    //
    //  TODO:  These bounds have to exported to the BaseUI class,
    //         template-wise...
    this.relBound.set(0.00f, 0.0f, 0.33f, 1.0f) ;
    this.absBound.set(20, 70, -40, -90) ;
    
    this.border = new Bordering(UI, BORDER_TEX) ;
    border.drawInset.set(-40, -40, 80, 80) ;
    border.absBound.set(10, 10, -20, -20) ;
    border.relBound.set(0, 0, 1, 1) ;
    border.attachTo(this) ;
    
    headerText = new Text(UI, BaseUI.INFO_FONT) ;
    headerText.relBound.set(0, 1, 1, 0) ;
    headerText.absBound.set(10, -10 -40, -20, 40) ;
    headerText.attachTo(this) ;
    
    detailText = new Text(UI, BaseUI.INFO_FONT) ;
    detailText.relBound.set(0, 0, 1, 1) ;
    detailText.absBound.set(10, 10, -20, -40 -20 -10) ;
    detailText.attachTo(this) ;
    detailText.getScrollBar().attachTo(this) ;

    this.selected = selected ;
    final String cats[] = (selected == null) ?
      null : selected.infoCategories() ;
    categoryID = 0 ;
  }
  
  
  protected void updateState() {
    if (selected != null && ! selected.inWorld()) {
      //if (true) throw new RuntimeException("Huh?") ;
      I.say(
        "SELECTION IS NO LONGER IN WORLD... "+
        selected+" "+selected.getClass().getName()
      ) ;
      UI.setSelection(null) ;
      return ;
    }
    updateText() ;
    super.updateState() ;
  }
  
  protected void updateText() {
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
    selected.writeInformation(detailText, categoryID) ;
  }
}











