/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.building.* ;
import src.game.common.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import java.lang.reflect.* ;



public class InstallTab extends InfoPanel {
  
  
  /**  Constant definitions, data fields and constructors-
    */
  final public static String
    TYPE_MILITARY  = "military",
    TYPE_COMMERCE  = "commerce",
    TYPE_AESTHETE  = "aesthete",
    TYPE_ARTIFICER = "artificer",
    TYPE_ECOLOGIST = "ecologist",
    TYPE_PHYSICIAN = "physician",
    
    TYPE_SCHOOL    = "school",
    TYPE_PRESERVE  = "preserve",
    
    TYPE_HIDDEN    = "hidden" ;
  final static int
    BUTTONS_ACROSS = 3,
    BUTTONS_HEIGHT = 40 ;
  
  
  private class Category {
    String name ;
    final List <InstallType> types = new List <InstallType> () ;
  }
  
  
  private class InstallType {
    Composite icon ;
    String name, description, category ;
    
    Class <Installation> buildClass ;
    Constructor buildCons ;
    Installation instanced ;
  }
  
  
  final BaseUI UI ;
  Button categoryButtons[] ;
  
  final Table <String, Category> categories = new Table <String, Category> () ;
  final Batch <InstallType> allTypes = new Batch <InstallType> () ;
  boolean instanced = false ;
  
  InstallType helpShown = null, listShown = null ;
  Category currentCategory = null, hoveredCategory = null ;
  
  
  InstallTab(BaseUI UI) {
    super(UI, null, BUTTONS_HEIGHT * 2) ;
    this.UI = UI ;
    setupCategories() ;
    setupInstallTypes() ;
  }
  

  
  /**  Compiling the list of all building types-
    */
  void setupCategories() {
    categoryButtons = new Button[] {
      buttonFor(TYPE_MILITARY , "military_category_button" , 0, 0),
      buttonFor(TYPE_COMMERCE , "commerce_category_button" , 1, 0),
      buttonFor(TYPE_AESTHETE , "aesthete_category_button" , 2, 0),
      buttonFor(TYPE_ARTIFICER, "artificer_category_button", 0, 1),
      buttonFor(TYPE_ECOLOGIST, "ecologist_category_button", 1, 1),
      buttonFor(TYPE_PHYSICIAN, "physician_category_button", 2, 1)
    } ;
  }
  
  
  private Button buttonFor(final String typeID, String img, int a, int d) {
    final String IMG_DIR = "media/GUI/Buttons/" ;
    final Button button = new Button(UI, IMG_DIR+img+".png", typeID) {
      protected void whenClicked() {
        currentCategory = categories.get(typeID) ;
      }
      protected void whenHovered() {
        hoveredCategory = categories.get(typeID) ;
      }
    } ;
    button.relBound.set(a * 1f / BUTTONS_ACROSS, 1, 1f / BUTTONS_ACROSS, 0) ;
    button.absBound.set(0, (d + 1) * -BUTTONS_HEIGHT, 0, BUTTONS_HEIGHT) ;
    button.attachTo(this) ;
    button.stretch = true ;
    final Category category = new Category() ;
    category.name = typeID ;
    categories.put(typeID, category) ;
    return button ;
  }
  
  
  void setupInstallTypes() {
    final Batch <Class> buildClasses = LoadService.loadClassesInDir(
      "src/game/base", "src.game.base"
    ) ;
    for (Class buildClass : buildClasses) {
      //
      //  Firstly, we need to ensure that the class refers to a type of venue
      //  and has an appropriate constructor.
      if (! Installation.class.isAssignableFrom(buildClass)) continue ;
      final Constructor cons ;
      try { cons = buildClass.getConstructor(Base.class) ; }
      catch (Exception e) { continue ; }
      //
      //  Secondly, construct the building type with an appropriate instance.
      final InstallType type = new InstallType() ;
      type.buildClass = buildClass ;
      type.buildCons = cons ;
      allTypes.add(type) ;
    }
  }
  
  
  void refreshInstance(InstallType type) {
    try {
      type.instanced = (Installation) type.buildCons.newInstance(UI.played()) ;
    }
    catch (Exception e) {
      I.say("PROBLEM REFRESHING INSTANCE OF: "+type.buildCons.getName()) ;
      I.report(e) ;
    }
  }
  
  
  protected void updateState() {
    //  TODO:  Refresh this every frame, or when the current base changes?
    if (! instanced) {
      //I.say("Refreshing build-type instances...") ;
      for (InstallType type : allTypes) {
        refreshInstance(type) ;
        //I.say("Checking build type: "+type.buildClass.getSimpleName()) ;
        final Installation instanced = type.instanced ;
        //
        //  Thirdly, ensure that this structure has appropriate UI data:
        if (
          (type.name        = instanced.fullName()     ) == null ||
          (type.icon        = instanced.portrait(UI)   ) == null ||
          (type.description = instanced.helpInfo()     ) == null ||
          (type.category    = instanced.buildCategory()) == null
        ) {
          I.say("UI information missing: "+type.buildClass.getSimpleName()) ;
          continue ;
        }
        else I.say("Adding build type: "+type.buildClass.getSimpleName()) ;
        //
        //  Finally, determine which category this structure belongs to-
        final Category match = categories.get(type.category) ;
        if (match == null) continue ;
        match.types.add(type) ;
      }
      instanced = true ;
    }
    super.updateState() ;
  }
  
  
  
  /**  Interface presented-
    */
  protected void updateText() {
    detailText.setText("") ;
    headerText.setText("") ;
    if (! Visit.arrayIncludes(categoryButtons, UI.selected())) {
      hoveredCategory = null ;
    }
    if (currentCategory == null) return ;
    
    final String name = currentCategory.name.toUpperCase() ;
    headerText.setText(name+" STRUCTURES") ;
    
    for (final InstallType type : currentCategory.types) {
      detailText.insert(type.icon, 40) ;
      detailText.append("  "+type.name) ;
      
      detailText.append(new Text.Clickable() {
        public void whenClicked() { initInstallTask(type) ; }
        public String fullName() { return "\n  (BUILD)" ; }
      }) ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() {
          listShown = (type == listShown) ? null : type ;
          helpShown = null ;
        }
        public String fullName() { return "  (LIST)" ; }
      }) ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() {
          helpShown = (type == helpShown) ? null : type ;
          listShown = null ;
        }
        public String fullName() { return "  (INFO)" ; }
      }) ;
      
      if (helpShown == type) {
        detailText.append("\n") ;
        detailText.append(type.description) ;
        detailText.append("\n") ;
      }
      if (listShown == type) {
        final Batch <Installation> installed = listInstalled(type) ;
        int ID = 0 ;
        if (installed.size() == 0) {
          detailText.append("\n  (no current installations)") ;
        }
        else for (Installation i : installed) {
          detailText.append("\n    ") ;
          final String label = i.fullName()+" No. "+(++ID) ;
          detailText.append(label, (Text.Clickable) i) ;
          //  You might also list location.
        }
        detailText.append("\n") ;
      }
      
      detailText.append("\n") ;
    }
  }
  
  
  Batch <Installation> listInstalled(InstallType type) {
    Batch <Installation> installed = new Batch <Installation> () ;
    final Tile zero = UI.world().tileAt(0, 0) ;
    for (Object o : UI.played().servicesNear(type.buildClass, zero, -1)) {
      if (! (o instanceof Installation)) continue ;
      installed.add((Installation) o) ;
    }
    return installed ;
  }
  
  
  /**  Actual placement of buildings-
    */
  void initInstallTask(InstallType type) {
    ///I.say("Beginning build task...") ;
    final InstallTask task = new InstallTask() ;
    task.toInstall = type.instanced ;
    UI.beginTask(task) ;
    refreshInstance(type) ;
  }
  
  
  class InstallTask implements UITask {
    
    
    Installation toInstall ;
    private boolean hasPressed = false ;
    Tile from, to ;
    
    
    public void doTask() {
      final Tile picked = UI.selection.pickedTile() ;
      
      if (hasPressed) {
        if (picked != null) to = picked ;
      }
      else {
        if (picked != null) to = from = picked ;
        if (UI.mouseDown() && from != null) {
          hasPressed = true ;
        }
      }
      final boolean canPlace = toInstall.pointsOkay(from, to) ;
      
      if (canPlace && hasPressed && ! UI.mouseDown()) {
        toInstall.doPlace(from, to) ;
        UI.endCurrentTask() ;
      }
      else {
        UI.rendering.clearDepth() ;
        toInstall.preview(canPlace, UI.rendering, from, to) ;
      }
    }
    
    public void cancelTask() {
      UI.endCurrentTask() ;
    }
    
    public Texture cursorImage() {
      return null ;
    }
  }
}








