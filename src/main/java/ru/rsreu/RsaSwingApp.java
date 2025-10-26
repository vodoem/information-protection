package ru.rsreu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * Простая учебная программа RSA на Swing + AWT.
 * Вкладки:
 *  - Генерация ключей (с вводом P,Q и авто-генерацией E,D)
 *  - Шифрование (по символьно согласно выбранному алфавиту)
 *  - Дешифрование (по числам)
 *
 * По умолчанию подставлен вариант из методички: P=2, Q=11, E=3, D=7, алфавит="1234567890".
 */
public class RsaSwingApp extends JFrame {

    // Общее состояние (ключи и алфавит), доступное вкладкам
    private final JTextField tfP = new JTextField("2");
    private final JTextField tfQ = new JTextField("11");
    private final JTextField tfN = new JTextField();
    private final JTextField tfPhi = new JTextField();
    private final JTextField tfE = new JTextField("3");
    private final JTextField tfD = new JTextField("7");
    private final JTextField tfAlphabet = new JTextField("1234567890");
    private final JTextArea taLog = new JTextArea(7, 20);

    // Поля шифрования/дешифрования
    private final JTextArea taPlain = new JTextArea(6, 40);
    private final JTextArea taCipher = new JTextArea(6, 40);
    private final JTextArea taDecrypted = new JTextArea(6, 40);

    private final SecureRandom rnd = new SecureRandom();

    public RsaSwingApp() {
        super("RSA — лабораторная (AWT/Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1) Ключи", buildKeysTab());
        tabs.addTab("2) Шифрование", buildEncryptTab());
        tabs.addTab("3) Дешифрование", buildDecryptTab());

        getContentPane().add(tabs);

        // Инициализируем вычисление N, φ(N) из текущих P,Q и E,D (для удобства)
        recalcKeyFields(false);
    }

    // ------- Построение вкладки "Ключи" -------
    private JPanel buildKeysTab() {
        JPanel root = panel(new GridBagLayout());
        GridBagConstraints c = gbc();

        // Ввод P, Q, алфавит
        JPanel pqPanel = panel(new GridBagLayout());
        GridBagConstraints cpq = gbc();
        addRow(pqPanel, cpq, new JLabel("P:"), tfP);
        addRow(pqPanel, cpq, new JLabel("Q:"), tfQ);
        addRow(pqPanel, cpq, new JLabel("Исходный алфавит:"), tfAlphabet);

        // Вычисленные значения
        JPanel calcPanel = panel(new GridBagLayout());
        GridBagConstraints cc = gbc();
        tfN.setEditable(false);
        tfPhi.setEditable(false);
        addRow(calcPanel, cc, new JLabel("N = P * Q:"), tfN);
        addRow(calcPanel, cc, new JLabel("φ(N) = (P-1)(Q-1):"), tfPhi);

        // Открытый/закрытый ключи
        JPanel keyPanel = panel(new GridBagLayout());
        GridBagConstraints ck = gbc();
        addRow(keyPanel, ck, new JLabel("Открытый ключ e:"), tfE);
        addRow(keyPanel, ck, new JLabel("Закрытый ключ d:"), tfD);

        JButton btnRecalc = new JButton("Пересчитать N и φ(N)");
        btnRecalc.addActionListener(e -> recalcKeyFields(true));

        JButton btnGenerate = new JButton("Сгенерировать e,d");
        btnGenerate.addActionListener(this::onGenerateKeys);

        JButton btnValidate = new JButton("Проверить ключи");
        btnValidate.addActionListener(e -> validateCurrentKeys(true));

        // Логи
        taLog.setEditable(false);
        JScrollPane spLog = new JScrollPane(taLog);
        spLog.setBorder(BorderFactory.createTitledBorder("Лог генерации / проверок"));

        // Размещение на вкладке
        c.gridx = 0; c.gridy = 0; c.weightx = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(titled(pqPanel, "Входные данные"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(titled(calcPanel, "Вычисленные параметры"), c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(titled(keyPanel, "Ключи"), c);

        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnRecalc);
        btns.add(btnGenerate);
        btns.add(btnValidate);
        c.gridx = 1; c.gridy = 1; c.weightx = 0.5; c.fill = GridBagConstraints.HORIZONTAL;
        root.add(btns, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        root.add(spLog, c);

        return root;
    }

    // ------- Построение вкладки "Шифрование" -------
    private JPanel buildEncryptTab() {
        JPanel root = panel(new GridBagLayout());
        GridBagConstraints c = gbc();

        taPlain.setLineWrap(true);
        taPlain.setWrapStyleWord(true);
        JScrollPane spPlain = new JScrollPane(taPlain);
        spPlain.setBorder(BorderFactory.createTitledBorder("Исходный текст"));

        JTextArea taOut = taCipher; // один и тот же объект хранится в поле
        taOut.setLineWrap(true);
        taOut.setWrapStyleWord(true);
        JScrollPane spCipher = new JScrollPane(taOut);
        spCipher.setBorder(BorderFactory.createTitledBorder("Зашифрованные числа (через пробел)"));

        JButton btnEncrypt = new JButton("Шифровать");
        btnEncrypt.addActionListener(this::onEncrypt);

        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(spPlain, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 1; c.weighty = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(spCipher, c);

        JPanel south = panel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnEncrypt);
        c.gridx = 0; c.gridy = 2; c.weightx = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        root.add(south, c);

        return root;
    }

    // ------- Построение вкладки "Дешифрование" -------
    private JPanel buildDecryptTab() {
        JPanel root = panel(new GridBagLayout());
        GridBagConstraints c = gbc();

        JTextArea taIn = taCipher; // общий буфер: удобно копировать между вкладками
        JScrollPane spIn = new JScrollPane(taIn);
        spIn.setBorder(BorderFactory.createTitledBorder("Входные зашифрованные числа (через пробел)"));

        taDecrypted.setLineWrap(true);
        taDecrypted.setWrapStyleWord(true);
        JScrollPane spOut = new JScrollPane(taDecrypted);
        spOut.setBorder(BorderFactory.createTitledBorder("Расшифрованный текст"));

        JButton btnDecrypt = new JButton("Дешифровать");
        btnDecrypt.addActionListener(this::onDecrypt);

        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(spIn, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 1; c.weighty = 0.5; c.fill = GridBagConstraints.BOTH;
        root.add(spOut, c);

        JPanel south = panel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnDecrypt);
        c.gridx = 0; c.gridy = 2; c.weightx = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        root.add(south, c);

        return root;
    }

    // ------- Обработчики -------

    private void onGenerateKeys(ActionEvent ev) {
        try {
            BigInteger p = readPositive(tfP.getText(), "P");
            BigInteger q = readPositive(tfQ.getText(), "Q");

            if (!isPrime(p)) throw new IllegalArgumentException("P не простое число");
            if (!isPrime(q)) throw new IllegalArgumentException("Q не простое число");
            if (p.equals(q)) throw new IllegalArgumentException("P и Q не должны совпадать");

            BigInteger n = p.multiply(q);
            BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

            // Если пользователь ввёл e — проверим корректность. Если пусто — подберём сами.
            BigInteger eVal;
            String eText = tfE.getText().trim();
            if (!eText.isEmpty()) {
                eVal = readPositive(eText, "e");
                if (eVal.compareTo(BigInteger.ONE) <= 0 || eVal.compareTo(phi) >= 0)
                    throw new IllegalArgumentException("e должно удовлетворять 1 < e < φ(N)");
                if (!eVal.gcd(phi).equals(BigInteger.ONE))
                    throw new IllegalArgumentException("e не взаимно просто с φ(N)");
            } else {
                eVal = pickRandomE(phi);
            }

            BigInteger dVal = modInverse(eVal, phi);

            tfN.setText(n.toString());
            tfPhi.setText(phi.toString());
            tfE.setText(eVal.toString());
            tfD.setText(dVal.toString());

            log("Генерация ключей завершена:\n" +
                    "  P=" + p + ", Q=" + q + "\n" +
                    "  N=" + n + ", φ(N)=" + phi + "\n" +
                    "  e=" + eVal + ", d=" + dVal + "\n");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void onEncrypt(ActionEvent ev) {
        try {
            RSAParams params = readParams();
            validateAlphabet(params.alphabet);

            String text = taPlain.getText();
            if (text == null) text = "";

            List<BigInteger> cipher = new ArrayList<>();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                int code = params.alphabet.indexOf(ch);
                if (code < 0)
                    throw new IllegalArgumentException("Символ '" + ch + "' отсутствует в алфавите");
                BigInteger m = BigInteger.valueOf(code);
                if (m.compareTo(params.n) >= 0)
                    throw new IllegalArgumentException("Код символа >= N. Увеличьте N или сократите алфавит");
                BigInteger ciph = powMod(m, params.e, params.n);
                cipher.add(ciph);
            }
            taCipher.setText(join(cipher));
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void onDecrypt(ActionEvent ev) {
        try {
            RSAParams params = readParams();
            validateAlphabet(params.alphabet);

            String line = taCipher.getText();
            if (line == null || line.trim().isEmpty()) {
                taDecrypted.setText("");
                return;
            }

            List<BigInteger> nums = parseNumbers(line);
            StringBuilder sb = new StringBuilder();
            for (BigInteger c : nums) {
                if (c.signum() < 0 || c.compareTo(params.n) >= 0)
                    throw new IllegalArgumentException("Число шифртекста вне диапазона [0, N)");
                BigInteger m = powMod(c, params.d, params.n);
                int idx = m.intValueExact();
                if (idx < 0 || idx >= params.alphabet.length())
                    throw new IllegalArgumentException("Получен код вне диапазона алфавита: " + idx);
                sb.append(params.alphabet.charAt(idx));
            }
            taDecrypted.setText(sb.toString());
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void recalcKeyFields(boolean verbose) {
        try {
            BigInteger p = readPositive(tfP.getText(), "P");
            BigInteger q = readPositive(tfQ.getText(), "Q");
            BigInteger n = p.multiply(q);
            BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            tfN.setText(n.toString());
            tfPhi.setText(phi.toString());
            if (verbose) log("Пересчёт: N=" + n + ", φ(N)=" + phi + "\n");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void validateCurrentKeys(boolean verbose) {
        try {
            RSAParams params = readParams();
            // Проверим взаимную простоту e и φ, а также корректность d как обратного к e по модулю φ
            if (!params.e.gcd(params.phi).equals(BigInteger.ONE))
                throw new IllegalArgumentException("gcd(e, φ(N)) != 1");
            BigInteger check = params.e.multiply(params.d).mod(params.phi);
            if (!check.equals(BigInteger.ONE))
                throw new IllegalArgumentException("e*d mod φ(N) != 1 (d не является мультипликативной обратной к e)");
            if (verbose) log("Ключи корректны: (e,n)=(" + params.e + "," + params.n + ") и d=" + params.d + "\n");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    // ------- Утилиты RSA -------

    private static BigInteger powMod(BigInteger a, BigInteger e, BigInteger n) {
        return a.modPow(e, n);
    }

    private static BigInteger modInverse(BigInteger a, BigInteger m) {
        return a.modInverse(m);
    }

    private BigInteger pickRandomE(BigInteger phi) {
        // Попробуем популярные варианты: 65537, 17, 3 — если подходят
        int[] common = {65537, 17, 3, 5, 7, 11, 13};
        for (int v : common) {
            BigInteger cand = BigInteger.valueOf(v);
            if (cand.compareTo(BigInteger.ONE) > 0 && cand.compareTo(phi) < 0 && cand.gcd(phi).equals(BigInteger.ONE))
                return cand;
        }
        // Иначе случайный перебор
        for (int i = 0; i < 10_000; i++) {
            BigInteger cand = new BigInteger(phi.bitLength(), rnd);
            if (cand.compareTo(BigInteger.TWO) < 0 || cand.compareTo(phi.subtract(BigInteger.ONE)) >= 0) continue;
            if (cand.gcd(phi).equals(BigInteger.ONE)) return cand;
        }
        throw new IllegalStateException("Не удалось подобрать e");
    }

    private static boolean isPrime(BigInteger n) {
        // Для учебных целей — достаточно isProbablePrime(20)
        return n != null && n.compareTo(BigInteger.TWO) >= 0 && n.isProbablePrime(20);
    }

    private static BigInteger readPositive(String s, String name) {
        try {
            BigInteger v = new BigInteger(s.trim());
            if (v.signum() <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            throw new IllegalArgumentException(name + ": введите положительное целое число");
        }
    }

    private static List<BigInteger> parseNumbers(String s) {
        List<BigInteger> res = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s, " ,;\n\t");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            res.add(new BigInteger(tok));
        }
        return res;
    }

    private static String join(List<BigInteger> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private void validateAlphabet(String alphabet) {
        if (alphabet == null || alphabet.isEmpty())
            throw new IllegalArgumentException("Алфавит не задан");
        // Проверим повторы символов
        for (int i = 0; i < alphabet.length(); i++) {
            char ch = alphabet.charAt(i);
            if (alphabet.indexOf(ch) != alphabet.lastIndexOf(ch))
                throw new IllegalArgumentException("Алфавит содержит повторяющийся символ: '" + ch + "'");
        }
        // Проверим, что |алфавит| <= N (иначе часть кодов будет >= N)
        BigInteger n = new BigInteger(Objects.requireNonNullElse(tfN.getText().trim(), "0"));
        if (n.signum() <= 0) throw new IllegalArgumentException("N не вычислен. Пересчитайте N");
        if (BigInteger.valueOf(alphabet.length()).compareTo(n) > 0)
            throw new IllegalArgumentException("Размер алфавита должен быть \u2264 N (сейчас |A|=" + alphabet.length() + ", N=" + n + ")");
    }

    private RSAParams readParams() {
        BigInteger p = readPositive(tfP.getText(), "P");
        BigInteger q = readPositive(tfQ.getText(), "Q");
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = readPositive(tfE.getText(), "e");
        BigInteger d = readPositive(tfD.getText(), "d");
        return new RSAParams(n, phi, e, d, tfAlphabet.getText());
    }

    private void log(String msg) {
        taLog.append(msg);
        if (!msg.endsWith("\n")) taLog.append("\n");
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
        log("[ОШИБКА] " + msg + "\n");
    }

    // ------- Вспомогательная верстка -------

    private static JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private static void addRow(JPanel parent, GridBagConstraints c, JComponent left, JComponent right) {
        int row = parent.getComponentCount();
        c.gridx = 0; c.gridy = row; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        parent.add(left, c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        parent.add(right, c);
    }

    private static JComponent titled(JComponent inner, String title) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createTitledBorder(title));
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    // ------- Модель параметров RSA -------
    private record RSAParams(BigInteger n, BigInteger phi, BigInteger e, BigInteger d, String alphabet) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RsaSwingApp().setVisible(true));
    }
}
