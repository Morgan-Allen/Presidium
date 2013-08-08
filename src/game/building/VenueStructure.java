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
  final static int DEFAULT_INTEGRITY = 100 ;
  
  final static int MAX_NUM_UPGRADES = 6 ;
  final static float UPGRADE_HP_BONUSES[] = {
    //0.15f, 0.1f, 0.1f, 0.5f, 0.5f, 0.5f
    0.15f, 0.25f, 0.35f, 0.4f, 0.45f, 0.5f
  } ;
  
  
  final Venue venue ;
  private int baseIntegrity = DEFAULT_INTEGRITY ;
  private float integrity = baseIntegrity ;
  //  float armour, shields ;
  //  Item materials[] ;
  //  List <Upgrade> upgrades ;
  
  
  
  VenueStructure(Venue venue) {
    super(venue) ;
    this.venue = venue ;
  }
  
  
  public void setupStats(int base) {
    integrity = baseIntegrity = base ;
  }
  
  
  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    baseIntegrity = s.loadInt() ;
    integrity = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(baseIntegrity) ;
    s.saveFloat(integrity) ;
  }
  
  
  
  //  TODO:  You also have to make way for upgrades.
  /**  Queries and modifications-
    */
  public void repairBy(float repairs) {
    if (repairs < 0) I.complain("NEGATIVE REPAIR!") ;
    final int max = maxIntegrity() ;
    integrity += repairs ;
    if (integrity >= max) { integrity = max ; toggleForRepairs(false) ; }
  }
  
  
  public void takeDamage(float damage) {
    if (damage < 0) I.complain("NEGATIVE DAMAGE!") ;
    final int max = maxIntegrity() ;
    if (integrity == max && damage > 0) toggleForRepairs(true) ;
    integrity -= damage ;
    if (integrity <= 0) integrity = 0 ;
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
    return baseIntegrity ;
  }
  
  
  protected void updateStructure(int numUpdates) {
  }
  
  
  /*
  public void beginUpgrade(String name) {
    
  }
  
  
  public void removeUpgrade(String name) {
    
  }
  //*/
}
















