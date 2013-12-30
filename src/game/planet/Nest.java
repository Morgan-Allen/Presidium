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

public class Nest extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    BROWSER_SEPARATION = World.SECTOR_SIZE,
    PREDATOR_SEPARATION = BROWSER_SEPARATION * 2,
    BROWSER_RATIO   = 12,
    PREDATOR_RATIO  = 8 ,
    MAX_CROWDING    = 10,
    BROWSING_SAMPLE = 8 ,
    NEW_SITE_SAMPLE = 5 ,
    DEFAULT_BREED_INTERVAL = World.STANDARD_DAY_LENGTH ;
  
  private static boolean verbose = false ;
  
  
  final Species species ;
  private float idealPopEstimate = -1 ;
  
  
  public Nest(
    int size, int high, int entranceFace,
    Species species, Model lairModel
  ) {
    super(size, high, entranceFace, null) ;
    this.species = species ;
    attachSprite(lairModel.makeSprite()) ;
  }
  
  
  public Nest(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    idealPopEstimate = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(idealPopEstimate) ;
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  public int owningType() { return Element.ELEMENT_OWNS ; }
  
  
  public boolean allowsEntry(Mobile m) {
    return (m instanceof Actor) && ((Actor) m).species() == species ;
  }
  
  
  
  /**  Methods for determining crowding and site placement-
    */
  private static boolean occupied(Nest n) {
    return n.personnel.residents().size() > 0 ;
  }
  
  
  private static int idealBrowserPop(
    Target lair, Species species, World world
  ) {
    //
    //  Firstly, ensure there is no other lair within lair placement range.
    final int range = BROWSER_SEPARATION ;
    int numLairsNear = 0, alikeLairs = 0 ;
    for (Object o : world.presences.matchesNear(Nest.class, lair, range * 2)) {
      final Nest l = (Nest) o ;
      if (l == lair || ! occupied(l)) continue ;
      numLairsNear++ ;
      if (l.species == species) {
        if (Spacing.distance(l, lair) < range) return -1 ;
        alikeLairs++ ;
      }
      else if (Spacing.distance(l, lair) < range / 2) return -1 ;
    }
    //
    //  Secondly, sample the fertility & biomass of nearby terrain (within half
    //  of lair placement range.)  (We average with presumed default growth
    //  levels for local flora.)
    float avg = 0 ;
    final Tile o = world.tileAt(lair) ;
    int numSamples = BROWSING_SAMPLE ; while (numSamples-- > 0) {
      final Tile t = world.tileAt(
        o.x + Rand.range(-range, range),
        o.y + Rand.range(-range, range)
      ) ;
      if (t == null) continue ;
      final Habitat h = t.habitat() ;
      if (h.isOcean || h.isWaste) continue ;
      else {
        final float moisture = Visit.clamp((h.moisture - 2) / 10f, 0, 1) ;
        avg += moisture / 4f ;
        if (t.owner() instanceof Flora) {
          final Flora f = (Flora) t.owner() ;
          avg += (f.growStage() + 0.5f) * moisture / 1.5f ;
        }
      }
    }
    avg /= BROWSING_SAMPLE ;
    if (verbose) I.say("\nFinding ideal browser population at "+lair) ;
    if (verbose) I.add("  Average fertility at "+lair+" is "+avg) ;
    avg *= range * range ;
    if (verbose) I.add(", total: "+avg+"\n") ;
    //
    //  Then return the correct number of inhabitants for the location-
    float adultMass = species.baseBulk * species.baseSpeed ;
    float rarity = 1f - (alikeLairs / (numLairsNear + 1)) ;
    float idealPop = (avg * rarity) / (adultMass * BROWSER_RATIO) ;
    if (verbose) I.say("  Ideal population is: "+idealPop) ;
    return (int) (idealPop + 0.5f) ;
  }
  
  
  private static float idealPredatorPop(
    Target lair, Species species, World world
  ) {
    //
    //  Sample the lairs within hunting range, ensure there are no other
    //  predator lairs in that area, and tally the total of available prey.
    final int range = PREDATOR_SEPARATION ;
    int numLairsNear = 0, alikeLairs = 0 ;
    float totalPreyMass = 0 ;
    //
    //  TODO:  Since separation requirements ought, ideally, to be symmetric,
    //  consider putting them in a single reference function?
    for (Object o : world.presences.matchesNear(Nest.class, lair, range)) {
      final Nest l = (Nest) o ;
      if (l == lair || ! occupied(l)) continue ;
      if (l.species.type == Species.Type.PREDATOR) {
        if (Spacing.distance(lair, l) < range) return -1 ;
      }
      else for (Actor a : l.personnel.residents()) {
        totalPreyMass += l.species.baseBulk * l.species.baseSpeed * 10 / 2f ;
      }
      numLairsNear++ ;
      if (l.species == species) alikeLairs++ ;
    }
    
    //
    //  TODO:  TO ALLOW PROPERLY FOR RARITY EFFECTS, YOU'LL HAVE TO SAMPLE OUT
    //  TO TWICE NORMAL HUNTING RANGE!
    
    if (verbose) I.say("Total prey biomass: "+totalPreyMass) ;
    final float rarity = 1f - (alikeLairs / (numLairsNear + 1)) ;
    float adultMass = species.baseBulk * species.baseSpeed ;
    if (verbose) I.say("Adult biomass: "+adultMass) ;
    float idealPop = (totalPreyMass * rarity) / (adultMass * PREDATOR_RATIO) ;
    if (verbose) I.say("Ideal population: "+idealPop) ;
    return (int) (idealPop - 0.5f) ;
  }
  
  
  public static float idealNestPop(
    Species species, Target site, World world, boolean cached
  ) {
    if (cached && site instanceof Nest) {
      final float estimate = ((Nest) site).idealPopEstimate ;
      if (estimate != -1) return estimate ;
    }
    float idealPop = 0 ; for (int n = NEW_SITE_SAMPLE ; n-- > 0 ;) {
      if (species.predator()) {
        idealPop += idealPredatorPop(site, species, world) ;
      }
      else {
        idealPop += idealBrowserPop(site, species, world) ;
      }
    }
    return idealPop / NEW_SITE_SAMPLE ;
  }
  
  
  public static float crowdingFor(Actor actor) {
    return crowdingFor(actor.mind.home(), actor.species(), actor.world()) ;
  }
  
  
  public static float crowdingFor(Boardable home, Object species, World world) {
    if (! (home instanceof Venue)) return MAX_CROWDING ;
    if (! (species instanceof Species)) return MAX_CROWDING ;
    final Venue venue = (Venue) home ;
    float actualPop = 0 ; for (Actor a : venue.personnel.residents()) {
      if (a.health.alive()) actualPop++ ;
    }
    final float idealPop = idealNestPop(
      (Species) species, venue, world, true
    ) ;
    if (idealPop <= 0) return MAX_CROWDING ;
    if (verbose && I.talkAbout == venue) {
      I.say("Actual/ideal population: "+actualPop+"/"+idealPop) ;
    }
    return Visit.clamp(actualPop / (1 + idealPop), 0, MAX_CROWDING) ;
  }
  
  
  public static int forageRange(Species s) {
    return (s.type == Species.Type.PREDATOR) ?
      PREDATOR_SEPARATION :
      BROWSER_SEPARATION  ;
  }
  
  
  protected static Nest findNestFor(Fauna fauna) {
    //
    //  If you're homeless, or if home is overcrowded, consider moving into a
    //  vacant lair, or building a new one.
    final World world = fauna.world() ;
    final float range = forageRange(fauna.species) ;
    if (crowdingFor(fauna) > 0.5f) {
      Nest bestNear = null ;
      float bestRating = 0 ;
      for (Object o : world.presences.sampleFromKey(
        fauna, world, 5, null, Nest.class
      )) {
        final Nest l = (Nest) o ;
        if (l.species != fauna.species) continue ;
        final float dist = Spacing.distance(l, fauna) ;
        if (dist > range) continue ;
        final float crowding = crowdingFor(l, l.species, world) ;
        if (crowding > 0.5f) continue ;
        float rating = (1 - crowding) * range / (range + dist) ;
        rating *= Rand.avgNums(2) * 4 ;
        if (rating > bestRating) { bestRating = rating ; bestNear = l ; }
      }
      if (bestNear != null) return bestNear ;
    }
    //
    //  If no existing lair is suitable, try establishing a new one-
    Tile toTry = Spacing.pickRandomTile(fauna, range * 2, world) ;
    toTry = Spacing.nearestOpenTile(toTry, fauna) ;
    if (toTry == null) return null ;
    return siteNewLair(fauna.species, toTry, world) ;
  }
  
  
  protected static Nest siteNewLair(
    Species species, final Target client, final World world
  ) {
    final float range = forageRange(species) ;
    final Nest newLair = species.createNest() ;
    final TileSpread spread = new TileSpread(world.tileAt(client)) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(client, t) < range ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        newLair.setPosition(t.x, t.y, world) ;
        return newLair.canPlace() ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) {
      final float idealPop = idealNestPop(species, newLair, world, false) ;
      if (idealPop <= 0) return null ;
      return newLair ;
    }
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (numUpdates % 10 != 0) return ;
    
    final float idealPop = idealNestPop(species, this, world, false) ;
    final float inc = 10f / World.STANDARD_DAY_LENGTH ;
    if (idealPopEstimate == -1) idealPopEstimate = idealPop ;
    else {
      idealPopEstimate *= 1 - inc ;
      idealPopEstimate += idealPop * inc ;
    }
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
    
    int idealPop = 1 + (int) idealNestPop(species, this, world, true) ;
    int actualPop = personnel.residents().size() ;
    d.append("\n  Population: "+actualPop+"/"+idealPop) ;
    
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

