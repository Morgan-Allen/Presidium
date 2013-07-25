/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;



public class Selection implements UIConstants {
  
  
  /**  Field definitions and accessors-
    */
  final BaseUI UI ;
  
  
  private Tile pickTile ;
  private Fixture pickFixture ;
  private Mobile pickMobile ;
  private Mission pickMission ;
  
  private Selectable hovered, selected ;
  
  
  Selection(BaseUI UI) {
    this.UI = UI ;
  }

  public void loadState(Session s) throws Exception {
    final Target lastSelect = s.loadTarget() ;
    setSelected((Selectable) lastSelect) ;
  }
  

  public void saveState(Session s) throws Exception {
    s.saveTarget((Target) selected) ;
  }
  
  
  public Selectable hovered()  { return hovered  ; }
  public Selectable selected() { return selected ; }
  
  public Tile    pickedTile   () { return pickTile    ; }
  public Fixture pickedFixture() { return pickFixture ; }
  public Mobile  pickedMobile () { return pickMobile  ; }
  
  
  
  /**  
    */
  boolean updateSelection(World world, Viewport port, UIGroup infoPanel) {
    //
    //  If a UI element is selected, don't pick anything else-
    if (UI.selected() != null) {
      pickTile = null ;
      pickMobile = null ;
      pickFixture = null ;
      hovered = null ;
      return false ;
    }
    //
    //  Our first task to see what the different kinds of object currently
    //  being hovered over are-
    hovered = null ;
    pickTile = world.pickedTile(UI, port) ;
    pickFixture = world.pickedFixture(UI, port) ;
    pickMobile = world.pickedMobile(UI, port) ;
    pickMission = UI.played().pickedMission(UI, port) ;
    //
    //  Then, we see which type is given priority-
    if (pickMission != null) {
      hovered = pickMission ;
    }
    else if (pickMobile != null) {
      hovered = pickMobile ;
    }
    else if (pickFixture instanceof Selectable) {
      hovered = (Selectable) pickFixture ;
    }
    else {
      hovered = null ;
    }
    return true ;
  }
  

  public void setSelected(Selectable s) {
    if (s != null) {
      if ((s instanceof Element) && ((Element) s).inWorld()) {
        selected = s ;
        UI.camera.lockOn(selected) ;
      }
      UI.setInfoPanel(s.createPanel(UI)) ;
    }
    else if (selected != null) {
      UI.camera.lockOn(selected = null) ;
      UI.setInfoPanel(null) ;  //  Use default panel instead?
    }
  }
  
  
  
  /**  Rendering FX-
    */
  void renderWorldFX(Rendering rendering) {
    if (hovered instanceof Element && hovered != selected) {
      renderSelectFX((Element) hovered, Colour.transparency(0.5f), rendering) ;
    }
    if (selected instanceof Element) {
      renderSelectFX((Element) selected, Colour.WHITE, rendering) ;
    }
  }
  
  
  private void renderSelectFX(Element element, Colour c, Rendering r) {
    if (element.sprite() == null) return ;
    final Texture ringTex = (element instanceof Fixture) ?
      SELECT_SQUARE :
      SELECT_CIRCLE ;
    final PlaneFX hoverRing = new PlaneFX(ringTex, element.radius() * 2) ;
    hoverRing.colour = c ;
    hoverRing.position.setTo(element.sprite().position) ;
    r.addClient(hoverRing) ;
  }
}
















