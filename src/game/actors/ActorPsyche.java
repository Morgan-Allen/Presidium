


package src.game.actors ;
import src.game.common.* ;
import src.util.* ;



//
//  TODO:  Consider creating a dedicated subclass of this, just for Citizens?
//
//  Also TODO:  Consider moving the behaviour stack to this class?  Yeah.

public class ActorPsyche implements ActorConstants {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  final static int
    MAX_MEMORIES  = 100,
    MAX_RELATIONS = 100,
    MAX_VALUES    = 100 ;
  
  
  static class Memory {
    Class planClass ;
    Session.Saveable signature[] ;
    float timeBegun, timeEnded ;
  }
  
  
  final Actor actor ;
  final List <Memory> memories = new List <Memory> () ;
  final Table <Actor, Relation> relations = new Table <Actor, Relation> () ;
  
  
  ActorPsyche(Actor actor) {
    this.actor = actor ;
  }
  
  public void loadState(Session s) throws Exception {
  }
  
  public void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Methods related to relationships-
    */
  public float attraction(Actor other) {
    //
    //  For now, we assume that both need to be the same species-
    //  TODO:  Create/re-locate/use species traits.
    if (actor instanceof Citizen && other instanceof Citizen) ;
    else return 0 ;
    //
    //  TODO:  Create exceptions based on age and kinship modifiers.
    //
    //  First, we establish a few facts about each actor's sexual identity:
    float actorG = 0, otherG = 0 ;
    if (actor.traits.hasTrait(GENDER, "Male"  )) actorG = -1 ;
    if (actor.traits.hasTrait(GENDER, "Female")) actorG =  1 ;
    if (other.traits.hasTrait(GENDER, "Male"  )) otherG = -1 ;
    if (other.traits.hasTrait(GENDER, "Female")) otherG =  1 ;
    float attraction = other.traits.level(HANDSOME) * 3.33f ;
    attraction += otherG * other.traits.level(FEMININE) * 3.33f ;
    //
    //  Then compute attraction based on orientation-
    final String descO = actor.traits.levelDesc(ORIENTATION) ;
    float matchO = 0 ;
    if (descO.equals("Heterosexual")) {
      matchO = (actorG * otherG < 0) ? 1 : 0.33f ;
    }
    else if (descO.equals("Bisexual")) {
      matchO = 0.66f ;
    }
    else if (descO.equals("Homosexual")) {
      matchO = (actorG * otherG > 0) ? 1 : 0.33f ;
    }
    return attraction * matchO ;
  }
  
  
  public float relationTo(Actor other) {
    Relation r = relations.get(other) ;
    if (r == null) return 0 ;
    return r.attitude() ;
  }
  
  
  public void incRelation(Actor other, float inc) {
    Relation r = relations.get(other) ;
    if (r == null) relations.put(other, r = new Relation(actor, other)) ;
    r.incRelation(inc) ;
  }
  
  
  
  /**  Methods related to memories-
    */
  protected void planBegun(Behaviour planB) {
    if (! (planB instanceof Plan)) return ;
    final Plan plan = (Plan) planB ;
    //
    //  Create a memory of this plan...
    final Memory memory = new Memory() ;
    memory.planClass = plan.getClass() ;
    memory.signature = plan.signature ;
    memory.timeBegun = actor.world().currentTime() ;
    memories.addFirst(memory) ;
    if (memories.size() > MAX_MEMORIES) memories.removeLast() ;
  }
  
  
  protected void planEnded(Behaviour planB) {
    if (! (planB instanceof Plan)) return ;
    final Plan plan = (Plan) planB ;
    for (Memory memory : memories) {
      if (memory.equals(plan)) {
        memory.timeEnded = actor.world().currentTime() ;
        return ;
      }
    }
  }
  
  
  public float curiosity(Class planClass, Session.Saveable... assoc) {
    //
    //  Firstly, see if an existing memory/s match this one-
    Memory match = null ;
    for (Memory memory : memories) {
      if (memory.planClass != planClass) continue ;
      boolean matches = true ;
      for (int i = 0 ; i < assoc.length ; i++) {
        if (assoc[i] != memory.signature[i]) { matches = false ; break ; }
      }
      if (matches) { match = memory ; break ; }
    }
    //
    //  Then, calculate how curious about it the actor would be, based on how
    //  recently/often this event occured-
    
    //  More inquisitive actors have a higher initial attraction to novel
    //  stimuli, but take longer to recharge interest since the last event of
    //  this type.
    
    
    float curiosity = actor.traits.level(INQUISITIVE) ;
    if (match == null) {
      curiosity += 5 ;
    }
    else {
      final float timeGap = actor.world().currentTime() - match.timeEnded ;
      curiosity -= INQUISITIVE.maxVal ;
      curiosity += (timeGap * 2f / World.DEFAULT_DAY_LENGTH) ;
    }
    return Visit.clamp(curiosity / 10f, 0, 1) ;
  }
  
  
  public Class[] recentActivities() {
    final Class recent[] = new Class[memories.size()] ;
    int n = 0 ; for (Memory memory : memories) {
      recent[n++] = memory.planClass ;
    }
    return recent ;
  }
}









