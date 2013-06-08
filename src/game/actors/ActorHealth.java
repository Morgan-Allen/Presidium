/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
//import src.game.planet.Planet;
import src.util.* ;



public class ActorHealth implements ActorConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    MOVE_WALK  = 0,
    MOVE_RUN   = 1,
    MOVE_SNEAK = 2 ;
  
  final static float
    DEFAULT_PRIME    = 25,
    DEFAULT_LIFESPAN = 60,
    LIFE_EXTENDS     = 0.1f,

    DEFAULT_HEALTH = 10,
    MAX_INJURY  = 1.0f,
    MAX_FATIGUE = 1.5f,
    MAX_STRESS  = 0.5f,
    
    DEFAULT_SIGHT_RANGE = 8 ;
  
  
  final Actor actor ;
  
  private float
    birthDate,
    lifespan ;
  
  private float
    maxHealth = DEFAULT_HEALTH,
    injury,  //Add bleeding.
    fatigue, //Add sleep.
    stress ; //Add life-satisfaction.
  private float
    energy,
    nutrition,
    bulk ;
  private boolean
    isDead = false ;
  
  private int
    moveType = MOVE_WALK ;
  private float
    baseSpeed = 1,
    sightRange = 1 ;
  
  
  
  ActorHealth(Actor actor) {
    this.actor = actor ;
    //  These are defaults I'm using for now.  Will be more individualised
    //  later.  TODO:  THAT
    birthDate = 0 - DEFAULT_PRIME ;
    lifespan = DEFAULT_LIFESPAN * (1 + Rand.range(0, LIFE_EXTENDS)) ;
  }
  
  
  void loadState(Session s) throws Exception {
    birthDate = s.loadFloat() ;
    lifespan = s.loadFloat() ;
    
    injury  = s.loadFloat() ;
    fatigue = s.loadFloat() ;
    stress  = s.loadFloat() ;
    
    moveType = s.loadInt() ;
    baseSpeed = s.loadFloat() ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveFloat(birthDate) ;
    s.saveFloat(lifespan) ;
    
    s.saveFloat(injury ) ;
    s.saveFloat(fatigue) ;
    s.saveFloat(stress ) ;
    
    s.saveInt(moveType) ;
    s.saveFloat(baseSpeed) ;
  }
  
  
  /**  Methods related to growth, reproduction, aging and death.
    */
  public void takeSustenance(float amount, float quality) {
    final float MAX_ENERGY = 10, MAX_INTAKE = 1 ;
    amount = Visit.clamp(amount, 0, MAX_INTAKE) ;
    amount = Visit.clamp(amount, 0, MAX_ENERGY - energy) ;
    nutrition = (nutrition * (energy / MAX_ENERGY)) + (quality * amount) ;
    energy += amount ;
  }
  
  
  public int agingStage() {
    return 1 ;
    /*
    final float time = actor.world().currentTime() / World.DEFAULT_YEAR_LENGTH ;
    final float age = (time - birthDate) / lifespan ;
    return (int) (age * 4) ;
    //*/
  }
  
  
  
  /**  Methods related to sensing and motion-
    */
  public float moveRate() {
    float rate = baseSpeed ;
    //  You also have to account for the effects of fatigue and encumbrance...
    switch (moveType) {
      case (MOVE_SNEAK) : rate *= 0.50f ; break ;
      case (MOVE_WALK ) : rate *= 1.25f ; break ;
      case (MOVE_RUN  ) : rate *= 2.00f ; break ;
    }
    final int pathType = actor.origin().pathType() ;
    switch (pathType) {
      case (Tile.PATH_HINDERS) : rate *= 0.8f ; break ;
      case (Tile.PATH_CLEAR  ) : rate *= 1.0f ; break ;
      case (Tile.PATH_ROAD   ) : rate *= 1.2f ; break ;
    }
    return rate ;
  }
  
  
  public int moveType() { return moveType ; }
  
  
  public int sightRange() {
    final float range = 1 + (actor.traits.level(SURVEILLANCE) / 20f) ;
    return (int) (sightRange * range * DEFAULT_SIGHT_RANGE) ;
  }
  
  
  
  /**  Modifying and querying stress factors-
    */
  public void takeInjury(float taken) {
    injury += taken ;
    final float max = maxHealth * MAX_INJURY ;
    if (injury > max) injury = max ;
  }
  
  
  public void liftInjury(float lifted) {
    injury -= lifted ;
    if (injury < 0) injury = 0 ;
  }
  
  
  public float injury() {
    return injury ;
  }
  
  
  public void takeFatigue(float taken) {
    fatigue += taken ;
    final float max = maxHealth * MAX_FATIGUE ;
    if (fatigue > max) fatigue = max ;
  }
  
  
  public void liftFatigue(float lifted) {
    fatigue -= lifted ;
    if (fatigue < 0) fatigue = 0 ;
  }
  
  
  public float fatigue() {
    return fatigue ;
  }
  
  
  public void takeStress(float taken) {
    stress += taken ;
    final float max = maxHealth * MAX_STRESS ;
    if (stress > max) stress = max ;
  }
  
  
  public void liftStress(float lifted) {
    stress -= lifted ;
    if (stress < 0) stress = 0 ;
  }
  
  
  public float stress() {
    return stress ;
  }
  
  
  public boolean conscious() {
    return skillPenalty() < 1 ;
  }
  
  
  public float skillPenalty() {
    float sum = Visit.clamp((stress + fatigue + injury) / maxHealth, 0, 1) ;
    return sum * sum ;
  }
  
  
  void updateHealth(int numUpdates) {
    maxHealth = DEFAULT_HEALTH +
      (actor.traits.level(VIGOUR) / 2f) +
      (actor.traits.level(BRAWN ) / 2f) ;
    //
    //  Decrease energy.  If you have some left, alleviate injury, fatigue and
    //  stress.
  }
  
  
  /*
  void updateOnGrowth() {
    //
    //  Check for disease.  Try healing injury.
    //  If you have enough food, convert some of it into growth.
    //  If you've reached maximum size, convert growth into offspring (for
    //  animals.)
    //  ...Also, check for aging.
  }
  //*/
}









