/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.building.*;
import src.game.common.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import java.lang.reflect.Constructor ;
import org.lwjgl.input.Keyboard ;
import java.io.* ;






public class BuildingsTab extends InfoPanel {
  
  
  /**  Field definitions and constructors-
    */
  final BaseUI UI ;
  final List <InstallType> types = new List <InstallType> () ;
  InstallType helpShown = null ;
  
  
  BuildingsTab(BaseUI UI) {
    super(UI, null) ;
    this.UI = UI ;
    setupAllTypes() ;
  }
  

  
  /**  Compiling the list of all building types-
    */
  static class InstallType {
    Texture icon ;
    String name, description ;
    
    Class <Installation> buildClass ;
    Constructor buildCons ;
    Installation instanced ;
  }
  
  
  void setupAllTypes() {
    final Batch <Class> buildClasses = LoadService.loadClassesInDir(
      "src/game/base", "src.game.base"
    ) ;
    for (Class buildClass : buildClasses) try {
      //
      //  Secondly, we need to ensure that the class refers to a type of venue
      //  and has an appropriate constructor.
      if (! Installation.class.isAssignableFrom(buildClass)) continue ;
      final Constructor cons = buildClass.getConstructor(Base.class) ;
      if (cons == null) continue ;
      //
      //  Finally, construct the building type:
      final InstallType type = new InstallType() ;
      type.buildClass = buildClass ;
      type.buildCons = cons ;
      refreshInstance(type) ;
      type.name = type.instanced.fullName() ;
      type.icon = type.instanced.portrait() ;
      type.description = type.instanced.helpInfo() ;
      if (type.name == null || type.icon == null || type.description == null)
        continue ;
      //
      //
      ///I.say("New building type added: "+buildClass.getName()) ;
      types.add(type) ;
    }
    catch (Exception e) { continue ; }// I.report(e) ; continue ; }
  }
  
  
  void refreshInstance(InstallType type) {
    try {
      type.instanced = (Installation) type.buildCons.newInstance(UI.played()) ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  
  /**  Interface presented-
    */
  protected void updateText() {
    detailText.setText("") ;
    for (final InstallType type : types) {
      detailText.append("\n") ;
      detailText.insert(type.icon, 40) ;
      detailText.append("  "+type.name) ;
      detailText.append("\n  ") ;
      detailText.append("\n") ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() { initBuildTask(type, UI.rendering) ; }
        public String fullName() { return "(BUILD)" ; }
      }) ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() { helpShown = type ; }
        public String fullName() { return "(INFO)" ; }
      }) ;
      if (helpShown == type) {
        detailText.append("\n") ;
        detailText.append(type.description) ;
      }
      detailText.append("\n") ;
    }
  }
  
  
  
  /**  Actual placement of buildings-
    */
  void initBuildTask(final InstallType type, final Rendering rendering) {
    I.say("Beginning build task...") ;
    final InstallTask task = new InstallTask() ;
    task.toInstall = type.instanced ;
    UI.setTask(task) ;
    refreshInstance(type) ;
  }
  
  
  class InstallTask implements UITask {
    
    
    Installation toInstall ;
    private boolean hasPressed = false ;
    Tile from, to ;
    
    
    public void doTask() {
      final Tile picked = UI.pickedTile() ;
      
      if (hasPressed) {
        if (picked != null) to = picked ;
      }
      else {
        if (picked != null) to = from = picked ;
        if (UI.mouseDown() && from != null) {
          hasPressed = true ;
        }
      }
      
      /*
      if (to == null) to = from ;
      if (UI.mouseDown() && from != null) hasPressed = true ;
      if (UI.mouseDragged()) {
        to = UI.pickedTile() ;
      }
      //*/
      
      final boolean canPlace = toInstall.pointsOkay(from, to) ;
      
      if (canPlace && hasPressed && ! UI.mouseDown()) {
        toInstall.doPlace(from, to) ;
        UI.endTask() ;
      }
      else {
        UI.rendering.clearDepth() ;
        toInstall.preview(canPlace, UI.rendering, from, to) ;
      }
    }
    
    public void cancelTask() {
      UI.endTask() ;
    }
    
    public Texture cursorImage() {
      return null ;
    }
  }
}




/*
UITask buildTask = new UITask() {
  
  public void doTask() {
    final World world = UI.played().world ;
    final Venue installed = type.instanced ;
    final Tile tile = UI.pickedTile() ;
    if (tile == null) return ;
    //
    //  Check that nothing in the underlying area is of higher priority.
    installed.setPosition(tile.x, tile.y, world) ;
    final Sprite s = installed.sprite() ;
    //final Box2D area = new Box2D().setTo(installed.area()) ;
    //area.expandBy(1) ;
    //final boolean canPlace = canPlace(world, installed, area) ;
    final boolean canPlace = installed.canPlace() ;
    s.colour = canPlace ? Colour.GREEN : Colour.RED ;
    installed.position(s.position) ;
    rendering.clearDepth() ;
    rendering.addClient(s) ;
    //
    //  
    if (canPlace && UI.mouseClicked()) {
      installed.clearSurrounds() ;
      //clearArea(world, area) ;
      s.colour = null ;
      installed.enterWorldAt(tile.x, tile.y, world) ;
      UI.endTask() ;
      refreshInstance(type) ;
    }
  }
  
  public void cancelTask() {
    UI.endTask() ;
  }
  
  public Texture cursorImage() {
    return type.icon ;
  }
} ;
//*/








