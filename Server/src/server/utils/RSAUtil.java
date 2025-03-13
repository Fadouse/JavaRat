package server.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello RSA!
 *
 */
public class RSAUtil {
    public Map<Integer,String> keyMap=new HashMap<>();

    public void getKeyPair() throws Exception {
        //KeyPairGenerator类用于生成公钥和密钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        //初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024,new SecureRandom());
        //生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();//得到私钥
        PublicKey publicKey = keyPair.getPublic();//得到公钥
        //得到公钥字符串
        String publicKeyString=new String(Base64.getEncoder().encode(publicKey.getEncoded()));
        //得到私钥字符串
        String privateKeyString=new String(Base64.getEncoder().encode(privateKey.getEncoded()));
        //将公钥和私钥保存到Map
        keyMap.put(0,publicKeyString);//0表示公钥
        keyMap.put(1,privateKeyString);//1表示私钥
    }

    public String encryptStr(String str,String publicKey) throws Exception {
        //base64编码的公钥
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        RSAPublicKey pubKey= (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RAS加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,pubKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));
    }
    public String encryptBytes(byte[] bytes,String publicKey) throws Exception {
        //base64编码的公钥
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        RSAPublicKey pubKey= (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RAS加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,pubKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(bytes));
    }

    public String decryptStr(String str,String privateKey) throws Exception {
        //Base64解码加密后的字符串
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
        //Base64编码的私钥
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,priKey);
        return new String(cipher.doFinal(inputByte));
    }
    public byte[] decryptBytes(String str,String privateKey) throws Exception {
        //Base64解码加密后的字符串
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
        //Base64编码的私钥
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        PrivateKey priKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,priKey);
        return cipher.doFinal(inputByte);
    }
}

