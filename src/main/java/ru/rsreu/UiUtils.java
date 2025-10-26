package ru.rsreu;

import javax.swing.*;
import java.awt.*;

public final class UiUtils {
    private UiUtils() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
