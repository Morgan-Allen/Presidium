/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;
import src.graphics.common.* ;
import java.io.* ;


public class Alphabet {
  
  
  final public Texture fontTex ;
  final public Letter
    letters[],
    map[] ;
  
  public static Alphabet loadAlphabet(String path, String mmlFile) {
    path = LoadService.safePath(path) ;
    XML info = (XML.load(path + mmlFile)).child(0) ;
    String
      texFile = info.value("texture"),
      alphaFile = info.value("relAlpha"),
      mapFile = info.value("mapping") ;
    final int
      numLines = Integer.parseInt(info.value("lines")),
      lineHigh = Integer.parseInt(info.value("lhigh")) 
      ;
    return new Alphabet(path, texFile, alphaFile, mapFile, numLines, lineHigh) ;
  }
  
  
  public static class Letter {
    public char map ;
    public float
      umin,
      vmin,
      umax,
      vmax,
      width,
      height ;
  }
  
  
  public Alphabet(
    String path,
    String texFile, String alphaFile,
    String mapFile, int numLines, int lineHigh
  ) {
    //  Basic initialisation routine here.  The list of individual characters
    //  must be loaded first for comparison with scanned character blocks.
    int charMap[] = null ;
    final int mask[] = LoadService.getRGBA(path + texFile) ;
    try {
      FileInputStream mapS = new FileInputStream(new File(path + mapFile)) ;
      charMap = new int[mapS.available()] ;
      for(int n = 0 ; n < charMap.length ; n++) charMap[n] = mapS.read() ;
    }
    catch(IOException e) { I.add(" " + e) ; }
    fontTex = Texture.loadTexture(path+texFile, path+alphaFile) ;
    //
    //  This is where the actual scanning is done.  A single texture is used for
    //  all characters to avoid frequent texture re-bindings during rendering.
    //  The procedure looks for 'gaps' in the initial texture relAlpha as cues for
    //  where to demarcate individual letters.
    final int trueSize = fontTex.trueSize() ;
    int x, y, ind, line = 0, maxMap = 0 ;
    boolean scan = true ;
    List <Letter> scanned = new List <Letter> () ;
    Letter letter = null ;
    
    for( ; line < numLines ; line++) {
      y = lineHigh * line ;
      ind = (y * trueSize) + (x = 0) ;
      for( ; x < fontTex.xdim() ; x++, ind++) {
        if((mask[ind] & 0xff000000) != 0) {  //relAlpha value of pixel is nonzero.
          if(scan) {
            scan = false ;  //a letter starts here.
            
            letter = new Letter() ;
            letter.umin = ((float) x) / trueSize ;
            letter.map = (char) (charMap[scanned.size()]) ;
            if (letter.map > maxMap) maxMap = letter.map ;
          }
        }
        else {
          if(! scan) {
            //...and ends afterward on the first transparent pixel.
            scan = true ;
            letter.umax = ((float) x) / trueSize ;
            letter.vmin = ((float) y) / trueSize ;
            letter.vmax = ((float) y + lineHigh) / trueSize ;
            letter.height = lineHigh ;
            letter.width = (int) ((letter.umax - letter.umin) * trueSize) ;
            scanned.addLast(letter) ;
          }
        }
      }
    }
    
    map = new Letter[maxMap + 1] ;
    letters = new Letter[scanned.size()] ;
    ind = 0 ;
    for (Letter sLetter : scanned)
      map[(int) (sLetter.map)] = letters[ind++] = sLetter ;
  }
}