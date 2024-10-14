package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import java.util.Random;

public class RandomChineseStringGenerator {

    // 生成指定长度的随机中文字符串
    public static String generateFixedLengthChineseString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            sb.append(getRandomChineseCharacter(random));
        }

        return sb.toString();
    }

    // 生成指定长度范围的随机中文字符串
    public static String generateRandomLengthChineseString(int minLength, int maxLength) {
        Random random = new Random();
        int length = random.nextInt((maxLength - minLength) + 1) + minLength;

        return generateFixedLengthChineseString(length);
    }

    // 随机生成一个中文字符
    private static char getRandomChineseCharacter(Random random) {
        // Unicode 范围：0x4E00 (一) 到 0x9FA5 (龥)
        return (char) (random.nextInt(0x9FA5 - 0x4E00 + 1) + 0x4E00);
    }

    public static void main(String[] args) {
        // 示例：生成长度为10的中文字符串
        String fixedLengthString = generateFixedLengthChineseString(10);
        System.out.println("固定长度: " + fixedLengthString);

        // 示例：生成长度在5到15之间的随机中文字符串
        String randomLengthString = generateRandomLengthChineseString(15, 15);
        System.out.println("范围随机长度: " + randomLengthString);
    }
}
