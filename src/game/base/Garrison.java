

package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.I;
import src.util.Index;



public class Garrison extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constants, and save/load methods-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
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
    TECHNICAL_TRAINING = new Upgrade(
      "Technical Training",
      "Prepares your soldiers with the expertise needed to pilot vehicles "+
      "and mechanical armour.",
      200, null, 3, null, ALL_UPGRADES
    ),
    SURVIVAL_TRAINING = new Upgrade(
      "Survival Training",
      "Prepares your soldiers for guerilla warfare and wilderness survival.",
      200, null, 3, null, ALL_UPGRADES
    ),
    //*/
    VOLUNTEER_QUARTERS = new Upgrade(
      "Volunteer Quarters",
      "Dedicated in defence of their homes, a volunteer militia provides the "+
      "mainstay of your domestic forces.",
      100,
      Background.VOLUNTEER, 2, null, ALL_UPGRADES
    ),
    VETERAN_QUARTERS = new Upgrade(
      "Veteran Quarters",
      "Seasoned professional soldiers, veterans provide the backbone of your "+
      "officer corps and command structure.",
      150,
      Background.VETERAN, 1, null, ALL_UPGRADES
    ) ;
  
  protected Background[] careers() {
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
    if (! personnel.onShift(actor)) return null ;
    //
    //  Grab a random building nearby and patrol around it.  Especially walls.
    final Venue patrolled = (Venue) world.presences.randomMatchNear(
      base(), this, World.SECTOR_SIZE
    ) ;
    if (patrolled != null) {
      return new Patrolling(actor, patrolled, patrolled.radius() * 2) ;
    }
    //
    //  TODO:  You also need an option to train in relevant skills here.
    
    //
    //  TODO:  Implement patrols along the perimeter fence/shield walls...
    
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    
    ///setAsDestroyed() ;
    ///if (true) return ;
    
    
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







