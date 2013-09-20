


package src.user ;
import src.graphics.widgets.Text.* ;
import src.util.* ;
import java.lang.reflect.* ;



public class Call implements Clickable {
  
  
  final String label ;
  final Object client, args ;
  private Method method ;
  
  
  public Call(String label, Object client, String methodName, Object args) {
    this.label = label ;
    this.client = client ;
    this.args = args ;
    try {
      this.method = client.getClass().getMethod(methodName, Object.class) ;
      method.setAccessible(true) ;
    }
    catch (Exception e) { I.report(e) ; }
  }
  
  
  public String fullName() {
    return label ;
  }
  
  
  public void whenClicked() {
    try { method.invoke(client, args) ; }
    catch (Exception e) { I.report(e) ; }
  }
}