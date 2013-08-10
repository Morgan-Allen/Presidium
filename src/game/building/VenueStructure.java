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
  
  final static int MAX_NUM_UPGRADES = 6 ;
  final static float UPGRADE_HP_BONUSES[] = {
    //0.15f, 0.1f, 0.1f, 0.5f, 0.5f, 0.5f
    0.15f, 0.25f, 0.35f, 0.4f, 0.45f, 0.5f
  } ;
  
  
  final Venue venue ;
  private int state = STATE_INSTALL ;
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private float integrity = baseIntegrity ;
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
  
  
  public void setupStats(int baseIntegrity) {
    this.integrity = this.baseIntegrity = baseIntegrity ;
  }
  
  
  //
  //  TODO:  You also have to make way for upgrades.  And the Building plan
  //  needs to perform salvage as well as construction.
  /**  Queries and modifications-
    */
  public void repairBy(float repairs) {
    if (repairs < 0) I.complain("NEGATIVE REPAIR!") ;
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
    return baseIntegrity ;  //ADD UPGRADE BONUSES
  }
  
  
  public int integrity() {
    return (int) integrity ;
  }
  
  
  public int correctIntegrity() {
    if (state == STATE_SALVAGE) return -1 ;
    else return maxIntegrity() ;
  }
  
  
  protected boolean complete() {
    return (state != STATE_INSTALL) && (state != STATE_SALVAGE) ;
  }
  
  
  protected boolean allowsUse() {
    return complete() && repairLevel() > 0.5f ;
  }
  
  
  protected int state() {
    return state ;
  }
  
  
  protected void updateStructure(int numUpdates) {
  }
  
  
  /*
  public void beginUpgrade(String name) {
    
  }
  
  
  public void removeUpgrade(String name) {
    
  }
  //*/
  
  //  ...You should also include a method for reducing integrity, so that
  //  structural materials can be salvaged too.
}











