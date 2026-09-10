package icu.freedomIntrovert.biliSendCommAntifraud;

import java.math.BigInteger;
import java.util.HashMap;

public class BiliVideoId {
    private static final BigInteger XOR_CODE = new BigInteger("23442827791579");
    private static final BigInteger MASK_CODE = new BigInteger("2251799813685247");
    private static final BigInteger MAX_AID = BigInteger.ONE.shiftLeft(51);
    private static final BigInteger BASE = BigInteger.valueOf(58);
    private static final String DATA = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf";

    /**
     * AV 号转 BV 号
     */
    public static String av2bv(long aid) {
        char[] bytes = "BV1000000000".toCharArray();
        int bvIndex = bytes.length - 1;

        // (MAX_AID | aid) ^ XOR_CODE
        BigInteger tmp = MAX_AID.or(BigInteger.valueOf(aid)).xor(XOR_CODE);

        while (tmp.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divideAndRemainder = tmp.divideAndRemainder(BASE);
            bytes[bvIndex] = DATA.charAt(divideAndRemainder[1].intValue());
            tmp = divideAndRemainder[0];
            bvIndex--;
        }

        // 位置置换
        swap(bytes, 3, 9);
        swap(bytes, 4, 7);

        return new String(bytes);
    }

    /**
     * BV 号转 AV 号
     */
    public static long bv2av(String bvid) {
        char[] bvidArr = bvid.toCharArray();

        // 位置还原置换
        swap(bvidArr, 3, 9);
        swap(bvidArr, 4, 7);

        // 移除前缀 "BV1" 并进行 Base58 解码
        BigInteger tmp = BigInteger.ZERO;
        for (int i = 3; i < bvidArr.length; i++) {
            int index = DATA.indexOf(bvidArr[i]);
            tmp = tmp.multiply(BASE).add(BigInteger.valueOf(index));
        }

        // (tmp & MASK_CODE) ^ XOR_CODE
        return tmp.and(MASK_CODE).xor(XOR_CODE).longValue();
    }

    private static void swap(char[] arr, int i, int j) {
        char temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

}
