package chat.app.manager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import chat.app.manager.utils.GsonUtils;
import thrift.entity.ChatException;
import thrift.entity.ErrorType;
import thrift.entity.User;

public enum UserManager implements RemoteManager.ExceptionHandler {

    INSTANCE;

    private static final String USER = "user";

    public void init() {
        RemoteManager.INSTANCE.addExceptionHandler(this);
    }

    public boolean registered() {
        return LocalStorage.INSTANCE.contains(USER);
    }

    public void saveUser(User user) {
        LocalStorage.INSTANCE.setString(USER, GsonUtils.gson().toJson(user));
    }

    public User getSavedUser() {
        if (registered()) {
            return GsonUtils.gson().fromJson(LocalStorage.INSTANCE.getString(USER), User.class);
        } else {
            return null;
        }
    }

    public void clearSavedUser() {
        LocalStorage.INSTANCE.remove(USER);
    }

    @Override
    public void handleError(ChatException e) {
        if (e.getErrorType() == ErrorType.NO_SUCH_USER) {
            clearSavedUser();
        }
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
