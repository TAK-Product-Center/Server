package tak.server.federation.hub.ui.manage;

import java.util.ArrayList;
import java.util.List;

public class AuthorizedUsers {

    private List<AuthorizedUser> users;

    public AuthorizedUsers() {
        this.users = new ArrayList<AuthorizedUser>();
    }

    public List<AuthorizedUser> getUsers() {
        return this.users;
    }

    public void setUsers(List<AuthorizedUser> users) {
        this.users = users;
    }
}
