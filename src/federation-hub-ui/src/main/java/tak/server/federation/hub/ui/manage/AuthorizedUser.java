package tak.server.federation.hub.ui.manage;

public class AuthorizedUser {

    private String username;
    private String fingerprint;

    public AuthorizedUser(String username, String fingerprint) {
        this.username = username;
        this.fingerprint = fingerprint;
    }

    public AuthorizedUser() { }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFingerprint() {
        return this.fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (getClass() != o.getClass())
            return false;

        AuthorizedUser otherUser = (AuthorizedUser)o;
        return this.getUsername().equals(otherUser.getUsername()) &&
            this.getFingerprint().equals(otherUser.getFingerprint());
    }
}
