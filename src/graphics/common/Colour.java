/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import org.lwjgl.opengl.* ;

import src.util.Visit;



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
	
	
	private static float
	  max,
	  min,
	  gap,
	  hue,
	  sat,
	  val,
	  
	  dif,
	  mix,
	  right,
	  left ;
	private static int
	  sec ;
	private static Colour temp = new Colour() ;
	
	
	public float
	  r = 1,
	  g = 1,
	  b = 1,
	  a = 1 ;
	//red, green, blue, and alpha, respectively.
  
	
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
	
	
	/**  Sets this RGBA colour to represent the given Hue/Saturation/Value (or
	  *  Brightness) coordinates as a 3-valued float array: Alpha is not affected.
	  */
	public Colour setHSV(float hsv[]) {
		hue = hsv[0] * 6 ;
		sat = hsv[1] ;
		
		sec = (int)hue ;  //sector of colour spectrum.
		dif = hue - sec ;  //fractional part of spectrum position.
		mix = 1 - sat ;  //the degree to which the colour is 'impure.'
	  right = 1 - (sat * dif) ;  //components leaning towards either end
	  left = 1 - (sat * (1 - dif)) ;  //...of the spectrum.

		switch(sec) {
			case 0:
				r = val ;
				g = left ;
				b = mix ;
			break ;
			case 1:
				r = right ;
				g = val ;
				b = mix ;
			break ;
			case 2:
				r = mix ;
				g = val ;
				b = left ;
			break ;
			case 3:
				r = mix ;
				g = right ;
				b = val ;
			break ;
			case 4:
				r = left ;
				g = mix ;
				b = val ;
			break ;
			default:
				r = val ;
				g = mix ;
				b = right ;
			break ;
		}
		return setValue(hsv[2]) ;
	}
	
	
	/**  Sets the Value (or brightness) of this colour to the desired value.  An
	  *  RGB colour's maximum component defines brightness.
	  */
	public Colour setValue(float v) {
		val = value() ;
		if(val > 0) {
			val = v / val ;
			r *= val ;
			g *= val ;
			b *= val ;
		}
		else r = g = b = v ;
		return this ;
	}
	
	
	/**  Returns the Value (or Brightness) of this colour, (defined as the
	  *  maximum of RGB components.)
	  */
	public float value() {
		max = (r > g) ? r : g ;
		if(max < b) max = b ;
		return max ;
	}
	
	
	/**  Sets the argument colour to the complement of this Colour- opposite on
	  *  the HSV spectrum, 1 - value, and identical saturation.  (If the argument
	  *  is null, a new Colour is initialised.)
	  */
	public Colour complement(Colour result) {
		if(result == this) {
			complement(temp) ;
			set(temp) ;
			return this ;
		}
	  else if(result == null) result = new Colour() ;
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
		if((result == null) || (result.length != 3)) result = new float[3] ;
		
		max = (r > g) ? r : g ;
		min = (r < g) ? r : g ;
		if(max < b) max = b ;
		if(min > b) min = b ;
		val = max ;
		gap = max - min ;
		
		if (max * gap > 0) {
			sat = gap / max ;
			if(r == max) hue = (g - b) / gap ;
			else if(g == max) hue = 2 + ((b - r) / gap) ;
			else hue = 4 + ((r - g) / gap) ;
			
			hue /= 6 ;
			hue %= 1 ;
		}
		else hue = sat = val = 0 ;
		
		result[0] = hue ;
		result[1] = sat ;
		result[2] = val ;
		return result ;
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
	  *  TODO:  TEST THIS...
    */
	public void setFromBytes(byte vals[], int i) {
	  r = (vals[i + 0] + 128) / 255f ;
    g = (vals[i + 1] + 128) / 255f ;
    b = (vals[i + 2] + 128) / 255f ;
    a = (vals[i + 3] + 128) / 255f ;
	}
	
	
	/**  String description of this colour-
 	 */
	public String toString() {
	  return "(Colour RGBA: "+r+" "+g+" "+b+" "+a+")" ;
	}
}






