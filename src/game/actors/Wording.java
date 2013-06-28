


package src.game.actors ;
import src.game.base.* ;
import src.game.social.* ;
import src.util.* ;




public class Wording implements ActorConstants {
  
  
  
  /**  Todo:  You need to follow the generation-descriptions a little more
    *  closely.
    *  These methods generate names for new actors.
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
    //  Pyons have first names, but their second name is based on their fief of
    //  origin, or on some trait that they possess.
    PYON_MN[] = {
      "Habryl", "Danyl", "Doga", "Marec", "Hoet", "Ombar", "Hober", "Alav",
      "Dann", "Tereg", "Mun", "Terev", "Olvar", "Menif", "Halan", "Yun"
    },
    PYON_FN[] = {
      "Besa", "Lin", "Minda", "Abi", "Nan", "Jeme", "Saba", "Gili", "Oryl",
      "Geviev", "Nien", "Lanu", "Mai", "Niceb", "Mona", "Ambir", "Kayt"
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
        names.add((String) Rand.pickFrom(pick)) ;
      } break ;
      case (Vocation.LOWER_CLASS) : {
        final String pick[] = female ? PYON_FN : PYON_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
      } break ;
      case (Vocation.UPPER_CLASS) : {
        final String pick[] = female ? CITIZEN_FN : CITIZEN_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
        names.add((String) Rand.pickFrom(CITIZEN_LN)) ;
      } break ;
      case (Vocation.RULER_CLASS) : {
        final String pick[] = female ? HIGHBORN_FN : HIGHBORN_MN ;
        names.add((String) Rand.pickFrom(pick)) ;
        names.add("of "+actor.career().homeworld()) ;
      } break ;
    }
    return names.toArray(String.class) ;
  }
  
  
  
  /**  These methods return actors' voiceline responses to a variety of
    *  situations-
    */
  
  public static String response(Actor actor, Pledge pledgeMade) {
    return null ;
  }
}










