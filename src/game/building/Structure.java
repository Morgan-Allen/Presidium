/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.util.* ;
import java.lang.reflect.* ;



public class Structure {
  
  
  
  /**  Fields, definitions and save/load methods-
    */
  final public static int
    DEFAULT_INTEGRITY  = 100,
    DEFAULT_ARMOUR     = 1,
    DEFAULT_CLOAKING   = 0,
    DEFAULT_BUILD_COST = 50,
    DEFAULT_AMBIENCE   = 0 ;
  final public static float
    BURN_PER_SECOND = 1.0f,
    WEAR_PER_DAY    = 0.1f,
    REGEN_PER_DAY   = 0.2f ;
  
  final public static int
    STATE_NONE    =  0,
    STATE_INSTALL =  1,
    STATE_INTACT  =  2,
    STATE_SALVAGE =  3,
    STATE_RAZED   =  4 ;
  final static String STATE_DESC[] = {
    "N/A",
    "Installing",
    "Complete",
    "Salvaging",
    "N/A"
  } ;
  
  final public static int
    TYPE_VENUE   = 0,
    TYPE_FIXTURE = 1,
    TYPE_VEHICLE = 2,
    TYPE_CRAFTED = 3,
    TYPE_ANCIENT = 4,
    TYPE_ORGANIC = 5 ;
  
  final public static int
    NO_UPGRADES         = 0,
    SMALL_MAX_UPGRADES  = 3,
    NORMAL_MAX_UPGRADES = 6,
    BIG_MAX_UPGRADES    = 12,
    MAX_OF_TYPE         = 3 ;
  final static float UPGRADE_HP_BONUSES[] = {
    0,
    0.15f, 0.25f, 0.35f,
    0.4f , 0.45f, 0.5f ,
    0.5f , 0.55f, 0.55f, 0.6f , 0.6f , 0.65f
  } ;
  
  private static boolean verbose = false ;
  
  
  final Installation basis ;
  
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private int maxUpgrades = NO_UPGRADES ;
  private int
    buildCost     = DEFAULT_BUILD_COST,
    armouring     = DEFAULT_ARMOUR,
    cloaking      = DEFAULT_CLOAKING,
    ambienceVal   = DEFAULT_AMBIENCE,
    structureType = TYPE_VENUE ;
  
  private int state = STATE_INSTALL ;
  private float integrity = baseIntegrity ;
  private boolean burning ;
  
  private float upgradeProgress = 0 ;
  private int upgradeIndex = -1 ;
  private Upgrade upgrades[] = null ;
  private int upgradeStates[] = null ;
  
  
  
  Structure(Installation venue) {
    this.basis = venue ;
  }
  
  
  public void loadState(Session s) throws Exception {
    baseIntegrity = s.loadInt() ;
    maxUpgrades = s.loadInt() ;
    buildCost = s.loadInt() ;
    armouring = s.loadInt() ;
    cloaking  = s.loadInt() ;
    ambienceVal = s.loadInt() ;
    structureType = s.loadInt() ;
    
    state = s.loadInt() ;
    integrity = s.loadFloat() ;
    burning = s.loadBool() ;
    
    Index <Upgrade> AU = basis.allUpgrades() ;
    if (AU != null) {
      upgradeProgress = s.loadFloat() ;
      upgradeIndex = s.loadInt() ;
      upgrades = new Upgrade[maxUpgrades] ;
      upgradeStates = new int[maxUpgrades] ;
      for (int i = 0 ; i < maxUpgrades ; i++) {
        upgrades[i] = AU.loadMember(s.input()) ;
        upgradeStates[i] = s.loadInt() ;
      }
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(baseIntegrity) ;
    s.saveInt(maxUpgrades) ;
    s.saveInt(buildCost) ;
    s.saveInt(armouring) ;
    s.saveInt(cloaking) ;
    s.saveInt(ambienceVal) ;
    s.saveInt(structureType) ;
    
    s.saveInt(state) ;
    s.saveFloat(integrity) ;
    s.saveBool(burning) ;
    
    Index <Upgrade> AU = basis.allUpgrades() ;
    if (AU != null) {
      s.saveFloat(upgradeProgress) ;
      s.saveInt(upgradeIndex) ;
      for (int i = 0 ; i < maxUpgrades ; i++) {
        AU.saveMember(upgrades[i], s.output()) ;
        s.saveInt(upgradeStates[i]) ;
      }
    }
  }
  
  
  public void setupStats(
    int baseIntegrity,
    int armouring,
    int buildCost,
    int maxUpgrades,
    int type
    //boolean organic
  ) {
    this.integrity = this.baseIntegrity = baseIntegrity ;
    this.armouring = armouring ;
    this.buildCost = buildCost ;
    this.structureType = type ;
    //this.organic = organic ;
    
    this.maxUpgrades = maxUpgrades ;
    this.upgrades = new Upgrade[maxUpgrades] ;
    this.upgradeStates = new int[maxUpgrades] ;
  }
  
  
  public void updateStats(int baseIntegrity, int armouring, int cloaking) {
    final float condition = integrity * 1f / maxIntegrity() ;
    this.baseIntegrity = baseIntegrity ;
    this.armouring = armouring ;
    this.cloaking  = cloaking  ;
    integrity = condition * maxIntegrity() ;
    //checkMaintenance() ;
  }
  
  
  public void setAmbienceVal(float val) {
    this.ambienceVal = (int) val ;
  }
  
  
  
  /**  Regular updates-
    */
  protected void updateStructure(int numUpdates) {
    if (numUpdates % 5 == 0) checkMaintenance() ;
    //
    //  Firstly, check to see if you're still burning-
    if (burning) {
      takeDamage(Rand.num() * 2 * BURN_PER_SECOND) ;
      final float damage = maxIntegrity() - integrity ;
      if (armouring * 0.1f > Rand.num() * damage) burning = false ;
      //  TODO:  Consider spreading to nearby structures?
    }
    //
    //  Then, check for gradual wear and tear-
    if (
      (numUpdates % 10 == 0) &&
      takesWear() && (integrity > 0)
    ) {
      float wear = baseIntegrity * WEAR_PER_DAY ;
      wear *= 10f / World.STANDARD_DAY_LENGTH ;
      if (structureType == TYPE_FIXTURE) wear /= 2 ;
      if (structureType == TYPE_CRAFTED) wear *= 2 ;
      if (Rand.num() > armouring / (armouring + DEFAULT_ARMOUR)) {
        takeDamage(wear * Rand.num() * 2) ;
      }
    }
    //
    //  And finally, organic structures can regenerate health-
    if (regenerates()) {
      final float regen = baseIntegrity * REGEN_PER_DAY ;
      repairBy(regen / World.STANDARD_DAY_LENGTH) ;
    }
  }
  
  
  
  /**  Queries and modifications-
    */
  public int maxIntegrity() { return baseIntegrity + upgradeHP() ; }
  public int maxUpgrades() { return upgrades == null ? 0 : maxUpgrades ; }
  
  public int cloaking()  { return cloaking  ; }
  public int armouring() { return armouring ; }
  public int buildCost() { return buildCost ; }
  
  public int ambienceVal() { return intact() ? ambienceVal : 0 ; }
  
  public boolean intact() { return state == STATE_INTACT ; }
  public boolean destroyed() { return state == STATE_RAZED ; }
  public int buildState() { return state ; }
  
  public int repair() { return (int) integrity ; }
  public float repairLevel() { return integrity / maxIntegrity() ; }
  public boolean burning() { return burning ; }
  
  
  public boolean flammable() {
    return structureType == TYPE_VENUE || structureType == TYPE_VEHICLE ;
  }
  
  
  public boolean takesWear() {
    return structureType != TYPE_ANCIENT && structureType != TYPE_ORGANIC ;
  }
  
  
  public boolean regenerates() {
    return structureType == TYPE_ORGANIC ;
  }
  
  
  
  public void setState(int state, float condition) {
    this.state = state ;
    if (condition >= 0) this.integrity = maxIntegrity() * condition ;
    checkMaintenance() ;
  }
  
  
  public float repairBy(float inc) {
    final int max = maxIntegrity() ;
    final float oldI = this.integrity ;
    if (inc < 0 && integrity > max) {
      inc = Math.min(inc, integrity - max) ;
    }
    adjustRepair(inc) ;
    if (inc > Rand.num() * maxIntegrity()) burning = false ;
    return (integrity - oldI) / max ;
  }
  
  
  public void takeDamage(float damage) {
    if (basis.destroyed()) return ;
    if (damage < 0) I.complain("NEGATIVE DAMAGE!") ;
    adjustRepair(0 - damage) ;
    final float burnChance = damage * (1 - repairLevel()) / maxIntegrity() ;
    if (flammable() && Rand.num() < burnChance) burning = true ;
    if (integrity <= 0) basis.onDestruction() ;
  }
  
  
  public void setBurning(boolean burns) {
    if (! flammable()) return ;
    burning = burns ;
  }
  
  
  protected void adjustRepair(float inc) {
    final int max = maxIntegrity() ;
    integrity += inc ;
    if (integrity < 0) {
      state = STATE_RAZED ;
      ((Element) basis).setAsDestroyed() ;
      integrity = 0 ;
      checkMaintenance() ;
    }
    else if (integrity >= max) {
      if (state == STATE_INSTALL) basis.onCompletion() ;
      if (state != STATE_SALVAGE) state = STATE_INTACT ;
      integrity = max ;
    }
    checkMaintenance() ;
  }
  
  
  public boolean goodCondition() {
    return (repairLevel() >= 0.8f) && intact() ;
  }
  
  
  public boolean hasWear() {
    return needsSalvage() || integrity < maxIntegrity() ;
  }
  
  
  public boolean needsSalvage() {
    return state == STATE_SALVAGE || integrity > maxIntegrity() ;
  }
  
  
  public boolean needsUpgrade() {
    return nextUpgradeIndex() != -1 ;
  }
  
  
  protected void checkMaintenance() {
    //  TODO:  In principle, this should be extensible to Vehicles as well.
    if (! (basis instanceof Venue)) return ;
    final World world = ((Venue) basis).world() ;
    if (world == null) return ;
    final boolean needs = (state != STATE_RAZED) && (
      (state == STATE_SALVAGE) || (! goodCondition()) ||
      needsUpgrade() || burning
    ) ;
    if (verbose) I.sayAbout(basis, "Needs maintenance: "+needs) ;
    world.presences.togglePresence(
      basis, world.tileAt(basis), needs, "damaged"
    ) ;
  }
  
  
  protected int upgradeHP() {
    if (upgrades == null) return 0 ;
    int numUsed = 0 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] != null && upgradeStates[i] != STATE_INSTALL) numUsed++ ;
    }
    if (numUsed == 0) return 0 ;
    return (int) (baseIntegrity * UPGRADE_HP_BONUSES[numUsed]) ;
  }
  
  
  
  
  /**  Handling upgrades-
    */
  private int nextUpgradeIndex() {
    if (upgrades == null) return -1 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] != null && upgradeStates[i] != STATE_INTACT) return i ;
    }
    return -1 ;
  }
  
  
  private void deleteUpgrade(int atIndex) {
    final int LI = upgrades.length - 1 ;
    for (int i = atIndex ; i++ < LI ;) {
      upgrades[i - 1] = upgrades[i] ;
      upgradeStates[i - 1] = upgradeStates[i] ;
    }
    upgrades[LI] = null ;
    upgradeStates[LI] = STATE_INSTALL ;
  }
  
  
  public Upgrade upgradeInProgress() {
    if (upgradeIndex == -1) upgradeIndex = nextUpgradeIndex() ;
    if (upgradeIndex == -1) return null ;
    return upgrades[upgradeIndex] ;
  }
  
  
  public float advanceUpgrade(float progress) {
    if (upgradeIndex == -1) upgradeIndex = nextUpgradeIndex() ;
    if (upgradeIndex == -1) return 0 ;
    //
    //  Update progress, and store the change for return later-
    final int US = upgradeStates[upgradeIndex] ;
    final float oldP = upgradeProgress ;
    upgradeProgress = Visit.clamp(upgradeProgress + progress, 0, 1) ;
    float amount = upgradeProgress - oldP ;
    if (US == STATE_SALVAGE) amount *= -0.5f ;
    //
    //  If progress is complete, change the current upgrade's state:
    if (upgradeProgress >= 1) {
      final float condition = integrity * 1f / maxIntegrity() ;
      if (US == STATE_SALVAGE) deleteUpgrade(upgradeIndex) ;
      else upgradeStates[upgradeIndex] = STATE_INTACT ;
      upgradeProgress = 0 ;
      upgradeIndex = -1 ;
      integrity = maxIntegrity() * condition ;
    }
    return amount ;
  }
  
  
  public void beginUpgrade(Upgrade upgrade, boolean checkExists) {
    int atIndex = -1 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      ///I.sayAbout(venue, "Upgrade is: "+upgrades[i]) ;
      if (checkExists && upgrades[i] == upgrade) return ;
      if (upgrades[i] == null) { atIndex = i ; break ; }
    }
    if (atIndex == -1) I.complain("NO ROOM FOR UPGRADE!") ;
    upgrades[atIndex] = upgrade ;
    upgradeStates[atIndex] = STATE_INSTALL ;
    if (upgradeIndex == atIndex) upgradeProgress = 0 ;
    upgradeIndex = nextUpgradeIndex() ;
    checkMaintenance() ;
  }
  
  
  public void resignUpgrade(int atIndex) {
    if (upgrades[atIndex] == null) I.complain("NO SUCH UPGRADE!") ;
    upgradeStates[atIndex] = STATE_SALVAGE ;
    if (upgradeIndex == atIndex) upgradeProgress = 1 - upgradeProgress ;
    checkMaintenance() ;
  }
  
  
  public void resignUpgrade(Upgrade upgrade) {
    for (int i = upgrades.length ; i-- > 0 ;) {
      if (upgrades[i] == upgrade) { resignUpgrade(i) ; return ; }
    }
  }
  
  
  public Batch <Upgrade> workingUpgrades() {
    final Batch <Upgrade> working = new Batch <Upgrade> () ;
    if (upgrades == null) return working ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] != null && upgradeStates[i] == STATE_INTACT) {
        working.add(upgrades[i]) ;
      }
    }
    return working ;
  }
  
  
  public boolean upgradePossible(Upgrade upgrade) {
    //  Consider returning a String explaining the problem, if there is one?
    //  ...Or an error code of some kind?
    if (upgrades == null) return false ;
    boolean isSlot = false, hasReq = upgrade.required == null ;
    int numType = 0 ;
    for (Upgrade u : upgrades) {
      if (u == null) { isSlot = true ; break ; }
      if (u == upgrade.required) hasReq = true ;
      if (u == upgrade) numType++ ;
    }
    return isSlot && hasReq && numType < MAX_OF_TYPE ;
  }
  
  
  public int upgradeBonus(Object refers) {
    if (upgrades == null) return 0 ;
    int bonus = 0 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      final Upgrade u = upgrades[i] ;
      if (u == null || upgradeStates[i] != STATE_INTACT) continue ;
      if (u.refers == refers) bonus += u.bonus ;
    }
    return bonus ;
  }
  
  
  public int upgradeLevel(Upgrade type) {
    if (upgrades == null) return 0 ;
    int num = 0 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] == type && upgradeStates[i] == STATE_INTACT) num++ ;
    }
    return num ;
  }
  
  
  public int numUpgrades() {
    if (upgrades == null) return 0 ;
    int num = 0 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] == null || upgradeStates[i] != STATE_INTACT) continue ;
      num++ ;
    }
    return num ;
  }
  
  
  public float upgradeProgress() {
    return upgradeProgress ;
  }
  
  
  
  /**  Rendering and interface-
    */
  final static String UPGRADE_ERRORS[] = {
    "This facility lacks a prerequisite upgrade",
    "There are no remaining upgrade slots.",
    "No more than 3 upgrades of a single type."
  } ;
  
  
  protected Batch <String> descOngoingUpgrades() {
    final Batch <String> desc = new Batch <String> () ;
    if (upgrades == null) return desc ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] == null || upgradeStates[i] == STATE_INTACT) continue ;
      desc.add(upgrades[i].name+" ("+STATE_DESC[upgradeStates[i]]+")") ;
    }
    return desc ;
  }
  
  
  protected String currentUpgradeDesc() {
    if (upgradeIndex == -1) return null ;
    final Upgrade u = upgrades[upgradeIndex] ;
    return "Installing "+u.name+" ("+(int) (upgradeProgress * 100)+"%)" ;
  }
}

















