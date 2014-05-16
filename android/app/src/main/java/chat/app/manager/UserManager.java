package chat.app.manager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import thrift.entity.User;

public enum UserManager {

    INSTANCE;

    public static final String USERNAME = "username";
    public static final String PASSHASH = "passhash";

    public boolean registered() {
        return getSavedUser() != null;
    }

    public void saveUser(User user) {
        LocalStorage.INSTANCE.setString(USERNAME, user.getUsername());
        LocalStorage.INSTANCE.setString(PASSHASH, user.getPasshash());
    }

    public User getSavedUser() {
        if (LocalStorage.INSTANCE.contains(USERNAME)) {
            String username = LocalStorage.INSTANCE.getString(USERNAME);
            String passhash = LocalStorage.INSTANCE.getString(PASSHASH);
            return new User(username, passhash);
        } else {
            return null;
        }
    }

    public void clearSavedUser() {
        LocalStorage.INSTANCE.remove(USERNAME);
        LocalStorage.INSTANCE.remove(PASSHASH);
    }

    public String md5(String input) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte ch : messageDigest)
                hexString.append(Integer.toHexString(0xFF & ch));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
