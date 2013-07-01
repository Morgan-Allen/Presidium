


package src.game.social ;
import src.game.common.* ;


/*
 *  Reward types should include (in practice this should be used in foreign
 *  negotiations as well)-
 *  
 *  Cash Payment or Goods
 *  Personnel or Captives
 *  Support for Legislation
 *  Joining a Mission (Strike/Recover/Secure, Recon/Intel, Diplomacy)
 *  
 *  Permission to Marry
 *  Promotion and Demotion
 *  Pardon or Arrest
 *  Truce, Allegiance or Fealty
 */

public class Pledge implements Session.Saveable {
  
  
  public static enum Type {
    CASH,   //this.
    GOODS,
    PERSONNEL,
    LEGISLATION,  //this.
    
    JOIN_MISSION,
    
    ALLOW_MARRIAGE,  //this.
    ANNUL_MARRIAGE,
    PROMOTION,  //this.
    DEMOTION,
    PARDON,  //this.
    ARREST,
    VENDETTA,
    TRUCE,
    ALLIANCE,
    FEALTY
  }
  
  
  final Type type ;
  final float amount ;  //  What about term/duration?
  final Session.Saveable refers ;
  final Accountable madeTo ;
  
  
  public Pledge(Type type, float amount, Accountable madeTo) {
    this.type = type ;
    this.amount = amount ;
    this.refers = null ;
    this.madeTo = madeTo ;
  }
  
  
  public Pledge(Session s) throws Exception {
    s.cacheInstance(this) ;
    this.type = Type.values()[s.loadInt()] ;
    this.amount = s.loadFloat() ;
    this.refers = s.loadObject() ;
    this.madeTo = (Accountable) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(type.ordinal()) ;
    s.saveFloat(amount) ;
    s.saveObject(refers) ;
    s.saveObject((Session.Saveable) madeTo) ;
  }
}




