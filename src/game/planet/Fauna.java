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
//  I need abstract methods for-
//    rateMigratePoint()
//    getRestPoint()
//    getMating()
//    getFeeding()
//    getDefence()
//    fightWith()


//
//  For some reason, actual population densities seem to be about 50% higher
//  than they should be after initialisation.  Probably has to do with average
//  distances between particles with brownian motion, or something similar.
//  Whatever the reason, trim it down after initialisation?


public abstract class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final public static int
    GROUND_SAMPLE_RANGE = 16,
    PEER_SAMPLE_RANGE   = 16,
    SAMPLE_AREA         = 16 * 16 * 4,
    BROWSER_DENSITY     = 12,
    PREDATOR_RATIO      = 12 ;
  
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
  protected ActorPsyche initPsyche() {
    final Fauna fauna = this ;
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
    /*
    //  TODO:  React to any other organisms nearby, including running/defence.
    if (psyche.dangerLevel() > 0) {
      final Behaviour defence = nextDefence(psyche.seen()) ;
      if (defence != null) return defence ;
    }
    //*/
    return null ;
  }
  
  
  protected abstract Behaviour nextFeeding() ;
  
  
  protected Tile randomTileInRange(float range) {
    final Tile o = origin() ;
    ///final float range = health.sightRange() ;
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
    final float range = health.sightRange() * 2 ;
    for (int numTries = 3 ; numTries-- > 0 ;) {
      final Tile free = Spacing.nearestOpenTile(
        randomTileInRange(range), this
      ) ;
      if (free == null) continue ;
      float rating = rateMigratePoint(free) ;
      final float dist = Spacing.distance(origin(), free) ;
      rating *= 2 / (1 + (dist / range)) ;
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
  
  
  protected float rateMigratePoint(Tile point) {
    float rating = point.habitat().moisture() / 10f ;
    final Fauna near = specimens(
      point, PEER_SAMPLE_RANGE, this.species, null, 1
    ).first() ;
    if (near != null) {
      rating -= 1 - (Spacing.distance(this, near) / PEER_SAMPLE_RANGE) ;
    }
    return rating ;
  }
  
  //  TODO:  Have an abstract 'rateWanderPoint' method?
  
  
  public boolean actionMigrate(Fauna actor, Tile location) {
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
  
  
  //
  //  TODO:  Go to your nest, if you have one.
  protected abstract Target findRestPoint() ;
  
  
  public boolean actionRest(Fauna actor, Target location) {
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
        if (psyche.couldSwitchTo(mating)) {
          psyche.assignBehaviour(mating) ;
        }
      }
    }
  }
  
  //  TODO:  Make this abstract?
  protected void fightWith(Fauna competes) {
  }
  
  
  public float guessCrowding(Tile point) {
    final int WS = point.world.size, range = (int) (PEER_SAMPLE_RANGE * (
      species.type == Species.Type.PREDATOR ? 1.5f : 0.75f
    )) ;
    final Box2D limit = new Box2D().set(point.x, point.y, 0, 0) ;
    limit.expandBy(range).cropBy(new Box2D().set(0, 0, WS, WS)) ;
    
    float numPeers = 0, numPrey = 0, numHunters = 0 ;
    for (Target t : point.world.mobilesMap.visitNear(point, -1, limit)) {
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
  
  
  
  
  
  protected  Batch <Fauna> specimens(
    Tile point, float range, Species species, Species.Type type, int maxNum
  ) {
    final Batch <Fauna> specimens = new Batch <Fauna> () ;
    int numTries = 0 ;
    for (Target t : point.world.mobilesMap.visitNear(point, -1, null)) {
      if (t == this) continue ;
      if (type != null || species != null) {
        if (! (t instanceof Fauna)) continue ;
        final Fauna f = (Fauna) t ;
        if (species != null && f.species != species) continue ;
        if (type != null && f.species.type != type) continue ;
      }
      final float dist = Spacing.distance(point, t) + 1 ;
      if (range > 0 && dist > range) break ;
      if (++numTries > maxNum + 10) break ;
      specimens.add((Fauna) t) ;
      if (specimens.size() >= maxNum) break ;
    }
    return specimens ;
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
    
    ///d.append("\nCrowding: "+crowding) ;
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






/*
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
/*
    if (shouldMate) {
      final Behaviour mating = nextMating() ;
      if (psyche.couldSwitchTo(mating)) psyche.assignBehaviour(mating) ;
    }
  }
  //*/
/*
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
  final Fauna nearest = nearestSpecimen(point, range, species, type) ;
  if (nearest == null) return 0 ;
  final float dist = Spacing.distance(point, nearest) + 1 ;
  float crowdingGuess = (range * range) / (dist * dist) ;
  return crowdingGuess ;
}
//*/

