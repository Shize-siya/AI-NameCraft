package com.deepseek;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class ProgressManager {
    private JDialog progressDialog;

    public void showDialog(Project project, String message) {
        SwingUtilities.invokeLater(() -> {
            progressDialog = new JDialog();
            progressDialog.setTitle("Please Wait");
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JPanel panel = new JPanel(new FlowLayout());
            panel.add(new JLabel(message));
            panel.add(Box.createHorizontalStrut(10));

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar);

            progressDialog.setContentPane(panel);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(null);
            progressDialog.setModal(true);
            progressDialog.setVisible(true);
        });
    }

    public void hideDialog() {
        SwingUtilities.invokeLater(() -> {
            if (progressDialog != null) {
                progressDialog.dispose();
                progressDialog = null;
            }
        });
    }
}