/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.*;

import org.lwjgl.input.Keyboard ;


public class KeyInput {
  
  
  static List <Listener> listeners = new List <Listener> () ;
  static Batch <Character> pressed = new Batch <Character> () ;
  
  public abstract static class Listener {
    
    final private static char ALL_KEYS[] = {} ;
    
    protected abstract boolean pressEvent(char key, int keyID) ;
    final private char keys[] ;
    
    public Listener() { keys = ALL_KEYS ; }
    public Listener(char k) { keys = new char[] {k} ; }
    public Listener(char k1, char k2) { keys = new char[] {k1, k2} ; }
    public Listener(char k1, char k2, char k3) { keys = new char[] { k1, k2, k3 } ; }
  }
  
  
  public static void removeListener(Listener l) {
    listeners.remove(l) ;
  }
  
  public static void addListener(Listener l) {
    if (! listeners.contains(l)) listeners.addLast(l) ;
  }
  
  /*
  public static boolean isCharDown(char keyChar) {
    return matchKey(keyChar) != null ;
    Keyboard.isKeyDown(Keyboard.)
  }
  
  public static boolean isCharPressed(char keyChar) {
    final KeyDown down = matchKey(keyChar) ;
    return (down != null) && (down.state == PRESS) ;
  }//*/
  
  public static boolean wasKeyPressed(char c) {
    for (char m : pressed) if (c == m) return true ;
    return false ;
  }
  
  public static boolean isKeyDown(int ID) {
    return Keyboard.isKeyDown(ID) ;
    //for (KeyDown down : keysDown) if (down.keyID == ID) return true ;
    //return false ;
  }
  
  static void updateKeyboard() {
    //  TODO:  CERTAIN KEY EVENTS SEEM TO BE REPEATING.
    ///I.say("Updating keyboard...") ;
    final Batch <Character> newChars = new Batch <Character> () ;
    
    while(Keyboard.next()) {
      final int key = Keyboard.getEventKey() ;
      final boolean down = Keyboard.getEventKeyState() ;
      //final boolean repeat = Keyboard.isRepeatEvent() ;
      final char c = Keyboard.getEventCharacter() ;
      
      boolean wasDown = false ;
      for (Character p : pressed) if (p == c) wasDown = true ;
      if (! wasDown) newChars.add(c) ;
      //pressed.add(c) ;
      
      boolean match ;
      for (Listener listens : listeners) {
        match = false ; if (listens.keys == Listener.ALL_KEYS) match = true ;
        else for (char k : listens.keys) if (k == c) match = true ;
        if (match && down) listens.pressEvent(c, key) ;
      }
    }
    pressed.clear() ;
    for (Character c : newChars) pressed.add(c) ;
    //for (char c : pressed) I.say(c+" was pressed") ;
  }
}
  /*
  
  //  Here are the basic support classes needed for this to work.
  private static class KeyDown {
    char keyChar ;
    int keyID ;
    byte state = INIT ;
  }
  
  abstract static class Listener {
    final private char keys[] ;
    
    abstract boolean pressEvent(char key, int keyID) ; // { return false ; }
    
    Listener() { keys = ALL_KEYS ; }
    Listener(char k) { keys = new char[1] ; keys[0]= k ; }
    Listener(char k1, char k2) { char k[] = { k1, k2 } ; keys = k ; }
    Listener(char k1, char k2, char k3) { char k[] = { k1, k2, k3 } ; keys = k ; }
  }
  
  
  //  Here is the internal data needed to maintain state.
  final private static char ALL_KEYS[] = {} ;
  final private static byte
    INIT = 0,
    PRESS = 1,
    DOWN = 2,
    RELEASE = 3 ;
  final static KeyInput KEYBOARD = new KeyInput() ;
  private KeyInput() {}
  
  static List <Listener> listeners = new List <Listener> () ;
  static List <KeyDown> keysDown = new List <KeyDown> () ;
  
  public void keyPressed(KeyEvent press) { eventGate(press) ; }
  public void keyReleased(KeyEvent release) { eventGate(release) ; }
  public void keyTyped(KeyEvent typing) { eventGate(typing) ; }
  
  
  //  And here is the business end-
  static void removeListener(Listener l) { listeners.remove(l) ; }
  
  static void addListener(Listener l) {
    if (! listeners.contains(l))
      listeners.addLast(l) ;
  }
  
  static void updateKeyboard() { eventGate(null) ; }
  
  public static boolean isCharDown(char keyChar) {
    return matchKey(keyChar) != null ;
  }
  
  public static boolean isCharPressed(char keyChar) {
    final KeyDown down = matchKey(keyChar) ;
    return (down != null) && (down.state == PRESS) ;
  }
  
  public static boolean isKeyDown(int ID) {
    for (KeyDown down : keysDown) if (down.keyID == ID) return true ;
    return false ;
  }
  
  private static KeyDown matchKey(char keyChar) {
    for (KeyDown down : keysDown) if (down.keyChar == keyChar) return down ;
    return null ;
  }
  
  //  One big monster method to handle all input and ensure that AWT key events
  //  don't arrive in the middle of other processing.
  private static synchronized void eventGate(KeyEvent event) {
    
        
    if (event == null) {
      //  Update the keyBoard as a whole.
      for (KeyDown down : keysDown) {
        if (down.state == INIT) {
          for (Listener listens : listeners) {
            if (listens.keys == ALL_KEYS)
              listens.pressEvent(down.keyChar, down.keyID) ;
            else for (int k : listens.keys) if (k == down.keyChar)
              listens.pressEvent(down.keyChar, down.keyID) ;
          }
          down.state = PRESS ;
        }
        else
          down.state = DOWN ;
      }
      return ;
    }
    //  Otherwise, a specific key has been pressed:
    final char keyChar = event.getKeyChar() ;
    switch (event.getID()) {
      case (KeyEvent.KEY_PRESSED) :
        if (matchKey(keyChar) == null) {
          KeyDown down = new KeyDown() ;
          down.keyChar = keyChar ;
          down.keyID = event.getKeyCode() ;
          keysDown.addFirst(down) ;
        }
        return ;
      case (KeyEvent.KEY_RELEASED) :
        keysDown.remove(matchKey(keyChar)) ;
        return ;
    }
  }
  //*/
