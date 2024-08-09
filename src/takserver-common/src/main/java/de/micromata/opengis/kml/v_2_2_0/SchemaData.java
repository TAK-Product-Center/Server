//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.micromata.opengis.kml.v_2_2_0;

import de.micromata.opengis.kml.v_2_2_0.annotations.Obvious;
import de.micromata.opengis.kml.v_2_2_0.gx.SimpleArrayData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "SchemaDataType",
        propOrder = {"simpleData", "schemaDataExtension"}
)
@XmlRootElement(
        name = "SchemaData",
        namespace = "http://www.opengis.net/kml/2.2"
)
public class SchemaData extends AbstractObject implements Cloneable {
    @XmlElement(
            name = "SimpleData"
    )
    protected List<SimpleData> simpleData;
    @XmlElement(
            name = "SimpleArrayData",
            namespace = "http://www.google.com/kml/ext/2.2"
    )
    protected List<SimpleArrayData> schemaDataExtension;
    @XmlAttribute(
            name = "schemaUrl"
    )
    @XmlSchemaType(
            name = "anyURI"
    )
    protected String schemaUrl;

    public SchemaData() {
    }

    public List<SimpleData> getSimpleData() {
        if (this.simpleData == null) {
            this.simpleData = new ArrayList();
        }

        return this.simpleData;
    }

    public List<SimpleArrayData> getSchemaDataExtension() {
        if (this.schemaDataExtension == null) {
            this.schemaDataExtension = new ArrayList();
        }

        return this.schemaDataExtension;
    }

    public String getSchemaUrl() {
        return this.schemaUrl;
    }

    public void setSchemaUrl(String var1) {
        this.schemaUrl = var1;
    }

    public int hashCode() {
        int var2 = super.hashCode();
        var2 = 31 * var2 + (this.simpleData == null ? 0 : this.simpleData.hashCode());
        var2 = 31 * var2 + (this.schemaDataExtension == null ? 0 : this.schemaDataExtension.hashCode());
        var2 = 31 * var2 + (this.schemaUrl == null ? 0 : this.schemaUrl.hashCode());
        return var2;
    }

    public boolean equals(Object var1) {
        if (this == var1) {
            return true;
        } else if (var1 == null) {
            return false;
        } else if (!super.equals(var1)) {
            return false;
        } else if (!(var1 instanceof SchemaData)) {
            return false;
        } else {
            SchemaData var2 = (SchemaData)var1;
            if (this.simpleData == null) {
                if (var2.simpleData != null) {
                    return false;
                }
            } else if (!this.simpleData.equals(var2.simpleData)) {
                return false;
            }

            if (this.schemaDataExtension == null) {
                if (var2.schemaDataExtension != null) {
                    return false;
                }
            } else if (!this.schemaDataExtension.equals(var2.schemaDataExtension)) {
                return false;
            }

            if (this.schemaUrl == null) {
                if (var2.schemaUrl != null) {
                    return false;
                }
            } else if (!this.schemaUrl.equals(var2.schemaUrl)) {
                return false;
            }

            return true;
        }
    }

    public SimpleData createAndAddSimpleData(String var1) {
        SimpleData var2 = new SimpleData(var1);
        this.getSimpleData().add(var2);
        return var2;
    }

    public void setSimpleData(List<SimpleData> var1) {
        this.simpleData = var1;
    }

    public SchemaData addToSimpleData(SimpleData var1) {
        this.getSimpleData().add(var1);
        return this;
    }

    public void setSchemaDataExtension(List<SimpleArrayData> var1) {
        this.schemaDataExtension = var1;
    }

    public SchemaData addToSchemaDataExtension(SimpleArrayData var1) {
        this.getSchemaDataExtension().add(var1);
        return this;
    }

    @Obvious
    public void setObjectSimpleExtension(List<Object> var1) {
        super.setObjectSimpleExtension(var1);
    }

    @Obvious
    public SchemaData addToObjectSimpleExtension(Object var1) {
        super.getObjectSimpleExtension().add(var1);
        return this;
    }

    public SchemaData withSimpleData(List<SimpleData> var1) {
        this.setSimpleData(var1);
        return this;
    }

    public SchemaData withSchemaDataExtension(List<SimpleArrayData> var1) {
        this.setSchemaDataExtension(var1);
        return this;
    }

    public SchemaData withSchemaUrl(String var1) {
        this.setSchemaUrl(var1);
        return this;
    }

    @Obvious
    public SchemaData withObjectSimpleExtension(List<Object> var1) {
        super.withObjectSimpleExtension(var1);
        return this;
    }

    @Obvious
    public SchemaData withId(String var1) {
        super.withId(var1);
        return this;
    }

    @Obvious
    public SchemaData withTargetId(String var1) {
        super.withTargetId(var1);
        return this;
    }

    public SchemaData clone() {
        SchemaData var1 = (SchemaData)super.clone();
        var1.simpleData = new ArrayList(this.getSimpleData().size());
        Iterator var2 = this.simpleData.iterator();

        while(var2.hasNext()) {
            SimpleData var3 = (SimpleData)var2.next();
            var1.simpleData.add(var3.clone());
        }

        var1.schemaDataExtension = new ArrayList(this.getSchemaDataExtension().size());
        var2 = this.schemaDataExtension.iterator();

        while(var2.hasNext()) {
            SimpleArrayData var4 = (SimpleArrayData)var2.next();
            var1.schemaDataExtension.add(var4);
        }

        return var1;
    }
}
