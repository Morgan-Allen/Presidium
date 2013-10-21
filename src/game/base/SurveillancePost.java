

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



public class SurveillancePost extends Venue implements Economy {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    SurveillancePost.class, "media/Buildings/ecologist/surveyor.png", 5, 1
  ) ;
  
  
  GroupSprite camouflaged ;
  
  
  public SurveillancePost(Base base) {
    super(5, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 4, 150,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
    
    camouflaged = new GroupSprite() ;
  }
  
  
  public SurveillancePost(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    SurveillancePost.class, "surveillance_post_upgrades"
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
    ANIMAL_BREEDING = new Upgrade(
      "Animal Breeding",
      "Regulates the local population of predators and prey, breeding new "+
      "specimens when absent and performing cullings when necessary.",
      300,
      null, 1, SENSOR_PERIMETER, ALL_UPGRADES
    ),
    RIDER_PENS = new Upgrade(
      "Rider Pens",
      "Allows certain animal species to be domesticated for use as personal "+
      "mounts and companions on patrol.",
      200,
      null, 1, ANIMAL_BREEDING, ALL_UPGRADES
    ),
    EXPLORER_STATION = new Upgrade(
      "Explorer Station",
      "Explorers are rugged outdoorsman that combine scientific curiosity "+
      "with a respect for natural ecosystems and basic self-defence training.",
      100,
      null, 1, null, ALL_UPGRADES
    ) ;
  
  
  //
  //  TODO:  Place a Flesh Still nearby, if you're culling the herds.
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    //
    //  Return a hunting expedition.   And... just explore the place.  You'll
    //  want to make this a bit more nuanced later.
    final Choice choice = new Choice(actor) ;
    final Actor p = Hunting.nextPreyFor(actor, World.SECTOR_SIZE * 2) ;
    if (p != null) {
      final Hunting h = new Hunting(actor, p, Hunting.TYPE_HARVEST) ;
      //h.priorityMod = Plan.ROUTINE ;
      choice.add(h) ;
    }
    final Tile t = Exploring.getUnexplored(actor.base().intelMap, actor) ;
    if (t != null) {
      I.say("TILE FOUND IS: "+t) ;
      final Exploring e = new Exploring(actor, actor.base(), t) ;
      e.priorityMod = Plan.ROUTINE ;
      choice.add(e) ;
    }
    else I.say("NOTHING LEFT TO EXPLORE?") ;
    return choice.weightedPick(actor.mind.whimsy()) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.EXPLORER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.EXPLORER) return nO + 2 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return null ; //new Service[] { WATER, PROTEIN, SPICE } ;
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
    return "Surveillance Post" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/redoubt_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Surveyors are responsible for exploring the hinterland of your "+
      "settlement, scouting for danger and regulating animal populations." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MILITANT ;
  }
}








