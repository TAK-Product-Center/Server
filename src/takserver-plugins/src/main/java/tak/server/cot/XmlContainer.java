

package tak.server.cot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;

public class XmlContainer
{

  protected Map<String,Object> context;
  protected Document doc;
  
  public XmlContainer()
  {
    context = new ConcurrentHashMap<String,Object>();
    doc = null;
  }
  
  public XmlContainer(XmlContainer src) {
    context = new ConcurrentHashMap<String,Object>(src.context);
    doc = (Document) src.doc.clone();
  }

  public XmlContainer(Document xml) {
    context = new ConcurrentHashMap<String,Object>();
    doc = xml;
  }
  
  public String asXml() {
    return doc.asXML();
  }
  
  public Document getDocument() {
    return doc;
  }

  public Document document() {
    return doc;
  }

  public boolean matchXPath(String b) {
    if(doc != null) {
      XPath xpath = DocumentHelper.createXPath(b);
      return xpath.booleanValueOf(doc);
    }
    return false;
  }

  public boolean hasContextKey(String key) {
    return context.containsKey(key);
  }

  public Object getContext(String key) {
    return context.get(key);
  }

  public <T> T getContext(String key, Class<T> type) {
    return (T) getContext(key);
  }

  public <T> T getContextOrElse(String key, T alternate) {
    T value = (T) getContext(key);
    
    if (value == null)
      value = alternate;
      
    return value;
  }
  
  public <T> T getContextOrStore(String key, T init) {
    T value = (T) getContextOrElse(key, init);
    
    if (value == init) {
      setContextValue(key, init);
    }
    
    return value;
  }
  
  public Object getContextValue(String key) {
    return context.get(key);
  }

  public <T> T getContextValueOrElse(String key, T alternate) {
    T current = (T) getContextValue(key);
    if (current != null) {
      return current;
    } else {
      return alternate;
    }
  }
  
  public <T> T getOrStoreContextValue(String key, T init) {
    T current = (T) getContextValue(key);
    
    if (current == null) {
      setContextValue(key, init);
      current = init;
    }
    
    return current;
  }

  public <T> T removeContext(String key, Class<T> type) {
    return (T) context.remove(key);
  }
  
  public Object removeContextValue(String key) {
    return context.remove(key);
  }
  
  public <T> T setContextAndCarry(String key, T value) {
	if (key != null) {
		if (value == null) {
			value = (T) new Object();
		}
		context.put(key, value);
	}
    return value;
  } 
  
  public <T> T setContext(String key, T value) {
    T old = (T) getContext(key);
    
    if (key != null && value != null) {
    	context.put(key, value);
    }
    
    return old;
  }

  public Object setContextValue(String key, Object value) {
    Object old = getContext(key);
    
    if (key != null) {
    	if (value == null) {
    		value = new Object();
    	}
    	
    	context.put(key, value);
    }
    
    return old;
  }
  
  public Map<String, Object> getContext() {
	  return context;
  }

public String toString() {
    return asXml();
  }
}
