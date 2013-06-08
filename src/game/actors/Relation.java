


package src.game.actors ;
import src.util.* ;



public class Relation {
  
  
  /**  Fields, constructors and identity functions-
    */
  final public static float
    MIN_ATT = -100,
    MAX_ATT =  100,
    ATT_DIE =  10 ;
  
  final public Accountable object, subject ;
  final private int hash ;
  
  private float attitude ;
  
  
  
  Relation(Accountable object, Accountable subject) {
    this(object, subject, 0) ;
  }
  
  Relation(Accountable object, Accountable subject, float init) {
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
  
  
  /**  Accessing and modifying the content of the relationship-
    */
  public float attitude() {
    return attitude ;
  }
  
  
  public void initRelation(float init) {
    attitude = init ;
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
}














