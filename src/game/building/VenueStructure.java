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
    DEFAULT_INTEGRITY = 100,
    DEFAULT_ARMOUR    = 10 ;
  final public static int
    STATE_INSTALL = 0,
    STATE_INTACT  = 1,
    STATE_REPAIR  = 2,
    STATE_SALVAGE = 3,
    STATE_RAZED   = 4 ;
  
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
  
  //  These don't actually need to be supplied on an individual basis.  They
  //  belong to the class, rather than to the object.
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private int maxUpgrades = NORMAL_MAX_UPGRADES ;
  private int buildCost, armouring ;

  private int state = STATE_INSTALL ;
  private float integrity = baseIntegrity ;

  
  
  //  int buildCost, armouring, restLevel, moraleLevel, pollutes ;
  //  Item materials[] ;
  //  List <Upgrade> upgrades ;
  
  
  
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
    
    state = s.loadInt() ;
    integrity = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(baseIntegrity) ;
    s.saveInt(maxUpgrades) ;
    s.saveInt(buildCost) ;
    s.saveInt(armouring) ;
    
    s.saveInt(state) ;
    s.saveFloat(integrity) ;
  }
  
  
  public void setupStats(
    int baseIntegrity,
    int armouring,
    int buildCost,
    int maxUpgrades
  ) {
    this.integrity = this.baseIntegrity = baseIntegrity ;
    this.armouring = armouring ;
    this.buildCost = buildCost ;
    this.maxUpgrades = maxUpgrades ;
  }
  
  
  public int armouring() { return armouring ; }
  
  
  
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
      state = STATE_RAZED ;
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
  
  
  public int correctIntegrity() {
    if (state == STATE_SALVAGE) return -1 ;
    //  TODO:  Base this on the number on upgrades NOT due for salvage.
    else return maxIntegrity() ;
  }
  
  protected boolean complete() {
    return (state != STATE_INSTALL) && (state != STATE_SALVAGE) ;
  }
  
  
  public int integrity() {
    return (int) integrity ;
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
  
  
  public boolean destroyed() {
    return state == STATE_RAZED ;
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
  //...It might be best to handle upgrades on an entirely different scale.
}








//
//  ...You should also include a method for reducing integrity, so that
//  structural materials can be salvaged too.

//  Okay.  You need to check to make sure that upgrades cannot be used while
//  construction is underway, or while the venue is badly damaged, in
//  proportion to the health bonus involved.


/*
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
//*/




