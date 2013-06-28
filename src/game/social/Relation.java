/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.social ;
import src.game.common.* ;
import src.util.* ;



public class Relation {
  
  
  /**  Fields, constructors, save/load methods and identity functions-
    */
  final public static int
    TYPE_GENERIC = 0,
    TYPE_CHILD   = 1,
    TYPE_PARENT  = 2,
    TYPE_SPOUSE  = 3,
    TYPE_SIBLING = 4,
    TYPE_LORD    = 5,
    TYPE_VASSAL  = 6 ;
  
  final public static float
    MIN_ATT = -100,
    MAX_ATT =  100,
    ATT_DIE =  10 ;
  
  final static String DESCRIPTORS[] = {
    "Soulmate",
    "Close",
    "Friendly",
    "Cordial",
    "Ambivalent",
    "Tense",
    "Emnity",
    "Hostile",
    "Nemesis"
  } ;
  
  
  final public Accountable object, subject ;
  final private int hash ;
  
  private float attitude = 0 ;
  private int type = TYPE_GENERIC ;
  
  
  
  public Relation(Accountable object, Accountable subject) {
    this(object, subject, 0) ;
  }
  
  
  public Relation(Accountable object, Accountable subject, float init) {
    this.object = object ;
    this.subject = subject ;
    this.hash = Table.hashFor(object, subject) ;
    this.attitude = init ;
  }
  
  
  public boolean equals(Object o) {
    final Relation r = (Relation) o ;
    return r.object == object && r.subject == subject ;
  }
  
  
  public int hashCode() {
    return hash ;
  }
  
  
  public static Relation loadFrom(Session s) throws Exception {
    final Relation r = new Relation(
      (Accountable) s.loadObject(), (Accountable) s.loadObject()
    ) ;
    r.attitude = s.loadFloat() ;
    r.type = s.loadInt() ;
    return r ;
  }
  
  
  public static void saveTo(Session s, Relation r) throws Exception {
    s.saveObject((Session.Saveable) r.object ) ;
    s.saveObject((Session.Saveable) r.subject) ;
    s.saveFloat(r.attitude) ;
    s.saveInt(r.type) ;
  }
  
  
  
  /**  Accessing and modifying the content of the relationship-
    */
  public float attitude() {
    return attitude ;
  }
  
  
  public int type() {
    return type ;
  }
  
  
  public String descriptor() {
    final float level = (MAX_ATT - attitude) / (MAX_ATT - MIN_ATT) ;
    final int DL = DESCRIPTORS.length ;
    return DESCRIPTORS[Visit.clamp((int) (level * (DL + 1)), DL)] ;
  }
  
  
  public void initRelation(float attitude, int type) {
    this.attitude = attitude ;
    this.type = type ;
  }
  
  
  public void incRelation(float inc) {
    //
    //  Roll dice matching current relationship against magnitude of event.
    final int numDice = (int) (Math.abs(attitude / ATT_DIE) + 0.5f) ;
    int roll = 0 ;
    for (int n = numDice ; n-- > 0 ;) roll += Rand.yes() ? 1 : 0 ;
    final float diff = Math.abs(inc) - (roll * ATT_DIE) ;
    //
    //  Raise/lower by half the margin of failure.
    if (diff > 0) {
      attitude += (inc > 0) ? (diff / 2) : (diff / -2) ;
      attitude = Visit.clamp(attitude, MIN_ATT, MAX_ATT) ;
    }
  }
  
  
  public void setType(int type) {
    this.type = type ;
  }
}



