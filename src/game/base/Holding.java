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
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



//
//  One person, plus family, per unit of housing.  Whoever has the cash makes
//  the purchases.  (In the case of slum housing the 'family' is really big,
//  and possibly quite fractious.  But they're assumed to share everything.)

//  Requirements come under three headings-
//  Building materials (parts, plastics, inscriptions, decor.)
//  Health and Entertainment (averaged over all occupants.)
//  Safety and Ambience (by location.)

//  Sort out power, water, and life support requirements.  (Bear in mind that a
//  a degree of life support should be provided by global biomass.)

//  Life support- 1 per inhabitant.
//  Power- for freeborn holdings or higher, 1 per tier.
//  Water- for citizen apartments or higher, 1 per tier.

//  pressfeed- for citizen apartments or higher.  Inscription for guildsmen.



public class Holding extends Venue implements BuildConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MAX_SIZE   = 2,
    MAX_HEIGHT = 4,
    NUM_LEVELS = 5,
    NUM_VARS   = 3 ;
  
  final static Conversion CONSTRUCT_NEEDS[] = {
    new Conversion(
      TRIVIAL_DC, ASSEMBLY
    ),
    new Conversion(
      1, PARTS,
      SIMPLE_DC, ASSEMBLY
    ),
    new Conversion(
      2, PARTS, 1, PLASTICS,
      ROUTINE_DC, ASSEMBLY
    ),
    new Conversion(
      3, PARTS, 2, PLASTICS,
      MODERATE_DC, ASSEMBLY
    ),
    new Conversion(
      4, PARTS, 3, PLASTICS, 1, CIRCUITRY,
      DIFFICULT_DC, ASSEMBLY
    ),
  } ;
  final static int
    OCCUPANCIES[] = { 4, 4, 4, 4, 4 },
    TAX_LEVELS[]  = { 0, 5, 10, 20, 35 },
    INTEGRITIES[] = { 15, 35, 80, 125, 200 },
    BUILD_COSTS[] = { 25, 60, 135, 225, 350 } ;
  
  final static String LEVEL_NAMES[] = {
    
    "Dreg Hives",
    "Scavenger Slums",
    "Field Tent",
    
    "Pyon Shacks",
    "Freeborn Holding",
    "Citizen Apartment",
    
    "Guilder Manse",
    "Knighted Estate",
    "Highborn Palace"
  } ;
  
  
  private int upgradeLevel, varID ;
  private List <HoldingExtra> extras = new List <HoldingExtra> () ;
  
  
  public Holding(Base belongs) {
    super(2, 2, ENTRANCE_EAST, belongs) ;
    this.upgradeLevel = 0 ;
    this.varID = Rand.index(NUM_VARS) ;
    structure.setupStats(
      INTEGRITIES[0], 5, BUILD_COSTS[0],
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    attachSprite(modelFor(this).makeSprite()) ;
  }
  
  
  public Holding(Session s) throws Exception {
    super(s) ;
    upgradeLevel = s.loadInt() ;
    varID = s.loadInt() ;
    s.loadObjects(extras) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(upgradeLevel) ;
    s.saveInt(varID) ;
    s.saveObjects(extras) ;
  }
  
  
  public int owningType() {
    return Element.FIXTURE_OWNS ;
  }
  
  
  public int upgradeLevel() {
    return upgradeLevel ;
  }
  
  
  
  /**  Upgrade listings-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Holding.class, "holding_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    TENT_LEVEL = new Upgrade(
      "Field Tent", "", 0, null, 0, null, ALL_UPGRADES
    ),
    PYON_LEVEL = new Upgrade(
      "Pyon Shacks", "", 0, null, 0, null, ALL_UPGRADES
    ),
    FREEBORN_LEVEL = new Upgrade(
      "Freeborn Holding", "", 0, null, 0, PYON_LEVEL, ALL_UPGRADES
    ),
    CITIZEN_LEVEL = new Upgrade(
      "Citizen Apartment", "", 0, null, 0, FREEBORN_LEVEL, ALL_UPGRADES
    ),
    GUILDSMAN_LEVEL = new Upgrade(
      "Guilder Manse", "", 0, null, 0, CITIZEN_LEVEL, ALL_UPGRADES
    ) ;
  
  
  
  /**  Moderating upgrades-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    
    if (personnel.residents().size() == 0) {
      structure.setState(Structure.STATE_SALVAGE, -1) ;
      return ;
    }
    if (! structure.intact()) return ;
    
    
    //
    //  First of all, we check if we have enough of various material goods to
    //  justify an upgrade-
    final float MC = materialConsumption(), margin = 0.501f ;
    boolean devolve = false, upgrade = true ;
    for (Item i : goodsNeeded(upgradeLevel).raw) {
      stocks.bumpItem(i.type, 0 - MC) ;
      if (stocks.amountOf(i) < i.amount - margin) devolve = true ;
    }
    for (Item i : goodsNeeded(upgradeLevel + 1).raw) {
      if (! stocks.hasItem(i)) upgrade = false ;
      stocks.forceDemand(i.type, i.amount + margin, 0) ;
    }
    
    //
    //  Then, check on consumption of foodstuffs-
    final float foodNeed = personnel.residents().size() * 1 ;
    for (Service t : ALL_FOOD_TYPES) {
      if (t == SPICE) {
        //  TODO:  Only noble households demand spice.
        stocks.forceDemand(t, 0, 0) ;
        continue ;
      }
      stocks.forceDemand(t, foodNeed, 0) ;
    }
    int targetLevel = upgradeLevel ;
    //
    //  Update demands for power, water and life support-
    //  TODO:  Put this in a separate method...
    if (targetLevel > 1) {
      stocks.forceDemand(POWER, targetLevel - 1, 0) ;
    }
    if (upgradeLevel > 1) {
      stocks.removeItem(Item.withAmount(POWER, 0.1f * (upgradeLevel - 1))) ;
    }
    //
    //  If so, we update the target upgrade level for the venue-
    if (devolve) targetLevel = upgradeLevel - 1 ;
    else if (upgrade) targetLevel = upgradeLevel + 1 ;
    targetLevel = Visit.clamp(targetLevel, NUM_LEVELS) ;
    checkForUpgrade(targetLevel) ;
    //
    //  And try updating any accessories associated with the structure-
    HoldingExtra.updateExtras(this, extras, numUpdates) ;
  }
  
  
  private void checkForUpgrade(int targetLevel) {
    if (targetLevel == upgradeLevel) return ;
    
    if (targetLevel > upgradeLevel) {
      final Upgrade target = (Upgrade) ALL_UPGRADES.members()[targetLevel] ;
      structure.beginUpgrade(target, true) ;
    }
    else {
      final Upgrade target = (Upgrade) ALL_UPGRADES.members()[upgradeLevel] ;
      structure.resignUpgrade(target) ;
    }
    if ((! structure.needsUpgrade()) && structure.goodCondition()) {
      upgradeLevel = targetLevel ;
      structure.updateStats(INTEGRITIES[targetLevel], 5) ;
      world.ephemera.addGhost(this, MAX_SIZE, sprite(), 2.0f) ;
      attachSprite(modelFor(this).makeSprite()) ;
      setAsEstablished(false) ;
    }
  }
  
  
  
  protected float materialConsumption() {
    final int maxPop = OCCUPANCIES[upgradeLevel] ;
    float count = 0 ;
    for (Actor r : personnel.residents()) if (r.aboard() == this) count++ ;
    count = 0.5f + (count / maxPop) ;
    return Structure.WEAR_PER_DAY * count / World.STANDARD_DAY_LENGTH ;
  }
  
  
  
  private Conversion goodsNeeded(int level) {
    return CONSTRUCT_NEEDS[Visit.clamp(level, NUM_LEVELS)] ;
  }
  
  
  protected float crowding() {
    final int maxPop = OCCUPANCIES[upgradeLevel] ;
    return personnel.residents().size() * 1f / maxPop ;
  }
  
  
  public Service[] goodsNeeded() {
    return new Service[] {
      CARBS, PARTS, PROTEIN, PLASTICS, GREENS, CIRCUITRY
    } ;
  }
  
  
  public Behaviour jobFor(Actor actor) { return null ; }
  protected Background[] careers() { return new Background[0] ; }
  public Service[] services() { return new Service[0] ; }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    IMG_DIR = "media/Buildings/merchant/" ;
  final public static Model
    FIELD_Q_MODEL = ImageModel.asIsometricModel(
      Holding.class, IMG_DIR+"field_tent.png", 2, 1
    ),
    STANDARD_MODELS[][] = ImageModel.fromTextureGrid(
      Holding.class, Texture.loadTexture(IMG_DIR+"all_housing.png"),
      4, 4, 2, ImageModel.TYPE_POPPED_BOX
    ),
    PALACE_MODELS[] = null,
    SLUM_MODELS[][] = null ;
  
  
  public void exitWorld() {
    super.exitWorld() ;
    for (Element e : extras) {
      base().paving.updatePerimeter((Fixture) e, false) ;
      e.setAsDestroyed() ;
    }
  }
  
  
  private static Model modelFor(Holding holding) {
    final int level = holding.upgradeLevel, VID = holding.varID ;
    if (level == 0) return FIELD_Q_MODEL ;
    else if (holding.upgradeLevel > 0) {
      if (level == 5) return PALACE_MODELS[VID] ;
      return STANDARD_MODELS[VID][level - 1] ;
    }
    else return SLUM_MODELS[VID][1 - level] ;
  }
  
  
  public String helpInfo() {
    return
      "Holdings provide comfort and privacy for your subjects, and create "+
      "an additional tax base for revenue." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_SPECIAL ;
  }
  
  
  public String fullName() {
    return LEVEL_NAMES[upgradeLevel + 2] ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;//Texture.loadTexture("media/GUI/Buttons/holding.gif") ;
  }
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "STAFF", "STOCKS" } ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID >= 3) return ;
    else super.writeInformation(d, categoryID, UI) ;
  }
  
  
  
  /**  Static helper methods for placement-
    */
  private static Tile searchPoint(Actor client) {
    if (client.AI.work() instanceof Venue) {
      return ((Venue) client.AI.work()).mainEntrance() ;
    }
    return client.origin() ;
  }


  private static float rateHolding(Actor actor, Holding holding, int level) {
    if (holding == null) return -1 ;
    float rating = 1 ;
    //
    //  TODO:  Base this in part on relations with other residents, possibly of
    //  an explicitly sexual/familial nature?
    rating *= (level + 1) * (2f - holding.crowding()) ;
    if (holding.inWorld()) rating += 0.5f ;
    rating -= actor.AI.greedFor(TAX_LEVELS[level]) * 5 ;
    rating -= Plan.rangePenalty(actor.AI.work(), holding) ;
    ///I.say("  Rating for holding is: "+rating) ;
    return rating ;
  }
  
  
  public static Holding newHoldingFor(Actor client) {
    final World world = client.world() ;
    final int maxDist = World.DEFAULT_SECTOR_SIZE ;
    final Holding holding = new Holding(client.base()) ;
    final Tile origin = searchPoint(client) ;
    final Vars.Bool found = new Vars.Bool() ;
    
    final TileSpread spread = new TileSpread(origin) {
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, origin) > maxDist) return false ;
        return ! t.blocked() ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        holding.setPosition(t.x, t.y, world) ;
        if (holding.canPlace()) { found.val = true ; return true ; }
        return false ;
      }
    } ;
    spread.doSearch() ;
    
    if (found.val == true) return holding ;
    else return null ;
  }
  
  
  //
  //  TODO:  Okay.  Actors should be looking out regularly for new places to
  //  live.  (But maybe this should be triggered by wandering?)
  public static Holding findHoldingFor(Actor client) {
    ///I.say("  Finding holding for: "+client) ;
    final World world = client.world() ;
    final Tile origin = searchPoint(client) ;
    final int maxSearched = 10 ;
    
    Holding best = null ;
    float bestRating = 0 ;
    
    if (client.AI.home() instanceof Holding) {
      final Holding h = (Holding) client.AI.home() ;
      bestRating = rateHolding(client, h, h.upgradeLevel) ;
    }
    
    int numSearched = 0 ;
    for (Object o : world.presences.matchesNear(Holding.class, origin, -1)) {
      if (++numSearched > maxSearched) break ;
      final Holding h = (Holding) o ;
      final float rating = rateHolding(client, h, h.upgradeLevel) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }

    final Holding h = newHoldingFor(client) ;
    if (h != null) {
      final float rating = rateHolding(client, h, h.upgradeLevel) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }
    
    return best ;
  }
}
















