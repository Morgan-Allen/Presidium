


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Archives extends Venue implements BuildConstants {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Archives.class, "media/Buildings/physician/archives.png", 4, 3
  ) ;
  

  public Archives(Base base) {
    super(4, 3, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      250, 4, 500,
      VenueStructure.BIG_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Archives(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  //
  //  TODO:  You might not want to implement these as upgrades?  They shouldn't
  //  really be contributing toward hit-points, for example.  Might be better
  //  to model them as imported items, or some kind of custom object?
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Archives.class, "archives_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  static {
    for (Skill skill : COGNITIVE_SKILLS) {
      if (skill.parent != INTELLECT) continue ;
      final Upgrade dataLink = new Upgrade(
        skill.name+" datalinks", "Allows research in "+skill.name,
        250, skill, 5, null, ALL_UPGRADES
      ) ;
    }
  }
  
  
  
  public Behaviour jobFor(Actor actor) {
    //
    //  Assist folks inside with their researches, or manufacture Inscriptions.
    
    return null ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.ARCHIVIST } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.ARCHIVIST) {
      return nO + 1 + (int) (structure.numUpgrades() / 3) ;
    }
    return 0 ;
  }
  
  
  protected Service[] services() {
    return new Service[] { INSCRIPTION } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Archives" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/archives_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Archives facilitate research and administration within your "+
      "settlement, and permit citizens a chance at self-education." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}







