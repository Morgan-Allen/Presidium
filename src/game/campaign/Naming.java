


package src.game.campaign ;
import src.game.actors.* ;
import src.util.* ;




public class Naming implements ActorConstants {
  
  
  
  /**  Here is functionality used to generate first and last names for a given
    *  actor-
    *  TODO:  Agglomerate into a single function that returns an array of
    *  Strings (first, middle, last name, official title, nickname, etc.)
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
  
  
  
  public static String[] namesFor(Citizen actor) {
    final Batch <String> names = new Batch <String> () ;
    final int standing = actor.career.birth().standing ;
    final boolean female = actor.traits.level(GENDER) > 0 ;
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
        names.add("of "+actor.career.homeworld()) ;
      } break ;
    }
    return names.toArray(String.class) ;
  }
}

  /*
  private static int standingOf(ActorProfile actor) {
    final Vocation birth = actor.birth() ;
    if (birth == null) return SLAVE_CLASS ;
    if (birth == HIGHBORN) return RULER_CLASS ;
    else return Math.min(UPPER_CLASS, (Integer) birth.standing) ;
  }
  
  private static String pickQuality(ActorProfile actor) {
    return ((Trait) Rand.pickFrom(actor.allTraits())).name ;
  }
  
  public static String firstNameFor(ActorProfile actor) {
    final int standing = standingOf(actor) ;
    final String[] names = actor.hasTrait(FEMALE) ?
       ALL_FEMALE_NAMES[standing] : ALL_MALE_NAMES[standing] ;
    return (String) Rand.pickFrom(names) ;
  }

  public static String lastNameFor(ActorProfile actor) {
    final int standing = standingOf(actor) ;
    final boolean female = actor.hasTrait(FEMALE) ;
    switch (standing) {
      case (SLAVE_CLASS) :
      case (LOWER_CLASS) :
        return "" ;
      case (UPPER_CLASS) :
        return " "+Rand.pickFrom(CITIZEN_LAST_NAMES) ;
      case (RULER_CLASS) :
        System birthPlace = null ;
        for (Vocation v : actor.career()) if (v.type == Vocation.Type.SYSTEM) {
          birthPlace = (System) v ; break ;
        }
        return " of "+birthPlace.rulers.name ;
    }
    return null ;
  }
  //*/





