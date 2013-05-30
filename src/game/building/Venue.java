/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.user.* ;
import src.util.* ;



//
//  Get rid of the entrances() and isEntrance() methods.  Just use the
//  Boardable interface and equivalents within.

public abstract class Venue extends Fixture implements
  Schedule.Updates, Boardable, Installation,
  Inventory.Owner, Citizen.Employment,// Paving.Hub,
  Selectable
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    ENTRANCE_NORTH = 0,
    ENTRANCE_EAST  = 1,
    ENTRANCE_SOUTH = 2,
    ENTRANCE_WEST  = 3,
    ENTRANCE_NONE  = -1,
    NUM_SIDES      = 4 ;
  
  
  int entranceFace ;
  Tile entrance ;
  
  Base base ;
  List <Mobile> inside = new List <Mobile> () ;
  //final public Paving paving = new Paving(this) ;
  
  final public Inventory stocks = new Inventory(this) ;
  final public VenuePersonnel personnel = new VenuePersonnel(this) ;
  final public VenueOrders orders = new VenueOrders(this) ;
  
  
  public Venue(int size, int high, int entranceFace, Base base) {
    super(size, high) ;
    this.base = base ;
    this.entranceFace = entranceFace ;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s) ;
    
    entranceFace = s.loadInt() ;
    entrance = (Tile) s.loadTarget() ;
    base = (Base) s.loadObject() ;
    s.loadObjects(inside) ;
    //paving.loadState(s) ;
    
    stocks.loadState(s) ;
    personnel.loadState(s) ;
    orders.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveInt(entranceFace) ;
    s.saveTarget(entrance) ;
    s.saveObject(base) ;
    s.saveObjects(inside) ;
    //paving.saveState(s) ;
    
    stocks.saveState(s) ;
    personnel.saveState(s) ;
    orders.saveState(s) ;
  }
  
  
  public int owningType() {
    return VENUE_OWNS ;
  }
  
  
  public Inventory inventory() {
    return stocks ;
  }
  
  
  public Base base() {
    return base ;
  }
  
  /*
  public Paving paving() {
    return paving ;
  }
  //*/
  
  
  
  /**  Installation and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    final World world = origin().world ;
    
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.owningType() >= owningType()) return false ;
    }
    final Boardable tempB[] = new Boardable[4] ;
    final Box2D tempA = new Box2D() ;
    for (Tile n : Spacing.perimeter(area(), world)) {
      if (n == null) return false ;
      if (! (n.owner() instanceof Boardable)) continue ;
      for (Boardable b : ((Boardable) n.owner()).canBoard(tempB)) {
        if (b == null) continue ;
        if (area().intersects(b.area(tempA))) return false ;
      }
    }
    
    if (! Spacing.perimeterFits(this)) return false ;
    if (mainEntrance().owningType() >= owningType()) return false ;
    return true ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    if (base != null) {
      base.toggleBelongs(this, true) ;
      updatePaving(true) ;
    }
    world.schedule.scheduleForUpdates(this) ;
    personnel.onWorldEntry() ;
    //paving.onWorldEntry() ;
  }
  
  
  public void exitWorld() {
    if (base != null) {
      base.toggleBelongs(this, false) ;
      updatePaving(false) ;
    }
    world.schedule.unschedule(this) ;
    personnel.onWorldExit() ;
    //paving.onWorldExit() ;
    super.exitWorld() ;
  }
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    final int off[] = Spacing.entranceCoords(size, size, entranceFace) ;
    entrance = world.tileAt(o.x + off[0], o.y + off[1]) ;
  }
  
  
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    if (base != null && numUpdates % 10 == 0) {
      orders.updateOrders() ;
      updatePaving(true) ;
    }
    //if (usesRoads() && numUpdates % 10 == 0) paving.updateRoutes() ;
  }

  
  
  /**  Implementing the Boardable interface-
    */
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
    }
  }
  
  
  public List <Mobile> inside() {
    return inside ;
  }
  
  
  public Tile mainEntrance() {
    return entrance ;
  }
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[1] ;
    else for (int i = batch.length ; i-- > 1 ;) batch[i] = null ;
    batch[0] = entrance ;
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable t) {
    return entrance == t ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.assignedBase() == base ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Tile o = origin() ;
    put.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
    return put ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    final Tile perim[] = Spacing.perimeter(area(), world) ;
    if (inWorld) {
      base.paving.updateJunction(mainEntrance(), true) ;
      //base.paving.toggleJunction(this, mainEntrance(), true) ;
      Paving.clearRoad(perim) ;
      world.terrain().maskAsPaved(perim, true) ;
    }
    else {
      base.paving.updateJunction(mainEntrance(), false) ;
      //base.paving.toggleJunction(this, mainEntrance(), true) ;
      world.terrain().maskAsPaved(perim, false) ;
    }
  }
  
  /*
  public boolean usesRoads() {
    return true ;
  }
  //*/
  
  

  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public void setWorker(Citizen actor, boolean is) {
    personnel.setWorker(actor, is) ;
  }
  
  protected abstract Vocation[] careers() ;
  protected abstract Item.Type[] goods() ;
  
  public Object[] services() {
    return goods() ;
  }
  
  
  
  /**  Installation interface-
    */
  public boolean pointsOkay(Tile from, Tile to) {
    if (from == null) return false ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    return canPlace() ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    clearSurrounds() ;
    enterWorld() ;
    if (sprite() != null) sprite().colour = null ;
  }


  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {
    if (from == null) return ;
    final Tile t = from ;
    final World world = t.world ;
    setPosition(t.x, t.y, t.world) ;
    final TerrainMesh overlay = world.terrain().createOverlay(
      world, surrounds(), false, Texture.WHITE_TEX
    ) ;
    overlay.colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(overlay) ;
    
    if (sprite() == null) return ;
    this.position(sprite().position) ;
    sprite().colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(sprite()) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String toString() {
    return fullName() ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID) {
    d.append("PERSONNEL:") ;
    
    final Vocation c[] = careers() ;
    if (c != null) for (final Vocation v : c) {
      d.append(new UserOption() {
        
        public void whenClicked() {
          personnel.recruitWorker(v) ;
        }
        
        public String fullName() {
          return "\n  Hire "+v.name ;
        }
      }) ;
    }
    for (Actor a : personnel.workers()) {
      d.append("\n  ") ;
      d.append(a) ;
    }
    
    if (! stocks.empty()) d.append("\n\nCURRENT STOCKS:") ;
    stocks.writeInformation(d) ;
    
    d.append("\n\nCURRENT ORDERS:") ;
    orders.writeInformation(d) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Working at the ") ;
    d.append(this) ;
  }
  
  
  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).setSelection(this) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
}















