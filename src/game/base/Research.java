/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Research extends Plan implements Economy {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  final static int
    BASE_LOOKUPS = 10,
    MAX_LOOKUPS = 100 ;
  
  private static boolean verbose = false ;
  
  final Archives archive ;
  private Skill topic ;
  private int numLookups = 0 ;
  
  
  public Research(
    Actor actor, Archives grounds
  ) {
    super(actor, grounds) ;
    this.archive = grounds ;
  }
  
  
  public Research(Session s) throws Exception {
    super(s) ;
    archive = (Archives) s.loadObject() ;
    topic = (Skill) s.loadObject() ;
    numLookups = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(archive) ;
    s.saveObject(topic) ;
    s.saveInt(numLookups) ;
  }
  
  
  
  /**  Priority and target evaluation-
    */
  public float priorityFor(Actor actor) {
    float impetus = CASUAL ;
    impetus *= actor.traits.scaleLevel(INQUISITIVE) ;
    //
    //  TODO:  Scale by importance of skill to actor...
    return Visit.clamp(impetus, 0, ROUTINE) ;
  }
  
  
  private static Skill pickResearchTopic(Actor actor, Archives archives) {
    final World world = actor.world() ;
    final Background v = actor.vocation() ;
    
    //
    //  Pick skills relevant to the actor's current job (or one they aspire to).
    //  TODO:  Have the actor's core AI make this selection at infrequent
    //  intervals, more thoroughly.  That's their 'ambition'.
    
    Background ambition = v ;
    float bestRating = Career.ratePromotion(v, actor) * Rand.num() * 2 ;
    for (Background b : Background.ALL_BACKGROUNDS) {
      if (b.guild == Background.NOT_A_GUILD) continue ;
      if (b.guild != v.guild && Rand.index(5) != 0) continue ;
      //
      //  Improve the rating if the career is available nearby:
      float rating = Career.ratePromotion(b, actor) ;
      if (world.presences.randomMatchNear(b, archives, -1) != null) {
        rating *= 2 ;
      }
      if (rating > bestRating) { bestRating = rating ; ambition = b ; }
    }
    if (ambition == null) return null ;
    
    //
    //  Then, pick out a skill relevant to the occupation-
    Skill topic = null ;
    bestRating = 0 ;
    for (Skill s : COGNITIVE_SKILLS) {
      final float
        bonus = archives.researchBonus(s),
        levelGap = ambition.skillLevel(s) - actor.traits.traitLevel(s),
        rating = (levelGap + 10) * (2 + bonus) * Rand.avgNums(2) ;
      if (rating > bestRating) { bestRating = rating ; topic = s ; }
    }
    return topic ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (numLookups > MAX_LOOKUPS) return null ;
    if (topic == null) topic = pickResearchTopic(actor, archive) ;
    if (topic == null) return null ;
    
    final float persist = actor.traits.scaleLevel(STUBBORN) ;
    if (numLookups > (BASE_LOOKUPS * persist)) {
      if (Rand.index(BASE_LOOKUPS) == 0) return null ;
    }
    
    final Action research = new Action(
      actor, archive,
      this, "actionResearch",
      Action.LOOK, "Researching "+topic
    ) ;
    return research ;
  }
  
  
  public boolean actionResearch(Actor actor, Archives archive) {
    if (topic == null) return false ;
    final ActorTraits AT = actor.traits ;
    float DC = (MODERATE_DC + AT.traitLevel(topic)) / 2f ;
    float XP = 1 ;
    if (
      AT.test(ACCOUNTING , ROUTINE_DC, 0.5f) &&
      AT.test(INSCRIPTION, SIMPLE_DC , 0.5f)
    ) XP++ ;
    XP += archive.researchBonus(topic) ;
    AT.test(topic, DC, XP) ;
    numLookups++ ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! describedByStep(d)) d.append("Researching "+topic) ;
    d.append(" at ") ;
    d.append(archive) ;
  }
}





