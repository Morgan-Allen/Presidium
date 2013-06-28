


package src.game.social ;


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

public class Pledge {
  
  
  public static enum Type {
    CASH,
    GOODS,
    PERSONNEL,
    LEGISLATION,
    
    JOIN_MISSION,
    
    ALLOW_MARRIAGE,
    ANNUL_MARRIAGE,
    PROMOTION,
    DEMOTION,
    PARDON,
    ARREST,
    VENDETTA,
    TRUCE,
    ALLIANCE,
    FEALTY
  }
}






