package com.github.mikolajed.intellijgitplugin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class RenameCommitActionTest {
    private RenameCommitAction action;
    private Project project;
    private AnActionEvent event;
    private GitRepositoryManager gitManager;
    private Git git;
    private GitRepository repo;
    private Presentation presentation;

    @BeforeEach
    void setUp() {
        action = new RenameCommitAction();
        project = mock(Project.class);
        event = mock(AnActionEvent.class);
        gitManager = mock(GitRepositoryManager.class);
        git = mock(Git.class);
        repo = mock(GitRepository.class);
        presentation = mock(Presentation.class);

        when(event.getProject()).thenReturn(project);
        when(event.getData(CommonDataKeys.PROJECT)).thenReturn(project);
        when(event.getPresentation()).thenReturn(presentation);
        when(project.getService(GitRepositoryManager.class)).thenReturn(gitManager);
        when(Git.getInstance()).thenReturn(git);
    }

    @Test
    void update_noGitRepo_disablesAction() {
        when(gitManager.getRepositories()).thenReturn(Collections.emptyList());
        action.update(event);
        verify(presentation).setEnabledAndVisible(false);
    }

    @Test
    void update_withGitRepo_enablesAction() {
        when(gitManager.getRepositories()).thenReturn(List.of(repo));
        action.update(event);
        verify(presentation).setEnabledAndVisible(true);
    }

    @Test
    void actionPerformed_sameMessage_skipsAmend() {
        try (var mockedStatic = mockStatic(Messages.class)) {
            when(gitManager.getRepositories()).thenReturn(List.of(repo));
            GitLineHandler logHandler = mock(GitLineHandler.class);
            GitCommandResult logResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(logResult);
            when(logResult.success()).thenReturn(true);
            when(logResult.getOutputAsJoinedString()).thenReturn("Test\nDetails");
            when(Messages.showMultilineInputDialog(eq(project), anyString(), eq("Rename Commit"), eq("Test\nDetails"), any(), any()))
                    .thenReturn("Test\nDetails");

            action.actionPerformed(event);

            mockedStatic.verify(() ->
                    Messages.showInfoMessage("New message is the same as the current one—no changes made.", "No Update"));
            verify(git, never()).runCommand(any(GitLineHandler.class));
        }
    }

    @Test
    void actionPerformed_newMessage_amendsSuccessfully() {
        try (var mockedStatic = mockStatic(Messages.class)) {
            when(gitManager.getRepositories()).thenReturn(List.of(repo));
            GitLineHandler logHandler = mock(GitLineHandler.class);
            GitCommandResult logResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(logResult).thenReturn(mock(GitCommandResult.class));
            when(logResult.success()).thenReturn(true);
            when(logResult.getOutputAsJoinedString()).thenReturn("Old\nStuff");
            when(Messages.showMultilineInputDialog(eq(project), anyString(), eq("Rename Commit"), eq("Old\nStuff"), any(), any()))
                    .thenReturn("New\nStuff");

            RenameCommitAction spyAction = spy(action);
            ArgumentCaptor<Task.Backgroundable> taskCaptor = ArgumentCaptor.forClass(Task.Backgroundable.class);
            doNothing().when(spyAction).queueTask(any());

            spyAction.actionPerformed(event);

            verify(spyAction).queueTask(taskCaptor.capture());
            Task.Backgroundable task = taskCaptor.getValue();

            GitCommandResult amendResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(amendResult);
            when(amendResult.success()).thenReturn(true);

            task.run(mock(ProgressIndicator.class));
            task.onSuccess();

            mockedStatic.verify(() ->
                    Messages.showInfoMessage("<b>Commit message updated to:</b>\nNew\nStuff", "Success"));
        }
    }

    @Test
    void actionPerformed_amendFails_showsError() {
        try (var mockedStatic = mockStatic(Messages.class)) {
            when(gitManager.getRepositories()).thenReturn(List.of(repo));
            GitLineHandler logHandler = mock(GitLineHandler.class);
            GitCommandResult logResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(logResult).thenReturn(mock(GitCommandResult.class));
            when(logResult.success()).thenReturn(true);
            when(logResult.getOutputAsJoinedString()).thenReturn("Old\nStuff");
            when(Messages.showMultilineInputDialog(eq(project), anyString(), eq("Rename Commit"), eq("Old\nStuff"), any(), any()))
                    .thenReturn("New\nStuff");

            RenameCommitAction spyAction = spy(action);
            ArgumentCaptor<Task.Backgroundable> taskCaptor = ArgumentCaptor.forClass(Task.Backgroundable.class);
            doNothing().when(spyAction).queueTask(any());

            spyAction.actionPerformed(event);

            verify(spyAction).queueTask(taskCaptor.capture());
            Task.Backgroundable task = taskCaptor.getValue();

            GitCommandResult amendResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(amendResult);
            when(amendResult.success()).thenReturn(false);
            when(amendResult.getErrorOutputAsJoinedString()).thenReturn("Something went wrong");

            task.run(mock(ProgressIndicator.class));
            task.onSuccess();

            mockedStatic.verify(() ->
                    Messages.showErrorDialog("Failed to rename commit: Something went wrong", "Error"));
        }
    }

    @Test
    void actionPerformed_emptyMessage_loopsUntilValid() {
        try (var mockedStatic = mockStatic(Messages.class)) {
            when(gitManager.getRepositories()).thenReturn(List.of(repo));
            GitLineHandler logHandler = mock(GitLineHandler.class);
            GitCommandResult logResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(logResult).thenReturn(mock(GitCommandResult.class));
            when(logResult.success()).thenReturn(true);
            when(logResult.getOutputAsJoinedString()).thenReturn("Original\nMessage");
            when(Messages.showMultilineInputDialog(eq(project), anyString(), eq("Rename Commit"), anyString(), any(), any()))
                    .thenReturn("") // First: empty
                    .thenReturn(" ") // Second: whitespace
                    .thenReturn("New\nMessage"); // Third: valid

            RenameCommitAction spyAction = spy(action);
            ArgumentCaptor<Task.Backgroundable> taskCaptor = ArgumentCaptor.forClass(Task.Backgroundable.class);
            doNothing().when(spyAction).queueTask(any());

            spyAction.actionPerformed(event);

            mockedStatic.verify(() ->
                    Messages.showWarningDialog("Commit message cannot be empty.", "Invalid Input"), times(2));
            verify(spyAction).queueTask(taskCaptor.capture());
            Task.Backgroundable task = taskCaptor.getValue();

            GitCommandResult amendResult = mock(GitCommandResult.class);
            when(git.runCommand(any(GitLineHandler.class))).thenReturn(amendResult);
            when(amendResult.success()).thenReturn(true);

            task.run(mock(ProgressIndicator.class));
            task.onSuccess();

            mockedStatic.verify(() ->
                    Messages.showInfoMessage("<b>Commit message updated to:</b>\nNew\nMessage", "Success"));
        }
    }
}