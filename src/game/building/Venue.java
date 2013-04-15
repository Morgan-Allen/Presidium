/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public abstract class Venue extends Fixture implements
  Inventory.Owner, Behaviour, Boardable, WorldSchedule.Updates
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
  List <RoadNetwork.Route> routes = new List <RoadNetwork.Route> () ;
  
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
    //*
    entranceFace = s.loadInt() ;
    entrance = (Tile) s.loadTarget() ;
    base = (Base) s.loadObject() ;
    s.loadObjects(inside) ;
    
    stocks.loadState(s) ;
    personnel.loadState(s) ;
    orders.loadState(s) ;
    //*/
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    //*
    s.saveInt(entranceFace) ;
    s.saveTarget(entrance) ;
    s.saveObject(base) ;
    s.saveObjects(inside) ;
    
    stocks.saveState(s) ;
    personnel.saveState(s) ;
    orders.saveState(s) ;
    //*/
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
  
  
  
  /**  Installation and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    final Box2D around = new Box2D().setTo(area()).expandBy(1) ;
    for (Tile t : origin().world.tilesIn(around, false)) {
      if (t == null || ! t.habitat().pathClear) return false ;
      if (t.owningType() >= this.owningType()) return false ;
    }
    return true ;
  }
  
  
  public void clearSurrounds() {
    final Box2D around = new Box2D().setTo(area()).expandBy(1) ;
    final World world = origin().world ;
    final Tile eT = entrance() ;
    for (Tile t : world.tilesIn(around, false)) {
      if (t.owner() != null) t.owner().exitWorld() ;
    }
    for (Tile t : world.tilesIn(area(), false)) {
      for (Mobile m : t.inside()) m.setPosition(eT.x, eT.y, world) ;
    }
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.schedule.scheduleForUpdates(this) ;
    if (base != null) base.toggleBelongs(this, true) ;
    //  TODO:  This is a temporary measure.  Abolish later.
    for (Vocation v : careers()) personnel.recruitWorker(v) ;
  }
  
  
  public void exitWorld() {
    if (base != null) base.toggleBelongs(this, false) ;
    world.schedule.unschedule(this) ;
    //  Consider moving this to the personnel class?
    for (Citizen c : personnel.workers()  ) c.setWorkVenue(null) ;
    for (Citizen c : personnel.residents()) c.setHomeVenue(null) ;
    super.exitWorld() ;
  }
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    final int off[] = Spacing.entranceCoords(size, size, entranceFace) ;
    entrance = world.tileAt(o.x + off[0], o.y + off[1]) ;
  }
  
  
  public Tile entrance() {
    return entrance ;
  }
  
  
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled() {
    orders.updateOrders() ;
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
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[1] ;
    else for (int i = batch.length ; i-- > 0 ;) batch[i] = null ;
    batch[0] = entrance() ;
    return batch ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Tile o = origin() ;
    put.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
    return put ;
  }
  
  
  public boolean usesRoads() {
    return true ;
  }
  
  
  protected List <RoadNetwork.Route> routes() {
    return routes ;
  }
  
  

  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public Behaviour nextStepFor(Actor actor) {
    return null ;
  }
  
  
  public float priorityFor(Actor actor) {
    //  Have a system of shifts?
    return ROUTINE ;
  }
  
  
  public boolean complete() {
    return false ;
  }
  
  
  public boolean monitor(Actor actor) {
    return false ;
  }
  
  
  public void abortStep() {}
  
  
  protected abstract Vocation[] careers() ;
  protected abstract Item.Type[] itemsMade() ;
  
  public Object[] services() {
    return itemsMade() ;
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
    /*
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
    //*/
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
}















