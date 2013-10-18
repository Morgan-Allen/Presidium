

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class Garrison extends Venue implements Economy {
  
  
  
  /**  Fields, constants, and save/load methods-
    */
  final static Model
    MODEL = ImageModel.asSolidModel(
      Garrison.class, "media/Buildings/military/house_garrison.png", 4, 4
    ) ;
  
  
  private DrillYard drillYard ;
  
  
  public Garrison(Base base) {
    super(4, 4, ENTRANCE_SOUTH, base) ;
    structure.setupStats(
      500, 20, 250,
      Structure.SMALL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Garrison(Session s) throws Exception {
    super(s) ;
    drillYard = (DrillYard) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(drillYard) ;
  }
  
  
  
  /**  Upgrades, economic functions and actor behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Garrison.class, "garrison_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    MELEE_TRAINING = new Upgrade(
      "Melee Training",
      "Prepares your soldiers for the rigours of close combat.",
      150, null, 3, null, ALL_UPGRADES
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Prepares your soldiers for ranged marksmanship.",
      150, null, 3, null, ALL_UPGRADES
    ),
    ENDURANCE_TRAINING = new Upgrade(
      "Endurance Training",
      "Prepares your soldiers for guerilla warfare and wilderness survival.",
      200, null, 3, null, ALL_UPGRADES
    ),
    PEACEKEEPER_TRAINING = new Upgrade(
      "Peacekeeper Training",
      "Educates your soldiers about the use of minimal force, local "+
      "contacts, and proper treatment of prisoners.",
      200, null, 3, null, ALL_UPGRADES
    ),
    VOLUNTEER_STATION = new Upgrade(
      "Volunteer Station",
      "Dedicated in defence of their homes, a volunteer militia provides the "+
      "mainstay of your domestic forces.",
      100,
      Background.VOLUNTEER, 2, null, ALL_UPGRADES
    ),
    VETERAN_STATION = new Upgrade(
      "Veteran Station",
      "Seasoned professional soldiers, veterans provide the backbone of your "+
      "officer corps and command structure.",
      150,
      Background.VETERAN, 1, VOLUNTEER_STATION, ALL_UPGRADES
    ) ;
  
  public Background[] careers() {
    return new Background[] { Background.VOLUNTEER, Background.VETERAN } ;
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v) ;
    if (v == Background.VOLUNTEER) return num + 2 ;
    if (v == Background.VETERAN  ) return num + 1 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] {} ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    
    //
    //  Grab a random building nearby and patrol around it.
    final float range = World.SECTOR_SIZE / 2f ;
    //
    //  TODO:  try to pick points far apart from eachother, and employ
    //  multiple samples for the purpose.
    
    final Venue
      init = (Venue) world.presences.randomMatchNear(base(), this, range),
      dest = (Venue) world.presences.randomMatchNear(base(), this, range) ;
    
    if (init instanceof ShieldWall || dest instanceof ShieldWall) {
      Target pick, other ;
      if (Rand.yes()) { pick = init ; other = dest ; }
      else            { pick = dest ; other = init ; }
      if (pick == null) pick = other ;
      final Patrolling s = Patrolling.sentryDuty(
        actor, (ShieldWall) pick, Rand.index(8)
      ) ;
      if (s != null) {
        s.priorityMod = Plan.ROUTINE ;
        choice.add(s) ;
      }
    }
    if (init != null && dest != null) {
      final Patrolling p = Patrolling.streetPatrol(actor, init, dest, world) ;
      if (p != null) {
        p.priorityMod = Plan.ROUTINE ;
        choice.add(p) ;
      }
    }
    return choice.weightedPick(actor.mind.whimsy()) ;
  }
  
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    updateDrillYard() ;
    if (! structure.intact()) return ;
  }
  
  
  protected void updateDrillYard() {
    if (drillYard == null || drillYard.destroyed()) {
      final DrillYard newYard = new DrillYard(this) ;
      final Tile o = world.tileAt(this) ;
      final TileSpread spread = new TileSpread(mainEntrance()) {
        
        protected boolean canAccess(Tile t) {
          if (Spacing.distance(t, o) > World.SECTOR_SIZE) return false ;
          return ! t.blocked() ;
        }
        
        protected boolean canPlaceAt(Tile t) {
          newYard.setPosition(t.x, t.y, t.world) ;
          if (newYard.canPlace()) return true ;
          return false ;
        }
      } ;
      spread.doSearch() ;
      if (spread.success()) {
        newYard.doPlace(newYard.origin(), null) ;
        drillYard = newYard ;
      }
    }
  }
  
  
  public void onDecommision() {
    super.onDecommission() ;
    if (drillYard != null) {
      drillYard.structure.setState(Structure.STATE_SALVAGE, -1) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Garrison" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/garrison_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Garrison sends regular patrols of sentries to enforce the peace "+
      "and keep a watch out for raiders or outlaws." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MILITANT ;
  }
}







