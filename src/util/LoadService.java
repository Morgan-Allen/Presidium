/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.awt.image.BufferedImage ;
import javax.imageio.ImageIO ;
import java.io.* ;



/**  This class provides various utility functions for loading and caching of
  *  external file resources.
  */
public class LoadService {
  
  protected static Table <String, Object>
    resCache = new Table <String, Object> (1000) ;
  
  
  /**  Caches the given resource.
    */
  public static void cacheResource(Object res, String key) {
    resCache.put(key, res) ;
  }

  /**  Returns the resource matching the given key (if cached- null otherwise.)
    */
  public static Object getResource(String key) {
    return resCache.get(key) ;
  }
  
  
  /**  Recursively loads all classes in the given directory.
    */
  public static Batch <Class> loadClassesInDir(
    String dirName, String packageName
  ) {
    //
    //  TODO:  THIS WON'T WORK IF YOU HAVE EVERYTHING PACKED INTO A .JAR FILE.
    //  YOU'LL NEED TO LOAD THINGS AS RESOURCES INSTEAD.  Look here-
    //  http://stackoverflow.com/questions/2393194/how-to-access-resources-in-jar-file
    final Batch <Class> loaded = new Batch <Class> () ;
    final char sep = java.io.File.separatorChar ;
    File baseDir = new File(dirName) ;
    if (baseDir.isDirectory())  for (File defined : baseDir.listFiles()) try {
      final String fileName = defined.getName() ;
      if (defined.isDirectory()) {
        loadClassesInDir(dirName+sep+fileName, packageName+"."+fileName) ;
        continue ;
      }
      if (! fileName.endsWith(".java")) continue ;
      final String className = fileName.substring(0, fileName.length() - 5) ;
      //I.say("Loading class: "+packageName+"."+className) ;
      loaded.add(Class.forName(packageName+"."+className)) ;
    }
    catch (Exception e) {}// I.report(e) ; }
    return loaded ;
  }
  
  
  final public static char REP_SEP = '/' ;
  

  /**  Writes a string to the given data output stream.
   */
  public static void writeString(DataOutputStream dOut, String s)
      throws IOException {
    byte chars[] = s.getBytes() ;
    dOut.writeInt(chars.length) ;
    dOut.write(chars) ;
  }
  
  
  /**  Reads a string from the given data input stream.
   */
  public static String readString(DataInputStream dIn)
      throws IOException {
    final int len = dIn.readInt() ;
    byte chars[] = new byte[len] ;
    dIn.read(chars) ;
    return new String(chars) ;
  }
  

  /**  Returns an image from the given file name.
    */
  public static BufferedImage getImage(String name) {
    try { return ImageIO.read(new java.io.File(name)) ; }
    catch(java.io.IOException e) {
      I.say("  PROBLEM LOADING IMAGE. " + name + " " + e.getMessage()) ;
      e.printStackTrace() ;
    }
    return null ;
  }
  

  /**  Returns the raw RGBA data for the given image file.
    */
  public static int[] getRGBA(String name) {
    return getRGBA(getImage(name)) ;
  }
  
  /**  Returns the raw RGBA data for the given image.
    */
  public static int[] getRGBA(BufferedImage image) {
    int
      x = image.getWidth(),
      y = image.getHeight(),
      rgba[] = new int[x * y] ;
    image.getRGB(0, 0, x, y, rgba, 0, x) ;
    return rgba ;
  }
  
  /**  Seperates the given full file path into it's path and file-name components,
    *  returning them as a two-element array- (path first, file name second.)
    */
  public static String[] sepPath(String fullPath) {
    char[] fP = fullPath.toCharArray() ;
    int ind ;
    for (ind = fP.length ; ind-- > 0 ;)
      if (fP[ind] == REP_SEP) break ;
    
    if (ind < fP.length) ind++ ;
    String PN[] = {
      new String(fP, 0, ind),
      new String(fP, ind, fP.length - ind)
    } ;
    return PN ;
  }

  /**  Returns a safe version of the given path-string (using the default separator.)
    */
  public static String safePath(String path) {
    return safePath(path, java.io.File.separatorChar) ;
  }
  
  /**  Returns a safe version of the given path-string (i.e, using the given seperator.)
    */
  public static String safePath(String path, char separator) {
    int l = path.length() ;
    char chars[] = new char[l] ;
    path.getChars(0, l, chars, 0) ;
    for (int x = 0 ; x < l ; x++) {
      switch(chars[x]) {
        case('/'):
        case('\\'):
          chars[x] = separator ;
        break ;
      }
    }
    return new String(chars) ;
  }
}

