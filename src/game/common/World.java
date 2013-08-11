/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.graphics.widgets.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;
import src.game.common.WorldSections.Section ;



public class World {
  
  
  /**  Common fields, default constructors, and save/load methods-
    */
  final public static int
    
    SECTION_RESOLUTION  = 8,
    DEFAULT_SECTOR_SIZE = 16,
    
    DEFAULT_DAY_LENGTH  = 300,
    DEFAULT_YEAR_LENGTH = DEFAULT_DAY_LENGTH * 60,
    
    GROWTH_INTERVAL = DEFAULT_DAY_LENGTH ;
  
  
  final public int size ;
  final Tile tiles[][] ;
  final public WorldSections sections ;
  final public Schedule schedule ;
  
  private Terrain terrain ;
  private RandomScan growth ;  //Move to the Planet or Terrain class...
  
  private List <Base> bases = new List <Base> () ;
  private float currentTime ;
  private List <Mobile> mobiles = new List <Mobile> () ;  //This may be dispensible?
  
  final public Activities activities ;
  final public PathingCache pathingCache ;
  //final public Paving paving ;
  final public Presences presences ;
  final public Ephemera ephemera ;
  
  
  
  public World(Terrain terrain) {
    this(terrain.mapSize) ;
    this.terrain = terrain ;
    terrain.initPatchGrid(SECTION_RESOLUTION) ;
  }
  
  
  public World(int size) {
    this.size = size ;
    tiles = new Tile[size][size] ;
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y] = new Tile(this, c.x, c.y) ;
    }
    sections = new WorldSections(this, SECTION_RESOLUTION) ;
    schedule = new Schedule() ;
    growth = new RandomScan(size) {
      protected void scanAt(int x, int y) { growthAt(x, y) ; }
    } ;
    activities = new Activities(this) ;
    pathingCache = new PathingCache(this) ;
    presences = new Presences(this) ;
    //mobilesMap = new PresenceMap(this, Mobile.class) ;
    ephemera = new Ephemera(this) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    //
    //  We load the tile-states first, as other objects may depend on this.
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y].loadState(s) ;
    }
    currentTime = s.loadFloat() ;
    schedule.loadFrom(s) ;
    
    terrain = (Terrain) s.loadObject() ;
    terrain.initPatchGrid(SECTION_RESOLUTION) ;
    ///I.say("FINISHED LOADING TERRAIN") ;
    
    growth.loadState(s) ;
    s.loadObjects(bases) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      toggleActive((Mobile) s.loadObject(), true) ;
    }
    activities.loadState(s) ;
    presences.loadState(s) ;
    ephemera.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    //
    //  We save the tile-states first, as other objects may depend on this.
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y].saveState(s) ;
    }
    s.saveFloat(currentTime) ;
    schedule.saveTo(s) ;
    
    s.saveObject(terrain) ;
    growth.saveState(s) ;
    s.saveObjects(bases) ;
    s.saveInt(mobiles.size()) ;
    for (Mobile m : mobiles) s.saveObject(m) ;
    activities.saveState(s) ;
    presences.saveState(s) ;
    ephemera.saveState(s) ;
  }
  
  
  
  /**  Utility methods for visiting tiles at specific coordinates.
    */
  public Tile tileAt(float x, float y) {
    try { return tiles[(int) (x + 0.5f)][(int) (y + 0.5f)] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  public Tile tileAt(int x, int y) {
    try { return tiles[x][y] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  public Tile tileAt(Target t) {
    final Vec3D v = t.position(null) ;
    return tileAt(v.x, v.y) ;
  }
  
  
  public Iterable <Tile> tilesIn(Box2D area, boolean safe) {
    final Box2D b = new Box2D().setTo(area) ;
    if (safe) b.cropBy(new Box2D().set(-0.5f, -0.5f, size, size)) ;
    return new Visit <Tile> ().grid(
      (int) (b.xpos() + 0.5f),
      (int) (b.ypos() + 0.5f),
      (int) b.xdim(),
      (int) b.ydim(),
      tiles
    ) ;
  }
  
  
  public Batch <Element> fixturesFrom(Box2D area) {
    final Batch <Element> from = new Batch <Element> () ;
    for (Tile t : tilesIn(area, true)) {
      final Element o = t.owner() ;
      if (o != null && o.origin() == t) from.add(o) ;
    }
    return from ;
  }
  
  
  
  /**  Update methods.
    */
  public void updateWorld() {
    sections.updateBounds() ;
    if (! GameSettings.frozen) {
      currentTime += 1f / PlayLoop.UPDATES_PER_SECOND ;
      schedule.advanceSchedule(currentTime) ;
      float growIndex = (currentTime % GROWTH_INTERVAL) ;
      growIndex *= size * size * 1f / GROWTH_INTERVAL ;
      growth.scanThroughTo((int) growIndex) ;
      for (Mobile m : mobiles) m.updateAsMobile() ;
    }
  }
  
  
  protected void growthAt(final int x, final int y) {
    Flora.tryGrowthAt(x, y, this, false) ;
    final Element owner = tiles[x][y].owner() ;
    if (owner != null) owner.onGrowth() ;
  }
  
  
  protected void toggleActive(Mobile m, boolean is) {
    if (is) {
      m.setEntry(mobiles.addLast(m)) ;
      presences.togglePresence(m, m.origin(), true ) ;
    }
    else {
      mobiles.removeEntry(m.entry()) ;
      presences.togglePresence(m, m.origin(), false) ;
    }
  }
  
  
  protected void registerBase(Base base, boolean active) {
    if (active) {
      bases.include(base) ;
      schedule.scheduleForUpdates(base) ;
    }
    else {
      schedule.unschedule(base) ;
      bases.remove(base) ;
    }
  }
  
  
  public Terrain terrain() {
    return terrain ;
  }
  
  
  public float currentTime() {
    return currentTime ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public float timeWithinFrame() {
    return currentTime + (PlayLoop.frameTime() / PlayLoop.FRAMES_PER_SECOND) ;
  }
  
  
  public static interface Visible {
    void renderFor(Rendering r, Base b) ;
    Sprite sprite() ;
  }
  
  
  public Batch <Section> visibleSections(Rendering rendering) {
    final Batch <Section> visibleSections = new Batch <Section> () ;
    sections.compileVisible(rendering.port, null, visibleSections, null) ;
    return visibleSections ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //
    //  First, we obtain lists of all current visible fixtures, actors, and
    //  terrain sections.
    final Batch <Section> visibleSections = new Batch <Section> () ;
    final List <Visible> allVisible = new List <Visible> () {
      protected float queuePriority(Visible e) {
        return e.sprite().depth ;
      }
    } ;
    sections.compileVisible(
      rendering.port, base,
      visibleSections, allVisible
    ) ;
    //
    //  We also render visible mobiles and ghosted SFX-
    Vec3D viewPos = new Vec3D() ;
    float viewRad = -1 ;
    for (Mobile active : this.mobiles) {
      if (active.sprite() == null || ! active.visibleTo(base)) continue ;
      active.viewPosition(viewPos) ;
      viewRad = (active.height() / 2) + active.radius() ;
      if (rendering.port.intersects(viewPos, viewRad)) {
        allVisible.add(active) ;
      }
    }
    for (Visible ghost : ephemera.visibleFor(rendering)) {
      allVisible.add(ghost) ;
    }
    //
    //  Then we register their associated media for rendering, in the correctly
    //  sorted order.
    for (Section section : visibleSections) {
      terrain.renderFor(section.area, rendering, currentTime) ;
      if (! GameSettings.noFog) {
        terrain.renderFogFor(section.area, base.intelMap.fogTex(), rendering) ;
      }
    }
    rendering.clearDepth() ;
    Vec3D deep = new Vec3D() ;
    for (Visible visible : allVisible) {
      final Sprite sprite = visible.sprite() ;
      rendering.port.viewMatrix(deep.setTo(sprite.position)) ;
      sprite.depth = 0 - deep.z ;
    }
    allVisible.queueSort() ;
    for (Visible visible : allVisible) {
      visible.renderFor(rendering, base) ;
    }
  }
  
  
  public Vec3D pickedGroundPoint(final HUD UI, final Viewport port) {
    //
    //  Here, we find the point of intersection between the line-of-sight
    //  underneath the mouse cursor, and the plane of the ground-
    final Vec3D origin = new Vec3D(UI.mouseX(), UI.mouseY(), 0) ;
    port.screenToIso(origin) ;
    final Vec3D vector = new Vec3D(0, 0, 1) ;
    port.viewInvert(vector) ;
    return origin.add(vector, 0 - origin.z / vector.z, null) ;
  }
  
  
  public Tile pickedTile(final HUD UI, final Viewport port) {
    final Vec3D onGround = pickedGroundPoint(UI, port) ;
    return tileAt(onGround.x, onGround.y) ;
  }
  
  
  public Fixture pickedFixture(final HUD UI, final Viewport port) {
    final Tile t = pickedTile(UI, port) ;
    if (t == null) return null ;
    if (t.owner() instanceof Fixture) return (Fixture) t.owner() ;
    return null ;
  }
  
  
  public Mobile pickedMobile(final HUD UI, final Viewport port) {
    //
    //  You may want to use some pre-emptive culling here in future.
    Mobile nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Mobile m : mobiles) {
      if (m.indoors() || ! (m instanceof Selectable)) continue ;
      final float selRad = (m.height() + m.radius()) / 2 ;
      final Vec3D selPos = m.viewPosition(null) ;
      if (! port.mouseIntersects(selPos, selRad, UI)) continue ;
      final float dist = port.isoToFlat(selPos).z ;
      if (dist < minDist) { nearest = m ; minDist = dist ; }
    }
    return nearest ;
  }
}













