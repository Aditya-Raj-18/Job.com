package com.jobcom.security;
import org.mindrot.jbcrypt.BCrypt;
public final class PasswordUtil {
 private PasswordUtil(){}
 public static String hash(String raw){return BCrypt.hashpw(raw,BCrypt.gensalt(12));}
 public static boolean verify(String raw,String hash){try{return raw!=null&&hash!=null&&BCrypt.checkpw(raw,hash);}catch(IllegalArgumentException ex){return false;}}
}
