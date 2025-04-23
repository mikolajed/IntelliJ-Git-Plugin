package com.github.mikolajed.intellijgitplugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

public class RenameCommitAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        GitRepositoryManager gitManager = project != null ? GitRepositoryManager.getInstance(project) : null;
        boolean hasGitRepo = gitManager != null && !gitManager.getRepositories().isEmpty();
        e.getPresentation().setEnabledAndVisible(hasGitRepo);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        GitRepositoryManager gitManager = GitRepositoryManager.getInstance(project);
        if (gitManager.getRepositories().isEmpty()) return;

        queueTask(new Task.Backgroundable(project, "Fetching last commit message", false) {
            GitCommandResult logResult;
            String currentMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitLineHandler logHandler = new GitLineHandler(project, gitManager.getRepositories().get(0).getRoot(), GitCommand.LOG);
                logHandler.addParameters("-1", "--pretty=%B");
                logResult = Git.getInstance().runCommand(logHandler);
                currentMessage = logResult.success() ? logResult.getOutputAsJoinedString().trim() : "";
            }

            @Override
            public void onSuccess() {
                String newMessage = null;
                while (newMessage == null || newMessage.trim().isEmpty()) {
                    newMessage = Messages.showMultilineInputDialog(project,
                            "Enter new commit message (use Enter for multiple lines):",
                            "Rename Commit",
                            newMessage == null ? currentMessage : newMessage,
                            Messages.getQuestionIcon(),
                            null);
                    if (newMessage == null) return; // User canceled
                    if (newMessage.trim().isEmpty()) {
                        Messages.showWarningDialog("Commit message cannot be empty.", "Invalid Input");
                    }
                }

                if (newMessage.trim().equals(currentMessage.trim())) {
                    Messages.showInfoMessage("New message is the same as the current one—no changes made.", "No Update");
                    return;
                }

                final String finalNewMessage = newMessage;
                queueTask(new Task.Backgroundable(project, "Renaming commit", true) {
                    GitCommandResult result;

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        GitLineHandler handler = new GitLineHandler(project, gitManager.getRepositories().get(0).getRoot(), GitCommand.COMMIT);
                        handler.addParameters("--amend", "-m", finalNewMessage);
                        result = Git.getInstance().runCommand(handler);
                    }

                    @Override
                    public void onSuccess() {
                        if (result != null && result.success()) {
                            Messages.showInfoMessage("<b>Commit message updated to:</b>\n" + finalNewMessage, "Success");
                        } else {
                            Messages.showErrorDialog("Failed to rename commit: " + (result != null ? result.getErrorOutputAsJoinedString() : "Unknown error"), "Error");
                        }
                    }
                });
            }
        });
    }

    public void queueTask(Task.Backgroundable task) {
        task.queue();
    }
}