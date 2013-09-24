


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class AirProcessor extends Venue implements BuildConstants {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    AirProcessor.class, "media/Buildings/ecologist/air_processor.png", 4, 2
  ) ;
  
  
  protected List <Crawler> crawlers = new List <Crawler> () ;
  protected float soilSamples = 0, monitorVal = 0 ;
  

  public AirProcessor(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      500, 15, 300,
      VenueStructure.SMALL_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public AirProcessor(Session s) throws Exception {
    super(s) ;
    s.loadObjects(crawlers) ;
    soilSamples = s.loadFloat() ;
    monitorVal = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(crawlers) ;
    s.saveFloat(soilSamples) ;
    s.saveFloat(monitorVal) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    AirProcessor.class, "air_processor_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    
    CARBONS_CYCLING = new Upgrade(
      "Carbons Cycling",
      "Improves output of petrocarbs and life support, speeds terraforming "+
      "and combats pollution.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    
    WATER_CYCLE_INTEGRATION = new Upgrade(
      "Water Cycle Integration",
      "Increases efficiency around desert and oceans terrain, and dispenses "+
      "small amounts of water.",
      200,
      null, 1, null, ALL_UPGRADES
    ),

    DUST_PANNING = new Upgrade(
      "Dust Panning",
      "Permits modest output of metal ores and fuel cores, and installs "+
      "automated crawlers to gather soil samples.",
      150,
      null, 1, CARBONS_CYCLING, ALL_UPGRADES
    ),
    
    SPICE_REDUCTION = new Upgrade(
      "Spice Reduction",
      "Employs microbial culture to capture minute quantities of spice from "+
      "the surrounding environment.",
      200,
      null, 1, WATER_CYCLE_INTEGRATION, ALL_UPGRADES
    ) ;
  
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  Consider upkeep, deliveries and supervision-
    choice.add(Deliveries.nextDeliveryFor(actor, this, services(), 5, world)) ;
    choice.add(new Building(actor, this)) ;
    if (! Planet.isNight(world)) choice.add(new Supervision(actor, this)) ;
    //
    //  Have the climate engineer gather soil samples, but only if they're
    //  very low.  (Automated crawlers would do this in bulk.)
    final Tile toSample = Spacing.pickRandomTile(this, 16, world) ;
    if (soilSamples < 2 && toSample.pathType() == Tile.PATH_CLEAR) {
      final Action actionSample = new Action(
        actor, toSample,
        this, "actionSoilSample",
        Action.BUILD, "Gathering soil samples"
      ) ;
      actionSample.setPriority(Action.ROUTINE) ;
      choice.add(actionSample) ;
    }
    if (actor.gear.amountOf(CRATES) > 0) {
      final Action returnSample = new Action(
        actor, this,
        this, "actionReturnSample",
        Action.LOOK, "Returning soil samples"
      ) ;
      returnSample.setPriority(Action.ROUTINE) ;
      choice.add(returnSample) ;
    }
    //
    //  Select and return-
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  public boolean actionSoilSample(Actor actor, Tile spot) {
    final boolean success = actor.traits.test(GEOPHYSICS, MODERATE_DC, 1) ;
    if (success) {
      final Item sample = Item.withType(CRATES, spot) ;
      actor.gear.addItem(sample) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionReturnSample(Actor actor, AirProcessor works) {
    for (Item sample : actor.gear.matches(CRATES)) {
      final Tile t = (Tile) sample.refers ;
      actor.gear.removeItem(sample) ;
      works.soilSamples += t.habitat().minerals() / 10f ;
    }
    return true ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.CLIMATE_ENGINEER) {
      return nO + 1 ;
    }
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    handleCrawlerOrders() ;
    //
    //  Output the various goods, depending on terrain, supervision and upgrade
    //  levels-
    final float
      waterBonus = structure.upgradeLevel(WATER_CYCLE_INTEGRATION),
      dustBonus = structure.upgradeLevel(DUST_PANNING),
      carbonBonus = structure.upgradeLevel(CARBONS_CYCLING),
      spiceBonus = structure.upgradeLevel(SPICE_REDUCTION) ;
    int powerNeed = 4 + (structure.numUpgrades() * 2) ;
    float yield = 2.0f / World.STANDARD_DAY_LENGTH ;
    stocks.incDemand(POWER, powerNeed, 1) ;
    stocks.bumpItem(POWER, powerNeed * -0.1f) ;
    yield *= stocks.amountOf(POWER) / powerNeed ;
    //
    //  Sample the local terrain and see if you get an extraction bonus-
    final Vec3D p = this.position(null) ;
    final Box2D area = new Box2D().set(p.x, p.y, 0, 0) ;
    area.expandBy(World.DEFAULT_SECTOR_SIZE) ;
    area.cropBy(new Box2D().set(0, 0, world.size - 1, world.size - 1)) ;
    float sumWater = 0, sumDesert = 0 ;
    //
    //  We employ random sampling for efficiency (and lolz.)
    for (int n = 10 ; n-- > 0 ;) {
      final Tile sampled = world.tileAt(
        Rand.rangeAvg(area.xpos(), area.xmax(), 2),
        Rand.rangeAvg(area.ypos(), area.ymax(), 2)
      ) ;
      final Habitat h = sampled.habitat() ;
      if (h == Habitat.OCEAN) sumWater += 10 ;
      else sumWater += h.moisture() / 2f ;
      if (h == Habitat.DESERT) sumDesert += 10 ;
      else sumDesert += (10 - h.moisture()) / 2f ;
    }
    sumWater /= 100 ;
    sumDesert /= 100 ;
    final float cycleBonus = (waterBonus * sumWater * sumDesert * 4) ;
    //
    //  Also, see if any soil samples have been collected lately.  (This bonus
    //  is higher if the venue is presently well-supervised.)
    float soilBonus = soilSamples / 5f ;
    final Actor mans = (Actor) Rand.pickFrom(personnel.workers()) ;
    if (mans.aboard() == this) {
      if (! mans.traits.test(GEOPHYSICS, SIMPLE_DC, 0.5f)) soilBonus /= 1.5f ;
      if (mans.traits.test(GEOPHYSICS, DIFFICULT_DC, 0.5f)) soilBonus *= 1.5f ;
    }
    //
    //  Finally, output the various biproducts and modify pollution/climate-
    yield *= 1 + cycleBonus + soilBonus ;
    
    stocks.bumpItem(WATER, yield * (1 + waterBonus) * sumWater) ;
    stocks.bumpItem(LIFE_SUPPORT, carbonBonus / World.STANDARD_DAY_LENGTH) ;
    stocks.bumpItem(PETROCARBS, carbonBonus / World.STANDARD_DAY_LENGTH) ;
    
    stocks.bumpItem(SPICE, yield * spiceBonus) ;
    stocks.bumpItem(METAL_ORE, yield * dustBonus) ;
    stocks.bumpItem(FUEL_CORES, yield * dustBonus) ;
    
    world.ecology().impingePollution(-2 * carbonBonus, this, true) ;
    final int mag = World.DEFAULT_SECTOR_SIZE ;
    world.ecology().pushClimate(Habitat.MEADOW, mag * mag * 5 * yield) ;
  }
  
  
  protected void handleCrawlerOrders() {
    //
    //  Update the proper number of automated crawlers.
    int numCrawlers = (1 + structure.upgradeLevel(DUST_PANNING)) / 2 ;
    if (crawlers.size() < numCrawlers) {
      final Crawler crawler = new Crawler() ;
      crawler.setHangar(this) ;
      crawlers.add(crawler) ;
    }
    if (crawlers.size() > numCrawlers) {
      for (Crawler c : crawlers) if (c.aboard() == this) {
        c.setAsDestroyed() ;
      }
    }
    //
    //  Send them out to collect soil samples, or bring them back to the venue-
    for (Crawler c : crawlers) {
      if (c.destroyed()) crawlers.remove(c) ;
      if (c.aboard() == this) {
        for (Item sample : c.cargo.matches(CRATES)) {
          final Tile sampled = (Tile) sample.refers ;
          soilSamples += sampled.habitat().minerals() / 20f ;
        }
        final Tile toSample = Spacing.pickRandomTile(this, 16, world) ;
        if (soilSamples < 10 && toSample.pathType() == Tile.PATH_CLEAR) {
          c.pathing.updateTarget(toSample) ;
        }
      }
      else {
        if (c.pathing.target() == c.aboard()) {
          final Tile sampled = c.origin() ;
          c.cargo.addItem(Item.withType(CRATES, sampled)) ;
        }
      }
    }
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.CORE_TECHNICIAN } ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      METAL_ORE, FUEL_CORES, PETROCARBS,
      SPICE, WATER, LIFE_SUPPORT
    } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Air Processor" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/air_processor_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Air Processors can modify the content of your planet's atmosphere, "+
      "helping to speed terraforming efforts and extract rare or heavy "+
      "elements as an economic biproduct." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}



