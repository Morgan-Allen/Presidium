/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;



public class GameSettings {
  
  
  public static boolean
    buildFree = false,
    hireFree  = false,
    noFog     = false,
    freePath  = false,
    frozen    = false ;
  
  /*
  public static void loadSettings(Session s) throws Exception {
    buildFree = s.loadBool() ;
    noFog = s.loadBool() ;
    freePath = s.loadBool() ;
    frozen = s.loadBool() ;
  }
  
  public static void saveSettings(Session s) throws Exception {
    s.saveBool(buildFree) ;
    s.saveBool(noFog) ;
    s.saveBool(freePath) ;
    s.saveBool(s.loadBool) ;
  }
  //*/
}
