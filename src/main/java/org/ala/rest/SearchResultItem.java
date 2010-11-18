package org.ala.rest;

import java.util.List;
import org.apache.lucene.document.Fieldable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.io.Serializable;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

@XStreamAlias("result")
public class SearchResultItem implements Serializable {

    String id;
    String name;
    String serial;
    String description;
    String state;
    String layerName;
    @XStreamAlias("xlink:href")
    @XStreamAsAttribute
    String link;
    private static final Logger logger = Logging.getLogger("org.ala.rest.SearchResultItem");

    SearchResultItem(String layerName, String idAttribute1) {
        this(layerName, idAttribute1, "");
    }

    SearchResultItem(String layerName, String idAttribute1, String idAttribute2) {
        this.id = layerName + "/" + idAttribute1;
        if (idAttribute2.compareTo("") != 0) {
            this.id += "/" + idAttribute2;
        }
        this.name = idAttribute1;
        this.layerName = layerName;
        this.link = "/geoserver/rest/gazetteer/";
        this.link += this.id.replace(" ", "_") + ".json";
    }

    SearchResultItem(List<Fieldable> fields, Boolean includeLink) {

        String idAttribute1 = "";
        String idAttribute2 = "";

        this.description = "";
        for (Fieldable field : fields) {
            if (field.name().contentEquals("name")) {
                this.name = field.stringValue();
            } else if (field.name().contentEquals("serial")) {
                this.serial = field.stringValue();
            } else if (field.name().contentEquals("state")) {
                this.state = field.stringValue();
            } else if (field.name().contentEquals("layerName")) {
                this.layerName = field.stringValue();
            } else if (field.name().contentEquals("idAttribute1")) {
                idAttribute1 = field.stringValue();
            } else if (field.name().contentEquals("idAttribute2")) {
                idAttribute2 = field.stringValue();
            } else {
                this.description += field.stringValue() + ",";
            }
        }
        this.id = this.layerName + "/" + idAttribute1.replace(" ", "_");

        if (idAttribute2.compareTo("") != 0){
            this.id += "/" + idAttribute2.replace(" ", "_");
        }

        if (!description.contentEquals("")) {
            description = description.substring(0, description.length() - 1);
        }
        if (includeLink == true) {
            this.link = "/geoserver/rest/gazetteer/";
            this.link += id + ".json";
        }
    }
}
