/*
 * Copyright (c) 2017 Patrick Scheibe, Dmitry Kashin, Athiele.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.halirutan.keypromoterx;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog;
import com.intellij.openapi.ui.DialogWrapper;
import de.halirutan.keypromoterx.statistic.KeyPromoterStatistics;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

/**
 * A custom notification class that allows for creating 1. tips if a short cut was missed and 2. a balloon asking if
 * the user wants to create a shortcut for an action that doesn't have one.
 *
 * @author Patrick Scheibe.
 */
public class KeyPromoterNotification {

  private static final NotificationGroup GROUP = NotificationGroupManager.getInstance().getNotificationGroup(
      KeyPromoterBundle.message("kp.notification.group")
  );

  public static void showStartupNotification() {
    final Notification notification = GROUP.createNotification(KeyPromoterBundle.message(
                "kp.notification.group"),
            KeyPromoterBundle.message("kp.notification.startup"),
            NotificationType.INFORMATION)
        .setImportant(true)
        .setImportantSuggestion(true)
        .setIcon(KeyPromoterIcons.KP_ICON)
        .addAction(new BrowseNotificationAction(
            KeyPromoterBundle.message("kp.notification.startup.link.name"),
            KeyPromoterBundle.message("kp.notification.startup.link"))
        );
    notification.notify(null);
  }

  static void showTip(KeyPromoterAction action, int count, ShowMode mode) {
    String title = "";
    switch (mode) {
      case NOTIFICATION -> title = KeyPromoterBundle.message("kp.notification.tip.title", action.getDescription());
      case DIALOG -> title = KeyPromoterBundle.message("kp.notification.group");
    }
    mode.showTip(title, action, count);
  }

  static void askToCreateShortcut(KeyPromoterAction action, ShowMode mode) {
    String title = KeyPromoterBundle.message("kp.notification.group");
    String message = KeyPromoterBundle.message("kp.notification.ask.new.shortcut", action.getDescription());

    mode.askToCreateShortcut(title, message, action);
  }

  /**
   * Provides click-able links to IDEA actions. On click, the keymap editor is opened showing the exact line where
   * the shortcut of an action can be edited/created.
   */
  private static class EditKeymapAction extends NotificationAction {
    private final KeyPromoterAction myAction;

    EditKeymapAction(KeyPromoterAction action) {
      super(action.getDescription());
      this.myAction = action;
    }

    EditKeymapAction(KeyPromoterAction action, String buttonText) {
      super(buttonText);
      this.myAction = action;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
      EditKeymapsDialog dialog = new EditKeymapsDialog(null, myAction.getIdeaActionID());
      getApplication().invokeLater(dialog::show);
    }
  }

  private static class SuppressTipAction extends NotificationAction {
    private final KeyPromoterStatistics statistics = getApplication().getService(KeyPromoterStatistics.class);
    private final KeyPromoterAction myAction;

    SuppressTipAction(KeyPromoterAction action) {
      super(KeyPromoterBundle.message("kp.notification.disable.message"));
      myAction = action;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
      statistics.suppressItem(myAction);
      notification.expire();
    }
  }

  public enum ShowMode {
    NOTIFICATION {
      @Override
      public void showTip(String title, KeyPromoterAction action, int count) {
        String countMessage = count > 1 ? count + " times" : count + " time";
        String message = KeyPromoterBundle.message("kp.notification.tip.message", action.getShortcut(), countMessage);

        Notification notification = GROUP.createNotification(title, message, NotificationType.INFORMATION)
            .setIcon(KeyPromoterIcons.KP_ICON)
            .addAction(new EditKeymapAction(action, KeyPromoterBundle.message("kp.notification.edit.shortcut")))
            .addAction(new SuppressTipAction(action));
        notification.notify(null);
      }
    },
    DIALOG {
      @Override
      public void showTip(String title, KeyPromoterAction action, int count) {
        String message = KeyPromoterBundle.message("kp.dialog.tip", action.getDescription(), count, action.getShortcut());

        DialogWrapper dialog = new KeyPromoterDialog(title, message, action);
        getApplication().invokeLater(dialog::show);
      }
    };

    public static ShowMode showModeFromSettings(KeyPromoterSettings settings) {
      return settings.hardMode ? DIALOG : NOTIFICATION;
    }

    public abstract void showTip(String title, KeyPromoterAction action, int count);

    public void askToCreateShortcut(String title, String message, KeyPromoterAction action) {
      Notification notification = GROUP.createNotification(title, message, NotificationType.INFORMATION)
          .setIcon(KeyPromoterIcons.KP_ICON)
          .addAction(new EditKeymapAction(action))
          .addAction(new SuppressTipAction(action));
      notification.notify(null);
    }
  }
}
