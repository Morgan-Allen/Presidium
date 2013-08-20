


package src.game.actors ;
import src.game.base.* ;
import src.game.social.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.util.* ;




public class Wording implements ActorConstants {
  
  
  
  /**  These methods generate names for new actors.
    */
  final static String
    //
    //  Natives only have first names, but might use son/daughter of X as a
    //  title, or a conspicuous trait.
    NATIVE_MN[] = {
      "Duor", "Huno", "Umun", "Tunto", "Parab", "Xsumo", "Zhaka", "Hoka"
    },
    NATIVE_FN[] = {
      "Khasi", "Mari", "Tesza", "Borab", "Haela", "Kaeli", "Hlara", "Ote"
    },
    //
    //  Pyons have first and second names as standard.
    //  TODO:  SEPARATE THESE FROM THE CITIZEN CLASS
    PYON_MN[] = {
      "Haber", "Danyl", "Jeme", "Marec", "Hoeb", "Ombar", "Tober", "Alav",
      "Dann", "Gereg", "Sunn", "Terev", "Olvar", "Menif", "Halan", "Yohn"
    },
    PYON_FN[] = {
      "Besa", "Linn", "Mina", "Abi", "Nana", "Dova", "Saba", "Keli", "Aryl",
      "Vina", "Nena", "Lanu", "Mai", "Neiv", "Mona", "Ambi", "Kayt", "Tesa",
    },
    //
    //  Citizens have first and second names as standard.
    CITIZEN_MN[] = PYON_MN,
    CITIZEN_FN[] = PYON_FN,
    CITIZEN_LN[] = {
      "Secundus", "Primus", "Tertius", "Vasov", "Olvaw", "Mallo", "Palev",
      "Obar", "Tiev", "Hanem", "Tsolo"
    },
    //
    //  Highborn always have family/house names, depending on their planet of
    //  origin, and may have additional titles.
    HIGHBORN_MN[] = {
      "Caliban", "Vladmar", "Ledo", "Cado", "Alexander", "Xerxes", "Poul",
      "Abbas"
    },
    HIGHBORN_FN[] = {
      "Meina", "Mnestra", "Aria", "Belise", "Ylande", "Vana", "Portia", "Vi",
      "Lysandre"
    },
    HIGHBORN_HN[] = {
      "Rigel", "Ursa", "Alyph", "Rana", "Maia", "Fomalhaut", "Aldebaran",
      "Regulus", "Suhail", "Antares", "Paleides", "Algol", "Orion",
      "Deneb", "Ares",
    } ;
  
  
  public static String[] namesFor(Human actor) {
    final Batch <String> names = new Batch <String> () ;
    final int standing = actor.career().birth().standing ;
    final boolean female = actor.traits.hasTrait(GENDER, "Female") ;
    switch (standing) {
      case (Vocation.SLAVE_CLASS) : {
        final String pick[] = female ? NATIVE_FN : NATIVE_MN ;
        if (Rand.yes()) {
          final String parents[] = female ? NATIVE_FN : NATIVE_MN ;
          final String title = female ? "daughter of " : "son of " ;
          names.add(title+Rand.pickFrom(parents)) ;
        }
        else {
          final Batch <Trait> traits = Rand.yes() ?
            actor.traits.physique() : actor.traits.personality() ;
          final Trait desc = (Trait) Rand.pickFrom(traits) ;
          names.add("the "+actor.traits.levelDesc(desc)) ;
        }
        names.add((String) Rand.pickFrom(pick)) ;
      } break ;
      case (Vocation.LOWER_CLASS) : {
        final String pick[] = female ? PYON_FN : PYON_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
        names.add((String) Rand.pickFrom(CITIZEN_LN)) ;
      } break ;
      case (Vocation.UPPER_CLASS) : {
        final String pick[] = female ? CITIZEN_FN : CITIZEN_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
        names.add((String) Rand.pickFrom(CITIZEN_LN)) ;
      } break ;
      case (Vocation.RULER_CLASS) : {
        final String pick[] = female ? HIGHBORN_FN : HIGHBORN_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
        names.add((String) Rand.pickFrom(HIGHBORN_HN)) ;
        names.add("of "+actor.career().homeworld()) ;
      } break ;
    }
    return names.toArray(String.class) ;
  }
  
  
  
  /**  These methods return actors' voiceline responses to a variety of
    *  situations.
    */
  final public static String
    
    VOICE_RECRUIT  = "Ready for action.",
    VOICE_DISMISS  = "I quit!",
    
    VOICE_COMBAT   = "Watch out!",
    VOICE_EXPLORE  = "To boldy go...",
    VOICE_ASSIST   = "Hang in there!",
    VOICE_RETREAT  = "Fall back!",
    VOICE_REPAIR   = "I can fix that up.",
    
    VOICE_GREETING = "Hello!",
    VOICE_GOODBYE  = "Goodbye...",
    VOICE_RELAX    = "Time to relax.",
    VOICE_LEVEL    = "I'm getting better at this!",
    VOICE_ITEM     = "A fine piece of kit!",
    
    VOICE_AGREE    = "Sure.",
    VOICE_MAYBE    = "...Maybe.",
    VOICE_REFUSE   = "No way!" ;
  
  
  public static String response(Actor actor, Pledge pledgeMade) {
    return null ;
  }
  
  
  //
  //  Base this off voice-keys instead.
  public static String phraseFor(Actor actor, Behaviour begun) {
    //
    //  This should be customised by vocation.
    
    if (begun instanceof Combat) {
      
    }
    
    return null ;
  }
}
















