package com.couchbase.lite.listener;

import java.util.UUID;

public class Credentials {

    private String login;
    private String password;

    public Credentials() {
        setRandomUsernamePassword();
    }

    public Credentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setRandomUsernamePassword() {
        login = UUID.randomUUID().toString();
        password = UUID.randomUUID().toString();
    }

    public boolean empty() {
        boolean loginEmpty = (login == null || login.isEmpty());
        boolean passwordEmpty = (password == null || password.isEmpty());
        return loginEmpty && passwordEmpty;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", login, password);
    }

    /**
     * Test whether two credentials are equal.
     *
     * Null/empty fields are considered equal to other null/empty fields.
     *
     * @return
     */
    @Override
    public boolean equals(Object o) {

        if (o instanceof Credentials) {
            Credentials otherCreds = (Credentials) o;

            boolean sameLogin = false;
            boolean samePassword = false;

            String otherCredsLogin = otherCreds.getLogin();
            String otherCredsPassword = otherCreds.getPassword();

            if (otherCredsLogin == null || otherCredsLogin.isEmpty()) {
                sameLogin = (getLogin() == null || getLogin().isEmpty());
            } else {
                sameLogin = getLogin().equals(otherCredsLogin);
            }

            if (otherCredsPassword == null || otherCredsPassword.isEmpty()) {
                samePassword = (getPassword() == null || getPassword().isEmpty());
            } else {
                samePassword = getPassword().equals(otherCredsPassword);
            }

            return sameLogin && samePassword;
        }
        return super.equals(o);
    }

}
