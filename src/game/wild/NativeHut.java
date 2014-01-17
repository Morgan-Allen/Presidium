


package src.game.wild ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;


//  You need openings for Hunters, Gatherers, and Chieftains.
//  Plus the Medicine Man, Marked One and Cargo Cultist.

//  Do I need a wider selection of structures?  ...For the moment.  For the
//  sake of safety.  Might expand on functions later.


public class NativeHut extends Venue {
  
  
  final static String IMG_DIR = "media/Buildings/lairs and ruins/" ;
  final static Model
    HUT_MODELS[][] = ImageModel.fromTextureGrid(
      NativeHut.class,
      Texture.loadTexture(IMG_DIR+"all_native_huts.png"),
      3, 3, 1.0f, ImageModel.TYPE_SOLID_BOX
    ) ;
  
  final static String TRIBE_NAMES[] = {
    //"Homaxquin (Cloud Eaters)",
    "Kon'E (Children of Rust)",
    "Ai Baru (Sand Runners)",
    "Ybetsi (The Painted)"
  } ;
  
  final static ImageModel[][][] HABITAT_KEYS = {
    //Habitat.TUNDRA_FLORA_MODELS,
    Habitat.WASTES_FLORA_MODELS,
    Habitat.DESERT_FLORA_MODELS,
    Habitat.FOREST_FLORA_MODELS,
  } ;
  private static int nextVar = 0 ;
  
  
  final public static int
    TRIBE_WASTES = 0,
    TRIBE_DESERT = 1,
    TRIBE_FOREST = 2,
    TYPE_HUT    = 0,
    TYPE_HALL   = 1,
    TYPE_SHRINE = 2 ;
  
  final int type, tribeID ;
  
  
  
  public static NativeHut newHut(NativeHall parent) {
    final NativeHut hut = new NativeHut(
      3, 2, TYPE_HUT, parent.tribeID, parent.base()
    ) ;
    parent.children.include(hut) ;
    return hut ;
  }
  
  
  public static NativeHall newHall(int tribeID, Base base) {
    return new NativeHall(tribeID, base) ;
  }
  
  
  
  protected NativeHut(
    int size, int height, int type, int tribeID, Base base
  ) {
    super(size, height, ENTRANCE_SOUTH, base) ;
    this.type = type ;
    this.tribeID = tribeID ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    
    final int varID = nextVar++ % 2 ;
    Model model = null ;
    if (type == TYPE_HUT ) model = HUT_MODELS[tribeID][varID] ;
    if (type == TYPE_HALL) model = HUT_MODELS[tribeID][2] ;
    attachModel(model) ;
    sprite().scale = size ;
  }
  
  
  
  public NativeHut(Session s) throws Exception {
    super(s) ;
    this.type = s.loadInt() ;
    this.tribeID = s.loadInt() ;
    sprite().scale = size ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(type) ;
    s.saveInt(tribeID) ;
  }
  
  
  
  /**  Placement and construction-
    */
  
  //  Chieftain's Halls need to assess the total fertility of the surrounding
  //  area and constrain populations accordingly (or go to war.)
  
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  
  
  protected void updatePaving(boolean inWorld) {
    if (! inWorld) {
      base().paving.updatePerimeter(this, null, false) ;
      return ;
    }
    
    final Batch <Tile> toPave = new Batch <Tile> () ;
    for (Tile t : Spacing.perimeter(area(), world)) {
      if (t.blocked()) continue ;
      boolean between = false ;
      for (int n : N_INDEX) {
        final int o = (n + 4) % 8 ;
        final Tile
          a = world.tileAt(t.x + N_X[n], t.y + N_Y[n]),
          b = world.tileAt(t.x + N_X[o], t.y + N_Y[o]) ;
        between =
          (a != null && a.owner() instanceof NativeHut) &&
          (b != null && b.owner() instanceof NativeHut) ;
        if (between) break ;
      }
      if (between) toPave.add(t) ;
    }
    
    base().paving.updatePerimeter(this, toPave, true) ;
  }



  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Native Hutment" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Native Hutments are simple but robust shelters constructed from local "+
      "materials by indigenous primitives." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_HIDDEN ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}





