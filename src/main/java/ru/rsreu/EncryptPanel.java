package ru.rsreu;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.math.BigInteger;
import java.util.List;

class EncryptPanel extends JPanel {
    private final JTextArea plainArea = new JTextArea(6, 40);
    private final JTextArea cipherArea = new JTextArea(6, 40);

    private final JFrame owner;
    private final KeysPanel keysPanel;
    private final RsaService service;

    EncryptPanel(JFrame owner, KeysPanel keysPanel, RsaService service, Document cipherDocument) {
        super(new GridBagLayout());
        this.owner = owner;
        this.keysPanel = keysPanel;
        this.service = service;
        cipherArea.setDocument(cipherDocument);
        buildUi();
    }

    private void buildUi() {
        GridBagConstraints c = baseConstraints();

        plainArea.setLineWrap(true);
        plainArea.setWrapStyleWord(true);
        JScrollPane plainScroll = new JScrollPane(plainArea);
        plainScroll.setBorder(BorderFactory.createTitledBorder("Исходный текст"));
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        add(plainScroll, c);

        cipherArea.setLineWrap(true);
        cipherArea.setWrapStyleWord(true);
        JScrollPane cipherScroll = new JScrollPane(cipherArea);
        cipherScroll.setBorder(BorderFactory.createTitledBorder("Зашифрованные числа (через пробел)"));
        c.gridy = 1;
        add(cipherScroll, c);

        JButton encrypt = new JButton("Шифровать");
        encrypt.addActionListener(e -> encrypt());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(encrypt);
        c.gridy = 2;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(actions, c);
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    private void encrypt() {
        try {
            RSAParams params = keysPanel.readParams();
            BigInteger n = keysPanel.getN();
            String alphabet = keysPanel.getAlphabet();
            service.validateAlphabet(alphabet, n);
            List<BigInteger> cipher = service.encrypt(plainArea.getText(), params, alphabet);
            cipherArea.setText(service.join(cipher));
        } catch (Exception ex) {
            UiUtils.showError(owner, ex.getMessage());
            keysPanel.appendLog("[ОШИБКА] " + ex.getMessage() + "\n");
        }
    }
}
