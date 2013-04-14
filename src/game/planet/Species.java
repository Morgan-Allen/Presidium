/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.actors.*;
import src.graphics.common.* ;



public class Species extends Trait {
  
  
  /*
  final public static Species
    //
    //  Humanoids first.
    PRIMITIVE   = new Species("Primitive"),
    MUTANT      = new Species("Mutant"),
    INFECTED    = new Species("Infected"),
    
    CHANGELING  = new Species("Changeling"),
    KRECH       = new Species("Krech"),
    JOVIAN      = new Species("Jovian"),
    //
    //  Insectiles-
    GIANT_ROACH = new Species("Giant Roach"),
    ROACHMAN    = new Species("Roachman"),
    TERMAGANT   = new Species("Termagant"),
    //
    //  Browsers and Predators-
    QUD         = new Species("Qud"), //X
    TRIGOX      = new Species("Trigox"),
    HAREEN      = new Species("Hareen"), //X
    DRIVODIL    = new Species("Drivodil"),
    GIGANS      = new Species("Gigans"), //Y
    LICTOVORE   = new Species("Lictovore"), //X
    //
    //  Artilects-
    DRONE       = new Species("Drone"), //Y
    TRIPOD      = new Species("Tripod"), //X
    CRANIAL     = new Species("Cranial"),
    //
    //  Silicates-
    REM_LEECH   = new Species("Rem Leech"),
    MERCURIAL   = new Species("Mercurial"),
    SERAPH      = new Species("Seraph")
  ;
  //*/
  
  
  String name ;
  Model model ;
  
  
  public Species(String name) {
    super(CATEGORIC, name, null) ;
  }
  
  
}


