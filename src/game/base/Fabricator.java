/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class Fabricator extends Venue implements Economy {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    Fabricator.class, "media/Buildings/aesthete/fabricator.png", 4, 2
  ) ;
  
  
  public Fabricator(Base base) {
    super(4, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      125, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Fabricator(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Foundry.class, "fabricator_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    POLYMER_LOOM = new Upgrade(
      "Polymer Loom",
      "Speeds the production of standard plastics and functional clothing.",
      200, PLASTICS, 2, null, ALL_UPGRADES
    ),
    ORGANIC_BONDING = new Upgrade(
      "Organic Bonding",
      "Allows for direct conversion of carbs to plastics, reduces squalor"+
      "and provides a mild bonus to plastics production.",
      250, CARBS, 1, null, ALL_UPGRADES
    ),
    FABRICATOR_STATION = new Upgrade(
      "Fabricator Station",
      "Fabricators are responsible for the bulk production of textiles, "+
      "domestic utensils and other lightweight goods.",
      100, Background.FABRICATOR, 1, POLYMER_LOOM, ALL_UPGRADES
    ),
    CUTTING_FLOOR = new Upgrade(
      "Cutting Floor",
      "Substantially eases the production of all outfit types.",
      150, null, 2, null, ALL_UPGRADES
    ),
    
    //
    //  TODO:  You might move these to the Sculptor Studio instead.
    
    DESIGN_STUDIO = new Upgrade(
      "Design Studio",
      "Facilitates the design and production of custom decor and commissions "+
      "for luxury outfits.",
      300, null, 1, CUTTING_FLOOR, ALL_UPGRADES
    ),
    AESTHETE_STATION = new Upgrade(
      "Aesthete Station",
      "Aesthetes are gifted, but often somewhat tempestuous individuals "+
      "with a flair for visual expression and eye-catching designs, able and "+
      "willing to cater to demanding patrons.",
      150, Background.AESTHETE, 1, DESIGN_STUDIO, ALL_UPGRADES
    )
  ;
  
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (structure.upgradeLevel(ORGANIC_BONDING) == 0) {
      stocks.translateDemands(1, PETROCARBS_TO_PLASTICS) ;
    }
    else stocks.translateBest(1, PETROCARBS_TO_PLASTICS, CARBS_TO_PLASTICS) ;
    
    final float powerNeed = 2 + (structure.numUpgrades() / 2f) ;
    stocks.bumpItem(POWER, powerNeed / -10) ;
    stocks.forceDemand(POWER, powerNeed, VenueStocks.TIER_CONSUMER) ;
    
    int pollution = 3 - (structure.upgradeBonus(ORGANIC_BONDING) * 2) ;
    world.ecology().impingeSqualor(pollution, this, true) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    final float powerCut = stocks.shortagePenalty(POWER) * 5 ;
    final int
      loomBonus = (5 * structure.upgradeLevel(POLYMER_LOOM)) / 2,
      bondBonus = 1 + structure.upgradeLevel(ORGANIC_BONDING) ;
    
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      if (o.made().type == DECOR) {
        o.checkBonus = (5 * structure.upgradeLevel(DESIGN_STUDIO)) / 2 ;
        if (stocks.amountOf(TROPHIES) > 0) o.checkBonus += bondBonus / 2 ;
      }
      else if (o.made().type == FINERY) {
        o.checkBonus = structure.upgradeLevel(CUTTING_FLOOR) + 2 ;
        o.checkBonus += (1 + structure.upgradeLevel(DESIGN_STUDIO)) / 2 ;
      }
      else {
        o.checkBonus = structure.upgradeLevel(CUTTING_FLOOR) + 2 ;
        o.checkBonus += structure.upgradeLevel(POLYMER_LOOM) ;
      }
      o.checkBonus -= powerCut ;
      o.timeMult = 5 ;
      choice.add(o) ;
    }
    
    final Manufacture m = (bondBonus <= 1) ?
      stocks.nextManufacture(
        actor, PETROCARBS_TO_PLASTICS
      ) :
      stocks.bestManufacture(
        actor, PETROCARBS_TO_PLASTICS, CARBS_TO_PLASTICS
      ) ;
    if (m != null) {
      if (m.conversion == PETROCARBS_TO_PLASTICS) {
        m.checkBonus = loomBonus + (bondBonus / 2) ;
      }
      else {
        m.checkBonus = (loomBonus / 2) + bondBonus ;
      }
      m.checkBonus -= powerCut ;
      choice.add(m) ;
    }
    
    return choice.weightedPick(actor.mind.whimsy()) ;
  }
  
  
  public int numOpenings(Background v) {
    int nO = super.numOpenings(v) ;
    if (v == Background.FABRICATOR) return nO + 1 ;
    if (v == Background.AESTHETE  ) return nO + 1 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      PLASTICS, DECOR, FINERY,
      OVERALLS, CAMOUFLAGE, SEALSUIT
    } ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.FABRICATOR, Background.AESTHETE } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Service[] goodsToShow() {
    return new Service[] { DECOR, PLASTICS, CARBS, PETROCARBS } ;
  }
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/fabricator_button.gif") ;
  }
  
  
  public String fullName() {
    return "Fabricator" ;
  }
  
  
  public String helpInfo() {
    return
      "The Fabricator manufactures plastics, pressfeed, decor and outfits "+
      "for your citizens." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_AESTHETE ;
  }
}




