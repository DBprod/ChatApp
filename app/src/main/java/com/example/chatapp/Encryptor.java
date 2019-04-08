package com.example.chatapp;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;

public class Encryptor {

    public static BigInteger[] generatePublicKey(BigInteger p, BigInteger q){
        BigInteger[] keys = new BigInteger[2];
        BigInteger e = new BigInteger(100, 10000, new Random());
        BigInteger m = p.subtract(new BigInteger("1")).multiply(q.subtract(new BigInteger("1")));
        while(!gcd(e, m).equals(new BigInteger("1"))){
            e = new BigInteger(100, 10000, new Random());
        }
        keys[0] = p.multiply(q); // modulo
        keys[1] = e; // exponent
        return keys;
    }

    public static BigInteger generatePrivateKey(BigInteger[] publicKey, BigInteger prime1, BigInteger prime2){
        BigInteger m = prime1.subtract(new BigInteger("1")).multiply(prime2.subtract(new BigInteger("1")));
        BigInteger privateKey = getInverse(publicKey[1], m);
        return privateKey;
    }

    public static BigInteger[] generatePrimes(){
        BigInteger p = new BigInteger(1024, 10000, new Random());
        BigInteger q = new BigInteger(1024, 10000, new Random());
        BigInteger[] primes = {p, q};
        System.out.println("Prime 1: " + p);
        System.out.println("Prime 2: " + q);
        return primes;
    }

    public static BigInteger encrypt(BigInteger message, BigInteger[] publicKey){
        return message.modPow(publicKey[1], publicKey[0]);
    }

    public static String encrypt(String message, BigInteger[] publicKey){
        BigInteger messageInt = new BigInteger(message.getBytes());
        BigInteger cipherInt = messageInt.modPow(publicKey[1], publicKey[0]);
        return cipherInt.toString();
    }

    public static BigInteger decrypt(BigInteger cipher, BigInteger[] publicKey, BigInteger privateKey){
        return cipher.modPow(privateKey, publicKey[0]);
    }

    public static String decrypt(String cipherIntString, BigInteger[] publicKey, BigInteger privateKey){
        BigInteger cipherInt = new BigInteger(cipherIntString);
        BigInteger decipherInt = cipherInt.modPow(privateKey, publicKey[0]);
        String decipherText = new String(decipherInt.toByteArray());
        return decipherText;
    }

    private static BigInteger gcd(BigInteger a,BigInteger b){
        if(b.compareTo(new BigInteger("0")) == 0)
            return a;
        return gcd(b, a.mod(b));
    }

    public static BigInteger getInverse(BigInteger a, BigInteger m){
        HashMap<BigInteger, BigInteger> map = new HashMap();
        map.put(a, new BigInteger("1"));
        map.put(m, new BigInteger("0"));
        BigInteger inverse = inverse(a, m, map);
        map.clear();
        if(inverse.compareTo(new BigInteger("0")) == -1){
            inverse = inverse.add(m);
        }
        return inverse;
    }
    private static BigInteger inverse(BigInteger a, BigInteger m, HashMap<BigInteger, BigInteger> map){ //Finds the inverse of 'a' mod 'm'
        BigInteger mod = m.mod(a);
        BigInteger div = m.divide(a);

        if(mod.compareTo(new BigInteger("1")) == 0){
            return map.get(m).subtract(div.multiply(map.get(a)));
        }
        if(mod.compareTo(new BigInteger("0")) == 0){
            return new BigInteger("0");
        }

        BigInteger numA = new BigInteger("0");

        if(map.containsKey(m)){
            numA = numA.add(map.get(m));
        }
        if(map.containsKey(a)){
            numA = numA.subtract(div.multiply(map.get(a)));
        }
        map.put(mod, numA);
        return inverse(mod, a, map);
    }
}