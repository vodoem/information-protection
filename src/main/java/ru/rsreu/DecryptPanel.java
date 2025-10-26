package ru.rsreu;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.math.BigInteger;
import java.util.List;

class DecryptPanel extends JPanel {
    private final JTextArea cipherArea = new JTextArea(6, 40);
    private final JTextArea plainArea = new JTextArea(6, 40);

    private final JFrame owner;
    private final KeysPanel keysPanel;
    private final RsaService service;

    DecryptPanel(JFrame owner, KeysPanel keysPanel, RsaService service, Document cipherDocument) {
        super(new GridBagLayout());
        this.owner = owner;
        this.keysPanel = keysPanel;
        this.service = service;
        cipherArea.setDocument(cipherDocument);
        buildUi();
    }

    private void buildUi() {
        GridBagConstraints c = baseConstraints();

        JScrollPane cipherScroll = new JScrollPane(cipherArea);
        cipherScroll.setBorder(BorderFactory.createTitledBorder("Входные зашифрованные числа (через пробел)"));
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        add(cipherScroll, c);

        plainArea.setLineWrap(true);
        plainArea.setWrapStyleWord(true);
        JScrollPane plainScroll = new JScrollPane(plainArea);
        plainScroll.setBorder(BorderFactory.createTitledBorder("Расшифрованный текст"));
        c.gridy = 1;
        add(plainScroll, c);

        JButton decrypt = new JButton("Дешифровать");
        decrypt.addActionListener(e -> decrypt());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(decrypt);
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

    private void decrypt() {
        try {
            String raw = cipherArea.getText();
            if (raw == null || raw.trim().isEmpty()) {
                plainArea.setText("");
                return;
            }
            RSAParams params = keysPanel.readParams();
            BigInteger n = keysPanel.getN();
            String alphabet = keysPanel.getAlphabet();
            service.validateAlphabet(alphabet, n);
            List<BigInteger> numbers = service.parseNumbers(raw);
            plainArea.setText(service.decrypt(numbers, params, alphabet));
        } catch (Exception ex) {
            UiUtils.showError(owner, ex.getMessage());
            keysPanel.appendLog("[ОШИБКА] " + ex.getMessage() + "\n");
        }
    }
}
