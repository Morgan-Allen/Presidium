/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.actors.* ;
import src.game.common.* ;
import src.util.* ;



public class VenueStructure extends Inventory {
  
  
  
  /**  Fields, definitions and save/load methods-
    */
  final static int
    DEFAULT_INTEGRITY = 100 ;
  final public static int
    STATE_INSTALL = 0,
    STATE_INTACT  = 1,
    STATE_REPAIR  = 2,
    STATE_SALVAGE = 3 ;
  
  final static int
    SMALL_MAX_UPGRADES  = 3,
    NORMAL_MAX_UPGRADES = 6,
    BIG_MAX_UPGRADES    = 12 ;
  final static float UPGRADE_HP_BONUSES[] = {
    //0.15f, 0.1f, 0.1f, 0.5f, 0.5f, 0.5f
    0.15f, 0.25f, 0.35f,
    0.4f , 0.45f, 0.5f ,
    0.5f , 0.55f, 0.55f, 0.6f , 0.6f , 0.65f
  } ;
  
  
  final Venue venue ;
  private int state = STATE_INSTALL ;
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private float integrity = baseIntegrity ;

  private int maxUpgrades = NORMAL_MAX_UPGRADES ;
  private Table <Upgrade, Integer> upgrades = new Table <Upgrade, Integer> () ;
  
  //  float armour, shields ;
  //  Item materials[] ;
  //  List <Upgrade> upgrades ;
  
  
  
  VenueStructure(Venue venue) {
    super(venue) ;
    this.venue = venue ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    state = s.loadInt() ;
    baseIntegrity = s.loadInt() ;
    integrity = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(state) ;
    s.saveInt(baseIntegrity) ;
    s.saveFloat(integrity) ;
  }
  
  
  public void setupStats(int baseIntegrity, int maxUpgrades) {
    this.integrity = this.baseIntegrity = baseIntegrity ;
    this.maxUpgrades = maxUpgrades ;
  }
  
  
  //
  //  TODO:  You also have to make way for upgrades.  And the Building plan
  //  needs to perform salvage as well as construction.
  /**  Queries and modifications-
    */
  public void repairBy(float repairs) {
    //  This can also be used to perform salvage.
    
      //  Implement burning, and try to cancel it out?
      //  if (burning && Rand.num() * baseIntegrity < repairs) burning = false ;
    
    //if (repairs < 0) I.complain("NEGATIVE REPAIR!") ;
    setIntegrity(integrity + repairs) ;
  }
  
  
  public void takeDamage(float damage) {
    if (damage < 0) I.complain("NEGATIVE DAMAGE!") ;
    setIntegrity(integrity - damage) ;
  }
  
  
  protected void setIntegrity(float level) {
    integrity = Visit.clamp(level, -1, maxIntegrity()) ;
    if (integrity < 0) {
      toggleForRepairs(false) ;
      //  TODO:  You need to leave some rubble behind!
      venue.setAsDestroyed() ;
    }
    //
    //  Toggle for repairs based on difference between current and correct
    //  integrity!
    else if (integrity < maxIntegrity()) {
      toggleForRepairs(true) ;
      if (state == STATE_INTACT) state = STATE_REPAIR ;
    }
    else {
      toggleForRepairs(false) ;
      if (state == STATE_INSTALL) venue.onCompletion() ;
      state = STATE_INTACT ;
    }
  }
  
  
  public void setState(int newState, float condition) {
    final int oldState = newState ;
    this.state = newState ;
    if (condition >= 0) setIntegrity(maxIntegrity() * condition) ;
    if (oldState == STATE_INTACT && newState == STATE_SALVAGE) {
      toggleForRepairs(true) ;
      venue.onDecommission() ;
    }
  }
  
  
  //
  //  TODO:  You need to toggle the venue as needing repairs whenever correct
  //  integrity does not match maximum integrity.
  private void toggleForRepairs(boolean needs) {
    final World world = venue.world() ;
    world.presences.togglePresence(
      venue, world.tileAt(venue), needs, "damaged"
    ) ;
  }
  
  
  public float repairLevel() {
    return integrity * 1f / maxIntegrity() ;
  }
  
  
  public int maxIntegrity() {
    final float upBonus = 1 ;// + UPGRADE_HP_BONUSES[upgrades.size() - 1] ;
    return (int) (baseIntegrity * upBonus) ;
  }
  
  
  public int integrity() {
    return (int) integrity ;
  }
  
  
  public int correctIntegrity() {
    if (state == STATE_SALVAGE) return -1 ;
    //  TODO:  Base this on the number on upgrades NOT due for salvage.
    else return maxIntegrity() ;
  }
  
  
  protected boolean complete() {
    return (state != STATE_INSTALL) && (state != STATE_SALVAGE) ;
  }
  
  
  protected boolean allowsUse() {
    return complete() && repairLevel() > 0.5f ;
  }
  
  
  protected boolean needsRepair() {
    return correctIntegrity() != integrity ;
  }
  
  
  protected int state() {
    return state ;
  }
  
  
  
  /**  Regular updates-
    */
  protected void updateStructure(int numUpdates) {
    //  if (burning) takeDamage(Rand.num() * 5) ;
    //  TODO:  Structures can also suffer breakdowns due to simple wear and
    //  tear.  (Ancient or organic structures are immune to this, and the
    //  latter can actively regenerate damage.)
  }
  
  
  
  
  /**  Handling upgrades-
    */
  //
  //  ...You should also include a method for reducing integrity, so that
  //  structural materials can be salvaged too.
  
  //  Okay.  You need to check to make sure that upgrades cannot be used while
  //  construction is underway, or while the venue is badly damaged, in
  //  proportion to the health bonus involved.
  
  public boolean canUse(Upgrade upgrade) {
    Integer useState = upgrades.get(upgrade) ;
    if (useState == null || useState != STATE_INTACT) return false ;
    return true ;
  }
  
  
  public boolean allows(Upgrade upgrade) {
    if (upgrades.size() >= maxUpgrades) return false ;
    if (upgrade.required == null) return true ;
    if (! upgrades.keySet().contains(upgrade.required)) return false ;
    return true ;
  }
  
  
  public void beginUpgrade(Upgrade begun) {
    upgrades.put(begun, STATE_INSTALL) ;
  }
  
  
  public void removeUpgrade(Upgrade removed) {
    ///upgrades.remove(removed) ;  // (Take off a chunk of health?)
  }
}













