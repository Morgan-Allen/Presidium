


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



/**  Used to represent the types of goods and services that venues can provide
  *  or produce.
  */
public class Service implements Session.Saveable {
  
  
  private static int nextID = 0 ;
  private static Batch allTypes = new Batch(), soFar = new Batch() ;
  
  final static String ITEM_PATH = "media/Items/" ;
  final static String DEFAULT_PIC = ITEM_PATH+"crate.gif" ;
  
  
  static Service[] typesSoFar() {
    Service t[] = (Service[]) soFar.toArray(Service.class) ;
    soFar.clear() ;
    return t ;
  }
  
  static Service[] allTypes() {
    return (Service[]) allTypes.toArray(Service.class) ;
  }
  
  
  final public int form ;
  final public String name ;
  final public int typeID = nextID++ ;
  
  final public int basePrice ;
  final public Texture pic ;
  final public Model model ;
  
  
  protected Service(Class typeClass, int form, String name, int basePrice) {
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
  
  
  public static Service loadConstant(Session s) throws Exception {
    return BuildConstants.ALL_ITEM_TYPES[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(typeID) ;
  }
  
  
  public Conversion materials() { return null ; }
  
  
  
  public String toString() { return name ; }
}









