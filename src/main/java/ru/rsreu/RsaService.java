package ru.rsreu;

import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

public class RsaService {
    private final SecureRandom random = new SecureRandom();

    public BigInteger calculateN(BigInteger p, BigInteger q) {
        return p.multiply(q);
    }

    public BigInteger calculatePhi(BigInteger p, BigInteger q) {
        return p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
    }

    public void ensurePrime(BigInteger value, String name) {
        if (value == null || value.compareTo(BigInteger.TWO) < 0 || !value.isProbablePrime(20)) {
            throw new IllegalArgumentException(name + " не простое число");
        }
    }

    public void ensureDistinct(BigInteger p, BigInteger q) {
        if (p.equals(q)) {
            throw new IllegalArgumentException("P и Q не должны совпадать");
        }
    }

    public BigInteger pickPublicExponent(BigInteger phi) {
        int[] common = {65537, 17, 3, 5, 7, 11, 13};
        for (int value : common) {
            BigInteger candidate = BigInteger.valueOf(value);
            if (isValidExponent(candidate, phi)) {
                return candidate;
            }
        }
        for (int i = 0; i < 10_000; i++) {
            BigInteger candidate = new BigInteger(phi.bitLength(), random);
            if (isValidExponent(candidate, phi)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Не удалось подобрать e");
    }

    public boolean isValidExponent(BigInteger e, BigInteger phi) {
        return e.compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0 && e.gcd(phi).equals(BigInteger.ONE);
    }

    public BigInteger inverse(BigInteger value, BigInteger modulo) {
        return value.modInverse(modulo);
    }

    public List<BigInteger> encrypt(String text, RSAParams params, String alphabet) {
        Objects.requireNonNull(text, "text");
        List<BigInteger> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int index = alphabet.indexOf(ch);
            if (index < 0) {
                throw new IllegalArgumentException("Символ '" + ch + "' отсутствует в алфавите");
            }
            BigInteger m = BigInteger.valueOf(index + 1L);
            if (m.compareTo(params.n()) >= 0) {
                throw new IllegalArgumentException("Код символа >= N. Увеличьте N или сократите алфавит");
            }
            result.add(m.modPow(params.e(), params.n()));
        }
        return result;
    }

    public String decrypt(List<BigInteger> cipher, RSAParams params, String alphabet) {
        StringBuilder builder = new StringBuilder();
        for (BigInteger value : cipher) {
            if (value.signum() < 0 || value.compareTo(params.n()) >= 0) {
                throw new IllegalArgumentException("Число шифртекста вне диапазона [0, N)");
            }
            BigInteger m = value.modPow(params.d(), params.n());
            int index = m.intValueExact() - 1;
            if (index < 0 || index >= alphabet.length()) {
                throw new IllegalArgumentException("Получен код вне диапазона алфавита: " + (index + 1));
            }
            builder.append(alphabet.charAt(index));
        }
        return builder.toString();
    }

    public List<BigInteger> parseNumbers(String text) {
        List<BigInteger> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(text, " ,;\n\t");
        while (tokenizer.hasMoreTokens()) {
            result.add(new BigInteger(tokenizer.nextToken()));
        }
        return result;
    }

    public String join(List<BigInteger> numbers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(numbers.get(i));
        }
        return builder.toString();
    }

    public void validateAlphabet(String alphabet, BigInteger n) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Алфавит не задан");
        }
        for (int i = 0; i < alphabet.length(); i++) {
            char ch = alphabet.charAt(i);
            if (alphabet.indexOf(ch) != alphabet.lastIndexOf(ch)) {
                throw new IllegalArgumentException("Алфавит содержит повторяющийся символ: '" + ch + "'");
            }
        }
        if (BigInteger.valueOf(alphabet.length()).compareTo(n) >= 0) {
            throw new IllegalArgumentException("Размер алфавита должен быть < N (|A|=" + alphabet.length() + ", N=" + n + ")");
        }
    }

    public BigInteger readPositive(String value, String name) {
        try {
            BigInteger parsed = new BigInteger(value.trim());
            if (parsed.signum() <= 0) {
                throw new NumberFormatException();
            }
            if (!parsed.isProbablePrime(20)) {
                throw new IllegalArgumentException(name + ": число должно быть простым");
            }
            return parsed;
        } catch (Exception ex) {
            throw new IllegalArgumentException(name + ": введите положительное простое целое число");
        }
    }

    public Document createCipherDocument() {
        return new PlainDocument();
    }
}
