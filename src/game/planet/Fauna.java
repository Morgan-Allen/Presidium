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
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public abstract class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final Species species ;
  
  
  public Fauna(Species species) {
    if (species == null) I.complain("NULL SPECIES!") ;
    this.species = species ;
    initStats() ;
    attachSprite(species.model.makeSprite()) ;
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    initStats() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
  }
  
  
  public Object species() { return species ; }
  protected abstract void initStats() ;
  
  
  
  /**  Shared behavioural methods-
    */
  protected ActorAI initAI() {
    final Fauna actor = this ;
    
    return new ActorAI(actor) {
      protected Behaviour nextBehaviour() {
        final Choice choice = new Choice(actor) ;
        if (species.browses()) choice.add(nextBrowsing()) ;
        if (species.goesHunt()) choice.add(nextHunting()) ;
        //  TODO:  Add the option to retreat.
        choice.add(nextResting()) ;
        choice.add(nextMigration()) ;
        choice.add(nextBuildingNest()) ;
        //  TODO:  Add the option to kill peers in case of overcrowding.
        return choice.weightedPick(actor.AI.whimsy()) ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
        //  TODO:  If you or your home are under attack, consider defending
        //  them from the aggressor.
      }
      
      protected Behaviour reactionTo(Mobile seen) {
        return null ;
      }
    } ;
  }
  

  
  protected Behaviour nextHunting() {
    final float sampleRange = Lair.PEER_SAMPLE_RANGE ;
    final int maxSampled = 10 ;
    Actor pickedPrey = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    final PresenceMap peers = world.presences.mapFor(Mobile.class) ;
    int numSampled = 0 ;
    
    for (Target t : peers.visitNear(this, sampleRange, null)) {
      if (++numSampled > maxSampled) break ;
      if (! (t instanceof Actor)) continue ;
      final Actor f = (Actor) t ;
      final Species s = (Species) f.species() ;
      //  TODO:  Skip over artilects, silicates and the like?
      
      final float danger = Combat.combatStrength(f) * Rand.num() ;
      final float dist = Spacing.distance(f, this) / sampleRange ;
      float rating = (1 - dist) / danger ;
      if (s.type != Species.Type.BROWSER) rating /= 2 ;
      if (! (t instanceof Fauna)) rating /= 2 ;
      
      if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
    }
    
    if (pickedPrey == null) return null ;
    final Hunting hunting = new Hunting(this, pickedPrey, Hunting.TYPE_FEEDS) ;
    return hunting ;
  }
  
  
  protected Behaviour nextBrowsing() {
    final PresenceMap PM = this.world().presences.mapFor(Flora.class) ;
    Flora f = (Flora) PM.pickRandomAround(this, health.sightRange()) ;
    if (f == null) f = (Flora) PM.pickNearest(this, -1) ;
    if (f == null) return null ;
    final Action browse = new Action(
      this, f,
      this, "actionBrowse",
      Action.STRIKE, "Browsing"
    ) ;
    browse.setMoveTarget(Spacing.nearestOpenTile(f.origin(), this)) ;
    final float priority = health.hungerLevel() * Action.PARAMOUNT ;
    browse.setPriority(priority - Plan.rangePenalty(this, f)) ;
    return browse ;
  }
  
  
  public boolean actionBrowse(Fauna actor, Flora eaten) {
    if (! eaten.inWorld()) return false ;
    eaten.incGrowth(-0.1f, actor.world(), false) ;
    actor.health.takeSustenance(1, 1) ;
    return true ;
  }
  
  
  protected Behaviour nextResting() {
    Target restPoint = this.origin() ;
    final Lair lair = (Lair) this.AI.home() ;
    if (lair != null && lair.inWorld() && lair.structure.intact()) {
      restPoint = lair ;
    }
    
    final Action rest = new Action(
      this, restPoint,
      this, "actionRest",
      Action.FALL, "Resting"
    ) ;
    final float priority = health.fatigueLevel() * Action.PARAMOUNT ;
    rest.setPriority(priority) ;
    return rest ;
  }
  
  
  public boolean actionRest(Fauna actor, Target point) {
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    final Lair lair = (Lair) actor.AI.home() ;
    if (lair != point) return true ;
    final float rating = species.goesHunt() ? lair.rateCrowding(world) : 1 ;
    //final float rating = lair.rateCrowding(world) ;
    ///if (BaseUI.isPicked(this)) I.say("Crowding is: "+rating) ;
    if (rating < 0 || lair.crowding() > 1) return false ;
    //
    //  If the venue's not too crowded, consider reproducing.
    if (Rand.index(4) > actor.health.agingStage()) return false ;
    if (actor.health.hungerLevel() > 0.5f) return false ;
    I.say("Giving birth to new "+actor.species.name+" at: "+point) ;
    //
    //  Don't breed if you're too young or too hungry.  Otherwise, produce
    //  offpsring in inverse proportion to lifespan-
    actor.health.loseSustenance(0.25f) ;
    final int maxKids = 1 + (int) Math.sqrt(10f / health.lifespan()) ;
    for (int numKids = 1 + Rand.index(maxKids) ; numKids-- > 0 ;) {
      final Fauna young = actor.species.newSpecimen() ;
      young.health.setupHealth(0, 1, 0) ;
      young.AI.setHomeVenue(lair) ;
      final Tile e = lair.mainEntrance() ;
      young.enterWorldAt(e.x, e.y, e.world) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextMigration() {
    final Lair lair = (Lair) this.AI.home() ;
    final float range = Lair.PEER_SAMPLE_RANGE ;
    Tile free = Spacing.pickRandomTile(this, range, world) ;
    free = Spacing.nearestOpenTile(free, this) ;
    
    if (free == null) return null ;
    if (lair != null && Spacing.distance(free, lair) > range * 2) return null ;
    
    final Action migrate = new Action(
      this, free,
      this, "actionMigrate",
      Action.LOOK, "Wandering"
    ) ;
    final float crowdBonus = (lair == null) ? 0 : lair.crowding() ;
    final float priority = Action.IDLE + (crowdBonus * Action.CASUAL) ;
    migrate.setPriority(priority - Plan.rangePenalty(lair, free)) ;
    return migrate ;
  }
  
  
  public boolean actionMigrate(Fauna actor, Tile point) {
    final Lair lair = (Lair) actor.AI.home() ;
    final boolean shouldNest = (lair == null) || (lair.crowding() > 0.5f) ;
    //
    //  If you're homeless, or if home is overcrowded, consider moving into a
    //  vacant lair, or building a new one.
    if (shouldNest) {
      int numTried = 0 ;
      for (Object t : world.presences.matchesNear(Lair.class, actor, null)) {
        if (++numTried > 5) break ;
        if (! (t instanceof Lair)) continue ;
        final Lair vacant = (Lair) t ;
        if (vacant.species == this.species && vacant.crowding() <= 0.5f) {
          actor.AI.setHomeVenue(vacant) ;
          return true ;
        }
      }
    }
    if (shouldNest) {
      final Lair newLair = actor.species.createLair() ;
      newLair.setPosition(point.x, point.y, actor.world()) ;
      float rating = newLair.rateCrowding(actor.world()) ;
      if (rating > 0) {
        actor.AI.setHomeVenue(newLair) ;
        newLair.clearSurrounds() ;
        newLair.enterWorld() ;
        newLair.structure.setState(VenueStructure.STATE_INTACT, 0.1f) ;
      }
      else return false ;
    }
    return true ;
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Lair lair = (Lair) this.AI.home() ;
    if (lair == null) return null ;
    final float repair = lair.structure.repairLevel() ;
    if (repair >= 1) return null ;
    final Action buildNest = new Action(
      this, lair,
      this, "actionBuildNest",
      Action.STRIKE, "Building Nest"
    ) ;
    buildNest.setMoveTarget(Spacing.pickFreeTileAround(lair, this)) ;
    if (! lair.structure.intact()) buildNest.setPriority(Action.ROUTINE) ;
    else buildNest.setPriority(((1f - repair) * Action.ROUTINE) + 2) ;
    return buildNest ;
  }
  
  
  public boolean actionBuildNest(Fauna actor, Lair nest) {
    if (! nest.inWorld()) nest.enterWorld() ;
    nest.structure.repairBy(nest.structure.maxIntegrity() / 10f) ;
    return true ;
  }
  
  
  
  
  
  /**  Rendering and interface methods-
    */
  protected float spriteScale() {
    float scale = health.agingStage() * 1f / ActorHealth.AGE_MAX ;
    return (float) Math.sqrt(scale + 0.5f) ;
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
    if (currentAction() != null) currentAction().describeBehaviour(d) ;
    else d.append(health.stateDesc()) ;
    d.append("\nNests at: ") ;
    if (AI.home() != null) d.append(AI.home()) ;
    else d.append("No nest") ;
    d.append("\nCondition:") ;
    final Batch <String> CD = health.conditionsDesc() ;
    for (String s : CD) d.append("\n  "+s) ;
    if (CD.size() == 0) d.append("\n  Okay") ;
    
    d.append("\n\n") ;
    d.append(species.info) ;
  }
}







