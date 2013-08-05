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
import src.graphics.sfx.* ;
import src.graphics.widgets.* ;
import src.util.* ;



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
    setSelected((Selectable) s.loadObject()) ;
  }
  

  public void saveState(Session s) throws Exception {
    s.saveObject(selected) ;
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
      selected = s ;
      UI.camera.lockOn(s.subject()) ;
      UI.setInfoPanel(s.createPanel(UI)) ;
    }
    else if (selected != null) {
      selected = null ;
      UI.camera.lockOn(null) ;
      UI.setInfoPanel(null) ;
    }
  }
  
  
  
  /**  Rendering FX-
    */
  void renderWorldFX(Rendering rendering) {
    final Target
      HS = (hovered  == null) ? null : hovered .subject(),
      SS = (selected == null) ? null : selected.subject() ;
    if (HS != null && HS != SS) {
      //renderSelectFX(hovered, HS, Colour.transparency(0.5f), rendering) ;
      hovered.renderSelection(rendering, true) ;
    }
    if (SS != null) {
      selected.renderSelection(rendering, false) ;
      //renderSelectFX(selected, SS, Colour.WHITE, rendering) ;
    }
  }
  
  
  public static void renderPlane(
    Rendering r, Vec3D pos, float radius, Colour c, Texture ringTex
  ) {
    final PlaneFX ring = new PlaneFX(ringTex, radius) ;
    ring.colour = c ;
    ring.position.setTo(pos) ;
    r.addClient(ring) ;
  }
}










