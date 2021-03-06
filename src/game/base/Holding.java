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
//
//  TODO:  Allow actors to study, relax, make love, play with kids, etc. at
//  home.


public class Holding extends Venue implements Economy {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MAX_SIZE   = 2,
    MAX_HEIGHT = 4,
    NUM_VARS   = 3 ;
  final static float
    CHECK_INTERVAL = 10,
    TEST_INTERVAL  = World.STANDARD_DAY_LENGTH,
    UPGRADE_THRESH = 0.66f,
    DEVOLVE_THRESH = 0.66f ;
  
  private static boolean verbose = true ;
  
  
  private int upgradeLevel, targetLevel, varID ;
  private List <HoldingExtra> extras = new List <HoldingExtra> () ;
  private int numTests = 0, upgradeCounter, devolveCounter ;
  
  
  
  public Holding(Base belongs) {
    super(2, 2, ENTRANCE_EAST, belongs) ;
    this.upgradeLevel = 0 ;
    this.varID = Rand.index(NUM_VARS) ;
    structure.setupStats(
      HoldingUpgrades.INTEGRITIES[0], 5, HoldingUpgrades.BUILD_COSTS[0],
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    attachSprite(modelFor(this).makeSprite()) ;
  }
  
  
  public Holding(Session s) throws Exception {
    super(s) ;
    upgradeLevel = s.loadInt() ;
    targetLevel  = s.loadInt() ;
    varID = s.loadInt() ;
    s.loadObjects(extras) ;
    numTests = s.loadInt() ;
    upgradeCounter = s.loadInt() ;
    devolveCounter = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(upgradeLevel) ;
    s.saveInt(targetLevel ) ;
    s.saveInt(varID) ;
    s.saveObjects(extras) ;
    s.saveInt(numTests) ;
    s.saveInt(upgradeCounter) ;
    s.saveInt(devolveCounter) ;
  }
  
  
  public int owningType() {
    return Element.FIXTURE_OWNS ;
  }
  
  
  public int upgradeLevel() {
    return upgradeLevel ;
  }
  
  
  
  /**  Upgrade listings-
    */
  public Index <Upgrade> allUpgrades() { return HoldingUpgrades.ALL_UPGRADES ; }

  
  
  
  /**  Moderating upgrades-
    */
  public void updateAsScheduled(int numUpdates) {
    if (numUpdates % 10 == 0 && structure.intact()) {
      HoldingExtra.updateExtras(this, extras, numUpdates) ;
    }
    super.updateAsScheduled(numUpdates) ;
    
    if (! structure.intact()) return ;
    consumeMaterials() ;
    updateDemands(upgradeLevel + 1) ;
    impingeSqualor() ;

    final int CHECK_TIME = 10 ;
    if (numUpdates % CHECK_TIME == 0) checkForUpgrade(CHECK_TIME) ;
    
    if (
      (targetLevel != upgradeLevel) &&
      (! structure.needsUpgrade()) &&
      structure.goodCondition()
    ) {
      upgradeLevel = targetLevel ;
      structure.updateStats(HoldingUpgrades.INTEGRITIES[targetLevel], 5, 0) ;
      world.ephemera.addGhost(this, MAX_SIZE, sprite(), 2.0f) ;
      attachModel(modelFor(this)) ;
      setAsEstablished(false) ;
    }
  }
  
  
  private boolean needsMet(int meetLevel) {
    if (personnel.residents().size() == 0) return false ;
    if (meetLevel <= HoldingUpgrades.LEVEL_TENT   ) return true  ;
    if (meetLevel >  HoldingUpgrades.LEVEL_GUILDER) return false ;
    final Object met = HoldingUpgrades.NEEDS_MET ;
    return
      HoldingUpgrades.checkAccess   (this, meetLevel, false) == met &&
      HoldingUpgrades.checkMaterials(this, meetLevel, false) == met &&
      HoldingUpgrades.checkSupport  (this, meetLevel, false) == met &&
      HoldingUpgrades.checkRations  (this, meetLevel, false) == met &&
      HoldingUpgrades.checkSpecial  (this, meetLevel, false) == met &&
      HoldingUpgrades.checkSurrounds(this, meetLevel, false) == met ;
  }
  
  
  private void checkForUpgrade(int CHECK_TIME) {
    
    boolean devolve = false, upgrade = false ;
    if (! needsMet(upgradeLevel)) devolve = true ;
    else if (needsMet(upgradeLevel + 1)) upgrade = true ;
    
    final boolean empty = personnel.residents().size() == 0 ;
    if (empty) { devolve = true ; upgrade = false ; }
    
    numTests += CHECK_TIME ;
    if (devolve) devolveCounter += CHECK_TIME ;
    if (upgrade) upgradeCounter += CHECK_TIME ;
    
    if (numTests >= TEST_INTERVAL) {
      targetLevel = upgradeLevel ;
      if (devolveCounter * 1f / numTests > DEVOLVE_THRESH) devolve = true ;
      if (upgradeCounter * 1f / numTests > UPGRADE_THRESH) upgrade = true ;
      if (devolve) targetLevel-- ;
      if (upgrade) targetLevel++ ;
      numTests = devolveCounter = upgradeCounter = 0 ;
      targetLevel = Visit.clamp(targetLevel, HoldingUpgrades.NUM_LEVELS) ;
      
      if (verbose && I.talkAbout == this) {
        if (numTests == 0) I.say("HOUSING TEST INTERVAL COMPLETE") ;
        I.say("Upgrade/Target levels: "+upgradeLevel+"/"+targetLevel) ;
        I.say("Could upgrade? "+upgrade+", devolve? "+devolve) ;
        I.say("Is Empty? "+empty) ;
      }
      
      if (devolve && empty) {
        if (verbose) I.sayAbout(this, "HOUSING IS CONDEMNED") ;
        structure.setState(Structure.STATE_SALVAGE, -1) ;
      }
      
      if (targetLevel == upgradeLevel) return ;
      final Object HU[] = HoldingUpgrades.ALL_UPGRADES.members() ;
      
      if (targetLevel > upgradeLevel) {
        final Upgrade target = (Upgrade) HU[ targetLevel] ;
        structure.beginUpgrade(target, true) ;
      }
      else {
        final Upgrade target = (Upgrade) HU[upgradeLevel] ;
        structure.resignUpgrade(target) ;
      }
    }
  }
  
  
  private void consumeMaterials() {
    //
    //  Decrement stocks and update demands-
    final float wear = Structure.WEAR_PER_DAY / World.STANDARD_DAY_LENGTH ;
    final int maxPop = HoldingUpgrades.OCCUPANCIES[upgradeLevel] ;
    float count = 0 ;
    for (Actor r : personnel.residents()) if (r.aboard() == this) count++ ;
    count = 0.5f + (count / maxPop) ;
    //
    //  Power, water and life support are consumed at a fixed rate, but other
    //  materials wear out depending on use (and more slowly.)
    for (Item i : HoldingUpgrades.materials(upgradeLevel).raw) {
      if (i.type.form == FORM_PROVISION) {
        stocks.bumpItem(i.type, i.amount * -0.1f) ;
      }
      else {
        stocks.bumpItem(i.type, i.amount * count * -wear) ;
      }
    }
  }
  
  
  private void updateDemands(int targetLevel) {
    targetLevel = Visit.clamp(targetLevel, HoldingUpgrades.NUM_LEVELS) ;
    
    for (Item i : HoldingUpgrades.materials(targetLevel).raw) {
      stocks.forceDemand(i.type, i.amount + 0.5f, VenueStocks.TIER_CONSUMER) ;
    }
    
    final float supportNeed = HoldingUpgrades.supportNeed(this, targetLevel) ;
    stocks.forceDemand(LIFE_SUPPORT, supportNeed, VenueStocks.TIER_CONSUMER) ;
    
    for (Item i : HoldingUpgrades.rationNeeds(this, targetLevel)) {
      stocks.forceDemand(i.type, i.amount, VenueStocks.TIER_CONSUMER) ;
    }

    for (Item i : HoldingUpgrades.specialGoods(this, targetLevel)) {
      stocks.forceDemand(i.type, i.amount, VenueStocks.TIER_CONSUMER) ;
    }
  }
  
  
  private void impingeSqualor() {
    int ambience = 1 + ((upgradeLevel - 2) * 2) ;
    ambience += (extras.size() * upgradeLevel) / 2 ;
    structure.setAmbienceVal(ambience) ;
  }
  
  
  public Service[] goodsNeeded() {
    
    final Batch <Service> needed = new Batch <Service> () ;
    int targetLevel = upgradeLevel + 1 ;
    targetLevel = Visit.clamp(targetLevel, HoldingUpgrades.NUM_LEVELS) ;
    
    //  Combine the listing of non-provisioned materials and demand for rations.
    //  (Note special goods, like pressfeed and datalinks, are delivered to the
    //  holding externally, and so are not included here.)
    for (Item i : HoldingUpgrades.materials(targetLevel).raw) {
      if (i.type.form == FORM_PROVISION) continue ;
      needed.add(i.type) ;
    }
    for (Item i : HoldingUpgrades.rationNeeds(this, targetLevel)) {
      needed.add(i.type) ;
    }
    return needed.toArray(Service.class) ;
  }
  
  
  protected List <HoldingExtra> extras() {
    return extras ;
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    final Service goods[] = goodsNeeded() ;
    final Delivery d = Deliveries.nextCollectionFor(
      actor, this, goods, 5, actor, actor.world()
    ) ;
    if (d != null) d.shouldPay = actor ;
    return d ;
  }
  
  
  public Background[] careers() { return new Background[0] ; }
  public Service[] services() { return new Service[0] ; }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    IMG_DIR = "media/Buildings/civilian/" ;
  final public static Model
    LOWER_CLASS_MODELS[][] = ImageModel.fromTextureGrid(
      Holding.class, Texture.loadTexture(IMG_DIR+"lower_class_housing.png"),
      3, 3, 2, ImageModel.TYPE_SOLID_BOX
    ),
    MIDDLE_CLASS_MODELS[][] = ImageModel.fromTextureGrid(
      Holding.class, Texture.loadTexture(IMG_DIR+"middle_class_housing.png"),
      3, 3, 2, ImageModel.TYPE_SOLID_BOX
    ),
    UPPER_CLASS_MODELS[][] = null ;
  
  
  public void exitWorld() {
    super.exitWorld() ;
    HoldingExtra.removeExtras(this, extras) ;
  }
  
  
  private static Model modelFor(Holding holding) {
    final int level = holding.upgradeLevel, VID = holding.varID ;
    if (level <= 1) {
      return LOWER_CLASS_MODELS[VID][level + 1] ;
    }
    if (level >= 5) return UPPER_CLASS_MODELS[VID][level - 5] ;
    return MIDDLE_CLASS_MODELS[VID][level - 2] ;
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
    return HoldingUpgrades.LEVEL_NAMES[upgradeLevel + 2] ;
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
    if (categoryID == 0) {
      final String
        uS = needMessage(upgradeLevel),
        tS = needMessage(upgradeLevel + 1) ;
      if (uS != null) {
        d.append("\n\n") ;
        d.append(uS) ;
      }
      else if (tS != null) {
        d.append("\n\n") ;
        d.append(tS) ;
      }
    }
  }
  
  
  private String needMessage(int meetLevel) {
    meetLevel = Visit.clamp(meetLevel, HoldingUpgrades.NUM_LEVELS) ;
    final Object met = HoldingUpgrades.NEEDS_MET ;
    final Object
      access    = HoldingUpgrades.checkAccess   (this, meetLevel, true),
      materials = HoldingUpgrades.checkMaterials(this, meetLevel, true),
      support   = HoldingUpgrades.checkSupport  (this, meetLevel, true),
      rations   = HoldingUpgrades.checkRations  (this, meetLevel, true),
      special   = HoldingUpgrades.checkSpecial  (this, meetLevel, true),
      surrounds = HoldingUpgrades.checkSurrounds(this, meetLevel, true) ;
    if (access    != met) return (String) access    ;
    if (materials != met) return (String) materials ;
    if (support   != met) return (String) support   ;
    if (rations   != met) return (String) rations   ;
    if (special   != met) return (String) special   ;
    if (surrounds != met) return (String) surrounds ;
    return null ;
  }
}








