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
import src.graphics.common.Colour;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public abstract class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final public Species species ;
  
  
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
  }
  


  /**  Shared behavioural methods-
    */
  protected ActorMind initAI() {
    final Fauna actor = this ;
    
    return new ActorMind(actor) {
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.weightedPick(actor.mind.whimsy()) ;
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
          if (f.species.goesHunt()) return -0.5f ;
        }
        return -0.25f ;
      }
    } ;
  }
  
  
  protected Behaviour nextHunting() {
    final Actor prey = Hunting.nextPreyFor(this, Lair.PREDATOR_SAMPLE_RANGE) ;
    if (prey == null) return null ;
    final Hunting hunting = new Hunting(this, prey, Hunting.TYPE_FEEDS) ;
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
    final Lair lair = (Lair) this.mind.home() ;
    if (lair != null && lair.inWorld() && lair.structure.intact()) {
      restPoint = lair ;
    }
    final Action rest = new Action(
      this, restPoint,
      this, "actionRest",
      Action.FALL, "Resting"
    ) ;
    final float fatigue = health.fatigueLevel() - 0.25f ;
    if (fatigue < 0) return null ;
    final float priority = Action.CASUAL + (fatigue * Action.URGENT / 0.75f) ;
    rest.setPriority(priority) ;
    return rest ;
  }
  
  
  public boolean actionRest(Fauna actor, Target point) {
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    final Lair lair = (Lair) actor.mind.home() ;
    if (lair != point) return true ;
    final float rating = lair.rateCurrentSite(world) ;
    if (rating < 0 || lair.crowding() > 1) return false ;
    //
    //  If the venue's not too crowded, consider reproducing.
    if (Rand.index(3) > actor.health.agingStage()) return false ;
    if (actor.health.hungerLevel() > 0.5f) return false ;
    //
    //  Don't breed if you're too young or too hungry.  Otherwise, produce
    //  offpsring in inverse proportion to lifespan-
    actor.health.loseSustenance(0.25f) ;
    final int maxKids = 1 + (int) Math.sqrt(10f / health.lifespan()) ;
    for (int numKids = 1 + Rand.index(maxKids) ; numKids-- > 0 ;) {
      final Fauna young = actor.species.newSpecimen() ;
      young.health.setupHealth(0, 1, 0) ;
      young.mind.setHomeVenue(lair) ;
      final Tile e = lair.mainEntrance() ;
      young.enterWorldAt(e.x, e.y, e.world) ;
      I.say("Giving birth to new "+actor.species.name+" at: "+point) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextMigration() {
    final Lair lair = (Lair) this.mind.home() ;
    final float range = species.forageRange() ;
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
    final Lair lair = (Lair) actor.mind.home() ;
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
          actor.mind.setHomeVenue(vacant) ;
          return true ;
        }
      }
    }
    if (shouldNest) return siteNewLair() != null ;
    return true ;
  }
  
  
  protected Lair siteNewLair() {
    final Actor actor = this ;
    final Lair newLair = species.createLair() ;
    final TileSpread spread = new TileSpread(origin()) {
      
      protected boolean canAccess(Tile t) {
        return Spacing.distance(actor, t) < 4 ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        newLair.setPosition(t.x, t.y, actor.world()) ;
        final float rating = newLair.rateCurrentSite(actor.world()) ;
        if (rating <= 0) return false ;
        actor.mind.setHomeVenue(newLair) ;
        newLair.clearSurrounds() ;
        newLair.enterWorld() ;
        newLair.structure.setState(Structure.STATE_INTACT, 0.1f) ;
        actor.goAboard(newLair, world) ;
        return true ;
      }
    } ;
    spread.doSearch() ;
    if (! newLair.inWorld()) return null ;
    return newLair ;
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Lair lair = (Lair) this.mind.home() ;
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
  
  
  protected Behaviour nextDefence(Actor near) {
    return new Retreat(this) ;
  }
  
  
  protected void addChoices(Choice choice) {
    if (species.browses()) choice.add(nextBrowsing()) ;
    if (species.goesHunt()) choice.add(nextHunting()) ;
    choice.add(nextDefence(null)) ;
    choice.add(nextResting()) ;
    choice.add(nextMigration()) ;
    choice.add(nextBuildingNest()) ;
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
    describeStatus(d) ;
    
    d.append("\nNests at: ") ;
    if (mind.home() != null) d.append(mind.home()) ;
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







