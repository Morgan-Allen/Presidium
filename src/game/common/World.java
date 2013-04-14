/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.planet.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.game.actors.Activities;
import src.game.common.WorldSections.Section ;
import src.game.common.WorldSections.Descent ;
import src.graphics.widgets.* ;



public class World {
  
  
  
  /**  Common fields, default constructors, and save/load methods-
    */
  final public static int
    SECTION_RESOLUTION = 4 ;
  
  
  final public int size ;
  final Tile tiles[][] ;
  final public WorldSections sections ;
  final public WorldSchedule schedule ;
  
  private Terrain terrain ;
  private RandomScan growth ;
  
  private List <Mobile> active = new List <Mobile> () ;
  private float currentTime ;
  final public Activities activities ;
  

  
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
    schedule = new WorldSchedule() ;
    growth = new RandomScan(size) {
      protected void scanAt(int x, int y) { growthAt(x, y) ; }
    } ;
    activities = new Activities(this) ;
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
    growth.loadState(s) ;
    
    for (int n = s.loadInt() ; n-- > 0 ;) {
      toggleActive((Mobile) s.loadObject(), true) ;
    }
    activities.loadState(s) ;
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
    
    s.saveInt(active.size()) ;
    for (Mobile m : active) s.saveObject(m) ;
    activities.saveState(s) ;
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
  
  
  public Terrain terrain() {
    return terrain ;
  }
  
  
  public float currentTime() {
    return currentTime ;
  }
  
  
  /**  Update methods.
    */
  public void updateWorld() {
    currentTime += 1f / PlayLoop.UPDATES_PER_SECOND ;
    sections.updateBounds() ;
    schedule.advanceSchedule(currentTime) ;
    
    float growIndex = (currentTime % Terrain.GROWTH_INTERVAL) ;
    growIndex *= size * size * 1f / Terrain.GROWTH_INTERVAL ;
    growth.scanThroughTo((int) growIndex) ;
    
    for (Mobile m : active) {
      m.updateAsMobile() ;
    }
  }
  
  
  protected void growthAt(final int x, final int y) {
    Flora.tryGrowthAt(x, y, this, false) ;
    final Element owner = tiles[x][y].owner() ;
    if (owner != null) owner.onGrowth() ;
  }
  
  
  protected void toggleActive(Mobile m, boolean is) {
    if (is) m.setEntry(active.addLast(m)) ;
    else active.removeEntry(m.entry()) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    //
    //  First, we obtain lists of all current visible fixtures, actors, and
    //  terrain sections.
    List <Element> visibleElements = new List <Element> () {
      protected float queuePriority(Element e) {
        return e.sprite().depth ;
      }
    } ;
    Batch <Section> visibleSections = new Batch <Section> () ;
    sections.compileVisible(
      rendering.port, base,
      visibleSections, visibleElements
    ) ;
    //
    //  We also render visible mobiles-
    Vec3D viewPos = new Vec3D() ;
    float viewRad = -1 ;
    for (Mobile active : this.active) {
      viewPos.setTo(active.position()) ;
      viewPos.z += active.height() / 2 ;
      viewRad = (active.height() / 2) + active.radius() ;
      if (rendering.port.intersects(viewPos, viewRad)) {
        visibleElements.add(active) ;
      }
    }
    //
    //  Then we register their associated media for rendering, in the correctly
    //  sorted order.
    for (Section section : visibleSections) {
      terrain.renderFor(section.x, section.y, rendering, currentTime) ;
      if (! GameSettings.noFog) {
        //I.say("Rendering fog") ;
        terrain.renderFogFor(section.x, section.y, base.fogMap(), rendering) ;
      }
    }
    rendering.clearDepth() ;
    Vec3D deep = new Vec3D() ;
    for (Element fixture : visibleElements) {
      final Sprite sprite = fixture.sprite() ;
      rendering.port.viewMatrix(deep.setTo(sprite.position)) ;
      sprite.depth = 0 - deep.z ;
    }
    visibleElements.queueSort() ;
    for (Element fixture : visibleElements) {
      fixture.renderFor(rendering, base) ;
    }
  }
  
  
  
  public Tile pickedTile(final HUD UI, final Viewport port) {
    final Vec3D origin = new Vec3D(UI.mouseX(), UI.mouseY(), 0) ;
    port.screenToIso(origin) ;
    final Vec3D vector = new Vec3D(0, 0, 1) ;
    port.viewInvert(vector) ;
    final Vec3D onGround = origin.add(vector, 0 - origin.z / vector.z, null) ;
    return tileAt(onGround.x, onGround.y) ;
  }
  
  
  public Fixture pickedFixture(final HUD UI, final Viewport port) {
    final Tile t = pickedTile(UI, port) ;
    if (t == null) return null ;
    if (t.owner() instanceof Fixture) return (Fixture) t.owner() ;
    return null ;
  }
  
  
  public Mobile pickedMobile(final HUD UI, final Viewport port) {
    final Tile t = pickedTile(UI, port) ;
    if (t == null) return null ;
    
    Batch <Element> near = new Batch <Element> () ;
    for (Element e : t.inside()) near.add(e) ;
    for (Tile n : t.allAdjacent(null)) if (n != null) {
      for (Element e : n.inside()) near.add(e) ;
    }
    
    Mobile nearest = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Element e : near) if (e instanceof Mobile) {
      final Mobile m = (Mobile) e ;
      if (m.aboard() != null) continue ;
      final float selRad = (m.height() + m.radius()) / 2 ;
      final Vec3D selPos = m.position(null) ;
      selPos.z += m.height() / 2 ;
      if (! port.mouseIntersects(selPos, selRad, UI)) continue ;
      final float dist = port.isoToFlat(selPos).z ;
      if (dist < minDist) { nearest = m ; minDist = dist ; }
    }
    return nearest ;
  }
}













