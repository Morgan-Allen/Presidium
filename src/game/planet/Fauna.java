/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;



public class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final static int
    BROWSER_RANGE   = 16,
    PREDATOR_RANGE  = 32,
    BROWSER_DENSITY = 8,
    PREDATOR_RATIO  = 4 ;
  
  final Species species ;
  private float crowding = 1.0f ;
  
  
  public Fauna(Species species) {
    if (species == null) I.complain("NULL SPECIES!") ;
    this.species = species ;
    attachSprite(species.model.makeSprite()) ;
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    crowding = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(crowding) ;
  }
  
  
  
  
  /**  Governing behaviour-
    */
  public boolean couldSwitch(Behaviour last, Behaviour next) {
    if (next == null) return false ;
    if (last == null) return true ;
    return next.priorityFor(this) >= (last.priorityFor(this) + 2) ;
  }
  
  
  public Behaviour nextBehaviour() {
    //  If you're hungry, eat.
    
    //  If you're tired, rest.
    
    //  If you bump into another member of the species, consider mating.
    
    //  Otherwise, wander about.
    
    final Tile
      o = origin(),
      pick = world.tileAt(o.x + Rand.range(-8, 8), o.y + Rand.range(-8, 8)),
      free = Spacing.nearestOpenTile(pick, this) ;
    if (free == null) return null ;
    final Action wander = new Action(
      this, free,
      this, "actionWander",
      Action.LOOK, "Wandering"
    ) ;
    return wander ;
  }
  
  
  public boolean actionBrowse(Fauna actor, Flora browsed) {
    browsed.incGrowth(-1, world) ;
    actor.health.takeSustenance(1, 1) ;
    return true ;
  }
  
  
  public boolean actionMate(Fauna actor, Fauna other) {
    return true ;
  }
  
  
  public boolean actionNest(Fauna actor, Target location) {
    return true ;
  }
  
  
  public boolean actionWander(Fauna actor, Tile location) {
    return true ;
  }
  
  
  
  /**  Regulating growth and reproduction-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    
    //
    //  FOR NOW, WE ASSUME THIS APPLIES ONLY TO BROWSERS. TODO: Fix that.
    //
    //  Firstly, we update our estimate of how crowded this browser feels-
    final float adjust = 1f / World.DEFAULT_DAY_LENGTH ;
    final float guessC = guessCrowding(Species.Type.BROWSER) ;
    crowding = (crowding * (1 - adjust)) + (guessC * adjust) ;
    
    final float idealPop = BROWSER_DENSITY * origin().habitat().biosphere ;
    
    final float
      spawnChance = adjust * idealPop / crowding,
      deathChance = adjust * crowding / idealPop ;
    //
    //  TODO:  These decisions should really be based on considerations of
    //  nutrition, bulk and overall health or disease.  Work on that.
    if (Rand.num() < spawnChance) {
      //  TODO:  Create a new, smaller creature of the same species!
    }
    if (Rand.num() < deathChance) {
      //  TODO:  Kill off this creature.
    }
  }
  
  
  float foodDensity() {
    //  In the case of browsers, check the underlying terrain's fertility.
    //  In the case of predators, check the density of browsers.
    return -1 ;
  }
  
  float peerDensity() {
    //  Browsers and predators look at the abundance of browsers and predators
    //  respectively, plus the density of their specific species.
    return -1 ;
  }
  
  
  float guessCrowding(Species.Type searched) {
    final float range = (searched == Species.Type.BROWSER) ?
      BROWSER_RANGE :
      PREDATOR_RANGE ;
    for (Target t : world().mobilesMap.visitNear(this, -1, null)) {
      if (searched != null) {
        if (! (t instanceof Fauna)) continue ;
        if (((Fauna) t).species.type != searched) continue ;
      }
      final float dist = Spacing.distance(this, t) + 1 ;
      if (dist > range) break ;
      float crowdingGuess = (range * range) / (dist * dist) ;
      return crowdingGuess ;
    }
    return -1 ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    // TODO Auto-generated method stub
    return null;
  }

  public Composite portrait(BaseUI UI) {
    // TODO Auto-generated method stub
    return null;
  }

  public String helpInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  public String[] infoCategories() {
    // TODO Auto-generated method stub
    return null;
  }

  public void writeInformation(Description d, int categoryID, BaseUI UI) {
  }
  
  public void describeBehaviour(Description d) {
    d.append(fullName()) ;
  }
}









