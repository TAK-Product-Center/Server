package mil.af.rl.rol;

/*
 * 
 * Simple representation of a {key, value} attribute
 * 
 */
public class Attribute {
    
    private String key;
    private String value;

    public Attribute() { }
    
    public Attribute(String key, String value) {
        this.key = key;
        this.value = value;
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(key);
        builder.append(" : ");
        builder.append(value);
        builder.append("}");
        return builder.toString();
    }
}
