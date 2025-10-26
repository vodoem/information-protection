package ru.rsreu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigInteger;

class KeysPanel extends JPanel {
    private final JTextField pField = new JTextField("2");
    private final JTextField qField = new JTextField("11");
    private final JTextField nField = new JTextField();
    private final JTextField phiField = new JTextField();
    private final JTextField eField = new JTextField("3");
    private final JTextField dField = new JTextField("7");
    private final JTextField alphabetField = new JTextField("1234567890");
    private final JTextArea logArea = new JTextArea(7, 20);

    private final RsaService service;
    private final JFrame owner;

    KeysPanel(JFrame owner, RsaService service) {
        super(new GridBagLayout());
        this.owner = owner;
        this.service = service;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUi();
        recalcKeyFields(false);
    }

    private void buildUi() {
        GridBagConstraints c = baseConstraints();

        JPanel input = wrap(buildInputs(), "Входные данные");
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        add(input, c);

        JPanel calculated = wrap(buildCalculated(), "Вычисленные параметры");
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.5;
        add(calculated, c);

        JPanel keys = wrap(buildKeys(), "Ключи");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.5;
        add(keys, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton recalc = new JButton("Пересчитать N и φ(N)");
        recalc.addActionListener(e -> recalcKeyFields(true));
        JButton generate = new JButton("Сгенерировать e,d");
        generate.addActionListener(e -> generateKeys());
        JButton validate = new JButton("Проверить ключи");
        validate.addActionListener(e -> validateKeys(true));
        actions.add(recalc);
        actions.add(generate);
        actions.add(validate);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(actions, c);

        logArea.setEditable(false);
        JScrollPane logPane = new JScrollPane(logArea);
        logPane.setBorder(BorderFactory.createTitledBorder("Лог генерации / проверок"));
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(logPane, c);
    }

    private JPanel buildInputs() {
        JPanel panel = new JPanel(new GridBagLayout());
        addRow(panel, new JLabel("P:"), pField);
        addRow(panel, new JLabel("Q:"), qField);
        addRow(panel, new JLabel("Исходный алфавит:"), alphabetField);
        return panel;
    }

    private JPanel buildCalculated() {
        JPanel panel = new JPanel(new GridBagLayout());
        nField.setEditable(false);
        phiField.setEditable(false);
        addRow(panel, new JLabel("N = P * Q:"), nField);
        addRow(panel, new JLabel("φ(N) = (P-1)(Q-1):"), phiField);
        return panel;
    }

    private JPanel buildKeys() {
        JPanel panel = new JPanel(new GridBagLayout());
        addRow(panel, new JLabel("Открытый ключ e:"), eField);
        addRow(panel, new JLabel("Закрытый ключ d:"), dField);
        return panel;
    }

    private void addRow(JPanel panel, JComponent left, JComponent right) {
        GridBagConstraints c = baseConstraints();
        int row = panel.getComponentCount();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(left, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(right, c);
    }

    private JPanel wrap(JComponent inner, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    void generateKeys() {
        try {
            BigInteger p = service.readPositive(pField.getText(), "P");
            BigInteger q = service.readPositive(qField.getText(), "Q");
            service.ensurePrime(p, "P");
            service.ensurePrime(q, "Q");
            service.ensureDistinct(p, q);

            BigInteger n = service.calculateN(p, q);
            BigInteger phi = service.calculatePhi(p, q);

            BigInteger e;
            String eText = eField.getText().trim();
            if (!eText.isEmpty()) {
                e = service.readPositive(eText, "e");
                if (!service.isValidExponent(e, phi)) {
                    throw new IllegalArgumentException("e должно удовлетворять 1 < e < φ(N) и gcd(e, φ(N)) = 1");
                }
            } else {
                e = service.pickPublicExponent(phi);
            }

            BigInteger d = service.inverse(e, phi);

            nField.setText(n.toString());
            phiField.setText(phi.toString());
            eField.setText(e.toString());
            dField.setText(d.toString());
            appendLog("Генерация ключей завершена:\n  P=" + p + ", Q=" + q + "\n  N=" + n + ", φ(N)=" + phi + "\n  e=" + e + ", d=" + d + "\n");
        } catch (Exception ex) {
            handleError(ex.getMessage());
        }
    }

    void recalcKeyFields(boolean verbose) {
        try {
            BigInteger p = service.readPositive(pField.getText(), "P");
            BigInteger q = service.readPositive(qField.getText(), "Q");
            BigInteger n = service.calculateN(p, q);
            BigInteger phi = service.calculatePhi(p, q);
            nField.setText(n.toString());
            phiField.setText(phi.toString());
            if (verbose) {
                appendLog("Пересчёт: N=" + n + ", φ(N)=" + phi + "\n");
            }
        } catch (Exception ex) {
            handleError(ex.getMessage());
        }
    }

    void validateKeys(boolean verbose) {
        try {
            RSAParams params = readParams();
            if (!params.e().gcd(params.phi()).equals(BigInteger.ONE)) {
                throw new IllegalArgumentException("gcd(e, φ(N)) != 1");
            }
            BigInteger check = params.e().multiply(params.d()).mod(params.phi());
            if (!check.equals(BigInteger.ONE)) {
                throw new IllegalArgumentException("e*d mod φ(N) != 1 (d не является мультипликативной обратной к e)");
            }
            if (verbose) {
                appendLog("Ключи корректны: (e,n)=(" + params.e() + "," + params.n() + ") и d=" + params.d() + "\n");
            }
        } catch (Exception ex) {
            handleError(ex.getMessage());
        }
    }

    RSAParams readParams() {
        BigInteger p = service.readPositive(pField.getText(), "P");
        BigInteger q = service.readPositive(qField.getText(), "Q");
        BigInteger n = service.calculateN(p, q);
        BigInteger phi = service.calculatePhi(p, q);
        BigInteger e = service.readPositive(eField.getText(), "e");
        BigInteger d = service.readPositive(dField.getText(), "d");
        return new RSAParams(n, phi, e, d);
    }

    String getAlphabet() {
        return alphabetField.getText();
    }

    BigInteger getN() {
        String value = nField.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("N не вычислен. Пересчитайте N");
        }
        return new BigInteger(value);
    }

    void appendLog(String message) {
        logArea.append(message);
        if (!message.endsWith("\n")) {
            logArea.append("\n");
        }
    }

    void handleError(String message) {
        UiUtils.showError(owner, message);
        appendLog("[ОШИБКА] " + message + "\n");
    }
}
