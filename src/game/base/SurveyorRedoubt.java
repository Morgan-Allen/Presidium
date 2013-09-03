

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.Lair;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class SurveyorRedoubt extends Venue implements BuildConstants {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    SurveyorRedoubt.class, "media/Buildings/ecologist/surveyor.png", 4, 1
  ) ;
  
  
  public SurveyorRedoubt(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 4, 150,
      VenueStructure.SMALL_MAX_UPGRADES, false
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public SurveyorRedoubt(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    //
    //  Return a hunting expedition.   And... just explore the place.  You'll
    //  want to make this a bit more nuanced later.
    final Choice choice = new Choice(actor) ;
    final Actor p = Hunting.nextPreyFor(actor, World.DEFAULT_SECTOR_SIZE * 2) ;
    if (p != null) {
      final Hunting h = new Hunting(actor, p, Hunting.TYPE_HARVEST) ;
      h.priorityMod = Plan.CASUAL ;
      choice.add(h) ;
    }
    final Tile t = Exploring.getUnexplored(actor.base().intelMap, actor) ;
    if (t != null) {
      final Exploring e = new Exploring(actor, actor.base(), t) ;
      e.priorityMod = Plan.CASUAL ;
      choice.add(e) ;
    }
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.SURVEYOR } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int nO = super.numOpenings(v) ;
    if (v == Vocation.SURVEYOR) return nO + 2 ;
    return 0 ;
  }
  
  
  protected Service[] services() {
    return new Service[] { WATER, PROTEIN, SPICE } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "The Surveyor's Redoubt" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/redoubt_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Surveyors are responsible for exploring the hinterland of your "+
      "settlement, scouting for danger and regulating animal populations." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}








