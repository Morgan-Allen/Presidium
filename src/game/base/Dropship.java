


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



/**  Trade ships come to deposit and collect personnel and cargo, typically at
  *  the landing strip... at the house garrison.
  *  
  *  TODO:  This needs to cover multiple cases- supply, smuggler and spacer.
  *  TODO:  If you're using DropZones for pathing interface, maybe there's no
  *  need for vehicles themselves to be boardable?  Either that, or you need to
  *  make the VenueOrders class based on Employment/Owner properties.
  */
public class Dropship extends Vehicle implements Inventory.Owner {
  
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model
    FREIGHTER_MODEL = MS3DModel.loadMS3D(
      Dropship.class, FILE_DIR, "dropship.ms3d", 1.0f
    ).loadXMLInfo(XML_PATH, "Dropship") ;
  
  
  final static float
    LOWER_FLIGHT_HEIGHT = 5,
    UPPER_FLIGHT_RADIUS = 15,
    UPPER_FLIGHT_TIME   = 20,
    LOWER_FLIGHT_TIME   = 10,
    TOTAL_FLIGHT_TIME = UPPER_FLIGHT_TIME + LOWER_FLIGHT_TIME ;
  final public static int
    STAGE_DESCENT  = 0,
    STAGE_LANDED   = 1,
    STAGE_BOARDING = 2,
    STAGE_ASCENT   = 3,
    STAGE_AWAY     = 4 ;
  final public static int
    MAX_CAPACITY   = 100,
    MAX_PASSENGERS = 10 ;
  
  
  private Vec2D liftOff = new Vec2D() ;
  private Vec2D landPos = new Vec2D() ;
  private float initTime = 0 ;
  private int stage = STAGE_AWAY ;
  
  
  
  public Dropship() {
    super() ;
    attachSprite(FREIGHTER_MODEL.makeSprite()) ;
    this.stage = STAGE_AWAY ;
  }
  
  
  public Dropship(Session s) throws Exception {
    super(s) ;
    liftOff.set(s.loadFloat(), s.loadFloat()) ;
    landPos = new Vec2D().set(s.loadFloat(), s.loadFloat()) ;
    initTime = s.loadFloat() ;
    stage = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(liftOff.x) ; s.saveFloat(liftOff.y) ;
    s.saveFloat(landPos.x) ; s.saveFloat(landPos.y) ;
    s.saveFloat(initTime) ;
    s.saveInt(stage) ;
  }
  
  
  public int owningType() { return Element.VENUE_OWNS ; }
  public int pathingType() { return Tile.PATH_BLOCKS ; }
  public float height() { return 2.0f ; }
  public float radius() { return 2.0f ; }
  
  


  /**  Perform the actual exchange of goods and people.
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    //I.say("Updating freighter, no. of passengers: "+inside().size()) ;
    if (stage == STAGE_LANDED) {
      for (Mobile m : inside()) if (! m.inWorld()) {
        final Tile e = dropPoint.entrances()[0] ;
        setInside(m, false) ;
        m.enterWorldAt(e.x, e.y, world) ;
        break ;
      }
    }
  }
  
  
  public Behaviour jobFor(Citizen actor) {
    ///I.say("dropship getting job for: "+actor) ;
    if (stage == STAGE_BOARDING) {
      final Action boardAction = new Action(
        actor, dropPoint,
        this, "actionBoard",
        Model.AnimNames.STAND, "boarding "+this
      ) ;
      boardAction.setPriority(Behaviour.URGENT) ;
      return boardAction ;
    }
    if (dropPoint instanceof DropZone) {
      return dropPoint.jobFor(actor) ;
    }
    return super.jobFor(actor) ;
  }
  
  
  public boolean actionBoard(Actor actor, DropZone zone) {
    actor.exitWorld() ;  //This may be a little extreme, but needed atm...
    this.setInside(actor, true) ;
    return true ;
  }
  
  
  public void beginBoarding() {
    if (stage != STAGE_LANDED) I.complain("Cannot board until landed!") ;
    stage = STAGE_BOARDING ;
  }
  
  
  public boolean allBoarded() {
    boolean all = true ;
    for (Citizen c : crew) {
      if (c.aboard() != this && c.aboard() != dropPoint) all = false ;
    }
    return all ;
  }
  
  
  
  /**  Handling the business of ascent and landing-
    */
  public void beginDescent(Venue landing) {
    if (inWorld()) I.complain("Already in world!") ;
    final World world = landing.world() ;
    final Box2D area = landing.area() ;
    this.dropPoint = landing ;
    this.landPos.set(
      area.xpos() + (area.xdim() / 2),
      area.ypos() + (area.ydim() / 2)
    ) ;
    this.entranceFace = Venue.ENTRANCE_SOUTH ;
    this.liftOff.setFromAngle(Rand.num() * 360) ;
    liftOff.scale(UPPER_FLIGHT_RADIUS).add(landPos) ;
    liftOff.x = Visit.clamp(liftOff.x, 0, world.size - 1) ;
    liftOff.y = Visit.clamp(liftOff.y, 0, world.size - 1) ;
    this.initTime = world.currentTime() ;
    this.stage = STAGE_DESCENT ;
    final Vec3D p = getShipPos(0) ;
    enterWorldAt((int) p.x, (int) p.y, world) ;
    updateAsMobile() ;
  }
  
  
  protected void completeDescent() {
    initTime = world.currentTime() ;
    for (Item item : cargo.allItems()) cargo.transfer(item, dropPoint) ;
  }
  
  
  public void beginAscent() {
    if (! inWorld()) I.complain("No longer in world!") ;
    if (dropPoint instanceof DropZone) dropPoint.exitWorld() ;
    initTime = world.currentTime() ;
    stage = STAGE_ASCENT ;
  }
  
  
  protected void completeAscent() {
    initTime = world.currentTime() ;
    exitWorld() ;
  }
  
  
  public int flightStage() {
    return stage ;
  }
  
  
  public boolean landed() {
    return stage > STAGE_DESCENT && stage < STAGE_ASCENT ;
  }
  
  
  public Venue landingSite() {
    return dropPoint ;
  }
  
  
  public float timeSinceDescent(World world) {
    if (! landed()) {
      return -1 ;
      //I.complain("Can't get descent time!") ;
    }
    return world.currentTime() - initTime ;
  }
  
  
  public float timeSinceAscent(World world) {
    if (stage != STAGE_AWAY) {
      return -1 ;
      //I.complain("Can't get ascent time!") ;
    }
    return world.currentTime() - initTime ;
  }
  
  
  
  /**  Updating motion over time-
    */
  protected void updateAsMobile() {
    ///I.say("Updating dropship...") ;
    super.updateAsMobile() ;
    if (stage == STAGE_DESCENT) {
      float time = Math.min(world.currentTime() - initTime, TOTAL_FLIGHT_TIME) ;
      this.nextPosition.setTo(getShipPos(time)) ;
      this.nextRotation = getShipRot(time, true) ;
      if (time == TOTAL_FLIGHT_TIME) {
        completeDescent() ;
        stage = STAGE_LANDED ;
      }
    }
    if (stage == STAGE_ASCENT) {
      float time = TOTAL_FLIGHT_TIME - (world.currentTime() - initTime) ;
      if (time < 0) {
        completeAscent() ;
        stage = STAGE_AWAY ;
      }
      else {
        this.nextPosition.setTo(getShipPos(time)) ;
        this.nextRotation = getShipRot(time, true) ;
      }
    }
  }
  
  
  public void abortMotion() {
    //  Not sure what to do here, exactly...
  }
  
  
  //  TODO:  Move this to the vehicle class?
  private Vec3D getShipPos(float time) {
    if (time < UPPER_FLIGHT_TIME) {
      final float
        progress = time / UPPER_FLIGHT_TIME,
        asAngle = (float) Math.toRadians(progress * 90),
        horiz = (float) Math.sin(asAngle),
        vert = (float) Math.cos(asAngle) ;
      final Vec3D pos = new Vec3D().set(
        (landPos.x * horiz) + (liftOff.x * (1 - horiz)),
        (landPos.y * horiz) + (liftOff.y * (1 - horiz)),
        LOWER_FLIGHT_HEIGHT + (UPPER_FLIGHT_RADIUS * vert)
      ) ;
      return pos ;
    }
    else {
      final float height = LOWER_FLIGHT_HEIGHT *
        (1 - ((time - UPPER_FLIGHT_TIME) / LOWER_FLIGHT_TIME)) ;
      return new Vec3D(landPos.x, landPos.y, height) ;
    }
  }
  
  
  private float getShipRot(float time, boolean descent) {
    final Vec2D initVec = new Vec2D(liftOff).sub(landPos).normalise() ;
    if (descent) initVec.scale(-1) ;
    final float rotate = time / (UPPER_FLIGHT_TIME + LOWER_FLIGHT_TIME) ;
    final float landRot = entranceFace * 360 / 4f ;
    final float
      initAngle = initVec.toAngle(),
      terpAngle = Vec2D.degreeDif(landRot, initAngle) * rotate ;
    return (initAngle + terpAngle + 360) % 360 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    float progress = 1 ;
    if (stage == STAGE_DESCENT || stage == STAGE_ASCENT) {
      progress = world.timeWithinFrame() - initTime ;
      if (stage == STAGE_ASCENT) progress = TOTAL_FLIGHT_TIME - progress ;
      if (progress > UPPER_FLIGHT_TIME) {
        progress -= UPPER_FLIGHT_TIME ;
        progress /= LOWER_FLIGHT_TIME ;
      }
      else progress = 0 ;
    }
    s.setAnimation("descend", Visit.clamp(progress, 0, 1)) ;
    super.renderFor(rendering, base) ;
  }
  
  public String fullName() {
    return "Freighter" ;
  }
  
  public Texture portrait() {
    return null ;
  }
  
  public String helpInfo() {
    return null ;
  }
  
  public void writeInformation(Description description, int categoryID) {
  }
}



///I.say("Performing boarding action...") ;

/*
public void setupWithStage(
  Base client, Box2D site, float facing, int stage, float timeElapsed
) {
  final World world = this.world == null ? client.world : this.world ;
  
  this.landingSite.setTo(site) ;
  this.landPos.set(
    site.xpos() + (site.xdim() / 2),
    site.ypos() + (site.ydim() / 2)
  ) ;
  this.entranceFace = facing ;
  this.liftOff.setFromAngle(Rand.num() * 360).scale(UPPER_FLIGHT_RADIUS) ;
  this.initTime = world.currentTime() - timeElapsed ;
  this.client = client ;
  this.stage = stage ;
  
  final boolean inWorld = stage != STAGE_AWAY ;
  if (inWorld && ! inWorld()) {
    final Vec3D pos = getShipPos(0) ;
    enterWorldAt((int) pos.x, (int) pos.y, world) ;
    updateAsMobile() ;
  }
  if (inWorld() && ! inWorld) {
    exitWorld() ;
  }
}
//*/

/*
public void writeInformation(Description text, int categoryID) {
  cargo.writeInformation(text) ;
}

public float scheduledInterval() { return 1 ; }

public void updateOnSchedule(int numUpdates) {
}
//*/


/*
A parabolic curve is y = x^2.  Flip that on it's side, and you have y = sqrt(x),
where y is lateral distance from the descent point.
Add the minimum descent height.

Parametric equation would be: X = 2at, Y = a(t^2)



*/









