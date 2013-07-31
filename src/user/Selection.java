/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.building.* ;
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
  public Mission pickedMission() { return pickMission ; }
  
  
  
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
      if ((s instanceof Target) && ((Target) s).inWorld()) {
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
    Target HE = null, SE = null ;
    if (hovered instanceof Element) HE = hovered ;
    if (hovered instanceof Mission) HE = ((Mission) hovered).subject() ;

    if (selected instanceof Element) SE = selected ;
    if (selected instanceof Mission) SE = ((Mission) selected).subject() ;
    
    if (HE instanceof Element && HE != SE) {
      renderSelectFX(HE, Colour.transparency(0.5f), rendering) ;
    }
    if (SE instanceof Element) {
      renderSelectFX(SE, Colour.WHITE, rendering) ;
    }
  }
  
  
  private void renderSelectFX(Target element, Colour c, Rendering r) {
    final Texture ringTex = (element instanceof Fixture) ?
      SELECT_SQUARE :
      SELECT_CIRCLE ;
    final float radius = (element instanceof Venue) ?
      ((((Venue) element).xdim() / 2f) + 1) :
      element.radius() * 2 ;
    final PlaneFX hoverRing = new PlaneFX(ringTex, radius) ;
    hoverRing.colour = c ;
    if (element instanceof Mobile) {
      hoverRing.position.setTo(((Mobile) element).viewPosition(null)) ;
    }
    else hoverRing.position.setTo(element.position(null)) ;
    r.addClient(hoverRing) ;
  }
}











