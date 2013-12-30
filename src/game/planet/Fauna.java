/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  You need to implement defence of home!


public abstract class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final public static float
    PLANT_CONVERSION = 4.0f,
    MEAT_CONVERSION  = 4.0f ;
  private static boolean verbose = false ;
  
  
  final public Species species ;
  private float breedMetre = 0.0f ;
  
  
  public Fauna(Species species) {
    if (species == null) I.complain("NULL SPECIES!") ;
    this.species = species ;
    initStats() ;
    attachSprite(species.model.makeSprite()) ;
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    breedMetre = s.loadFloat() ;
    initStats() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(breedMetre) ;
  }
  
  
  public Object species() { return species ; }
  protected abstract void initStats() ;
  
  
  
  /**  Registering abundance with the ecology class-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    world.ecology().impingeAbundance(this, false) ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    world.ecology().impingeAbundance(this, true) ;
    if (numUpdates % 10 == 0 && health.alive()) {
      float crowding = Nest.crowdingFor(this) ;
      if (crowding == 1) crowding += 0.1f ;
      float fertility = (health.agingStage() - 0.5f) * health.energyLevel() ;
      float breedInc = (1 - crowding) * 10 / Nest.DEFAULT_BREED_INTERVAL ;
      breedInc *= Visit.clamp(fertility, 0, ActorHealth.AGE_MAX) ;
      breedMetre = Visit.clamp(breedMetre + breedInc, 0, 1) ;
    }
  }
  


  /**  Shared behavioural methods-
    */
  protected ActorMind initAI() {
    final Fauna actor = this ;
    
    return new ActorMind(actor) {
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.weightedPick() ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
        //  TODO:  If you or your home are under attack, consider defending
        //  them from the aggressor.
      }
      
      protected Behaviour reactionTo(Element seen) {
        if (BaseUI.isPicked(this)) I.say(this+" has seen: "+seen) ;
        if (seen instanceof Actor) return nextDefence((Actor) seen) ;
        return nextDefence(null) ;
      }
      
      //
      //  We install some default relationships with other animals-
      public float relation(Actor other) {
        if (other instanceof Fauna) {
          final Fauna f = (Fauna) other ;
          if (f.species == species) return 0.25f ;
          if (f.species.type == Species.Type.BROWSER) return 0 ;
          if (f.species.predator()) return -0.5f ;
        }
        return -0.25f ;
      }
    } ;
  }
  
  
  protected Behaviour nextHunting() {
    final Actor prey = Hunting.nextPreyFor(this, false) ;
    if (prey == null) return null ;
    final Hunting hunting = Hunting.asFeeding(this, prey) ;
    return hunting ;
  }
  
  
  protected Behaviour nextBrowsing() {
    //final PresenceMap PM = this.world().presences.mapFor(Flora.class) ;
    final Batch <Flora> sampled = new Batch <Flora> () ;
    world.presences.sampleFromKey(this, world, 5, sampled, Flora.class) ;
    
    Flora picked = null ;
    float bestRating = 0 ;
    final float range = Nest.forageRange(species) * 2 ;
    for (Flora f : sampled) {
      final float dist = Spacing.distance(this, f) ;
      if (dist > range) continue ;
      float rating = f.growth * Rand.avgNums(2) ;
      rating *= range / (range + dist) ;
      if (rating > bestRating) { picked = f ; bestRating = rating ; }
    }
    if (picked == null) return null ;
    
    final Action browse = new Action(
      this, picked,
      this, "actionBrowse",
      Action.STRIKE, "Browsing"
    ) ;
    browse.setMoveTarget(Spacing.nearestOpenTile(picked.origin(), this)) ;
    float priority = ActorHealth.MAX_CALORIES - health.energyLevel() ;
    priority *= Action.PARAMOUNT ;
    browse.setPriority(priority - Plan.rangePenalty(this, picked)) ;
    return browse ;
  }
  
  
  public boolean actionBrowse(Fauna actor, Flora eaten) {
    if (! eaten.inWorld()) return false ;
    float bite = 0.1f * eaten.growth * 2 * health.maxHealth() / 10 ;
    eaten.incGrowth(0 - bite, actor.world(), false) ;
    actor.health.takeSustenance(bite * PLANT_CONVERSION, 1) ;
    return true ;
  }
  
  
  protected Behaviour nextResting() {
    Target restPoint = this.origin() ;
    final Nest nest = (Nest) this.mind.home() ;
    if (nest != null && nest.inWorld() && nest.structure.intact()) {
      restPoint = nest ;
    }
    final Action rest = new Action(
      this, restPoint,
      this, "actionRest",
      Action.FALL, "Resting"
    ) ;
    final float fatigue = health.fatigueLevel() ;
    if (fatigue < 0) return null ;
    final float priority = fatigue * Action.PARAMOUNT ;
    rest.setPriority(priority) ;
    return rest ;
  }
  
  
  public boolean actionRest(Fauna actor, Target point) {
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    final Nest nest = (Nest) actor.mind.home() ;
    if (nest != point) return true ;
    return true ;
  }
  
  
  protected Behaviour nextMigration() {
    Target wandersTo = null ;
    String description = null ;
    float priority = 0 ;
    
    final boolean crowded = Nest.crowdingFor(this) > 0.5f ;
    if (verbose) I.sayAbout(this, "Crowded? "+crowded) ;
    final Nest newNest = crowded ? Nest.findNestFor(this) : null ;
    if (newNest != null && newNest != mind.home()) {
      if (verbose) I.sayAbout(this, "Found new nest! "+newNest.origin()) ;
      wandersTo = newNest ;
      description = "Migrating" ;
      priority = Action.ROUTINE ;
    }
    
    else {
      final Target centre = mind.home() == null ? this : mind.home() ;
      wandersTo = Spacing.pickRandomTile(
        centre, Nest.forageRange(species) / 2, world
      ) ;
      description = "Wandering" ;
      priority = Action.IDLE * Planet.dayValue(world) ;
    }
    if (wandersTo == null) return null ;
    
    final Action migrates = new Action(
      this, wandersTo,
      this, "actionMigrate",
      Action.LOOK, description
    ) ;
    migrates.setPriority(priority) ;
    if (! wandersTo.inWorld()) {
      migrates.setMoveTarget(Spacing.pickFreeTileAround(wandersTo, this)) ;
    }
    return migrates ;
  }
  
  
  public boolean actionMigrate(Fauna actor, Target point) {
    if (point instanceof Nest) {
      final Nest nest = (Nest) point ;
      if (Nest.crowdingFor(this) < 0.5f) return false ;
      if (Nest.crowdingFor(nest, species, world) > 0.5f) return false ;
      
      if (! nest.inWorld()) {
        nest.clearSurrounds() ;
        nest.enterWorld() ;
        nest.structure.setState(Structure.STATE_INTACT, 0.01f) ;
      }
      actor.mind.setHome(nest) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Nest nest = (Nest) this.mind.home() ;
    if (nest == null) return null ;
    final float repair = nest.structure.repairLevel() ;
    if (repair >= 1) return null ;
    final Action buildNest = new Action(
      this, nest,
      this, "actionBuildNest",
      Action.STRIKE, "Building Nest"
    ) ;
    buildNest.setMoveTarget(Spacing.pickFreeTileAround(nest, this)) ;
    if (! nest.structure.intact()) buildNest.setPriority(Action.ROUTINE) ;
    else buildNest.setPriority(((1f - repair) * Action.ROUTINE) + 2) ;
    return buildNest ;
  }
  
  
  public boolean actionBuildNest(Fauna actor, Nest nest) {
    if (! nest.inWorld()) nest.enterWorld() ;
    nest.structure.repairBy(nest.structure.maxIntegrity() / 10f) ;
    return true ;
  }
  
  
  protected Behaviour nextBreeding() {
    if (mind.home() == null) return null ;
    final Action breeds = new Action(
      this, mind.home(),
      this, "actionBreed",
      Action.FALL, "Breeding"
    ) ;
    return breeds ;
  }
  
  
  public boolean actionBreed(Fauna actor, Nest nests) {
    actor.breedMetre = 0 ;
    final int maxKids = 1 + (int) Math.sqrt(10f / health.lifespan()) ;
    for (int numKids = 1 + Rand.index(maxKids) ; numKids-- > 0 ;) {
      final Fauna young = species.newSpecimen() ;
      young.health.setupHealth(0, 1, 0) ;
      young.mind.setHome(nests) ;
      final Tile e = nests.mainEntrance() ;
      young.enterWorldAt(e.x, e.y, e.world) ;
      I.say("Giving birth to new "+actor.species.name+" at: "+nests) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    return new Retreat(this) ;
  }
  
  
  protected void addChoices(Choice choice) {
    if (species.browser()) choice.add(nextBrowsing()) ;
    if (species.predator()) choice.add(nextHunting()) ;
    if (breedMetre >= 0.99f) choice.add(nextBreeding()) ;
    choice.add(nextDefence(null)) ;
    choice.add(nextResting()) ;
    choice.add(nextMigration()) ;
    choice.add(nextBuildingNest()) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float spriteScale() {
    return (float) Math.sqrt(health.ageLevel() + 0.5f) ;
  }
  
  
  public String fullName() {
    return health.agingDesc()+" "+species.name ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, species.portrait) ;
  }
  
  
  public String helpInfo() {
    return species.info ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    d.append("Is: ") ;
    describeStatus(d) ;
    
    d.append("\nNests at: ") ;
    if (mind.home() != null) {
      d.append(mind.home()) ;
      final int BP = (int) (breedMetre * 100) ;
      d.append("\n  Breeding condition: "+BP+"%") ;
    }
    else d.append("No nest") ;
    
    d.append("\nCondition: ") ;
    final Batch <String> CD = health.conditionsDesc() ;
    if (CD.size() == 0) d.append("Okay") ;
    else d.appendList("", CD) ;
    
    //d.append("\n\nCombat strength: "+Combat.combatStrength(this, null)) ;
    
    d.append("\n\n") ;
    d.append(species.info, Colour.LIGHT_GREY) ;
  }
}







