package tak.server.federation.hub.ui.graph;

import java.util.List;

/**
 * Created on 7/18/2017.
 */
public class EdgeFilter {
    String name;
    String filterObject;
    List<FilterArgument> args;
    String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FilterArgument> getArgs() {
        return args;
    }

    public void setArgs(List<FilterArgument> args) {
        this.args = args;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getFilterObject() {
        return filterObject;
    }

    public void setFilterObject(String filterObject) {
        this.filterObject = filterObject;
    }
}
