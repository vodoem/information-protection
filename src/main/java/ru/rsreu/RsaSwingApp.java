package ru.rsreu;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;

public class RsaSwingApp extends JFrame {
    public RsaSwingApp() {
        super("RSA — лабораторная (AWT/Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        RsaService service = new RsaService();
        KeysPanel keysPanel = new KeysPanel(this, service);
        Document cipherDocument = service.createCipherDocument();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1) Ключи", keysPanel);
        tabs.addTab("2) Шифрование", new EncryptPanel(this, keysPanel, service, cipherDocument));
        tabs.addTab("3) Дешифрование", new DecryptPanel(this, keysPanel, service, cipherDocument));

        getContentPane().add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RsaSwingApp().setVisible(true));
    }
}
