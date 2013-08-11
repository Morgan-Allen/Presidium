


package src.util ;
import java.io.* ;



//
//  Since I seem to be indexing things all over the place, a utility class for
//  the purpose seems in order.
public class Index {
  
  
  
  final String indexID ;
  final Class declares ;
  private Batch <Member> members = new Batch <Member> () ;
  private Member arrayM[] ;
  
  
  
  public Index(Class declares, String indexID) {
    this.declares = declares ;
    this.indexID = indexID ;
  }
  
  
  public void saveMember(Member m, DataOutputStream out) throws Exception {
    members() ;
    out.writeInt(m.indexID) ;
  }
  
  
  public Member loadMember(DataInputStream in) throws Exception {
    return members()[in.readInt()] ;
  }
  
  
  public Member[] members() {
    if (arrayM != null) return arrayM ;
    arrayM = (Member[]) members.toArray(Member.class) ;
    members = null ;
    return arrayM ;
  }
  
  
  
  /**  Intended for subclassing by external clients.
    */
  public static class Member {
    
    final public int indexID ;
    final public Index index ;
    
    
    protected Member(Index index) {
      if (index.arrayM != null) I.complain("CANNOT ADD MEMBERS AFTER INIT!") ;
      this.index = index ;
      this.indexID = index.members.size() ;
      index.members.add(this) ;
    }
  }
}



