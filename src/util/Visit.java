/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.util.Iterator ;


public class Visit <T> {
  
  
  public Visit() {}
  public void visit(T o) {} ;
  
  
  public static int clamp(int index, int range) {
    if (index < 0) return 0 ;
    if (index >= range) return range - 1 ;
    return index ;
  }
  
  public static float clamp(float value, float min, float max) {
    if (value < min) return min ;
    if (value > max) return max ;
    return value ;
  }
  
  
  public static Object last(Object o[]) {
    return o[o.length - 1] ;
  }
  
  
  /**  Returns true if the given array includes the given object.
    */
  public static boolean arrayIncludes(Object a[], Object e) {
    for (Object o : a) if (o == e) return true ;
    return false ;
  }
  
  /**  Used to convert an array of Float objects to an array of normal floats.
    */
  public static float[] fromFloats(Object[] a) {
    float f[] = new float[a.length] ;
    for (int i = f.length ; i-- > 0 ;) f[i] = (Float) a[i] ;
    return f ;
  }

  /**  Used to convert an array of Integer objects to an array of normal ints.
    */
  public static int[] fromIntegers(Object[] a) {
    int f[] = new int[a.length] ;
    for (int i = f.length ; i-- > 0 ;) f[i] = (Integer) a[i] ;
    return f ;
  }
  
  
  /**  Visits every point in the given area- a syntactic shortcut for array
    *  loops.
    */
  public static Iterable <Coord> grid(
    final int minX, final int minY,
    final int xD, final int yD,
    final int step
  ) {
    final int maxX = minX + xD, maxY = minY + yD ;
    final Coord passed = new Coord() ;
    final class iterates implements Iterator <Coord>, Iterable <Coord> {
      
      int x = minX, y = minY ;
      
      final public boolean hasNext() {
        return x < maxX && y < maxY ;
      }
      
      final public Coord next() {
        passed.x = x ;
        passed.y = y ;
        if ((y += step) == maxY) { y = minY; x += step ; }
        return passed ;
      }
      
      final public Iterator <Coord> iterator() { return this ; }
      public void remove() {}
    }
    return new iterates() ;
  }
  
  
  public static Iterable <Coord> grid(Box2D area) {
    return grid(
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim(),
    1) ;
  }
  
  
  public Iterable <T> grid(
    final int minX, final int minY,
    final int xD, final int yD,
    final T array[][]
  ) {
    final int maxX = minX + xD, maxY = minY + yD ;
    final class iterates implements Iterator <T>, Iterable <T> {
      int x = minX, y = minY ;
      
      final public boolean hasNext() {
        return x < maxX && y < maxY ;
      }
      
      final public T next() {
        T next = null ;
        try { next = array[x][y] ; }
        catch (ArrayIndexOutOfBoundsException e) {}
        if (++y == maxY) { y = minY; x++ ; }
        return next ;
      }
      
      final public Iterator <T> iterator() { return this ; }
      public void remove() {}
    }
    return new iterates() ;
  }
  
  
  
  public void visArray(T a[]) {
    for(int n = 0 ; n < a.length ; n++)
      visit(a[n]) ;
  }
  
  public void visArray(T a[][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
  
  public void visArray(T a[][][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
  
  public void visArray(T a[][][][]) {
    for(int n = 0 ; n < a.length ; n++)
      visArray(a[n]) ;
  }
}
