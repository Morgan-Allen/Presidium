/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;



public class CultureVats extends Venue implements BuildConstants {

  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    CultureVats.class, "media/Buildings/physician/culture_vats.png", 3, 3
  ) ;
  
  
  public CultureVats(Base base) {
    super(3, 3, ENTRANCE_NORTH, base) ;
    structure.setupStats(
      400, 3, 450,
      VenueStructure.NORMAL_MAX_UPGRADES, false
    ) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public CultureVats(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Implementation of employee behaviour-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    stocks.translateDemands(NIL_TO_SOMA) ;
    stocks.translateDemands(NIL_TO_STARCHES) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    
    //final Delivery d = stocks.nextDelivery(actor, services()) ;
    //if (d != null) return d ;
    
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) return o ;
    
    for (Conversion c : new Conversion[] {
      NIL_TO_STARCHES,
      NIL_TO_SOMA
    }) {
      final Manufacture m = stocks.nextManufacture(actor, c) ;
      if (m != null) return m ;
    }
    
    return null ;
  }
  
  
  protected Service[] services() {
    return new Service[] { STARCHES, PROTEIN, SOMA, MEDICINE } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.VAT_BREEDER } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.VAT_BREEDER) return nO + 2 ;
    return 0 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/culture_vats_button.gif") ;
  }

  public String fullName() {
    return "The Culture Vats" ;
  }
  
  public String helpInfo() {
    return
      "The Culture Vats manufacture soma, medicines, tissue cultures and "+
      "basic foodstuffs." ;
  }
  
  public String buildCategory() {
    return InstallTab.TYPE_PHYSICIAN ;
  }
}





