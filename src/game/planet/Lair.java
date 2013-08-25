/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;




//
//  TODO:  You need to be able to set default armour, HP, organic values etc.
//  for lairs of a given type, ideally from the Species class.

public class Lair extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    GROUND_SAMPLE_RANGE = 16,
    PEER_SAMPLE_RANGE   = 16,
    SAMPLE_AREA         = 16 * 16 * 4,
    BROWSER_DENSITY     = 12,
    PREDATOR_RATIO      = 12,
    LAIR_POPULATION     = 4 ;
  
  
  final Species species ;
  
  
  public Lair(
    int size, int high, int entranceFace,
    Species species, Model lairModel
  ) {
    super(size, high, entranceFace, null) ;
    this.species = species ;
    attachSprite(lairModel.makeSprite()) ;
  }
  
  
  public Lair(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  protected Vocation[] careers() { return new Vocation[0] ; }
  protected Service[] services() { return new Service[0] ; }
  
  
  protected int peerRange() {
    return (int) (PEER_SAMPLE_RANGE * (
      species.type == Species.Type.PREDATOR ? 1.5f : 0.75f
    )) ;
  }
  
  
  public void setAsEstablished(boolean isDone) {
    super.setAsEstablished(isDone) ;
    if (! isDone) return ;
    final Vec3D p = position(null) ;
    final float r = peerRange(), s = world.size - 1 ;
    //
    //  Populate this venue with suitable specimens-
    for (int n = LAIR_POPULATION ; n-- > 0 ;) {
      //
      //  Find a suitable entry point:
      final float
        x = Visit.clamp(p.x + Rand.range(-r, r), 0, s),
        y = Visit.clamp(p.y + Rand.range(-r, r), 0, s) ;
      Tile enters = world.tileAt(x, y) ;
      enters = Spacing.nearestOpenTile(enters, enters) ;
      //
      //  Fill up the surrounds with specimens.
      final Fauna specimen = species.newSpecimen() ;
      specimen.AI.setHomeVenue(this) ;
      specimen.health.setupHealth(
        Rand.num(),  //current age
        (Rand.num() + 1) / 2,  //overall health
        0.1f  //accident chance
      ) ;
      specimen.enterWorldAt(enters.x, enters.y, world) ;
    }
  }
  
  
  public boolean actionBuildNest(Fauna actor, Lair lair) {
    return true ;
  }
  
  
  
  /**  Determining site suitability-
    */
  //
  //  TODO:  This should probably be evaluated on a sector-by-sector basis, or
  //  at least cached at regular intervals.
  
  protected float rateCrowding(World world) {
    if ((! inWorld()) && (! canPlace())) return -1 ;
    //
    //  Iterate over nearby lairs and estimate total abundance of peers.
    final int WS = world.size, range = peerRange(), LP = LAIR_POPULATION ;
    final Tile point = world.tileAt(this) ;
    final Box2D limit = new Box2D().set(point.x, point.y, 0, 0) ;
    limit.expandBy(range).cropBy(new Box2D().set(0, 0, WS, WS)) ;
    final PresenceMap venueMap = world.presences.mapFor(Venue.class) ;
    final boolean byLairs = ! species.goesHunt() ;
    //
    //  
    boolean nearWrongStructure = false ;
    float numPeers = 0, numPrey = 0, numHunters = 0 ;
    for (Target t : venueMap.visitNear(point, -1, limit)) {
      if (! (t instanceof Lair)) {
        nearWrongStructure = true ;
        continue ;
      }
      final Lair l = (Lair) t ;
      final Species s = l.species ;
      float count = l.crowding() * LP ;
      if (l == this && byLairs) { count -= LP ; if (count < 0) continue ; }
      if (s == species) numPeers += count ;
      if (s.browses()) numPrey += count ;
      if (s.goesHunt()) numHunters += count ;
    }
    if (nearWrongStructure) return -1 ;
    //
    //  
    float rating = 0 ;
    if (species.browses()) {
      rating += rateForBrowser(point, limit, numPeers, numPrey, numHunters) ;
    }
    if (species.goesHunt()) {
      rating += rateForHunters(point, limit, numPeers, numPrey, numHunters) ;
    }
    return rating ;
  }
  
  
  protected float rateForBrowser(
    Tile point, Box2D limit,
    float numPeers, float numPrey, float numHunters
  ) {
    final float fertility = evalFertility(point) ;
    if (fertility == 0) return 0 ;
    final float idealPop = BROWSER_DENSITY * fertility / 1.5f ;
    final float rarity = (((numPrey + 1) / (numPeers + 1)) + 1) / 2f ;
    final float density = numPrey * (SAMPLE_AREA / limit.area()) ;
    return (idealPop * rarity) - density ;
  }
  
  
  protected float rateForHunters(
    Tile point, Box2D limit,
    float numPeers, float numPrey, float numHunters
  ) {
    if (numPrey == 0) return 0 ;
    float idealPop = numPrey / (PREDATOR_RATIO * 1.5f) ;
    float rarity = (((numHunters + 1) / (numPeers + 1)) + 1) / 2f ;
    final float density = numHunters * (SAMPLE_AREA / limit.area()) ;
    return (idealPop * rarity) - density ;
  }

  //
  //  TODO:  This sort of thing might be outsourced to the GrowthMap class, or
  //  something similar.
  protected float evalFertility(Tile point) {
    final Box2D limit = new Box2D().set(point.x, point.y, 0, 0) ;
    limit.expandBy(GROUND_SAMPLE_RANGE) ;
    float sumF = 0, numT = 0, weight ;
    for (Tile t : point.world.tilesIn(limit, true)) {
      weight = 1 - (Spacing.axisDist(t, point) / (GROUND_SAMPLE_RANGE + 1)) ;
      sumF += t.habitat().moisture() * weight ;
      numT += weight ;
    }
    sumF /= numT * 10 ;
    return sumF ;
  }
  
  
  public float crowding() {
    return personnel.residents().size() * 1f / LAIR_POPULATION ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return species.name+" Nest" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }

  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.append("\nCondition: ") ;
    d.append((int) (structure.repairLevel() * 100)+"%") ;
    d.append("\nNesting: ") ;
    if (personnel.residents().size() == 0) d.append("Unoccupied") ;
    else for (Actor actor : personnel.residents()) {
      d.append("\n  ") ;
      d.append(actor) ;
    }
    
    d.append("\n\n") ;
    d.append(species.info) ;
  }
  
  
  public String helpInfo() {
    return null ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}










