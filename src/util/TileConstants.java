/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;



public interface TileConstants {
  
  final public static int
    //  Starts north, going clockwise:
    N  = 0,
    NE = 1,
    E  = 2,
    SE = 3,
    S  = 4,
    SW = 5,
    W  = 6,
    NW = 7,
    N_X[]        = {  0,  1,  1,  1,  0, -1, -1, -1  },
    N_Y[]        = {  1,  1,  0, -1, -1, -1,  0,  1  },
    N_INDEX[]    = {  N, NE,  E, SE,  S, SW,  W, NW  },
    N_ADJACENT[] = {  N,      E,      S,      W      },
    N_DIAGONAL[] = {     NE,     SE,     SW,     NW  } ;
}