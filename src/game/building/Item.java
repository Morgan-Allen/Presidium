/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.common.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;



/**  More representative of the asbtract 'listing' of an item than a specific
  *  concrete object.
  *  
  *  Setting quality or amount to -1 will allow for matches with items of any
  *  quality and amount.
  */
public class Item implements VenueConstants {
  
  
  /**  Type definition.
    */
  public static class Type implements Session.Saveable {
    
    
    private static int nextID = 0 ;
    private static Batch allTypes = new Batch(), soFar = new Batch() ;
    
    final static String ITEM_PATH = "media/Items/" ;
    final static String DEFAULT_PIC = ITEM_PATH+"crate.gif" ;
    
    
    static Type[] typesSoFar() {
      Type t[] = (Type[]) soFar.toArray(Type.class) ;
      soFar.clear() ;
      return t ;
    }
    
    static Type[] allTypes() {
      return (Type[]) allTypes.toArray(Type.class) ;
    }
    
    
    final public int form ;
    final public String name ;
    final public int typeID = nextID++ ;
    
    final public int basePrice ;
    final public Texture pic ;
    final public Model model ;
    
    
    protected Type(Class typeClass, int form, String name, int basePrice) {
      this.form = form ;
      this.name = name ;
      this.basePrice = basePrice ;
      final String imagePath = ITEM_PATH+name+".gif" ;
      if (new java.io.File(imagePath).exists()) {
        this.pic = Texture.loadTexture(imagePath) ;
        this.model = ImageModel.asIsometricModel(
          typeClass, imagePath, 0.5f, 0.5f
        ) ;
      }
      else {
        this.pic = Texture.loadTexture(DEFAULT_PIC) ;
        this.model = ImageModel.asIsometricModel(
          typeClass, DEFAULT_PIC, 0.5f, 0.5f
        ) ;
      }
      soFar.add(this) ;
      allTypes.add(this) ;
    }
    
    public static Type loadConstant(Session s) throws Exception {
      return ALL_ITEM_TYPES[s.loadInt()] ;
    }
    
    public void saveState(Session s) throws Exception {
      s.saveInt(typeID) ;
    }
    
    public String toString() { return name ; }
  }
  
  
  
  /**  Field definitions, standard constructors and save/load functionality-
    */
  final public static float ANY = Float.NEGATIVE_INFINITY ;
  final public static int MAX_QUALITY = 5 ;
  
  
  final public Type type ;
  final public float amount ;
  final public Session.Saveable refers ;
  
  
  public Item(Type type, float amount, Session.Saveable refers) {
    this.type = type ;
    this.amount = amount ;
    this.refers = refers ;
  }
  
  public static Item loadFrom(Session s) throws Exception {
    final int typeID = s.loadInt() ;
    if (typeID == -1) return null ;
    return new Item(ALL_ITEM_TYPES[typeID], s.loadFloat(), s.loadObject()) ;
  }
  
  public static void saveTo(Session s, Item item) throws Exception {
    if (item == null) { s.saveInt(-1) ; return ; }
    s.saveInt(item.type.typeID) ;
    s.saveFloat(item.amount) ;
    s.saveObject(item.refers) ;
  }
  
  public Item(Item item) {
    this(item.type, item.amount, item.refers) ;
  }

  public Item(Item item, float amount) {
    this(item.type, amount, item.refers) ;
  }
  
  public Item(Type type) {
    this(type, 1) ;
  }
  
  public Item(Type type, float amount) {
    this(type, amount > 0 ? amount : ANY, null) ;
  }
  
  
  public boolean equals(Object o) {
    final Item i = (Item) o ;
    return i.type == type && i.refers == refers ;
  }
  
  
  public int hashCode() {
    return type.typeID * 13 + (refers == null ? 0 : (refers.hashCode() % 13)) ;
  }
  
  
  
  /**  Matching/equality functions-
    */
  public boolean matchKind(Item item) {
    if (this.type != item.type) return false ;
    if (this.refers != null) {
      if (item.refers == null) return false ;
      if (! this.refers.equals(item.refers)) return false ;
    }
    return true ;
  }
  
  public boolean matches(Item item) {
    if (item == null) return false ;
    if (amount != ANY && item.amount < this.amount) return false ;
    return matchKind(item) ;
  }
  
  public boolean isMatch() {
    return amount == ANY ;
  }
  
  
  /**  Rendering/interface functions-
    */
  public String toString() {
    return ((int) amount)+" "+type ;
  }
}






