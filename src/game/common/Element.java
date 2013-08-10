/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.graphics.common.* ;
import src.util.* ;



public abstract class Element implements
  Target, Session.Saveable, World.Visible
{
  
  
  /**  Common fields, basic constructors, and save/load methods
    */
  final public static int
    NOTHING_OWNS     = 0,
    ENVIRONMENT_OWNS = 1,
    FIXTURE_OWNS     = 2,
    VENUE_OWNS       = 3,
    TERRAIN_OWNS     = 4 ;
  
  final protected static int
    PROP_IN_WORLD  = 1 << 0,
    PROP_DESTROYED = 1 << 2 ;
  

  private Sprite sprite ;
  private Object flagged ;
  
  protected World world ;
  private Tile location ;
  private float inceptTime ;
  private int properties ;
  
  
  
  public Element() {
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.loadBool() ? s.world() : null ;
    location = (Tile) s.loadTarget() ;
    inceptTime = s.loadFloat() ;
    sprite = Model.loadSprite(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(world != null) ;
    s.saveTarget(location) ;
    s.saveFloat(inceptTime) ;
    Model.saveSprite(sprite, s.output()) ;
  }
  
  
  
  /**  Life-cycle methods-
    */
  public boolean canPlace() {
    if (location == null) return false ;
    if (location.blocked()) return false ;
    return true ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    if (inWorld()) I.complain("Already in world...") ;
    setPosition(x, y, world) ;
    this.toggleProperty(PROP_IN_WORLD, true) ;
    this.world = world ;
    this.inceptTime = world.currentTime() ;
    if (owningType() != NOTHING_OWNS) location.setOwner(this) ;
  }
  
  
  public void setAsDestroyed() {
    if (! inWorld()) I.complain("Never entered world...") ;
    this.toggleProperty(PROP_DESTROYED, true) ;
    world.ephemera.addGhost(origin(), radius() * 2, sprite) ;
    exitWorld() ;
  }
  
  
  public void exitWorld() {
    if (! inWorld()) I.complain("Never entered world...") ;
    if (owningType() != NOTHING_OWNS) location.setOwner(null) ;
    this.toggleProperty(PROP_IN_WORLD, false) ;
    this.world = null ;
  }
  
  
  public void setPosition(float x, float y, World world) {
    this.location = world.tileAt(x, y) ;
    if (location == null) I.complain("Bad location for element: "+x+" "+y) ;
  }
  
  
  public void enterWorld() {
    if (location == null) I.complain("Position never set!") ;
    enterWorldAt(location.x, location.y, location.world) ;
  }
  
  
  public boolean destroyed() {
    return hasProperty(PROP_DESTROYED) ;
  }
  
  
  public boolean inWorld() {
    return world != null ;
  }
  
  
  public World world() {
    return world ;
  }
  
  
  
  /**  Properties, both hard-wired and custom.
    */
  public int owningType() {
    return ENVIRONMENT_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS ;
  }
  
  
  protected void toggleProperty(int prop, boolean has) {
    if (has) properties |= prop ;
    else properties &= ~prop ;
  }
  
  
  protected boolean hasProperty(int prop) {
    return (properties & prop) == prop ;
  }
  
  
  public void flagWith(Object f) {
    flagged = f ;
  }
  
  
  public Object flaggedWith() {
    return flagged ;
  }
  
  
  
  /**  Timing-associated methods-
    */
  protected void onGrowth() {
  }
  
  
  public void setAsEstablished(boolean isGrown) {
    if (isGrown) inceptTime = -10 ;
    else inceptTime = world.currentTime() ;
  }
  
  
  /**  Methods related to specifying position and size-
    */
  public Tile origin() {
    return location ;
  }
  
  
  public int xdim() { return 1 ; }
  public int ydim() { return 1 ; }
  public int zdim() { return 1 ; }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    return put.set(
      location.x - 0.5f, location.y - 0.5f,
      xdim(), ydim()
    ) ;
  }
  
  
  public Vec3D position(Vec3D v) {
    return location.position(v) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  public float height() {
    return 1 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    v = position(v) ;
    v.z += height() / 2 ;
    return v ;
  }
  
  
  protected boolean visibleTo(Base base) {
    float fog = base.intelMap.fogAt(origin()) ;
    if (fog == 0) return false ;
    else sprite.fog = fog ;
    return true ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    float timeGone = world().currentTime() - inceptTime ;
    timeGone += PlayLoop.frameTime() / PlayLoop.UPDATES_PER_SECOND ;
    if (timeGone < 1) sprite.colour = Colour.transparency(timeGone) ;
    else sprite.colour = null ;
    position(sprite.position) ;
    sprite.update() ;
    rendering.addClient(sprite) ;
  }
  
  
  protected void attachSprite(Sprite sprite) {
    this.sprite = sprite ;
  }
  
  
  public Sprite sprite() {
    return sprite ;
  }
}



