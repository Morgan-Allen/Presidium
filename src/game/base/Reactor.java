


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Reactor extends Venue implements BuildConstants {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Reactor.class, "media/Buildings/artificer/reactor.png", 4, 2
  ) ;
  
  private float meltdown = 0.0f ;
  

  public Reactor(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      300, 10, 300,
      VenueStructure.NORMAL_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Reactor(Session s) throws Exception {
    super(s) ;
    meltdown = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(meltdown) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Reactor.class, "reactor_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    WASTE_PROCESSING = new Upgrade(
      "Waste Processing",
      "Reduces the rate at which fuel rods are consumed and ameliorates "+
      "pollution.",
      350,
      null, 1, null, ALL_UPGRADES
    ),
    
    ISOTOPE_CONVERSION = new Upgrade(
      "Isotope Conversion",
      "Allows metal ores to be synthesised into fuel rods and facilitates "+
      "production of atomics.",
      500,
      null, 1, WASTE_PROCESSING, ALL_UPGRADES
    ),
    
    FEEDBACK_SENSORS = new Upgrade(
      "Feedback Sensors",
      "Reduces the likelihood of meltdown occuring when the reactor is "+
      "damaged or under-supervised, and reduces the likelihood of sabotage.",
      400,
      null, 1, null, ALL_UPGRADES
    ),
    
    FUSION_CONFINEMENT = new Upgrade(
      "Fusion Confinement",
      "Increases power output while limiting pollution and decreasing the "+
      "severity of any meltdowns.",
      350,
      null, 1, FEEDBACK_SENSORS, ALL_UPGRADES
    ),
    
    QUALIA_WAVEFORM_INTERFACE = new Upgrade(
      "Qualia Waveform Interface",
      "Allows reactor output to contribute slightly towards regeneration of "+
      "psi points and range of psyon abilities.",
      250,
      null, 1, FEEDBACK_SENSORS, ALL_UPGRADES
    ),
    
    CORE_TECHNICIAN_QUARTERS = new Upgrade(
      "Core Technician Quarters",
      "Core Technicians provide the expertise and vigilance neccesary to "+
      "monitor reactor output and synthesise nuclear biproducts.",
      100,
      Background.CORE_TECHNICIAN, 1, null, ALL_UPGRADES
    )
  ;
  
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null ;
    //
    //  First and foremost, check to see whether a meltdown is in progress, and
    //  arrest it if possible:
    final Choice choice = new Choice(actor) ;
    if (meltdown >= 0.1f) {
      final Action check = new Action(
        actor, this,
        this, "actionCheckMeltdown",
        Action.LOOK,
        meltdown < 0.5f ? "Checking core condition" : "Containing Meltdown!"
      ) ;
      check.setPriority(Action.CRITICAL * (meltdown + 0.5f)) ;
      choice.add(check) ;
    }
    if (! personnel.onShift(actor)) return choice.weightedPick(0) ;
    //
    //  Then check to see if anything needs manufacture-
    final Manufacture m = stocks.nextManufacture(actor, METALS_TO_FUEL) ;
    if (m != null && stocks.amountOf(METAL_ORE) >= 1) {
      m.checkBonus = 5 * structure.upgradeLevel(ISOTOPE_CONVERSION) ;
      choice.add(m) ;
    }
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      o.checkBonus = 5 * structure.upgradeLevel(ISOTOPE_CONVERSION) ;
      choice.add(o) ;
    }
    //
    //  Failing that, just keep the place in order-
    choice.add(new Supervision(actor, this)) ;
    return choice.weightedPick(0) ;
  }
  
  
  public boolean actionCheckMeltdown(Actor actor, Reactor reactor) {
    float diagnoseDC = 5 + ((1 - meltdown) * 20) ;
    final int FB = structure.upgradeLevel(FEEDBACK_SENSORS) ;
    diagnoseDC -= FB * 5 ;
    
    boolean success = true ;
    if (Rand.yes()) {
      success &= actor.traits.test(FIELD_THEORY, diagnoseDC, 0.5f) ;
      success &= actor.traits.test(CHEMISTRY, 5, 0.5f) ;
    }
    else {
      success &= actor.traits.test(ASSEMBLY, diagnoseDC, 0.5f) ;
      success &= actor.traits.test(SHIELD_AND_ARMOUR, 5, 0.5f) ;
    }
    if (success) {
      meltdown -= (1f + FB) / World.STANDARD_DAY_LENGTH ;
    }
    return true ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.CORE_TECHNICIAN) {
      return nO + 1 ;
    }
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    checkMeltdownAdvance() ;
    if (! structure.intact()) return ;
    //
    //  Calculate output of power and consumption of fuel-
    float fuelConsumed = 0.01f, powerOutput = 5 ;
    fuelConsumed *= 2 / (2f + structure.upgradeLevel(WASTE_PROCESSING)) ;
    powerOutput *= (2f + structure.upgradeLevel(FUSION_CONFINEMENT)) / 2 ;
    if (stocks.amountOf(POWER) >= 50 + (powerOutput * 10)) {
      fuelConsumed /= 10 ;
      powerOutput = 0 ;
    }
    final Item fuel = Item.withAmount(FUEL_CORES, fuelConsumed) ;
    if (stocks.hasItem(fuel)) stocks.removeItem(fuel) ;
    else powerOutput /= 5 ;
    stocks.bumpItem(POWER, powerOutput) ;
    //
    //  Update demand for raw materials-
    stocks.forceDemand(FUEL_CORES, stocks.demandFor(POWER) / 5f, 0) ;
    if (structure.upgradeLevel(ISOTOPE_CONVERSION) > 0) {
      stocks.translateDemands(METALS_TO_FUEL, 1) ;
    }
    //
    //  If possible, assist in recovery of psi points-
    final int PB = structure.upgradeLevel(QUALIA_WAVEFORM_INTERFACE) ;
    final Actor ruler = base().ruler() ;
    if (PB > 0 && ruler != null && ruler.aboard() instanceof Bastion) {
      ruler.health.adjustPsy(PB / 100f) ;
    }
    //
    //  Output pollution-
    int pollution = 10 ;
    pollution -= structure.upgradeLevel(WASTE_PROCESSING) * 2 ;
    pollution -= structure.upgradeLevel(FUSION_CONFINEMENT) ;
    world.ecology().impingePollution(pollution, this, true) ;
  }
  
  
  protected void checkMeltdownAdvance() {
    if ((! structure.intact()) && meltdown == 0) return ;
    //
    //  Firstly, calculate the overall risk of a meltdown occurring-
    float meltdownChance = 1 - structure.repairLevel() ;
    meltdownChance *= 1 + (stocks.demandFor(POWER) / 20f) ;
    if (! isManned()) meltdownChance *= 2 ;
    if (stocks.amountOf(FUEL_CORES) == 0) meltdownChance /= 5 ;
    meltdownChance /= (1f + structure.upgradeLevel(FEEDBACK_SENSORS)) ;
    //
    //  ...and inflict any actual damage, if your luck is poor.
    if (Rand.num() < (meltdownChance / World.STANDARD_DAY_LENGTH)) {
      final float melt = 0.1f * Rand.num() ;
      meltdown += melt ;
      if (meltdown >= 1) performMeltdown() ;
      final float damage = melt * meltdown * 2 * Rand.num() ;
      structure.takeDamage(damage) ;
    }
  }
  
  
  protected void onDestruction() {
    performMeltdown() ;
  }
  
  
  protected void performMeltdown() {
    final int safety = 1 + structure.upgradeLevel(FUSION_CONFINEMENT) ;
    //
    //  TODO:  INTRODUCE RADIATION VALUES.
    int radiationVal = (125 / safety) - 25 ;
    radiationVal *= meltdown * Rand.avgNums(3) * 2 ;
    final Tile centre = world.tileAt(this) ;
    world.ecology().impingeSqualor(radiationVal, centre, false) ;
    //
    //  TODO:  Add Explosion FX and damage nearby structures (and citizens.)
    //
    //  Damage the structure, but cut back the meltdown somewhat:
    final float damage = structure.repairLevel() * structure.maxIntegrity() ;
    structure.takeDamage(damage * meltdown / safety) ;
    meltdown /= 1 + (Rand.num() * safety) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.CORE_TECHNICIAN } ;
  }
  
  
  public Service[] services() {
    return new Service[] { POWER, ATOMICS } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Reactor" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/reactor_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Reactor provides a copious supply of power to your settlement and "+
      "is essential to manufacturing fuel rods and atomics, but can produce "+
      "dangerous levels of pollution." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ARTIFICER ;
  }
}







