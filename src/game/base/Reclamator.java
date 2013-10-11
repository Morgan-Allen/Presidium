/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet;
//import src.game.planet.Planet ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Reclamator extends Venue implements EconomyConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    Reclamator.class, "media/Buildings/aesthete/PLAZA.png", 4, 2
  ) ;
  
  
  public Reclamator(Base belongs) {
    super(4, 2, ENTRANCE_EAST, belongs) ;
    structure.setupStats(75, 1, 500, 0, Structure.TYPE_FIXTURE) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Reclamator(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  protected Background[] careers() { return null ; }
  
  
  public Service[] services() {
    return new Service[] { WATER, LIFE_SUPPORT } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.incDemand(POWER, 10, VenueStocks.TIER_CONSUMER, 1) ;
    stocks.bumpItem(POWER, -1, 10) ;
    float power = Visit.clamp(1 - stocks.shortagePenalty(POWER), 0.1f, 1) ;
    stocks.bumpItem(WATER, 2 * power, 20) ;
    stocks.bumpItem(LIFE_SUPPORT, 5 * power, 100) ;
    world.ecology().impingeSqualor(-10 * power, this, true) ;
  }
  
  

  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Reclamator" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/PLAZA_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Reclamator provides an abundance of water and life support to your "+
      "settlement as well as a social focal point, but requires power and "+
      "frequent maintenance." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}


