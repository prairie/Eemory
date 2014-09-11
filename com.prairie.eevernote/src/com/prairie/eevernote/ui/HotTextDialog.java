package com.prairie.eevernote.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.evernote.edam.error.EDAMUserException;
import com.prairie.eevernote.Constants;
import com.prairie.eevernote.EEProperties;
import com.prairie.eevernote.client.EEClipper;
import com.prairie.eevernote.client.EEClipperFactory;
import com.prairie.eevernote.client.ENNote;
import com.prairie.eevernote.client.impl.ENNoteImpl;
import com.prairie.eevernote.exception.EDAMUserExceptionHandler;
import com.prairie.eevernote.util.ConstantsUtil;
import com.prairie.eevernote.util.EclipseUtil;
import com.prairie.eevernote.util.IDialogSettingsUtil;
import com.prairie.eevernote.util.ListUtil;
import com.prairie.eevernote.util.LogUtil;
import com.prairie.eevernote.util.MapUtil;
import com.prairie.eevernote.util.StringUtil;

public class HotTextDialog extends Dialog implements ConstantsUtil, Constants {

    public static final int SHOULD_NOT_SHOW = EECLIPPERPLUGIN_HOTINPUTDIALOG_SHOULD_NOT_SHOW_ID;

    private final Shell shell;
    private static HotTextDialog thisDialog;

    private EEClipper clipper;

    private Map<String, String> notebooks; // <Name, Guid>
    private Map<String, String> notes; // <Name, Guid>
    private List<String> tags;

    private SimpleContentProposalProvider noteProposalProvider;

    private Map<String, Text> fields;
    private ENNote quickSettings;
    // <Field Property, <Field Property, Field Value>>
    private Map<String, Map<String, String>> matrix;

    private boolean fatal = false;

    public HotTextDialog(final Shell parentShell) {
        super(parentShell);
        shell = parentShell;
        notebooks = MapUtil.map();
        notes = MapUtil.map();
        tags = ListUtil.list();
        clipper = EEClipperFactory.getInstance().getEEClipper();
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_HOTINPUTDIALOG_SHELL_TITLE));
    }

    @Override
    protected void setShellStyle(final int newShellStyle) {
        super.setShellStyle(newShellStyle | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        // container.setLayoutData(new GridData(GridData.FILL_BOTH));
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        // ------------

        if (shouldShow(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_GUID)) {

            Text notebookField = createLabelTextField(container, EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK);
            addField(EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK, notebookField);
            try {
                new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor) {
                        monitor.beginTask("Fetching notebooks...", IProgressMonitor.UNKNOWN);
                        try {
                            notebooks = clipper.listNotebooks();
                        } catch (Throwable e) {
                            // ignore, not fatal
                            LogUtil.logCancel(e);
                        }
                        monitor.done();
                    }
                });
                EclipseUtil.enableFilteringContentAssist(notebookField, notebooks.keySet().toArray(new String[notebooks.size()]));
            } catch (Throwable e) {
                MessageDialog.openError(shell, EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_CONFIGURATIONS_ERROROCCURRED), e.getLocalizedMessage());
            }
        }

        // ------------

        if (shouldShow(SETTINGS_SECTION_NOTE, SETTINGS_KEY_GUID)) {
            Text noteField = createLabelTextField(container, EECLIPPERPLUGIN_CONFIGURATIONS_NOTE);
            addField(EECLIPPERPLUGIN_CONFIGURATIONS_NOTE, noteField);
            final String notebook = getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK);
            try {
                new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor) {
                        monitor.beginTask("Fetching notes...", IProgressMonitor.UNKNOWN);
                        try {
                            notes = clipper.listNotesWithinNotebook(ENNoteImpl.forNotebookGuid(IDialogSettingsUtil.getBoolean(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_CHECKED) ? IDialogSettingsUtil.get(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_GUID) : notebooks.get(notebook)));
                        } catch (Throwable e) {
                            // ignore, not fatal
                            LogUtil.logCancel(e);
                        }
                        monitor.done();
                    }
                });
                noteProposalProvider = EclipseUtil.enableFilteringContentAssist(noteField, notes.keySet().toArray(new String[notes.size()]));
            } catch (Throwable e) {
                MessageDialog.openError(shell, EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_CONFIGURATIONS_ERROROCCURRED), e.getLocalizedMessage());
            }
            if (IDialogSettingsUtil.getBoolean(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_CHECKED)) {
                noteField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(final FocusEvent e) {
                        if (shouldRefresh(EECLIPPERPLUGIN_CONFIGURATIONS_NOTE, EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK)) {
                            final String hotebook = getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK);
                            BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        notes = clipper.listNotesWithinNotebook(ENNoteImpl.forNotebookGuid(notebooks.get(hotebook)));
                                    } catch (Throwable e) {
                                        // ignore, not fatal
                                        LogUtil.logCancel(e);
                                    }
                                }
                            });
                            String[] ns = notes.keySet().toArray(new String[notes.size()]);
                            Arrays.sort(ns);
                            noteProposalProvider.setProposals(ns);
                        }
                    }
                });
            }
        }

        // ------------

        if (shouldShow(SETTINGS_SECTION_TAGS, SETTINGS_KEY_NAME)) {
            Text tagsField = createLabelTextField(container, EECLIPPERPLUGIN_CONFIGURATIONS_TAGS);
            addField(EECLIPPERPLUGIN_CONFIGURATIONS_TAGS, tagsField);
            try {
                new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor) {
                        monitor.beginTask("Fetching tags...", IProgressMonitor.UNKNOWN);
                        try {
                            tags = clipper.listTags();
                        } catch (Throwable e) {
                            // ignore, not fatal
                            LogUtil.logCancel(e);
                        }
                        monitor.done();
                    }
                });
                EclipseUtil.enableFilteringContentAssist(tagsField, tags.toArray(new String[tags.size()]), TAGS_SEPARATOR);
            } catch (Throwable e) {
                MessageDialog.openError(shell, EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_CONFIGURATIONS_ERROROCCURRED), e.getLocalizedMessage());
            }
        }

        // ------------

        if (shouldShow(SETTINGS_SECTION_COMMENTS, SETTINGS_KEY_NAME)) {
            addField(EECLIPPERPLUGIN_CONFIGURATIONS_COMMENTS, createLabelTextField(container, EECLIPPERPLUGIN_CONFIGURATIONS_COMMENTS));
        }

        return container;
    }

    private void authInProgress() {
        // Auth
        try {
            new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) {
                    monitor.beginTask("Authenticating...", IProgressMonitor.UNKNOWN);
                    try {
                        clipper = EEClipperFactory.getInstance().getEEClipper(IDialogSettingsUtil.get(SETTINGS_KEY_TOKEN), false);
                    } catch (EDAMUserException e) {
                        fatal = true;
                        new EDAMUserExceptionHandler().handleDesingTime(shell, e);
                    } catch (Throwable e) {
                        // ignore, not fatal
                        LogUtil.logWarning(e);
                    }
                    monitor.done();
                }
            });
        } catch (Throwable e) {
            // ignore, not fatal
            LogUtil.logWarning(e);
        }
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(450, 200);
    }

    @Override
    protected void okPressed() {
        saveQuickSettings();
        if (!confirmDefault()) {
            return;
        }
        super.okPressed();
    }

    private boolean confirmDefault() {
        boolean confirm = false;
        String msg = StringUtils.EMPTY;
        if (shouldShow(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_GUID) && StringUtils.isBlank(quickSettings.getNotebook().getGuid())) {
            msg += "notebook";
            confirm = true;
        }
        if (shouldShow(SETTINGS_SECTION_NOTE, SETTINGS_KEY_GUID) && StringUtils.isBlank(quickSettings.getGuid())) {
            msg += COMMA + StringUtils.SPACE + "note";
            confirm = true;
        }
        msg = "No existing " + msg + " found, default will be used?";
        return confirm ? MessageDialog.openQuestion(shell, EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_HOTINPUTDIALOG_SHELL_TITLE), msg) : true;
    }

    private void saveQuickSettings() {
        quickSettings = new ENNoteImpl();

        quickSettings.getNotebook().setName(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK));
        quickSettings.getNotebook().setGuid(notebooks.get(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTEBOOK)));

        quickSettings.setName(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTE));
        quickSettings.setGuid(notes.get(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_NOTE)));

        quickSettings.setTags(ListUtil.toList(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_TAGS).split(ConstantsUtil.TAGS_SEPARATOR)));
        quickSettings.setComments(getFieldValue(EECLIPPERPLUGIN_CONFIGURATIONS_COMMENTS));
    }

    public ENNote getQuickSettings() {
        return quickSettings;
    }

    private boolean shouldRefresh(final String uniqueKey, final String property) {
        return fieldValueChanged(uniqueKey, property);
    }

    private boolean fieldValueChanged(final String uniqueKey, final String property) {
        if (matrix == null) {
            matrix = MapUtil.map();
        }
        Map<String, String> map = matrix.get(uniqueKey);
        if (map == null) {
            map = MapUtil.map();
            matrix.put(uniqueKey, map);
        }
        if (!StringUtil.equalsInLogic(getFieldValue(property), map.get(property))) {
            map.put(property, getFieldValue(property));
            return true;
        }
        return false;
    }

    public static int show(final Shell shell) {
        if (shouldShow()) {
            thisDialog = new HotTextDialog(shell);
            thisDialog.authInProgress();
            return thisDialog.fatal ? CANCEL : thisDialog.open();
        }
        return HotTextDialog.SHOULD_NOT_SHOW;
    }

    protected static boolean shouldShow() {
        return shouldShow(SETTINGS_SECTION_NOTEBOOK, SETTINGS_KEY_GUID) || shouldShow(SETTINGS_SECTION_NOTE, SETTINGS_KEY_GUID) || shouldShow(SETTINGS_SECTION_TAGS, SETTINGS_KEY_NAME) || shouldShow(SETTINGS_SECTION_COMMENTS, SETTINGS_KEY_NAME);
    }

    private static boolean shouldShow(final String property, final String key) {
        boolean checked = IDialogSettingsUtil.getBoolean(property, SETTINGS_KEY_CHECKED);
        String value = IDialogSettingsUtil.get(property, key);
        return checked && StringUtils.isBlank(value);
    }

    protected Text createLabelTextField(final Composite container, final String labelText) {
        Label label = new Label(container, SWT.NONE);
        label.setText(EEProperties.getProperties().getProperty(labelText) + COLON);

        Text text = new Text(container, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        return text;
    }

    protected String getFieldValue(final String property) {
        Text text = (Text) getField(property);
        if (text == null) {
            return null;
        }
        return text.getText().trim();
    }

    protected Control getField(final String property) {
        if (fields == null) {
            return null;
        }
        return fields.get(property);
    }

    protected void addField(final String key, final Text value) {
        if (fields == null) {
            fields = MapUtil.map();
        }
        fields.put(key, value);
    }

    public static HotTextDialog getThis() {
        return thisDialog;
    }

}