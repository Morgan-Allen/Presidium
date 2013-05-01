


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class ExcavationShaft extends Venue implements VenueConstants {
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String
    IMG_DIR = "media/Buildings/artificer aura/" ;
  final static ImageModel
    MODEL = ImageModel.asIsometricModel(
      ExcavationShaft.class, IMG_DIR+"excavation_shaft.gif", 4, 2
    ) ;
  
  
  //Batch <Tile> excavated = new Batch <Tile> () ;
  List <Fixture> shaftsDug ;
  boolean seekCarbons, seekMetals, seekIsotopes ;
  
  
  
  public ExcavationShaft(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base) ;
    attachSprite(MODEL.makeSprite()) ;
  }


  public ExcavationShaft(Session s) throws Exception {
    super(s) ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Economic functions-
    */
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.EXCAVATOR } ;
  }
  
  
  protected Item.Type[] itemsMade() {
    return new Item.Type[] { CARBONS, METALS, ISOTOPES } ;
  }
  
  //
  //  See if there is space for a new shaft to be sunk.  If so, go out there
  //  and sink it.  Plant the shaft surface above.  Mine all the adjoining areas
  //  and take the result to the smelters on the surface.
  //  ...Favour areas that have the highest concentration of desired minerals-
  //  carbons, metals, or isotopes.  So, you're using a spread.
  
  //  Okay.  That seems straightforward enough.
  public Behaviour nextStepFor(Actor actor) {
    return null ;
  }
  
  
  void placeNextShaft() {
    
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Shaft" ;
  }


  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/excavation_button.gif") ;
  }


  public String helpInfo() {
    return
      "Excavation Shafts permit extraction of useful mineral wealth from "+
      "the terrain surrounding your settlement." ;
  }
}





//
//  TODO:  You want to gradually expand the area of excavated tiles around
//  this site, with some kind of surface manifestation thereof.
//  Exacavated tiles produce minerals continuously, but their yield
//  declines over time (1/(n + 1)), where n is the number of days(?) since
//  discovery.
//  TODO:  I also want actors to be seen *moving* between these sites.
//  Okay.  Smelters/drills (3 types.)  Main shafts.  Claimed territory.
//  TODO:  This should bear some relation to underground mineral deposits.


