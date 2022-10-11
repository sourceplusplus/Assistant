/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.settings;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorTextField;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.marker.service.ArtifactConditionService;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.sourcemarker.status.util.AutocompleteField;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;
import static spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.INTELLIJ_PRODUCT_CODES;
import static spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.PYCHARM_PRODUCT_CODES;

public class LiveLogConfigurationPanel extends JPanel {

    private final XDebuggerExpressionComboBox comboBox;
    private XExpression condition;
    private int hitLimit = 100;
    private int expirationInMinutes = 15;
    private int rateLimitCount = 1;
    private String rateLimitStep = "second";

    public LiveLogConfigurationPanel(AutocompleteField autocompleteField, InlayMark inlayMark) {
        PsiFile psiFile = inlayMark.getSourceFileMarker().getPsiFile();
        XSourcePosition sourcePosition = XDebuggerUtil.getInstance().createPosition(
                psiFile.getVirtualFile(), inlayMark.getLineNumber()
        );

        XDebuggerEditorsProvider editorsProvider;
        String productCode = ApplicationInfo.getInstance().getBuild().getProductCode();
        if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
            try {
                editorsProvider = (XDebuggerEditorsProvider) Class.forName(
                        "com.jetbrains.python.debugger.PyDebuggerEditorsProvider"
                ).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else if (INTELLIJ_PRODUCT_CODES.contains(productCode)) {
            try {
                editorsProvider = (XDebuggerEditorsProvider) Class.forName(
                        "org.jetbrains.java.debugger.JavaDebuggerEditorsProvider"
                ).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported product code: " + productCode);
        }
        comboBox = new XDebuggerExpressionComboBox(
                psiFile.getProject(), editorsProvider, "LiveLogCondition",
                sourcePosition, false, false
        );

        initComponents();

        EditorTextField editorTextField = (EditorTextField) comboBox.getEditorComponent();
        editorTextField.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                autocompleteField.setShowSaveButton(isChanged());
            }
        });
        expiration15MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration30MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration1HrButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration3HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration6HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration12HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration24HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));

        hitLimitSpinner.addChangeListener(changeEvent -> autocompleteField.setShowSaveButton(isChanged()));

        rateLimitCountSpinner.addChangeListener(changeEvent -> autocompleteField.setShowSaveButton(isChanged()));
        rateLimitStepCombobox.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));

        conditionPanel.add(comboBox.getComponent());
    }

    public void setConditionByString(String condition) {
        if (condition == null) {
            setCondition(null);
        } else {
            setCondition(XExpressionImpl.fromText(ArtifactConditionService.fromLiveConditional(condition)));
        }
    }

    public void setCondition(XExpression condition) {
        this.condition = condition;
        ApplicationManager.getApplication().runWriteAction(() -> comboBox.setExpression(condition));
    }

    public XExpression getCondition() {
        return comboBox.getExpression();
    }

    public void setHitLimit(int hitLimit) {
        this.hitLimit = hitLimit;
        hitLimitSpinner.setValue(hitLimit);
    }

    public int getHitLimit() {
        return (int) hitLimitSpinner.getValue();
    }

    public int getExpirationInMinutes() {
        if (expiration15MinButton.isSelected()) {
            return 15;
        } else if (expiration30MinButton.isSelected()) {
            return 30;
        } else if (expiration1HrButton.isSelected()) {
            return 60;
        } else if (expiration3HrsButton.isSelected()) {
            return 60 * 3;
        } else if (expiration6HrsButton.isSelected()) {
            return 60 * 6;
        } else if (expiration12HrsButton.isSelected()) {
            return 60 * 12;
        } else if (expiration24HrsButton.isSelected()) {
            return 60 * 24;
        } else {
            throw new IllegalStateException();
        }
    }

    public void setExpirationInMinutes(int value) {
        this.expirationInMinutes = value;

        if (value == 15) {
            expiration15MinButton.setSelected(true);
        } else if (value == 30) {
            expiration30MinButton.setSelected(true);
        } else if (value == 60) {
            expiration1HrButton.setSelected(true);
        } else if (value == 60 * 3) {
            expiration3HrsButton.setSelected(true);
        } else if (value == 60 * 6) {
            expiration6HrsButton.setSelected(true);
        } else if (value == 60 * 12) {
            expiration12HrsButton.setSelected(true);
        } else if (value == 60 * 24) {
            expiration24HrsButton.setSelected(true);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getRateLimitCount() {
        return (int) rateLimitCountSpinner.getValue();
    }

    public void setRateLimitCount(int count) {
        this.rateLimitCount = count;
        rateLimitCountSpinner.setValue(count);
    }

    public String getRateLimitStep() {
        return (String) rateLimitStepCombobox.getSelectedItem();
    }

    public void setRateLimitStep(String step) {
        this.rateLimitStep = step;
        rateLimitStepCombobox.setSelectedItem(message(step));
    }

    public boolean isChanged() {
        return ((condition == null && !getCondition().getExpression().isEmpty()) || (condition != null && !Objects.equals(condition.getExpression(), getCondition().getExpression())))
                || hitLimit != getHitLimit()
                || expirationInMinutes != getExpirationInMinutes()
                || rateLimitCount != getRateLimitCount()
                || !Objects.equals(rateLimitStep, getRateLimitStep());
    }

    public void setNewDefaults() {
        setCondition(getCondition());
        setHitLimit(getHitLimit());
        setExpirationInMinutes(getExpirationInMinutes());
        setRateLimitCount(getRateLimitCount());
        setRateLimitStep(getRateLimitStep());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        setBackground(getBackgroundColor());
        panel4 = new JPanel();
        lblCondition = new JLabel();
        conditionPanel = new JPanel();
        separator2 = new JSeparator();
        panel6 = new JPanel();
        lblHint = new JLabel();
        hitLimitSpinner = new JSpinner();
        separator1 = new JSeparator();
        panel3 = new JPanel();
        lblExpirationDate = new JLabel();
        panel1 = new JPanel();
        expiration15MinButton = new JRadioButton();
        expiration30MinButton = new JRadioButton();
        expiration1HrButton = new JRadioButton();
        expiration3HrsButton = new JRadioButton();
        expiration6HrsButton = new JRadioButton();
        expiration12HrsButton = new JRadioButton();
        expiration24HrsButton = new JRadioButton();
        separator3 = new JSeparator();
        panel5 = new JPanel();
        label6 = new JLabel();
        panel2 = new JPanel();
        rateLimitCountSpinner = new JSpinner();
        label7 = new JLabel();
        rateLimitStepCombobox = new JComboBox<>();

        //======== this ========
        setBorder(getPanelBorder());
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]" +
            "[fill]" +
            "[100,fill]",
            // rows
            "[]" +
            "[]" +
            "[]"));

        //======== panel4 ========
        {
            panel4.setBackground(null);
            panel4.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[100,grow,fill]",
                // rows
                "[]" +
                "[]"));

            //---- lblCondition ----
            lblCondition.setText(message("condition"));
            lblCondition.setFont(SMALLER_FONT);
            panel4.add(lblCondition, "cell 0 0");

            //======== conditionPanel ========
            {
                conditionPanel.setMinimumSize(new Dimension(0, 27));
                conditionPanel.setLayout(new BorderLayout());
            }
            panel4.add(conditionPanel, "cell 0 1");
        }
        add(panel4, "cell 0 0");

        //---- separator2 ----
        separator2.setOrientation(SwingConstants.VERTICAL);
        separator2.setPreferredSize(new Dimension(3, 50));
        add(separator2, "cell 1 0");

        //======== panel6 ========
        {
            panel6.setBackground(null);
            panel6.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]",
                // rows
                "[]" +
                "[grow]"));

            //---- lblHint ----
            lblHint.setText(message("hit_limit"));
            lblHint.setFont(SMALLER_FONT);
            panel6.add(lblHint, "cell 0 0");

            //---- hitLimitSpinner ----
            hitLimitSpinner.setBackground(null);
            hitLimitSpinner.setModel(new SpinnerNumberModel(100, 1, null, 1));
            panel6.add(hitLimitSpinner, "cell 0 1");
        }
        add(panel6, "cell 2 0");
        add(separator1, "cell 0 1 3 1");

        //======== panel3 ========
        {
            panel3.setBackground(null);
            panel3.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[150,fill]" +
                "[fill]" +
                "[150,fill]",
                // rows
                "[]" +
                "[]"));

            //---- lblExpirationDate ----
            lblExpirationDate.setText(message("expiration_date"));
            lblExpirationDate.setFont(SMALLER_FONT);
            panel3.add(lblExpirationDate, "cell 0 0");

            //======== panel1 ========
            {
                panel1.setBackground(null);
                panel1.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]",
                    // rows
                    "[]"));

                //---- expiration15MinButton ----
                expiration15MinButton.setText("15 " + message("minutes"));
                expiration15MinButton.setSelected(true);
                expiration15MinButton.setBackground(null);
                expiration15MinButton.setFont(SMALLER_FONT);
                panel1.add(expiration15MinButton, "cell 0 0");

                //---- expiration30MinButton ----
                expiration30MinButton.setText("30 " + message("minutes"));
                expiration30MinButton.setBackground(null);
                expiration30MinButton.setFont(SMALLER_FONT);
                panel1.add(expiration30MinButton, "cell 1 0");

                //---- expiration1HrButton ----
                expiration1HrButton.setText("1 " + message("hour"));
                expiration1HrButton.setBackground(null);
                expiration1HrButton.setFont(SMALLER_FONT);
                panel1.add(expiration1HrButton, "cell 2 0");

                //---- expiration3HrsButton ----
                expiration3HrsButton.setText("3 " + message("hours"));
                expiration3HrsButton.setBackground(null);
                expiration3HrsButton.setFont(SMALLER_FONT);
                panel1.add(expiration3HrsButton, "cell 3 0");

                //---- expiration6HrsButton ----
                expiration6HrsButton.setText("6 " + message("hours"));
                expiration6HrsButton.setBackground(null);
                expiration6HrsButton.setFont(SMALLER_FONT);
                panel1.add(expiration6HrsButton, "cell 4 0");

                //---- expiration12HrsButton ----
                expiration12HrsButton.setText("12 " + message("hours"));
                expiration12HrsButton.setBackground(null);
                expiration12HrsButton.setFont(SMALLER_FONT);
                panel1.add(expiration12HrsButton, "cell 5 0");

                //---- expiration24HrsButton ----
                expiration24HrsButton.setText("24 " + message("hours"));
                expiration24HrsButton.setBackground(null);
                expiration24HrsButton.setFont(SMALLER_FONT);
                panel1.add(expiration24HrsButton, "cell 6 0");
            }
            panel3.add(panel1, "cell 0 1 3 1");
        }
        add(panel3, "cell 0 2");

        //---- separator3 ----
        separator3.setOrientation(SwingConstants.VERTICAL);
        separator3.setPreferredSize(new Dimension(3, 50));
        add(separator3, "cell 1 2");

        //======== panel5 ========
        {
            panel5.setBackground(null);
            panel5.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[fill]",
                // rows
                "[]" +
                "[grow]"));

            //---- label6 ----
            label6.setText(message("hit_throttle"));
            label6.setFont(SMALLER_FONT);
            panel5.add(label6, "cell 0 0");

            //======== panel2 ========
            {
                panel2.setBackground(null);
                panel2.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "0[fill]" +
                    "[fill]" +
                    "[fill]",
                    // rows
                    "[]"));

                //---- rateLimitCountSpinner ----
                rateLimitCountSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
                rateLimitCountSpinner.setBackground(null);
                panel2.add(rateLimitCountSpinner, "cell 0 0");

                //---- label7 ----
                label7.setText(message("per"));
                panel2.add(label7, "cell 1 0");

                //---- rateLimitStepCombobox ----
                rateLimitStepCombobox.setModel(new DefaultComboBoxModel<>(new String[] {
                    message("second"),
                    message("minute"),
                    message("hour")
                }));
                panel2.add(rateLimitStepCombobox, "cell 2 0");
            }
            panel5.add(panel2, "cell 0 1,grow");
        }
        add(panel5, "cell 2 2");

        //---- expirationButtonGroup ----
        ButtonGroup expirationButtonGroup = new ButtonGroup();
        expirationButtonGroup.add(expiration15MinButton);
        expirationButtonGroup.add(expiration30MinButton);
        expirationButtonGroup.add(expiration1HrButton);
        expirationButtonGroup.add(expiration3HrsButton);
        expirationButtonGroup.add(expiration6HrsButton);
        expirationButtonGroup.add(expiration12HrsButton);
        expirationButtonGroup.add(expiration24HrsButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel4;
    private JLabel lblCondition;
    private JPanel conditionPanel;
    private JSeparator separator2;
    private JPanel panel6;
    private JLabel lblHint;
    private JSpinner hitLimitSpinner;
    private JSeparator separator1;
    private JPanel panel3;
    private JLabel lblExpirationDate;
    private JPanel panel1;
    private JRadioButton expiration15MinButton;
    private JRadioButton expiration30MinButton;
    private JRadioButton expiration1HrButton;
    private JRadioButton expiration3HrsButton;
    private JRadioButton expiration6HrsButton;
    private JRadioButton expiration12HrsButton;
    private JRadioButton expiration24HrsButton;
    private JSeparator separator3;
    private JPanel panel5;
    private JLabel label6;
    private JPanel panel2;
    private JSpinner rateLimitCountSpinner;
    private JLabel label7;
    private JComboBox<String> rateLimitStepCombobox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
