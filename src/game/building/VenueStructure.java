/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.user.BaseUI;
import src.util.* ;



//
//  Modify this so that Vehicles can possess it too?  Put in an interface?
//
//  I want to have some kind of external progress metre for research/upgrades.


public class VenueStructure extends Inventory {
  
  
  
  /**  Fields, definitions and save/load methods-
    */
  final static int
    DEFAULT_INTEGRITY  = 100,
    DEFAULT_ARMOUR     = 10,
    DEFAULT_BUILD_COST = 200 ;
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
    NO_UPGRADES         = 0,
    SMALL_MAX_UPGRADES  = 3,
    NORMAL_MAX_UPGRADES = 6,
    BIG_MAX_UPGRADES    = 12,
    MAX_OF_TYPE         = 3 ;
  final static float UPGRADE_HP_BONUSES[] = {
    //0.15f, 0.1f, 0.1f, 0.5f, 0.5f, 0.5f
    0.15f, 0.25f, 0.35f,
    0.4f , 0.45f, 0.5f ,
    0.5f , 0.55f, 0.55f, 0.6f , 0.6f , 0.65f
  } ;
  
  
  final Venue venue ;
  
  //
  //  These don't actually need to be supplied on an individual basis.  They
  //  belong to the class, rather than to the object.  Maybe Upgrades could
  //  serve this function, as a set of default stats for the structure?
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private int maxUpgrades = NO_UPGRADES ;
  private int
    buildCost = DEFAULT_BUILD_COST,
    armouring = DEFAULT_ARMOUR ;
  private boolean organic ;
  //  private Item materials[] ;

  private int state = STATE_INSTALL ;
  private float integrity = baseIntegrity ;
  private boolean burning ;
  
  private float upgradeProgress = 0 ;
  private int upgradeIndex = -1 ;
  private Upgrade upgrades[] = null ;
  private int upgradeStates[] = null ;
  
  
  
  VenueStructure(Venue venue) {
    super(venue) ;
    this.venue = venue ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    baseIntegrity = s.loadInt() ;
    maxUpgrades = s.loadInt() ;
    buildCost = s.loadInt() ;
    armouring = s.loadInt() ;
    organic = s.loadBool() ;
    
    state = s.loadInt() ;
    integrity = s.loadFloat() ;
    burning = s.loadBool() ;
    
    final Index <Upgrade> AU = venue.allUpgrades() ;
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
    super.saveState(s) ;
    s.saveInt(baseIntegrity) ;
    s.saveInt(maxUpgrades) ;
    s.saveInt(buildCost) ;
    s.saveInt(armouring) ;
    s.saveBool(organic) ;
    
    s.saveInt(state) ;
    s.saveFloat(integrity) ;
    s.saveBool(burning) ;
    
    final Index <Upgrade> AU = venue.allUpgrades() ;
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
    boolean organic
  ) {
    this.integrity = this.baseIntegrity = baseIntegrity ;
    this.armouring = armouring ;
    this.buildCost = buildCost ;
    this.organic = organic ;
    
    this.maxUpgrades = maxUpgrades ;
    this.upgrades = new Upgrade[maxUpgrades] ;
    this.upgradeStates = new int[maxUpgrades] ;
  }
  
  
  public void updateStats(int baseIntegrity, int armouring) {
    this.baseIntegrity = baseIntegrity ;
    this.armouring = armouring ;
    checkMaintenance() ;
  }
  
  
  
  /**  Queries and modifications-
    */
  public int maxIntegrity() { return baseIntegrity + upgradeHP() ; }
  public int armouring() { return armouring ; }
  public int maxUpgrades() { return upgrades == null ? 0 : maxUpgrades ; }
  
  public boolean intact() { return state == STATE_INTACT ; }
  public boolean destroyed() { return state == STATE_RAZED ; }
  public int buildState() { return state ; }
  
  public int repair() { return (int) integrity ; }
  public float repairLevel() { return integrity / maxIntegrity() ; }
  public boolean burning() { return burning ; }
  
  
  public void setState(int state, float condition) {
    this.state = state ;
    this.integrity = maxIntegrity() * condition ;
    checkMaintenance() ;
  }
  
  
  public void repairBy(float inc) {
    final int max = maxIntegrity() ;
    if (inc < 0 && integrity > max) {
      inc = Math.min(inc, integrity - max) ;
    }
    adjustRepair(inc) ;
    if (inc > Rand.num() * maxIntegrity()) burning = false ;
  }
  
  
  public void takeDamage(float damage) {
    if (damage < 0) I.complain("NEGATIVE DAMAGE!") ;
    adjustRepair(0 - damage) ;
    if (damage > Rand.num() * maxIntegrity()) burning = true ;
  }
  
  
  protected void adjustRepair(float inc) {
    final int max = maxIntegrity() ;
    integrity += inc ;
    if (integrity < 0) {
      state = STATE_RAZED ;
      venue.setAsDestroyed() ;
      integrity = 0 ;
    }
    else if (integrity >= max) {
      if (state == STATE_INSTALL) venue.onCompletion() ;
      if (state != STATE_SALVAGE) state = STATE_INTACT ;
      integrity = max ;
    }
    checkMaintenance() ;
  }
  
  
  public boolean needsRepair() {
    return needsSalvage() || integrity < maxIntegrity() ;
  }
  
  
  public boolean needsSalvage() {
    return state == STATE_SALVAGE || integrity > maxIntegrity() ;
  }
  
  
  public boolean needsUpgrade() {
    return nextUpgradeIndex() != -1 ;
  }
  
  
  protected void checkMaintenance() {
    final boolean needs = needsRepair() || needsUpgrade() ;
    if (BaseUI.isPicked(venue)) I.say(venue+" needs maintenance? "+needs) ;
    final World world = venue.world() ;
    world.presences.togglePresence(
      venue, world.tileAt(venue), needs, "damaged"
    ) ;
  }
  
  
  protected int upgradeHP() {
    if (upgrades == null) return 0 ;
    int numUsed = 0 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] != null && upgradeStates[i] != STATE_INSTALL) numUsed++ ;
    }
    if (numUsed == 0) return 0 ;
    return (int) (baseIntegrity * UPGRADE_HP_BONUSES[numUsed - 1]) ;
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
  
  
  public void advanceUpgrade(float progress) {
    if (upgradeIndex == -1) upgradeIndex = nextUpgradeIndex() ;
    if (upgradeIndex == -1) return ;// I.complain("NO UPGRADES REMAINING.") ;
    upgradeProgress += progress ;
    ///I.say("Upgrade progress is: "+upgradeProgress) ;
    //
    //  You may also want to deduct any credits or materials associated with
    //  construction.
    if (upgradeProgress >= 1) {
      final float condition = integrity * 1f / maxIntegrity() ;
      final int US = upgradeStates[upgradeIndex] ;
      if (US == STATE_SALVAGE) deleteUpgrade(upgradeIndex) ;
      else upgradeStates[upgradeIndex] = STATE_INTACT ;
      upgradeProgress = 0 ;
      upgradeIndex = -1 ;
      integrity = maxIntegrity() * condition ;
    }
  }
  
  
  public void beginUpgrade(Upgrade upgrade) {
    int atIndex = -1 ;
    for (int i = 0 ; i < upgrades.length ; i++) {
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
    if (upgradeIndex == atIndex) upgradeProgress = 0 ;
    checkMaintenance() ;
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
  
  
  public int numLevels(Upgrade type) {
    if (upgrades == null) return 0 ;
    int num = 0 ;
    for (Upgrade u : upgrades) if (u == type) num++ ;
    return num ;
  }
  
  
  public float upgradeProgress() {
    return upgradeProgress ;
  }
  
  
  
  /**  Regular updates-
    */
  protected void updateStructure(int numUpdates) {
    if (burning) {
      takeDamage(Rand.num() * 2) ;
      final float damage = maxIntegrity() - integrity ;
      if (armouring > Rand.num() * damage) burning = false ;
    }
    if (numUpdates % 10 == 0) {
      final float wear = baseIntegrity / World.DEFAULT_DAY_LENGTH ;
      if (2 > Rand.num() * armouring) takeDamage(wear * Rand.num()) ;
    }
  }
  
  
  
  /**  Rendering and interface-
    */
  final static String UPGRADE_ERRORS[] = {
    "This facility lacks a prerequisite upgrade",
    "There are no remaining upgrade slots.",
    "No more than 3 upgrades of a single type."
  } ;
  
  protected Batch <String> descUpgrades() {
    final Batch <String> desc = new Batch <String> () ;
    if (upgrades == null) return desc ;
    for (int i = 0 ; i < upgrades.length ; i++) {
      if (upgrades[i] == null) break ;
      //if (upgradeStates[i] != STATE_INTACT) working = true ;
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


//
//  I'm still not entirely clear on how the whole HP bonus thing is supposed to
//  work out.

















