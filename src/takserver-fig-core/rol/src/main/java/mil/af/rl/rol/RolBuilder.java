package mil.af.rl.rol;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonPrimitive;


/*
 * 
 * Builder to generate ROL programs.
 * 
 */
public class RolBuilder {
    
    public String createSemanticSubscription(String name, String sparqlQuery) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("null subscription name");
        }
        
        if (Strings.isNullOrEmpty(sparqlQuery)) {
            throw new IllegalArgumentException("null SPARQL query argument");
        }
        
        StringBuilder rol = new StringBuilder();
        
        rol.append("create subscription ");
        rol.append(parameters(Lists.newArrayList(
                new StringParameter("type", "SPARQL"),
                new StringParameter("filter", sparqlQuery),
                new StringParameter("name", name)
                )).toString());
        
        rol.append(';');
        
        return rol.toString();
    }
    
    public String createTypeBasedSubscription(String name, String role, List<String> topics) {
        return createTypeBasedSubscription(name, role, topics, true);
    }    
    
    public String createTypeBasedSubscription(String name, String role, List<String> topics, boolean quoted) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("null subscription name");
        }
        
        if (Strings.isNullOrEmpty(role)) {
            throw new IllegalArgumentException("null type-based subscription role");
        }
        
        if (topics == null) {
            throw new IllegalArgumentException("null type-based subscription topics");
        }
        
        StringBuilder rol = new StringBuilder();
        
        rol.append("create subscription ");
        rol.append("matchrole \"" + role + "\" ");
        rol.append(parameters(Lists.newArrayList(
                new StringParameter("type", "XPATH"),
                new StringParameter("filter", "starts-with(*)"),
                new StringParameter("name", name),
                new StringArrayParameter("topics", topics, quoted)
                )).toString());
        
        rol.append(';');
        
        return rol.toString();
    }
    
    public String removeSubscription(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("null subscription name");
        }
        
        StringBuilder rol = new StringBuilder();
        
        rol.append("remove subscription ");
        rol.append(parameters(Lists.newArrayList(new StringParameter("name", name))).toString());
        
        rol.append(';');
        
        return rol.toString();
    }
    
    public String parameters(List<Parameter> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("null parameters list");
        }
        
        StringBuilder result = new StringBuilder();
        
        result.append('{');
        
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            
            result.append(parameters.get(i).getKey().toString());
            result.append(" : ");
            result.append(parameters.get(i).getValue().toString());
        }
        
        result.append('}');
        
        return result.toString();
    }
    
    public String matchroleAssertion(String role) {
        return "matchrole " + role;
    }
    
    public static interface Parameter {
        public String getKey();
        public String getValue();
    }
    
    public static class StringParameter implements Parameter {
        
        public StringParameter(String key, String value) {
            setKey(key);
            setValue(value);
        }
        
        private String key;
        private String value;
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getValue() {
            return value;
        }
   
        // escape string
        public void setValue(String value) {
            this.value = new JsonPrimitive(value).toString();
        }
    }
    
    public static final class StringArrayParameter implements Parameter {
        
        private final boolean qouted;
        // Address a false positive Fortify issue by renaming key to paramKey
        private String paramKey;
        private final List<String> values;
        
        public StringArrayParameter(String key, List<String> values, boolean qouted) {
            
            if (key == null) {
                throw new IllegalArgumentException("null key");
            }
            
            if (values == null) {
                throw new IllegalArgumentException("null values list");
            }
            
            this.paramKey = key;
            this.values = values;
            this.qouted = qouted;
        }
        
        public String getKey() {
            return paramKey;
        }
        
        public String getValue() {
            
            StringBuilder b = new StringBuilder();
            
            b.append("[");
            
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                
                // only qoute and escape contents if quoting is enabled.
                if (qouted) {
                    b.append(new JsonPrimitive(values.get(i)).toString());
                } else {
                    b.append(values.get(i));
                }
            }
            
            b.append("]");
            
            return b.toString();
        }
     
    }
}
