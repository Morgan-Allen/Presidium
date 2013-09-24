


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
    AirProcessor.class, "media/Buildings/ecologist/air_processor.png", 3, 2
  ) ;
  
  private static boolean verbose = false ;
  
  
  protected List <Crawler> crawlers = new List <Crawler> () ;
  protected float soilSamples = 0, monitorVal = 0 ;
  

  public AirProcessor(Base base) {
    super(3, 2, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      500, 15, 300,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
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
      300,
      null, 1, WATER_CYCLE_INTEGRATION, ALL_UPGRADES
    ) ;
  
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  Consider upkeep, deliveries and supervision-
    choice.add(Deliveries.nextDeliveryFor(actor, this, services(), 10, world)) ;
    choice.add(new Building(actor, this)) ;
    if (! Planet.isNight(world)) choice.add(new Supervision(actor, this)) ;
    //
    //  Have the climate engineer gather soil samples, but only if they're
    //  very low.  (Automated crawlers would do this in bulk.)
    final Tile toSample = pickSample() ;
    if (soilSamples < 2 && toSample != null) {
      final Action actionSample = new Action(
        actor, toSample,
        this, "actionSoilSample",
        Action.BUILD, "Gathering soil samples"
      ) ;
      actionSample.setProperties(Action.QUICK) ;
      actionSample.setPriority(Action.ROUTINE) ;
      choice.add(actionSample) ;
    }
    final float numSamples = actor.gear.amountOf(SAMPLES) ;
    if (numSamples > 0) {
      final Action returnSample = new Action(
        actor, this,
        this, "actionReturnSample",
        Action.LOOK, "Returning soil samples"
      ) ;
      returnSample.setProperties(Action.QUICK) ;
      returnSample.setPriority(Action.ROUTINE + numSamples - 1) ;
      choice.add(returnSample) ;
    }
    //
    //  Select and return-
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  private Tile pickSample() {
    final int range = World.DEFAULT_SECTOR_SIZE * 2 ;
    Tile picked = null ;
    float bestRating = 0 ;
    for (int n = 10 ; n-- > 0 ;) {
      final Tile s = Spacing.pickRandomTile(this, range, world) ;
      if (s == null || s.pathType() != Tile.PATH_CLEAR) continue ;
      float rating = s.habitat().minerals() ;
      rating /= 10 + Spacing.distance(s, this) ;
      if (rating > bestRating) { picked = s ; bestRating = rating ; }
    }
    return picked ;
  }
  
  
  public boolean actionSoilSample(Actor actor, Tile spot) {
    final boolean success = actor.traits.test(GEOPHYSICS, MODERATE_DC, 1) ;
    for (int n = success ? (1 + Rand.index(3)) : 1 ; n-- > 0 ;) {
      final Item sample = Item.withType(SAMPLES, spot) ;
      actor.gear.addItem(sample) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionReturnSample(Actor actor, AirProcessor works) {
    for (Item sample : actor.gear.matches(SAMPLES)) {
      final Tile t = (Tile) sample.refers ;
      actor.gear.removeItem(sample) ;
      if (verbose) I.sayAbout(actor, "Sample size: "+t.habitat().minerals()) ;
      works.soilSamples += (t.habitat().minerals() / 10f) + 0.5f ;
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
    
    if (verbose) I.sayAbout(this, "\nUPDATING OUTPUT OF "+this) ;
    handleCrawlerOrders() ;
    //
    //  Output the various goods, depending on terrain, supervision and upgrade
    //  levels-
    final float
      waterBonus  = 2 + structure.upgradeLevel(WATER_CYCLE_INTEGRATION),
      carbonBonus = 1 + structure.upgradeLevel(CARBONS_CYCLING),
      dustBonus   = 1 + (structure.upgradeLevel(DUST_PANNING) * 2),
      spiceBonus  = structure.upgradeLevel(SPICE_REDUCTION) / 2 ;
    final float
      SDL = World.STANDARD_DAY_LENGTH ;
    
    int powerNeed = 4 + (structure.numUpgrades() * 2) ;
    stocks.incDemand(POWER, powerNeed, 1) ;
    stocks.bumpItem(POWER, powerNeed * -0.1f) ;
    float yield = 2 * Math.min(1, stocks.amountOf(POWER) * 2 / powerNeed) ;
    
    if (verbose) I.sayAbout(this, "  Basic yield is: "+yield) ;
    
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
    
    if (verbose) I.sayAbout(
      this, "  Water cycle bonus: "+cycleBonus+
      ", water/desert: "+sumWater+"/"+sumDesert
    ) ;
    //
    //  Also, see if any soil samples have been collected lately.  (This bonus
    //  is higher if the venue is presently well-supervised.)
    float soilBonus = soilSamples / 5f ;
    final Actor mans = (Actor) Rand.pickFrom(personnel.workers()) ;
    if (mans != null && mans.aboard() == this) {
      if (! mans.traits.test(GEOPHYSICS, SIMPLE_DC, 0.5f)) soilBonus /= 1.5f ;
      if (mans.traits.test(GEOPHYSICS, DIFFICULT_DC, 0.5f)) soilBonus *= 1.5f ;
    }
    else soilBonus /= 2 ;
    
    if (verbose) I.sayAbout(this, "  Soil samples "+soilSamples+
      ", bonus: "+soilBonus
    ) ;
    
    //
    //  Here, we handle the lighter/more rarified biproducts-
    yield *= 1 + cycleBonus ;
    if (verbose) I.sayAbout(this, "  Yield/day with cycle bonus: "+yield) ;
    stocks.bumpItem(WATER, yield * (1 + waterBonus) * sumWater * 10 / SDL, 15) ;
    stocks.bumpItem(LIFE_SUPPORT, yield * carbonBonus * 100 / SDL, 15) ;
    stocks.bumpItem(PETROCARBS, yield * carbonBonus / SDL, 15) ;
    
    //
    //  And here, the heavier elements-
    soilSamples = Visit.clamp(soilSamples - (10f / SDL), 0, 10) ;
    yield *= 1 + soilBonus ;
    if (verbose) I.sayAbout(this, "  Yield/day with soil bonus: "+yield) ;
    stocks.bumpItem(SPICE, yield * spiceBonus / SDL, 10) ;
    stocks.bumpItem(METAL_ORE, yield * dustBonus / SDL, 10) ;
    stocks.bumpItem(FUEL_CORES, yield * dustBonus / SDL, 10) ;
    
    //
    //  In either cause, modify pollution and climate effects-
    //
    //  TODO:  Actually, arrange things so that the processor increases *local*
    //  pollution, while reducing global pollution (because it's messy and
    //  noisy, but good for the atmosphere.)  Not In My Backyard, IOW.
    
    world.ecology().impingePollution(-2 * carbonBonus * yield, this, true) ;
    final int mag = World.DEFAULT_SECTOR_SIZE ;
    world.ecology().pushClimate(Habitat.MEADOW, mag * mag * 5 * yield) ;
  }
  
  
  protected void handleCrawlerOrders() {
    //
    //  Send them out to collect soil samples, or bring them back to the venue-
    for (Crawler c : crawlers) {
      if (verbose) I.sayAbout(c, "Updating orders for "+c) ;
      if (c.destroyed()) {
        if (verbose) I.sayAbout(c, c+" was destroyed!") ;
        crawlers.remove(c) ;
        continue ;
      }
      if (c.aboard() == this) {
        for (Item sample : c.cargo.matches(SAMPLES)) {
          final Tile sampled = (Tile) sample.refers ;
          soilSamples += sampled.habitat().minerals() / 20f ;
          c.cargo.removeItem(sample) ;
        }
        final Tile toSample = pickSample() ;
        if (toSample != null && soilSamples < 10) {
          c.pathing.updateTarget(toSample) ;
        }
      }
      else {
        if (c.pathing.target() == c.aboard()) {
          final Tile sampled = c.origin() ;
          c.cargo.addItem(Item.withType(SAMPLES, sampled)) ;
          c.pathing.updateTarget(this) ;
        }
      }
    }
    //
    //  Update the proper number of automated crawlers.
    int numCrawlers = (1 + structure.upgradeLevel(DUST_PANNING)) / 2 ;
    I.sayAbout(this, "Updating crawlers.  Proper count: "+numCrawlers) ;
    if (crawlers.size() < numCrawlers) {
      final Crawler crawler = new Crawler() ;
      crawler.enterWorldAt(this, world) ;
      crawler.goAboard(this, world) ;
      crawler.setHangar(this) ;
      crawlers.add(crawler) ;
    }
    if (crawlers.size() > numCrawlers) {
      for (Crawler c : crawlers) if (c.aboard() == this) {
        if (verbose) I.sayAbout(this, "Too many crawlers.  Salvaging: "+c) ;
        //  TODO:  PERFORM ACTUAL CONSTRUCTION/SALVAGE
        c.setAsDestroyed() ;
        crawlers.remove(c) ;
        return ;
      }
    }
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.CLIMATE_ENGINEER } ;
  }
  
  
  public Service[] services() {
    return new Service[] {
      METAL_ORE, FUEL_CORES, PETROCARBS,
      SPICE, WATER, LIFE_SUPPORT
    } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
     0, -0.0f,
     0,  0.9f,
     0,  1.8f,
     0,  2.5f,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { METAL_ORE, FUEL_CORES, PETROCARBS, SPICE } ;
  }
  
  //
  //  TODO:  You have to show items in the back as well, behind a sprite
  //  overlay for the facade of the structure.
  protected float goodDisplayAmount(Service good) {
    return Math.min(5, stocks.amountOf(good)) ;
  }
  
  
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
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    if (categoryID == 0) {
      d.append("\n\n  Soil Samples: "+(int) (soilSamples + 0.5f)) ;
    }
  }
  
}









