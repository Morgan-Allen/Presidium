/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.planet.Planet;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.user.Description;
import src.util.* ;



/**  This is a special brand of 'venue' specifically intended to allow ease of
  *  interface with vehicles.  It occupies a single tile, just outside the
  *  vehicle door.
  */
public class DropZone extends Venue implements VenueConstants {
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  Vehicle landing ;
  
  
  public static DropZone findLandingSite(Vehicle landing, final Base base) {
    //
    //  Firstly, determine a reasonable starting position-
    final World world = base.world ;
    final Tile midTile = world.tileAt(world.size / 2, world.size / 2) ;
    final Target nearest = base.randomServiceNear(base, midTile, -1) ;
    if (nearest == null) return null ;
    final Tile init = Spacing.nearestOpenTile(world.tileAt(nearest), midTile) ;
    if (init == null) return null ;
    return findLandingSite(landing, init, base) ;
  }
  
  
  public static DropZone findLandingSite(
    Vehicle landing, final Tile init, final Base base
  ) {
    final DropZone zone = new DropZone(landing, base) ;
    //
    //  Then, spread out to try and find a decent landing site-
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > Planet.SECTOR_SIZE) return false ;
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        zone.setPosition(t.x, t.y, base.world) ;
        return zone.canPlace() ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) return zone ;
    return null ;
  }
  
  
  protected DropZone(Vehicle landing, Base base) {
    super(sizeFor(landing), 0, Venue.ENTRANCE_SOUTH, base) ;
    this.landing = landing ;
  }
  
  
  private static int sizeFor(Vehicle landing) {
    return (int) Math.ceil(landing.radius() * 2) ;
  }
  
  
  public DropZone(Session s) throws Exception {
    super(s) ;
    landing = (Vehicle) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(landing) ;
  }
  
  
  
  /**  Satisfying supply and demand-
    */
  protected Vocation[] careers() {
    return new Vocation[0] ;
  }
  
  protected Item.Type[] goods() {
    return ALL_ITEM_TYPES ;
  }
  
  public boolean usesRoads() {
    return false ;
  }
  
  public Behaviour jobFor(Citizen actor) {
    return orders.nextDelivery(actor, goods()) ;
  }
  
  
  
  /**  Entry and exit methods, that similarly need to be overwritten.
    */
  public int owningType() {
    return Element.VENUE_OWNS ;
  }
  
  
  public int pathType() {
    ///I.say("Path type for drop zone blocked? "+landing.landed()) ;
    return landing.landed() ? Tile.PATH_BLOCKS : Tile.PATH_CLEAR ;
  }
  
  
  public Boardable[] canBoard(Boardable[] batch) {
    if (batch == null) batch = new Boardable[2] ;
    super.canBoard(batch) ;
    if (landing.landed()) batch[1] = landing ;
    //I.say("DropZone can board: "+batch[0]+" and "+batch[1]) ;
    return batch ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Drop Zone for "+landing.fullName() ;
  }

  public Texture portrait() {
    return landing.portrait() ;
  }
  
  public String helpInfo() {
    return landing.helpInfo() ;
  }
  
  public void writeInformation(Description d, int categoryID) {
    landing.writeInformation(d, categoryID) ;
  }
}








