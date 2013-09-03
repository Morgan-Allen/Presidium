/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.InstallTab;
import src.user.Composite;



public class Bastion extends Venue implements BuildConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Bastion.class, "media/Buildings/military/bastion.png", 7, 5
  ) ;
  
  
  public Bastion(Base base) {
    super(7, 5, ENTRANCE_EAST, base) ;
    structure.setupStats(
      650, 15, 1000,
      VenueStructure.BIG_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Bastion(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  public Behaviour jobFor(Actor actor) {
    return new Patrolling(actor, this, this.radius()) ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] {} ;
  }
  
  
  protected Service[] services() {
    return new Service[] { SERVICE_ADMIN } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/bastion_button.gif") ;
  }
  
  
  public String fullName() { return "The Bastion" ; }
  
  
  public String helpInfo() {
    return
      "The Bastion is your seat of command for the settlement as a "+
      "whole, and houses your family, advisors and bodyguards." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
}



