/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import src.util.* ;
import java.util.Random ;


public class Rand {
  
  final public static Random
    GEN = new Random(),
    PREVIEW = new Random() ;
  
  final public static float num() { return GEN.nextFloat() ; }
  final public static boolean yes() { return GEN.nextBoolean() ; }
  final public static int index(int s) { return GEN.nextInt(s) ; }
  
  final public static float range(float min, float max) {
    return min + ((max - min) * GEN.nextFloat()) ;
  }
  
  final public static float rangeAvg(float min, float max, int n) {
    float total = 0 ;
    for (int r = n ; r-- > 0 ;) total += range(min, max) ;
    return total / n ;
  }
  
  final public static int rollDice(int num, int sides) {
    int total = 0 ;
    while (num-- > 0) total += 1 + (int) (num() * sides) ;
    return total ;
  }
  
  final public static float avgNums(final int n) {
    float total = 0 ;
    for (int r = n ; r-- > 0 ;) total += num() ;
    return total / n ;
  }
  
  final public static Object pickFrom(Object[] array) {
    if (array.length == 0) return null ;
    return array[GEN.nextInt(array.length)] ;
  }
  
  final public static Object pickFrom(Series list) {
    return pickFrom(list.toArray()) ;
  }

  final public static Object pickFrom(Series list, Series weights) {
    return pickFrom(list.toArray(), weights.toArray()) ;
  }
  
  final public static Object pickFrom(Object array[], Object weights[]) {
    float sumWeights = 0 ;
    for (Object o : weights) sumWeights += (Float) o ;
    if (sumWeights == 0) return pickFrom(array) ;
    final float pickWith = Rand.num() * sumWeights ;
    //
    //  Having summed the weights, and picked a random 'interval' within the
    //  'span' of values, pick the object entry underlying that interval.
    sumWeights = 0 ; int i = 0 ; for (Object o : weights) {
      sumWeights += (Float) o ;
      if (pickWith < sumWeights) return array[i] ;
      else i++ ;
    }
    return null ;
  }
}

