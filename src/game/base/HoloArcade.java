/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Integrate with the performance and recreation behaviours!


public class HoloArcade extends Venue implements Economy {
  
  

  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    HoloArcade.class, "media/Buildings/aesthete/arcade_redux_preview.png", 4, 2
  ) ;
  
  
  
  public HoloArcade(Base base) {
    super(4, 2, Venue.ENTRANCE_SOUTH, base) ;
    structure.setupStats(
      300, 4, 400,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public HoloArcade(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    HoloArcade.class, "arcade_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    MEDIA_EXHIBIT = new Upgrade(
      "Media Exhibit",
      "Provides space and time for pre-recorded art installations to be "+
      "placed on prominent display.",
      250, GRAPHIC_DESIGN, 1, null, ALL_UPGRADES
    ),
    LARP_ARENA = new Upgrade(
      "LARP Arena",
      "Allows historical, dramatic, or purely imaginative scenarios to be "+
      "collaboratively enacted, simulated or improvised.",
      300, MASQUERADE, 1, MEDIA_EXHIBIT, ALL_UPGRADES
    ),
    MEDITATION_SUITE = new Upgrade(
      "Meditation Suite",
      "Trains visitors to clarify and focus their thoughts and feelings.",
      150, INTELLECT, 1, null, ALL_UPGRADES
    ),
    VIRTUAL_SPORTS = new Upgrade(
      "Virtual Sports",
      "Helps visitors to maintain and improve their physical fitness.",
      200, BRAWN, 1, null, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  TODO:  Effectiveness needs to vary based on availability of circuitry
    //  and datalinks...
    //stocks.incDemand(CIRCUITRY, 2, VenueStocks.TIER_CONSUMER, 1) ;
    stocks.incDemand(DATALINKS, 2, VenueStocks.TIER_CONSUMER, 1) ;
  }
  
  
  
  public Background[] careers() {
    return null ;
  }
  
  
  public Service[] services() {
    return new Service[] { SERVICE_PERFORM } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected Service[] goodsToShow() {
    return new Service[] { DATALINKS } ;
  }
  
  
  public String fullName() {
    return "Holo-Arcade" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/arcade_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Holo-Arcade affords musicians, artists, performers and the lay "+
      "public a chance to congregate and either exhibit or appreciate their "+
      "work." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
}




/*
HYPNOGOGIC_TRAINING = new Upgrade(
  "Hypnogogic Training",
  "Weaves subliminal suggestions into the display matrix to induce "+
  "desired mental states.  May trigger addictive response.",
  400, null, 1, null, ALL_UPGRADES
),
ESP_INDUCTION = new Upgrade(
  "ESP Induction",
  "Helps triggers emergence of latent psyonic abilities within "+
  "susceptible subjects.",
  350, null, 1, HYPNOGOGIC_TRAINING, ALL_UPGRADES
) ;
//*/
/*
final static int
  HYPNO_NONE      =  0,
  HYPNO_LEARNING  =  1,
  HYPNO_PLEASURE  =  2,
  HYPNO_OBEDIENCE =  3,
  HYPNO_ESP       =  4,
  HYPNO_TYPES[]   = { 0, 1, 2, 3, 4 } ;

//
//  TODO:  Cut these out or simplify.  (Save for detention bloc?)
final static String HYPNO_NAMES[] = {
  "None",
  "Accelerated Learning",
  "Pleasure Centre Stimulus",
  "Obedience Conditioning",
  "ESP Trigger Induction"
} ;
private int hypnoType = HYPNO_NONE ;
//*/


/*
public void writeInformation(Description d, int categoryID, HUD UI) {
  super.writeInformation(d, categoryID, UI) ;
  if (categoryID != 3 || ! structure.intact()) return ;
  
  final boolean hasESP = structure.upgradeLevel(ESP_INDUCTION) > 0 ;
  
  if (structure.upgradeLevel(HYPNOGOGIC_TRAINING) > 0) {
    d.append("\n\nHypnogogic Training:") ;
    for (final int type : HYPNO_TYPES) {
      if (type == HYPNO_ESP && ! hasESP) continue ;
      d.append("\n  ") ;
      d.append(new Description.Link(HYPNO_NAMES[type]) {
        public void whenClicked() { hypnoType = type ; }
      }, type == hypnoType ? Colour.YELLOW : Colour.BLUE) ;
    }
  }
}
//*/