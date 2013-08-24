/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.planet ;
import src.game.building.VenueStructure;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;




/*
final public static int
  GROUND_SAMPLE_RANGE = 16,
  PEER_SAMPLE_RANGE   = 16,
  SAMPLE_AREA         = 16 * 16 * 4,
  BROWSER_DENSITY     = 12,
  PREDATOR_RATIO      = 12 ;
//*/
//
//  I need abstract methods for-
//    rateMigratePoint()
//    getRestPoint()
//    getMating()
//    getFeeding()
//    getDefence()
//    fightWith()



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
    ///crowding = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    ///s.saveFloat(crowding) ;
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
        choice.add(nextBrowsing()) ;
        choice.add(nextResting()) ;
        choice.add(nextMigration()) ;
        choice.add(nextBuildingNest()) ;
        return choice.weightedPick(actor.AI.whimsy()) ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
        //  TODO:  If you or your home are under attack, either defend or
        //  retreat.
      }
      
      protected Behaviour reactionTo(Mobile seen) {
        return null ;
      }
    } ;
  }
  
  
  //
  //  TODO:  Outsource this to the ActorAI or possibly DangerMap classes.
  private float rangePenalty(Target a, Target b) {
    if (a == null || b == null) return 0 ;
    return Spacing.distance(a, b) * 2f / Terrain.SECTOR_SIZE ;
  }
  
  
  private int crowding() {
    final Lair lair = (Lair) this.AI.home() ;
    return (lair == null) ? 0 : lair.personnel.residents().size() ;
  }

  
  
  protected Behaviour nextHunting() {
    
    return null ;
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
    browse.setPriority(priority - rangePenalty(this, f)) ;
    return browse ;
  }
  
  
  public boolean actionBrowse(Fauna actor, Flora eaten) {
    if (! eaten.inWorld()) return false ;
    eaten.incGrowth(-0.25f, actor.world(), false) ;
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
    I.say(actor+" HAS BEGUN RESTING.") ;
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    final Lair lair = (Lair) actor.AI.home() ;
    if (lair != point) return true ;
    final int crowding = lair.personnel.residents().size() ;
    if (crowding >= Lair.LAIR_POPULATION) return false ;
    //
    //  If the venue's not too crowded, consider reproducing.
    if (Rand.index(4) < actor.health.agingStage()) return false ;
    if (actor.health.hungerLevel() > 0.5f) return false ;
    I.say("Giving birth to new "+actor.species.name+" at: "+point) ;
    //
    //  Don't breed if you're too young or too hungry...
    actor.health.loseSustenance(0.25f) ;
    final Fauna young = actor.species.newSpecimen() ;
    young.health.setupHealth(0, 1, 0) ;
    young.AI.setHomeVenue(lair) ;
    final Tile e = lair.mainEntrance() ;
    young.enterWorldAt(e.x, e.y, e.world) ;
    return true ;
  }
  
  
  protected Behaviour nextMigration() {
    final Lair lair = (Lair) this.AI.home() ;
    final float range = Lair.PEER_SAMPLE_RANGE ;
    Tile free = Spacing.pickRandomTile(this, range) ;
    free = Spacing.nearestOpenTile(free, this) ;
    if (free == null) return null ;
    if (lair != null && Spacing.distance(free, lair) > range * 2) return null ;
    
    final Action migrate = new Action(
      this, free,
      this, "actionMigrate",
      Action.LOOK, "Migrating"
    ) ;
    final float priority = Action.IDLE + crowding() ;
    migrate.setPriority(priority - rangePenalty(lair, free)) ;
    return migrate ;
  }
  
  
  public boolean actionMigrate(Fauna actor, Tile point) {
    final Lair lair = (Lair) actor.AI.home() ;
    final boolean shouldNest =
      (lair == null) ||
      (lair.personnel.residents().size() > Lair.LAIR_POPULATION / 2) ;
    //
    //  If you're homeless, or if home is overcrowded, consider building a new
    //  nest for yourself.
    if (shouldNest) {
      final Lair newLair = actor.species.createLair() ;
      newLair.setPosition(point.x, point.y, actor.world()) ;
      float rating = newLair.ratePosition(actor.world()) ;
      if (rating > 0) {
        actor.AI.setHomeVenue(newLair) ;
        newLair.enterWorld() ;
        newLair.structure.setState(VenueStructure.STATE_INSTALL, 0.1f) ;
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
    else buildNest.setPriority(((1f - repair) * Action.ROUTINE)) ;
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
    return species.name ;//+" "+this.hashCode() ;
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
    d.append("\nIs: ") ;
    if (currentAction() != null) {
      currentAction().describeBehaviour(d) ;
    }
    else d.append(health.stateDesc()) ;
    
    d.append("\n\nCondition:") ;
    final Batch <String> CD = health.conditionsDesc() ;
    for (String s : CD) d.append("\n  "+s) ;
    if (CD.size() == 0) d.append("\n  Okay") ;
    
    d.append("\n\n") ;
    d.append(species.info) ;
  }
}







/*
//  TODO:  React to any other organisms nearby, including running/defence.
if (psyche.dangerLevel() > 0) {
  final Behaviour defence = nextDefence(psyche.seen()) ;
  if (defence != null) return defence ;
}
//*/


/*
protected Behaviour nextMating() {
  //
  //  Find the nearest Fauna of the same species and adult age.  Converge on
  //  them.  TODO:  That.
  final Tile free = Spacing.nearestOpenTile(origin(), this) ;
  if (free == null) return null ;
  final Action birthing = new Action(
    this, free,
    this, "actionBirth",
    Action.STRIKE, "Birthing"
  ) ;
  birthing.setPriority(Behaviour.CRITICAL) ;
  return birthing ;
}


public boolean actionBirth(Fauna fauna, Tile location) {
  if (health.energyLevel() < 0.5f) return false ;
  //
  //  Pop out a baby version of whatever species you belong to!
  I.say("Giving birth to new "+species.name+" at: "+location) ;
  final Fauna young = species.newSpecimen() ;
  young.health.setupHealth(0, 1, 0) ;
  young.enterWorldAt(location.x, location.y, world()) ;
  health.loseSustenance(0.25f) ;
  return true ;
}



//
//  TODO:  This should probably be evaluated on a sector-by-sector basis, or
//  possibly by lairs.


public void updateAsScheduled(int numUpdates) {
  super.updateAsScheduled(numUpdates) ;
}
//*/


/*
if ((! inWorld()) || (! health.conscious())) return ;
//
//  And, once per day, attempt actual reproduction-
int time = numUpdates ;
time += health.ageLevel() * World.DEFAULT_DAY_LENGTH ;
if (time % World.DEFAULT_DAY_LENGTH == 0) {
  //
  //  Don't reproduce if you're too crowded, or too hungry-
  final float crowdGuess = guessCrowding(origin()) ;
  boolean shouldMate = crowdGuess < 1 ;
  if (health.energyLevel() < 0.5f) shouldMate = false ;
  //
  //  Predators need to thin eachother out if they're too crowded, and also
  //  reproduce more slowly-
  if (species.type == Species.Type.PREDATOR) {
    if (Rand.num() < crowdGuess - 2.0f) {
      //  TODO:  Don't select the target here.  Leave that to the method.
      final Fauna competes = specimens(
        origin(), -1, null, Species.Type.PREDATOR, 1
      ).first() ;
      if (competes != null) fightWith(competes) ;
    }
    if (Rand.index(PREDATOR_RATIO) != 0) {
      shouldMate = false ;
    }
  }
  //
  //  If everything else checks out, consider making a baby-
  if (shouldMate) {
    final Behaviour mating = nextMating() ;
    if (AI.couldSwitchTo(mating)) {
      AI.assignBehaviour(mating) ;
    }
  }
}
//*/


/*
public float guessCrowding(Tile point) {
  final int WS = point.world.size, range = (int) (PEER_SAMPLE_RANGE * (
    species.type == Species.Type.PREDATOR ? 1.5f : 0.75f
  )) ;
  final Box2D limit = new Box2D().set(point.x, point.y, 0, 0) ;
  limit.expandBy(range).cropBy(new Box2D().set(0, 0, WS, WS)) ;
  
  final PresenceMap mobilesMap = point.world.presences.mapFor(Mobile.class) ;
  float numPeers = 0, numPrey = 0, numHunters = 0 ;
  for (Target t : mobilesMap.visitNear(point, -1, limit)) {
    if (! (t instanceof Actor)) continue ;
    final Actor f = (Actor) t ;
    final Species s = (Species) f.species() ;
    if (s == species) numPeers++ ;
    if (s.type == Species.Type.BROWSER) numPrey++ ;
    if (s.type == Species.Type.PREDATOR) numHunters++ ;
  }
  
  if (species.type == Species.Type.BROWSER) {
    final float fertility = evalFertility(point) ;
    if (fertility == 0) return 100 ;
    float idealPop = BROWSER_DENSITY * fertility ;
    float rarity = (((numPrey + 1) / (numPeers + 1)) + 1) / 2f ;
    return numPrey * (SAMPLE_AREA / limit.area()) / (idealPop * rarity) ;
  }
  else {
    if (numPrey == 0) return 100 ;
    float idealPop = numPrey / PREDATOR_RATIO ;
    float rarity = (((numHunters + 1) / (numPeers + 1)) + 1) / 2f ;
    return numHunters / (idealPop * rarity) ;
  }
}


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
//*/



