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
  final List <BuildingType> types = new List <BuildingType> () ;
  
  
  BuildingsTab(BaseUI UI) {
    super(UI, null) ;
    this.UI = UI ;
    setupAllTypes() ;
  }
  

  
  /**  Compiling the list of all building types-
    */
  static class BuildingType {
    Texture icon ;
    String name, description ;
    
    Class <Venue> buildClass ;
    Constructor buildCons ;
    Venue instanced ;
  }
  
  
  void setupAllTypes() {
    final Batch <Class> buildClasses = LoadService.loadClassesInDir(
      "src/game/base", "src.game.base"
    ) ;
    for (Class buildClass : buildClasses) try {
      //
      //  Secondly, we need to ensure that the class refers to a type of venue
      //  and has an appropriate constructor.
      if (! Venue.class.isAssignableFrom(buildClass)) continue ;
      final Constructor cons = buildClass.getConstructor(Base.class) ;
      if (cons == null) continue ;
      //
      //  Finally, construct the building type:
      final BuildingType type = new BuildingType() ;
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
  
  
  void refreshInstance(BuildingType type) {
    try {
      type.instanced = (Venue) type.buildCons.newInstance(UI.played()) ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  
  /**  Interface presented-
    */
  protected void updateText() {
    detailText.setText("") ;
    for (final BuildingType type : types) {
      detailText.append("\n") ;
      detailText.insert(type.icon, 40) ;
      detailText.append("  "+type.name) ;
      detailText.append("\n  ") ;
      //
      //
      detailText.append("\n") ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() { initBuildTask(type, UI.rendering) ; }
        public String fullName() { return "(BUILD)" ; }
      }) ;
      detailText.append("\n") ;
      detailText.append(type.description) ;
      /*
      detailText.append(new Text.Clickable() {
        public void whenClicked() {
          
        }
        public String fullName() { return "(INFO)" ; }
      }) ;
      //*/
      detailText.append("\n") ;
      //
      //
    }
  }
  
  
  
  /**  Actual placement of buildings-
    */
  void initBuildTask(final BuildingType type, final Rendering rendering) {
    I.say("Beginning build task...") ;
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
    UI.setTask(buildTask) ;
  }
  
  
  /*
  private boolean canPlace(World world, Venue installed, Box2D area) {
    //  You also need to ensure there are no actors in the area, or expel them
    //  if they are.
    for (Tile t : world.tilesIn(area, false)) {
      if (t == null || ! t.habitat().pathClear) return false ;
      final Element e = t.owner() ;
      if (e != null && e.owningType() >= installed.owningType()) return false ;
    }
    return true ;
  }
  
  
  private void clearArea(World world, Box2D area) {
    for (Tile t : world.tilesIn(area, false)) {
      final Element e = t.owner() ;
      if (e != null) e.exitWorld() ;
    }
  }
  //*/
}









