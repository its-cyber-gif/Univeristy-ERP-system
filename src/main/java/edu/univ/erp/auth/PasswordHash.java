package edu.univ.erp.auth;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHash {
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }
    public static boolean verify(String plain, String hash) {
        return BCrypt.checkpw(plain, hash);
    }
}

