package com.deepseek;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ClassNameGeneratorAction extends AnAction {

    private final ProgressManager progressManager = new ProgressManager();

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabled(editor != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        if (editor == null || project == null) return;

        CaretModel caretModel = editor.getCaretModel();
        String selectedText = caretModel.getCurrentCaret().getSelectedText();

        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showWarningDialog(project, "Please select some text first", "No Text Selected");
            return;
        }

        String apiKey = getApiKey(project);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return;
        }

        progressManager.showDialog(project, "Generating class name...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                DeepSeekClient client = new DeepSeekClient(apiKey);
                String className = client.generateClassName(selectedText);

                SwingUtilities.invokeLater(() -> {
                    progressManager.hideDialog();
                    handleClassNameResult(project, editor, className);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progressManager.hideDialog();
                    Messages.showErrorDialog(project, "Failed to generate class name: " + ex.getMessage(), "Error");
                });
            }
        });
    }

    private String getApiKey(Project project) {

        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {

            if (SwingUtilities.isEventDispatchThread()) {
                return Messages.showInputDialog(
                        project,
                        "Please enter your DeepSeek API key:",
                        "API Key Configuration",
                        Messages.getQuestionIcon()
                );
            } else {
                final String[] result = new String[1];
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        result[0] = Messages.showInputDialog(
                                project,
                                "Please enter your DeepSeek API key:",
                                "API Key Configuration",
                                Messages.getQuestionIcon()
                        );
                    });
                    return result[0];
                } catch (Exception e) {
                    Messages.showErrorDialog(project, "Failed to get API key: " + e.getMessage(), "Error");
                    return null;
                }
            }
        }
        return apiKey;
    }

    private void handleClassNameResult(Project project, Editor editor, String className) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handleClassNameResult(project, editor, className));
            return;
        }

        String[] options = {"Insert at current position", "Cancel"};
        int result = Messages.showDialog(
                project,
                "Generated class name: " + className + "\n\nPlease choose an action:",
                "Class Name Generated",
                options, 0,
                Messages.getInformationIcon()
        );

        if (result == 0) {
            insertClassName(project, editor, className);
        }
    }

    private void insertClassName(Project project, Editor editor, String className) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            CaretModel caretModel = editor.getCaretModel();
            int offset = caretModel.getOffset();
            document.insertString(offset, className);
        });
    }
}