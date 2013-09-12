/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.game.common.Session.Saveable ;
import src.game.planet.Terrain;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;

import java.lang.reflect.* ;


public abstract class Plan implements Saveable, Behaviour {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Saveable signature[] ;
  final int hash ;
  protected Actor actor ;
  protected Behaviour nextStep = null ;
  
  public float priorityMod = 0 ;
  
  
  
  protected Plan(Actor actor, Saveable... signature) {
    this(actor, ROUTINE, null, signature) ;
  }
  
  
  protected Plan(
    Actor actor, float priority, String desc,
    Saveable... signature
  ) {
    this.actor = actor ;
    this.signature = signature ;
    this.hash = Table.hashFor((Object[]) signature) ;
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this) ;
    actor = (Actor) s.loadObject() ;
    final int numS = s.loadInt() ;
    signature = new Saveable[numS] ;
    for (int i = 0 ; i < numS ; i++) signature[i] = s.loadObject() ;
    this.hash = Table.hashFor((Object[]) signature) ;
    this.priorityMod = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor) ;
    s.saveInt(signature.length) ;
    for (Saveable o : signature) s.saveObject(o) ;
    s.saveFloat(priorityMod) ;
  }
  
  
  public boolean matchesPlan(Plan p) {
    for (int i = 0 ; i < signature.length ; i++) {
      final Object s = signature[i], pS = p.signature[i] ;
      if (s == null && pS != null) return false ;
      if (! s.equals(pS)) return false ;
    }
    return true ;
  }
  
  public int planHash() {
    return hash ;
  }
  
  
  public boolean valid() {
    for (Saveable o : signature) if (o instanceof Target) {
      final Target t = (Target) o ;
      if (! t.inWorld()) return false ;
    }
    if (actor != null && ! actor.inWorld()) return false ;
    return true ;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public boolean monitor(Actor actor) {
    return false ;
  }
  

  public void abortBehaviour() {
    ///I.say("Aborting plan! "+this) ;
    actor.AI.cancelBehaviour(this) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (this.actor != actor) {
      this.actor = actor ;
      nextStep = null ;
    }
    if (! actor.AI.agenda.includes(this)) {
      if (valid()) return getNextStep() ;
      else return null ;
    }
    if (nextStep == null || nextStep.complete()) {
      if (valid()) nextStep = getNextStep() ;
      else { onceInvalid() ; nextStep = null ; }
    }
    return nextStep ;
  }
  
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep() ;
  
  
  public Behaviour nextStep() {
    return nextStepFor(actor) ;
  }
  
  
  public boolean complete() {
    return begun() && nextStep() == null ;
  }
  
  
  public boolean begun() {
    return actor != null ;
  }
  
  
  public Actor actor() {
    return actor ;
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  final private static float IL2 = 1 / (float) Math.log(2) ;
  
  
  public static float rangePenalty(Target a, Target b) {
    if (a == null || b == null) return 0 ;
    final float SS = World.DEFAULT_SECTOR_SIZE ;
    final float dist = Spacing.distance(a, b) * 1.0f / SS ;
    if (dist <= 1) return dist ;
    return IL2 * (float) Math.log(dist) ;
  }
  
  
  public static float competition(Class planClass, Target t, Actor actor) {
    float competition = 0 ;
    final World world = actor.world() ;
    for (Behaviour b : world.activities.targeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b ;
        if (plan.getClass() != planClass) continue ;
        if (plan.actor() == actor) continue ;
        competition += plan.successChance() ;
      }
    }
    return competition ;
  }
  
  
  public static int upgradeBonus(Target location, Object refers) {
    if (! (location instanceof Venue)) return 0 ;
    final Venue v = (Venue) location ;
    if (refers instanceof Upgrade) {
      return v.structure.upgradeLevel((Upgrade) refers) ;
    }
    else return v.structure.upgradeBonus(refers) ;
  }
  
  
  protected float successChance() {
    return 1 ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String toString() {
    final StringDescription desc = new StringDescription() ;
    describeBehaviour(desc) ;
    return desc.toString() ;
  }
  
  
  /**  Validation methods, intended to ensure that Plans can be stored
    *  compactly as memories-
    */
  private static Table <Class, Boolean> validations = new Table(100) ;
  
  
  private static boolean validatePlanClass(Class planClass) {
    final Boolean valid = validations.get(planClass) ;
    if (valid != null) return valid ;
    
    final String name = planClass.getSimpleName() ;
    boolean okay = true ;
    int dataSize = 0 ;
    
    for (Field field : planClass.getFields()) {
      final Class type = field.getType() ;
      if (type.isPrimitive()) dataSize += 4 ;
      else if (Saveable.class.isAssignableFrom(type)) dataSize += 4 ;
      else {
        I.complain(name+" contains non-saveable data: "+field.getName()) ;
        okay = false ;
      }
    }
    if (dataSize > 40) {
      I.complain(name+" has too many data fields.") ;
      okay = false ;
    }
    
    validations.put(planClass, okay) ;
    return okay ;
  }
}







