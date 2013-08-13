/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.graphics.cutout.* ;
import src.graphics.sfx.* ;
import src.user.* ;
import src.util.* ;



public abstract class Venue extends Fixture implements
  Schedule.Updates, Boardable, Installation,
  Inventory.Owner, CitizenAI.Employment,
  Selectable
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    ENTRANCE_NORTH =  0,
    ENTRANCE_EAST  =  1,
    ENTRANCE_SOUTH =  2,
    ENTRANCE_WEST  =  3,
    ENTRANCE_NONE  = -1,
    NUM_SIDES      =  4 ;
  
  
  BuildingSprite buildSprite ;
  Healthbar healthbar ;
  
  int entranceFace ;
  Tile entrance ;
  
  Base base ;
  List <Mobile> inside = new List <Mobile> () ;
  
  final public Inventory stocks = new Inventory(this) ;
  final public VenuePersonnel personnel = new VenuePersonnel(this) ;
  final public VenueOrders orders = new VenueOrders(this) ;
  final public VenueStructure structure = new VenueStructure(this) ;
  
  
  
  public Venue(int size, int high, int entranceFace, Base base) {
    super(size, high) ;
    this.base = base ;
    this.entranceFace = entranceFace ;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s) ;
    buildSprite = (BuildingSprite) sprite() ;
    
    entranceFace = s.loadInt() ;
    entrance = (Tile) s.loadTarget() ;
    base = (Base) s.loadObject() ;
    s.loadObjects(inside) ;
    
    stocks.loadState(s) ;
    personnel.loadState(s) ;
    orders.loadState(s) ;
    structure.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveInt(entranceFace) ;
    s.saveTarget(entrance) ;
    s.saveObject(base) ;
    s.saveObjects(inside) ;
    
    stocks.saveState(s) ;
    personnel.saveState(s) ;
    orders.saveState(s) ;
    structure.saveState(s) ;
  }
  
  
  public int owningType() { return VENUE_OWNS ; }
  public Inventory inventory() { return stocks ; }
  public Base base() { return base ; }
  protected Index allUpgrades() { return null ; }
  
  
  
  /**  Installation and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    final World world = origin().world ;
    
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.owningType() >= owningType()) return false ;
    }
    //
    //  ...I'm not sure I remember what's happening here.  Figure out?
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
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    final int off[] = Spacing.entranceCoords(size, size, entranceFace) ;
    entrance = world.tileAt(o.x + off[0], o.y + off[1]) ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.presences.togglePresence(this, true , services()) ;
    if (base != null) updatePaving(true) ;
    world.schedule.scheduleForUpdates(this) ;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, false, services()) ;
    if (base != null) updatePaving(false) ;
    world.schedule.unschedule(this) ;
    super.exitWorld() ;
  }
  
  
  public void setAsEstablished(boolean isDone) {
    super.setAsEstablished(isDone) ;
    ///if (isDone) structure.setState(VenueStructure.STATE_INTACT, 1.0f) ;
  }
  
  
  public void onCompletion() {
    world.ephemera.addGhost(origin(), size, buildSprite.scaffolding()) ;
    setAsEstablished(false) ;
    personnel.onCompletion() ;
  }
  
  
  public void onDecommission() {
    world.ephemera.addGhost(origin(), size, buildSprite.baseSprite()) ;
    setAsEstablished(false) ;
    personnel.onDecommission() ;
  }
  
  
  public void setAsDestroyed() {
    super.setAsDestroyed() ;
    personnel.onDecommission() ;
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
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Tile o = origin() ;
    put.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
    return put ;
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
  
  
  
  /**  Updates and life cycle-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    if (numUpdates % 10 == 0) {
      orders.updateOrders() ;
      if (base != null) updatePaving(true) ;
    }
    structure.updateStructure(numUpdates) ;
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
  
  
  
  /**  Recruiting staff and assigning manufacturing tasks-
    */
  //
  //  TODO:  You may want to get rid of or relocate this, since the plan is to
  //  use upgrades for recruitment.
  public void setWorker(Actor actor, boolean is) {
    personnel.setWorker(actor, is) ;
  }
  
  
  public int numOpenings(Vocation v) {
    int num = 0 ;
    ///for (Upgrade u : structure.upgrades) if (u.refers == v) num++ ;
    return num ;
  }
  
  
  //  Careers, services, goods produced, and goods required.  Space has to be
  //  reserved for both item-orders and service-seating.  And what about shifts
  //  at work?
  protected abstract Vocation[] careers() ;
  protected abstract Item.Type[] services() ;
  
  
  
  /**  Installation interface-
    */
  public boolean pointsOkay(Tile from, Tile to) {
    //  You have to check for visibility too.  Have a Base argument?
    if (from == null) return false ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    return canPlace() ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (sprite() != null) sprite().colour = null ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    clearSurrounds() ;
    enterWorld() ;
    if (GameSettings.buildFree) {
      structure.setState(VenueStructure.STATE_INTACT , 1) ;
    }
    else {
      structure.setState(VenueStructure.STATE_INSTALL, 0) ;
    }
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
  protected void attachSprite(Sprite sprite) {
    if (! (sprite instanceof ImageSprite)) {
      I.complain("VENUES MUST HAVE IMAGE-BASED SPRITES!") ;
    }
    buildSprite = new BuildingSprite(sprite, size, high) ;
    super.attachSprite(buildSprite) ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //final Sprite s = sprite() ;
    position(buildSprite.position) ;
    buildSprite.updateCondition(structure.repairLevel(), structure.complete()) ;
    
    if (healthbar == null) healthbar = new Healthbar() ;
    healthbar.level = structure.repairLevel() ;
    healthbar.size = structure.maxIntegrity() ;
    healthbar.matchTo(buildSprite) ;
    healthbar.position.z += height() ;
    rendering.addClient(healthbar) ;
    
    super.renderFor(rendering, base) ;
  }


  public String toString() {
    return fullName() ;
  }
  
  
  
  public String[] infoCategories() {
    //  Condition, stocks, personnel and upgrades.  Okay.
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    
    d.append("INTEGRITY: ") ;
    d.append(structure.integrity()+" / "+structure.maxIntegrity()) ;

    //  TODO:  List Applicants here as well, whether local or offworld.
    d.append("\n\nPERSONNEL:") ;
    if (personnel.workers().size() == 0) d.append("\n  No workers.") ;
    else for (Actor a : personnel.workers()) {
      d.append("\n  ") ; d.append(a) ;
    }
    d.append("\n\nVISITORS:") ;
    if (inside.size() == 0) d.append("\n  No visitors.") ;
    else for (Mobile m : inside) if (m instanceof Actor) {
      d.append("\n  ") ; d.append(m) ;
    }
    
    if (! stocks.empty()) d.append("\n\nCURRENT STOCKS:") ;
    stocks.writeInformation(d) ;
    /*
    d.append("\n\nCURRENT ORDERS:") ;
    orders.writeInformation(d) ;
    //*/
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Working at the ") ;
    d.append(this) ;
  }
  
  
  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).selection.setSelected(this) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_SQUARE
    ) ;
  }
  
  
  public Target subject() {
    return this ;
  }
}















