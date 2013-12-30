

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class SurveyStation extends Venue implements Economy {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    SurveyStation.class, "media/Buildings/ecologist/surveyor.png", 4.8f, 1
  ) ;
  
  
  GroupSprite camouflaged ;
  FleshStill still ;
  
  
  public SurveyStation(Base base) {
    super(5, 1, Venue.ENTRANCE_NORTH, base) ;
    structure.setupStats(
      150, 4, 150,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
    camouflaged = new GroupSprite() ;
  }
  
  
  public SurveyStation(Session s) throws Exception {
    super(s) ;
    still = (FleshStill) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(still) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    SurveyStation.class, "surveillance_post_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    THERMAL_CAMOUFLAGE = new Upgrade(
      "Thermal Camouflage",
      "Reduces the Surveillance Post's thermal signature and light output, "+
      "making it harder for outsiders to detect.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    SENSOR_PERIMETER = new Upgrade(
      "Sensor Perimeter",
      "Installs automatic sensors attuned to sound and motion, making it "+
      "difficult for intruders to approach unannounced.",
      100,
      null, 1, null, ALL_UPGRADES
    ),
    NATIVE_MISSION = new Upgrade(
      "Native Mission",
      "Improves outreach to local tribal communities, raising the odds of "+
      "peaceful contact and recruitment from their ranks.",
      300,
      null, 1, null, ALL_UPGRADES
    ),
    CAPTIVE_BREEDING = new Upgrade(
      "Animal Breeding",
      "Breeds new specimens of local wildlife for use as food stock or "+
      "personal companions.",
      300,
      null, 1, SENSOR_PERIMETER, ALL_UPGRADES
    ),
    GUERILLA_TRAINING = new Upgrade(
      "Guerilla Training",
      "Emphasises combat, stealth and survival exercises relevant in a "+
      "military capacity.",
      200,
      null, 1, THERMAL_CAMOUFLAGE, ALL_UPGRADES
    ),
    EXPLORER_STATION = new Upgrade(
      "Explorer Station",
      "Explorers are rugged outdoorsman that combine scientific curiosity "+
      "with a respect for natural ecosystems and basic self-defence training.",
      100,
      null, 1, null, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  Return a hunting expedition.   And... just explore the place.  You'll
    //  want to make this a bit more nuanced later.
    if (still != null && ! still.destroyed()) {
      final Delivery d = Deliveries.nextDeliveryFor(
        actor, still, still.services(), 5, world
      ) ;
      choice.add(d) ;
      final Actor p = Hunting.nextPreyFor(actor, true) ;
      if (p != null) {
        final Hunting h = Hunting.asProcess(actor, p, still) ;
        choice.add(h) ;
      }
    }
    
    final Tile t = Exploring.getUnexplored(actor.base().intelMap, actor) ;
    if (t != null) {
      I.say("TILE FOUND IS: "+t) ;
      final Exploring e = new Exploring(actor, actor.base(), t) ;
      e.priorityMod = Plan.ROUTINE ;
      choice.add(e) ;
    }
    //else I.say("NOTHING LEFT TO EXPLORE?") ;
    
    if (structure.upgradeLevel(CAPTIVE_BREEDING) > 0) {
      final Fauna toTend = AnimalHusbandry.nextHandled(this) ;
      if (toTend != null) {
        choice.add(new AnimalHusbandry(actor, this, toTend)) ;
      }
    }
    
    final SensorPost newPost = SensorPost.locateNewPost(this) ;
    if (newPost != null) {
      final Action collects = new Action(
        actor, newPost,
        this, "actionCollectSensor",
        Action.REACH_DOWN, "Collecting sensor"
      ) ;
      collects.setMoveTarget(this) ;
      final Action plants = new Action(
        actor, newPost.origin(),
        this, "actionPlantSensor",
        Action.REACH_DOWN, "Planting sensor"
      ) ;
      plants.setMoveTarget(Spacing.pickFreeTileAround(newPost, actor)) ;
      choice.add(new Steps(actor, this, Plan.ROUTINE, collects, plants)) ;
    }
    
    return choice.weightedPick() ;
  }
  
  
  public boolean actionCollectSensor(Actor actor, SensorPost post) {
    actor.gear.addItem(Item.withReference(SAMPLES, post)) ;
    return true ;
  }
  
  
  public boolean actionPlantSensor(Actor actor, Tile t) {
    SensorPost post = null ;
    for (Item i : actor.gear.matches(SAMPLES)) {
      if (i.refers instanceof SensorPost) {
        post = (SensorPost) i.refers ;
        actor.gear.removeItem(i) ;
      }
    }
    if (post == null) return false ;
    post.setPosition(t.x, t.y, world) ;
    if (! Spacing.perimeterFits(post)) return false ;
    post.enterWorld() ;
    return true ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.EXPLORER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.EXPLORER) {
      return nO + 2 + structure.upgradeLevel(EXPLORER_STATION) ;
    }
    return 0 ;
  }
  
  
  public Service[] services() {
    return null ; //new Service[] { WATER, PROTEIN, SPICE } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.forceDemand(CARBS, 5, VenueStocks.TIER_CONSUMER) ;
    
    if (still == null || still.destroyed()) {
      final Tile o = Spacing.pickRandomTile(this, 4, world) ;
      still = (FleshStill) Placement.establishVenue(
        new FleshStill(this), o.x, o.y, GameSettings.buildFree, world
      ) ;
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  


  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base()) super.renderFor(rendering, base) ;
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      this.position(camouflaged.position) ;
      camouflaged.fog = this.fogFor(base) ;
      rendering.addClient(camouflaged) ;
    }
  }
  
  
  public String fullName() {
    return "Survey Station" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/redoubt_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Survey Stations are responsible for exploring the hinterland of your "+
      "settlement, scouting for danger and regulating animal populations." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}








