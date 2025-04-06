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

        String newMessage = Messages.showInputDialog(project,
                "Enter new commit message:",
                "Rename Commit",
                Messages.getQuestionIcon());
        if (newMessage != null && !newMessage.trim().isEmpty()) {
            new Task.Backgroundable(project, "Renaming Commit", true) {
                GitCommandResult result;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    GitLineHandler handler = new GitLineHandler(project, gitManager.getRepositories().get(0).getRoot(), GitCommand.COMMIT);
                    handler.addParameters("--amend", "-m", newMessage);
                    result = Git.getInstance().runCommand(handler);
                }

                @Override
                public void onSuccess() {
                    if (result != null && result.success()) {
                        Messages.showInfoMessage("Commit message updated to: " + newMessage, "Success");
                    } else {
                        Messages.showErrorDialog("Failed to rename commit: " + (result != null ? result.getErrorOutputAsJoinedString() : "Unknown error"), "Error");
                    }
                }
            }.queue();
        }
    }
}