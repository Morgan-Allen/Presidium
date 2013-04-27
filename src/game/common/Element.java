/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.graphics.common.* ;
import src.util.* ;



public abstract class Element implements Target, Session.Saveable {
  
  
  /**  Common fields, basic constructors, and save/load methods
    */
  final public static int
    NOTHING_OWNS     = 0,
    ENVIRONMENT_OWNS = 1,
    FIXTURE_OWNS     = 2,
    VENUE_OWNS       = 3 ;

  private Sprite sprite ;
  private Object flagged ;
  
  protected World world ;
  private Tile location ;
  
  
  
  public Element() {
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.loadBool() ? s.world() : null ;
    location = (Tile) s.loadTarget() ;
    final Model model = Model.loadModel(s.input()) ;
    if (model != null) attachSprite(model.makeSprite()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveBool(world != null) ;
    s.saveTarget(location) ;
    Model.saveModel(sprite.model(), s.output()) ;
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
    this.world = world ;
    if (owningType() != NOTHING_OWNS) location.setOwner(this) ;
  }
  
  public void exitWorld() {
    if (! inWorld()) I.complain("Never entered world...") ;
    if (owningType() != NOTHING_OWNS) location.setOwner(null) ;
    this.world = null ;
  }
  
  public void setPosition(float x, float y, World world) {
    this.location = world.tileAt(x, y) ;
    if (location == null) I.complain("Bad location for element: "+x+" "+y) ;
  }
  
  public boolean inWorld() {
    return world != null ;
  }
  
  public World world() {
    return world ;
  }
  
  public int owningType() {
    return ENVIRONMENT_OWNS ;
  }
  
  public int pathType() {
    return Tile.PATH_BLOCKS ;
  }
  
  public void flagWith(Object f) {
    /*
    if (f != null && flagged != null) {
      I.complain("PREVIOUS FLAGGING WAS NOT CLEARED- "+f+" "+flagged) ;
    }
    if (f == null && flagged == null) {
      I.complain("PREVIOUS FLAGGING ALREADY CLEARED!") ;
    }
    //*/
    flagged = f ;
  }
  
  public Object flaggedWith() {
    return flagged ;
  }
  
  protected void onGrowth() {
  }
  
  
  /**  Methods related to specifying position and size-
    */
  public Tile origin() {
    return location ;
  }
  
  public int xdim() { return 1 ; }
  public int ydim() { return 1 ; }
  public int zdim() { return 1 ; }
  
  public Box2D area() {
    return new Box2D().set(
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
    return true ;
  }
  
  protected void renderFor(Rendering rendering, Base base) {
    position(sprite.position) ;
    rendering.addClient(sprite) ;
  }
  
  protected void attachSprite(Sprite sprite) {
    this.sprite = sprite ;
  }
  
  public Sprite sprite() {
    return sprite ;
  }
}



