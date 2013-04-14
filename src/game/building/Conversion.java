/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;




public class Conversion implements Session.Saveable, VenueConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public Item raw[], out[] ;
  final public Skill skills[] ;
  final public float skillDCs[] ;
  //  Class venueClass?
  
  
  public Conversion(Session s) throws Exception {
    s.cacheInstance(this) ;
    raw = new Item[s.loadInt()] ;
    for (int i = 0 ; i < raw.length ; i++) raw[i] = Item.loadFrom(s) ;
    out = new Item[s.loadInt()] ;
    for (int i = 0 ; i < out.length ; i++) out[i] = Item.loadFrom(s) ;
    skills = new Skill[s.loadInt()] ;
    skillDCs = new float[skills.length] ;
    for (int i = 0 ; i < skills.length ; i++) {
      skills[i] = (Skill) ActorConstants.ALL_TRAIT_TYPES[s.loadInt()] ;
      skillDCs[i] = s.loadFloat() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(raw.length) ;
    for (Item i : raw) Item.saveTo(s, i) ;
    s.saveInt(out.length) ;
    for (Item i : out) Item.saveTo(s, i) ;
    s.saveInt(skills.length) ;
    for (int i = 0 ; i < skills.length ; i++) {
      s.saveInt(skills[i].traitID) ;
      s.saveFloat(skillDCs[i]) ;
    }
  }
  

  public Conversion(Object... args) {
    //
    //  Initially, we record raw materials first, and assume a default
    //  quantity of 1 each (this is also the default used for skill DCs.)
    //allConversions.add(this) ;
    float num = 1 ;
    boolean recRaw = true ;
    //  Set up temporary storage variables.
    Class v = null ;
    Batch rawB = new Batch(), outB = new Batch(), skillB = new Batch() ;
    Batch rawN = new Batch(), outN = new Batch(), skillN = new Batch() ;
    //
    //  Iterate over all arguments-
    for (Object o : args) {
      if (o instanceof Integer) num = (Integer) o ;
      else if (o instanceof Float) num = (Float) o ;
      else if (o instanceof Class) v = (Class) o ;
      else if (o instanceof Skill) { skillB.add(o) ; skillN.add(num) ; }
      else if (o == TO) recRaw = false ;
      else if (o instanceof Item.Type) {
        if (recRaw) { rawB.add(o) ; rawN.add(num) ; }
        else        { outB.add(o) ; outN.add(num) ; }
      }
    }
    //
    //  Then assign the final tallies-
    int i ;
    raw = new Item[rawB.size()] ;
    for (i = 0 ; i < rawB.size() ; i++) raw[i] = new Item(
      (Item.Type) rawB.atIndex(i), (Float) rawN.atIndex(i)
    ) ;
    out = new Item[outB.size()] ;
    for (i = 0 ; i < outB.size() ; i++) out[i] = new Item(
      (Item.Type) outB.atIndex(i), (Float) outN.atIndex(i)
    ) ;
    skills = (Skill[]) skillB.toArray(Skill.class) ;
    skillDCs = Visit.fromFloats(skillN.toArray()) ;
  }
  
  
  static Conversion[] parse(Object args[][]) {
    Conversion c[] = new Conversion[args.length] ;
    for (int i = c.length ; i-- > 0 ;) c[i] = new Conversion(args[i]) ;
    return c ;
  }
}



