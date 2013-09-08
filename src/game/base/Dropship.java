


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



/**  Trade ships come to deposit and collect personnel and cargo.
  */
//
//...You also need to try making deliveries/collections, based on what
//nearby venues have either a shortage or excess of.  Particularly at the
//supply depot.

//
//NOTE:  This class has been prone to bugs where sprite position appears to
//'jitter' when passing over blocked tiles below, due to the mobile class
//attempting to 'correct' position after each update.  This class must treat
//all tiles as passable to compensate.



public class Dropship extends Vehicle implements
  Inventory.Owner, BuildConstants
{
  
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String SHIP_NAMES[] = {
    "The Rusty Mariner",
    "The Solar Wind",
    "The Blue Nebula",
    "The Dejah Thoris",
    "The Royal Organa",
    "The Consort Irulan",
    "The Century Wake",
    "The Lacrimosa",
    "The Firebrat",
    "The Orion Belt",
    "The Polaris",
    "The Water Bearer",
    "The Bottle of Klein",
    "The Occam Razor",
    "The Event Horizon"
  } ;
  
  
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model
    FREIGHTER_MODEL = MS3DModel.loadMS3D(
      Dropship.class, FILE_DIR, "dropship.ms3d", 1.0f
    ).loadXMLInfo(XML_PATH, "Dropship") ;
  
  final public static int
    STAGE_DESCENT  = 0,
    STAGE_LANDED   = 1,
    STAGE_BOARDING = 2,
    STAGE_ASCENT   = 3,
    STAGE_AWAY     = 4 ;
  final public static int
    MAX_CAPACITY   = 100,
    MAX_PASSENGERS = 5 ;
  final public static float
    INIT_DIST = 10.0f,
    INIT_HIGH = 10.0f,
    TOP_SPEED =  5.0f ;
  
  
  private Vec3D aimPos = new Vec3D() ;
  private float stageInceptTime = 0 ;
  private int stage = STAGE_AWAY ;
  private int nameID = -1 ;
  
  
  
  public Dropship() {
    super() ;
    attachSprite(FREIGHTER_MODEL.makeSprite()) ;
    this.stage = STAGE_AWAY ;
    this.nameID = Rand.index(SHIP_NAMES.length) ;
  }
  
  
  public Dropship(Session s) throws Exception {
    super(s) ;
    aimPos.loadFrom(s.input()) ;
    stageInceptTime = s.loadFloat() ;
    stage = s.loadInt() ;
    nameID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    aimPos.saveTo(s.output()) ;
    s.saveFloat(stageInceptTime) ;
    s.saveInt(stage) ;
    s.saveInt(nameID) ;
  }
  
  
  public int owningType() { return Element.VENUE_OWNS ; }
  public int pathType() { return Tile.PATH_BLOCKS ; }
  public float height() { return 2.0f ; }
  public float radius() { return 2.0f ; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public Behaviour jobFor(Actor actor) {
    
    //  TODO:  You'll have to restrict deliveries to certain venues, I think.
    //  Have the customer come to you instead.
    /*
    final Delivery d = Delivery.nextDeliveryFrom(
      this, actor, CARRIED_ITEM_TYPES
    ) ;
    if (d != null) return d ;
    if (actor.isDoing(Delivery.class)) return null ;
    //*/
    
    if (stage == STAGE_BOARDING) {
      final Action boardAction = new Action(
        actor, this,
        this, "actionBoard",
        Action.STAND, "boarding "+this
      ) ;
      final float priority = timeLanded() / 10 ;
      boardAction.setPriority(Visit.clamp(priority, 0, Action.PARAMOUNT)) ;
      return boardAction ;
    }
    return null ;
  }
  
  
  public boolean actionBoard(Actor actor, Dropship ship) {
    ship.setInside(actor, true) ;
    //  TODO:  Consider doing this when the ship gets away instead.
    actor.exitWorld() ;
    return true ;
  }
  
  
  public void beginBoarding() {
    if (stage != STAGE_LANDED) I.complain("Cannot board until landed!") ;
    stage = STAGE_BOARDING ;
  }
  
  
  public boolean allAboard() {
    for (Actor c : crew()) {
      if (c.aboard() != this) return false ;
      if (c.currentAction() != null && ! c.isDoing("actionBoard", null)) {
        return false ;
      }
    }
    return true ;
  }
  
  
  protected void offloadPassengers() {
    final int size = 2 * (int) Math.ceil(radius()) ;
    final int EC[] = Spacing.entranceCoords(size, size, entranceFace) ;
    final Box2D site = this.area(null) ;
    final Tile o = world.tileAt(site.xpos() + 0.5f, site.ypos() + 0.5f) ;
    final Tile exit = world.tileAt(o.x + EC[0], o.y + EC[1]) ;
    this.dropPoint = exit ;
    
    for (Mobile m : inside()) if (! m.inWorld()) {
      m.enterWorldAt(exit.x, exit.y, world) ;
    }
    inside.clear() ;
  }
  
  
  
  /**  Handling the business of ascent and landing-
    */
  public void beginDescent(Box2D site, World world) {
    ///I.say("BEGINNING DESCENT") ;
    aimPos.set(
      site.xpos() + (site.xdim() / 2),
      site.ypos() + (site.ydim() / 2),
      0
    ) ;
    final Tile entry = Spacing.pickRandomTile(
      world.tileAt(aimPos.x, aimPos.y), INIT_DIST, world
    ) ;
    enterWorldAt(entry.x, entry.y, world) ;
    nextPosition.set(entry.x, entry.y, INIT_HIGH) ;
    nextRotation = 0 ;
    setHeading(nextPosition, nextRotation, true, world) ;
    entranceFace = Venue.ENTRANCE_SOUTH ;
    stage = STAGE_DESCENT ;
    stageInceptTime = world.currentTime() ;
  }
  
  
  private void performLanding(World world, Box2D site) {
    //
    //  Clear any detritus around the perimeter-
    for (Tile t : world.tilesIn(site, false)) {
      if (t.owner() != null) t.owner().setAsDestroyed() ;
    }
    //
    //  Claim tiles in the middle as owned, and evacuate any occupants-
    site = new Box2D().setTo(site).expandBy(-1) ;
    for (Tile t : world.tilesIn(site, false)) t.setOwner(this) ;
    for (Tile t : world.tilesIn(site, false)) {
      for (Mobile m : t.inside()) if (m != this) {
        final Tile e = Spacing.nearestOpenTile(m.origin(), m) ;
        m.setPosition(e.x, e.y, world) ;
      }
    }
    //
    //  Offload cargo and passengers-
    offloadPassengers() ;
  }
  
  
  public void beginAscent() {
    ///I.say("BEGINNING ASCENT") ;
    if (landed()) {
      final Box2D site = new Box2D().setTo(landArea()).expandBy(-1) ;
      for (Tile t : world.tilesIn(site, false)) t.setOwner(null) ;
    }
    final Tile exits = Spacing.pickRandomTile(origin(), INIT_DIST, world) ;
    aimPos.set(exits.x, exits.y, INIT_HIGH) ;
    this.dropPoint = null ;
    stage = STAGE_ASCENT ;
    stageInceptTime = world.currentTime() ;
  }
  
  
  public void completeDescent() {
    nextPosition.setTo(position.setTo(aimPos)) ;
    rotation = nextRotation = 0 ;
    performLanding(world, landArea()) ;
    offloadPassengers() ;
    stageInceptTime = world.currentTime() ;
    stage = STAGE_LANDED ;
  }
  
  
  public boolean landed() {
    return stage == STAGE_LANDED || stage == STAGE_BOARDING ;
  }
  
  
  public float timeLanded() {
    if (stage == STAGE_AWAY || stage == STAGE_DESCENT) return - 1 ;
    return world.currentTime() - stageInceptTime ;
  }
  
  
  public float timeAway(World world) {
    return world.currentTime() - stageInceptTime ;
  }
  
  
  public int flightStage() {
    return stage ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    final float height = position.z / INIT_HIGH ;
    //
    //  Check to see if ascent or descent are complete-
    if (stage == STAGE_ASCENT && height >= 1) {
      exitWorld() ;
      stage = STAGE_AWAY ;
      stageInceptTime = world.currentTime() ;
    }
    if (stage == STAGE_DESCENT && height <= 0) {
      performLanding(world, landArea()) ;
      stage = STAGE_LANDED ;
      stageInceptTime = world.currentTime() ;
    }
    //
    //  Otherwise, adjust motion-
    if (inWorld() && ! landed()) adjustFlight(height) ;
  }
  
  
  private void adjustFlight(float height) {
    //
    //  Firstly, determine what your current position is relative to the aim
    //  point-
    final Vec3D disp = aimPos.sub(position, null) ;
    final Vec2D heading = new Vec2D().setTo(disp).scale(-1) ;
    //
    //  Calculate rate of lateral speed and descent-
    final float UPS = 1f / PlayLoop.UPDATES_PER_SECOND ;
    final float dist = heading.length() / INIT_DIST ;
    final float speed = (((dist * dist) * TOP_SPEED) + 1) * UPS ;
    float ascent = TOP_SPEED * UPS / 4 ;
    ascent = Math.min(ascent, Math.abs(position.z - aimPos.z)) ;
    if (stage == STAGE_DESCENT) ascent *= -1 ;
    //
    //  Then head toward the aim point-
    if (disp.length() > speed) disp.scale(speed / disp.length()) ;
    nextPosition.setTo(position).add(disp) ;
    nextPosition.z = position.z + ascent ;
    //
    //  And adjust rotation-
    float angle = (heading.toAngle() * height) + (0 * (1 - height)) ;
    final float
      angleDif = Vec2D.degreeDif(angle, rotation),
      absDif   = Math.abs(angleDif), maxRotate = 90 * UPS ;
    if (height < 0.5f && absDif > maxRotate) {
      angle = rotation + (maxRotate * (angleDif > 0 ? 1 : -1)) ;
      angle = (angle + 360) % 360 ;
    }
    nextRotation = angle ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    if (stage == STAGE_DESCENT) {
      ///I.say("  LANDING AREA: "+landArea()) ;
      if (! checkLandingArea(world, landArea())) {
        beginAscent() ;
      }
    }
  }
  
  
  public boolean blocksMotion(Boardable t) { return false ; }
  public void pathingAbort() {}
  
  
  
  /**  Code for finding a suitable landing site-
    */
  private Box2D landArea() {
    final int size = (int) Math.ceil(radius()) ;
    final Box2D area = new Box2D().set(aimPos.x, aimPos.y, 0, 0) ;
    area.expandBy(size + 1) ;
    return area ;
  }
  
  
  protected boolean checkLandingArea(World world, Box2D area) {
    for (Tile t : world.tilesIn(area, false)) {
      if (t == null) return false ;
      if (t.owner() == this) continue ;
      if (t.owningType() > Element.ELEMENT_OWNS) return false ;
    }
    return true ;
  }
  
  
  public static Box2D findLandingSite(Dropship landing, final Base base) {
    //
    //  Firstly, determine a reasonable starting position-
    final World world = base.world ;
    final Tile midTile = world.tileAt(world.size / 2, world.size / 2) ;
    final Target nearest = world.presences.randomMatchNear(base, midTile, -1) ;
    if (nearest == null) return null ;
    final Tile init = Spacing.nearestOpenTile(world.tileAt(nearest), midTile) ;
    if (init == null) return null ;
    return findLandingSite(landing, init, base) ;
  }
  
  
  public static Box2D findLandingSite(
    final Dropship landing, final Tile init, final Base base
  ) {
    //
    //  Then, spread out to try and find a decent landing site-
    final Box2D area = landing.landArea() ;
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > World.DEFAULT_SECTOR_SIZE) return false ;
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        area.xpos(t.x - 0.5f) ;
        area.ypos(t.y - 0.5f) ;
        return landing.checkLandingArea(base.world, area) ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) return area ;
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float height = this.viewPosition(null).z / INIT_HIGH ;
    
    final float fadeProgress = height < 0.5f ? 1 : ((1 - height) * 2) ;
    s.colour = Colour.transparency(fadeProgress) ;
    
    final float animProgress = height > 0.5f ? 0 : ((0.5f - height) * 2) ;
    s.setAnimation("descend", Visit.clamp(animProgress, 0, 1)) ;
    
    super.renderFor(rendering, base) ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return ;
    float fadeout = sprite().colour.a ;
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      Colour.transparency(fadeout * (hovered ? 0.5f : 1)),
      Selection.SELECT_CIRCLE
    ) ;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Dropship" ;
    return SHIP_NAMES[nameID] ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Dropships ferry initial colonists and startup supplies to your "+
      "fledgling settlement, courtesy of your homeworld's generosity." ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.appendList("Crew: ", crew()) ;
    d.appendList("\n\nPassengers: ", inside()) ;
    d.appendList("\n\nCargo: ", cargo.allItems()) ;
    d.append("\n\n") ; d.append(helpInfo()) ;
  }
}




