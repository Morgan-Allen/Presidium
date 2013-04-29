/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import src.util.* ;
import org.lwjgl.opengl.* ;
import java.awt.Color ;



/**  Standard RGBA colour class with utility functions for conversion to other
  *  formats and setting non-standard qualities such as brightness and
  *  complement.
  */
public class Colour {
  
  
  final public static Colour
    HIDE        = new Colour(),
    WHITE       = new Colour().set(1, 1, 1, 1),
    NONE        = new Colour().set(0, 0, 0, 0),
    RED         = new Colour().set(1, 0, 0, 1),
    GREEN       = new Colour().set(0, 1, 0, 1),
    BLUE        = new Colour().set(0, 0, 1, 1),
    GREY        = new Colour().set(0.5f, 0.5f, 0.5f, 1),
    BLACK       = new Colour().set(0, 0, 0, 1),
    TRANSLUCENT = new Colour().set(1, 1, 1, 0.5f) ;
  
  final public static Colour
    TRANSPARENCIES[] = new Colour[100] ;
  static {
    for (int n = 100 ; n-- > 0 ;) {
      Colour t = TRANSPARENCIES[n] = new Colour() ;
      t.set(1, 1, 1, n / 100f) ;
    }
  }
  
  public static Colour transparency(float a) {
    return TRANSPARENCIES[Visit.clamp((int) (a * 100), 100)] ;
  }
	
	
	public float
	  r = 1,
	  g = 1,
	  b = 1,
	  a = 1 ;
  
	
	public Colour() {}
	
	public Colour(float r, float g, float b, float a) {
	  set(r, g, b, a) ;
	}	
	
  public Colour(float r, float g, float b) {
    set(r, g, b, 1) ;
  }

  /**  Binds this colour to the given GL context.  Convenience method.
     */
  public void bindColour() {
    GL11.glColor4f(r, g, b, a) ;
  }
	
	
	/**  Sets this colour to match the argument Colour values.
	  */
	public Colour set(Colour colour) {
		r = colour.r ;
		g = colour.g ;
		b = colour.b ;
		a = colour.a ;
    return this ;
	}
	
	
	/**  Sets this colour to match given RGBA component values.
	  */
	public Colour set(float rc, float gc, float bc, float ac) {
		r = rc ;
		g = gc ;
		b = bc ;
		a = ac ;
    return this ;
	}
	
	
	/**  Sets the Value (or brightness) of this colour to the desired value.  An
	  *  RGB colour's maximum component defines brightness.
	  */
	public Colour setValue(float v) {
		final float val = value() ;
		if (val > 0) {
			r *= v / val ;
			g *= v / val ;
			b *= v / val ;
		}
		else r = g = b = v ;
		return this ;
	}
	
	
	/**  Returns the Value (or Brightness) of this colour, (defined as the
	  *  maximum of RGB components.)
	  */
	public float value() {
	  return Math.max(r, Math.max(g, b)) ;
	}
	
	
	/**  Sets the argument colour to the complement of this Colour- opposite on
	  *  the HSV spectrum, 1 - value, and identical saturation.  (If the argument
	  *  is null, a new Colour is initialised.)
	  */
	public Colour complement(Colour result) {
		if (result == this) {
		  final Colour temp = new Colour() ;
			complement(temp) ;
			set(temp) ;
			return this ;
		}
	  else if (result == null) result = new Colour() ;
		result.r = g + b / 2 ;
		result.g = r + b / 2 ;
		result.b = r + g / 2 ;
		result.a = a ;
		result.setValue(1 - value()) ;
		return result ;
	}
	
	
	/**  Returns the Hue/Saturation/Value coordinates of this Colour encoded in a
	  *  3 value float array (which, if null, is initialised and then returned.)
	  */
	public float[] getHSV(float[] result) {
		if ((result == null) || (result.length != 3)) result = new float[3] ;
		Color.RGBtoHSB((int) (r * 255), (int) (g * 255), (int) (b * 255), result) ;
		return result ;
	}
	
	
  /**  Sets this RGBA colour to represent the given Hue/Saturation/Value (or
    *  Brightness) coordinates as a 3-valued float array: Alpha is not affected.
    */
  public Colour setHSV(float hsv[]) {
    int bytes = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) ;
    r = (((bytes >> 16) & 0xff) / 255f) ;
    g = (((bytes >> 8 ) & 0xff) / 255f) ;
    b = (((bytes >> 0 ) & 0xff) / 255f) ;
    a = 1 ;
    return this ;
  }
	
	
	/**  Converts this colour into byte format-
  	*/
	public void storeByteValue(byte puts[], int i) {
    puts[i + 0] = (byte) (r * 255) ;
    puts[i + 1] = (byte) (g * 255) ;
    puts[i + 2] = (byte) (b * 255) ;
    puts[i + 3] = (byte) (a * 255) ;
	}
	
	
	/**  Sets RGBA values from the given byte format-
    */
	public void setFromBytes(byte vals[], int i) {
	  r = (vals[i + 0] & 0xff) / 255f ;
    g = (vals[i + 1] & 0xff) / 255f ;
    b = (vals[i + 2] & 0xff) / 255f ;
    a = (vals[i + 3] & 0xff) / 255f ;
	}
	
	
	/**  String description of this colour-
 	 */
	public String toString() {
	  return "(Colour RGBA: "+r+" "+g+" "+b+" "+a+")" ;
	}
}






