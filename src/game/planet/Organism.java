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


//
//  The basic problem here appears to be that the predators simply eat their
//  prey faster than they can possibly reproduce.  Shoot.

//  Well, in principle one Micovore needs to eat one Quud per day to stay alive.
//  If the Quud outnumber the Micovore 4 to 1, and can reproduce once per day,
//  then they ought to be okay.



public abstract class Organism extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  //  TODO:  I'm not sure explicit ratios are useful here?
  final public static int
    BROWSER_RANGE   = 16,
    PREDATOR_RANGE  = 32,
    BROWSER_DENSITY = 32,
    PREDATOR_RATIO  = 16 ;
  
  final Species species ;
  private float crowding = 1.0f ;
  //TODO:  Track food and peer density separately?  No.  Get rid of this field
  //  entirely, and use periodic surveys instead.
  
  
  public Organism(Species species) {
    if (species == null) I.complain("NULL SPECIES!") ;
    this.species = species ;
    initStats() ;
    attachSprite(species.model.makeSprite()) ;
  }
  
  
  public Organism(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    initStats() ;
    crowding = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(crowding) ;
  }
  
  
  public Object species() { return species ; }
  
  protected abstract void initStats() ;
  
  
  
  /**  Shared behavioural methods-
    */
  protected ActorPsyche initPsyche() {
    final Organism fauna = this ;
    return new ActorPsyche(this) {
      protected Behaviour nextBehaviour() {
        return fauna.nextBehaviour() ;
      }
      protected Behaviour reactionTo(Mobile seen) {
        return fauna.reactionTo(seen) ;
      }
    } ;
  }
  
  
  protected Behaviour nextBehaviour() {
    
    final Choice choice = new Choice(this) ;
    //  TODO:  Use a Choice here instead of a decision tree.
    //
    //  If you're scared, either run or defend yourself.
    
    //  TODO:  Put this in the reactionTo method.
    /*
    //  TODO:  React to any other organisms nearby, including mating.
    if (psyche.dangerLevel() > 0) {
      final Behaviour defence = nextDefence(psyche.seen()) ;
      if (defence != null) return defence ;
    }
    //*/
    //
    //  If you're hungry, eat.
    final float hunger = 1f - health.energyLevel() ;
    if (hunger > 0.33f) {
      final Behaviour feeding = nextFeeding() ;
      if (feeding != null) {
        feeding.setPriority((hunger - 0.33f) * Behaviour.CRITICAL / 0.66f) ;
        choice.add(feeding) ;
      }
    }
    //
    //  If you're tired, rest.
    final float fatigue = health.fatigueLevel() ;
    if (fatigue > 0.33f) {
      final Behaviour resting = nextResting() ;
      if (resting != null) {
        resting.setPriority((fatigue - 0.33f) * Behaviour.CRITICAL / 0.66f) ;
        choice.add(resting) ;
      }
    }
    //
    //  Otherwise, wander about.
    final Behaviour migration = nextMigration() ;
    if (migration != null) {
      migration.setPriority(Behaviour.CASUAL) ;
      choice.add(migration) ;
    }
    //
    //  Pick one of the above and initiate the behaviour-
    return choice.weightedPick() ;
  }
  
  
  protected Behaviour reactionTo(Mobile seen) {
    return null ;
  }
  
  
  protected abstract Behaviour nextFeeding() ;
  
  
  protected Tile randomSeenTile() {
    final Tile o = origin() ;
    final float range = health.sightRange() ;
    return o.world.tileAt(
      o.x + Rand.range(-range, range),
      o.y + Rand.range(-range, range)
    ) ;
  }
  
  
  protected Behaviour nextMigration() {
    //
    //  We favour wandering into areas that are perceived as more attractive-
    //  i.e, have fewer competitors and more food- i.e, less crowded.
    Tile best = null ; float bestRating = Float.NEGATIVE_INFINITY ;
    for (int numTries = 3 ; numTries-- > 0 ;) {
      final Tile free = Spacing.nearestOpenTile(randomSeenTile(), this) ;
      if (free == null) continue ;
      final float rating = 0 - guessCrowding(free) ;
      if (rating > bestRating) { bestRating = rating ; best = free ; }
    }
    if (best == null) return null ;
    final Action migrate = new Action(
      this, best,
      this, "actionMigrate",
      Action.LOOK, "Migrating"
    ) ;
    return migrate ;
  }
  
  
  public boolean actionMigrate(Organism actor, Tile location) {
    return true ;
  }
  
  
  protected Behaviour nextResting() {
    final Target restPoint = findRestPoint() ;
    if (restPoint == null) return null ;
    final Action resting = new Action(
      this, restPoint,
      this, "actionRest",
      Action.FALL, "Resting"
    ) ;
    return resting ;
  }
  
  
  protected abstract Target findRestPoint() ;
  
  
  public boolean actionRest(Organism actor, Target location) {
    health.setState(ActorHealth.STATE_RESTING) ;
    return true ;
  }
  
  
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
  
  
  public boolean actionBirth(Organism fauna, Tile location) {
    //
    //  Pop out a baby version of whatever species you belong to!
    I.say("Giving birth to new "+species.name+" at: "+location+", crowding: "+crowding) ;
    final Organism young = species.newSpecimen() ;
    young.health.setupHealth(0, 1, 0) ;
    young.enterWorldAt(location.x, location.y, world()) ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    ///try {
    super.updateAsScheduled(numUpdates) ;
    if ((! inWorld()) || (! health.conscious())) return ;
    final float crowdGuess = guessCrowding(origin()) ;
    final float adjust = 1f / World.DEFAULT_DAY_LENGTH ;
    crowding = (crowding * (1 - adjust)) + (crowdGuess * adjust) ;
    //*
    //
    //  And, once per day, attempt actual reproduction-
    if (numUpdates % World.DEFAULT_DAY_LENGTH == 0) {
      //  TODO:  Implement this-
      //  If you bump into another member of your species, consider mating.
      boolean shouldMate = Rand.num() > crowding ;
      if (health.agingStage() == 0) shouldMate = false ;
      /*
      if (species.type == Species.Type.PREDATOR) {
        if (Rand.index(PREDATOR_RATIO) != 0) shouldMate = false ;
      }
      //*/
      //boolean shouldMate = true ;
      ///if (Rand.avgNums(2) > health.energyLevel()) shouldMate = false ;
      //if (Rand.avgNums(2) > health.agingStage() / 4f) shouldMate = false ;
      //if (Rand.avgNums(2) < crowding) shouldMate = false ;
      if (shouldMate) {
        final Behaviour mating = nextMating() ;
        if (psyche.couldSwitchTo(mating)) psyche.assignBehaviour(mating) ;
      }
    }
    //*/
  }
  
  
  protected float guessCrowding(Tile point) {
    float foodDensity = 0, peerDensity = 0 ;
    if (species.type == Species.Type.PREDATOR) {
      foodDensity = guessCrowding(point, null, Species.Type.BROWSER) ;
      foodDensity /= PREDATOR_RATIO ;
      peerDensity = guessCrowding(point, null, Species.Type.PREDATOR) ;
    }
    else {
      final int BR = BROWSER_RANGE / 2 ;
      final Tile seen = point.world.tileAt(
        point.x + Rand.range(-BR, BR),
        point.y + Rand.range(-BR, BR)
      ) ;
      if (seen != null) foodDensity = seen.habitat().moisture / 10f ;
      foodDensity *= BROWSER_DENSITY ;
      peerDensity = guessCrowding(point, null, Species.Type.BROWSER) ;
    }
    peerDensity += guessCrowding(point, species, null) ;
    peerDensity /= 2 ;
    final float crowdGuess = (peerDensity + 0.1f) / (foodDensity + 0.1f) ;
    return crowdGuess ;
  }
  
  
  protected float guessCrowding(
    Tile point, Species species, Species.Type type
  ) {
    final float range = (this.species.type == Species.Type.PREDATOR) ?
      PREDATOR_RANGE :
      BROWSER_RANGE ;
    final Organism nearest = nearestSpecimen(point, range, species, type) ;
    if (nearest == null) return 0 ;
    final float dist = Spacing.distance(point, nearest) + 1 ;
    float crowdingGuess = (range * range) / (dist * dist) ;
    return crowdingGuess ;
  }
  
  
  protected Organism nearestSpecimen(
    Tile point, float range, Species species, Species.Type type
  ) {
    int numTries = 0 ;
    for (Target t : point.world.mobilesMap.visitNear(point, -1, null)) {
      if (t == this) continue ;
      if (type != null || species != null) {
        if (! (t instanceof Organism)) continue ;
        final Organism f = (Organism) t ;
        if (species != null && f.species != species) continue ;
        if (type != null && f.species.type != type) continue ;
      }
      final float dist = Spacing.distance(point, t) + 1 ;
      if (range > 0 && dist > range) break ;
      if (++numTries > 10) break ;
      return (Organism) t ;
    }
    return null ;
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
  
  
  public Composite portrait(BaseUI UI) {
    return new Composite(UI, species.portrait) ;
  }
  
  
  public String helpInfo() {
    return species.info ;
  }
  
  
  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    
    final int maxHealth = (int) health.maxHealth() ;

    final int hunger = (int) ((1 - health.energyLevel()) * maxHealth) ;
    d.append("\nAge: "+health.agingDesc()) ;
    d.append("\nHunger: "+hunger+"/"+maxHealth) ;
    d.append("\nStress:  "+(int) (health.stressLevel()  * maxHealth)) ;
    d.append("\nFatigue: "+(int) (health.fatigueLevel() * maxHealth)) ;
    d.append("\nInjury:  "+(int) (health.injuryLevel()  * maxHealth)) ;
    
    d.append("\nCrowding: "+crowding) ;
    d.append("\n\nCurrently: ") ;
    if (currentAction() != null) {
      currentAction().describeBehaviour(d) ;
    }
    else if (health.isDead()) d.append("Dead") ;
    else d.append("Resting") ;
    
    d.append("\n\n") ;
    d.append(species.info) ;
  }
}



