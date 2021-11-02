package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.api.SourceMark;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.protocol.SourceMarkerServices;
import com.sourceplusplus.protocol.instrument.*;
import com.sourceplusplus.protocol.instrument.meter.LiveMeter;
import com.sourceplusplus.protocol.instrument.meter.MeterType;
import com.sourceplusplus.protocol.instrument.meter.MetricValue;
import com.sourceplusplus.protocol.instrument.meter.MetricValueType;
import com.sourceplusplus.protocol.instrument.meter.event.LiveMeterRemoved;
import com.sourceplusplus.protocol.service.live.LiveInstrumentService;
import com.sourceplusplus.sourcemarker.command.AutocompleteFieldRow;
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys;
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitColumnInfo;
import com.sourceplusplus.sourcemarker.settings.LiveMeterConfigurationPanel;
import com.sourceplusplus.sourcemarker.status.util.AutocompleteField;
import io.vertx.core.json.Json;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sourceplusplus.marker.SourceMarker.conditionParser;
import static com.sourceplusplus.protocol.instrument.LiveInstrumentEventType.METER_REMOVED;
import static com.sourceplusplus.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;

public class MeterStatusBar extends JPanel implements StatusBar, VisibleAreaListener {

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private EditorImpl editor;
    private JWindow popup;
    private LiveMeterConfigurationPanel configurationPanel;
    private boolean disposed = false;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;
    private final String placeHolderText = "Meter Condition";
    private LiveMeter liveMeter;
    private LiveBreakpointStatusPanel statusPanel;
    private JPanel wrapper;
    private JPanel panel;
    private JLabel expandLabel;
    private boolean expanded = false;
    private final ListTableModel commandModel = new ListTableModel<>(
            new ColumnInfo[]{
                    new BreakpointHitColumnInfo("Meter Data"),
                    new BreakpointHitColumnInfo("Time")
            },
            new ArrayList<>(), 0, SortOrder.DESCENDING);

    public MeterStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.scopeVars = scopeVars.stream().map(it -> new AutocompleteFieldRow() {
            public String getText() {
                return it;
            }

            public String getDescription() {
                return null;
            }

            public Icon getIcon() {
                return IconLoader.getIcon("/nodes/variable.png");
            }
        }).collect(Collectors.toList());
        lookup = text -> scopeVars.stream()
                .filter(v -> {
                    String var = substringAfterLast(" ", text.toLowerCase());
                    return !var.isEmpty() && !var.equalsIgnoreCase(v) && v.toLowerCase().contains(var);
                }).map(it -> new AutocompleteFieldRow() {
                    public String getText() {
                        return it;
                    }

                    public String getDescription() {
                        return null;
                    }

                    public Icon getIcon() {
                        return IconLoader.getIcon("/nodes/variable.png");
                    }
                })
                .limit(7)
                .collect(Collectors.toList());

        this.inlayMark = inlayMark;

        initComponents();
        setupComponents();
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveMeter = (LiveMeter) liveInstrument;
        setupAsActive();
    }

    public void setWrapperPanel(JPanel wrapperPanel) {
        this.wrapper = wrapperPanel;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
        meterConditionField.hideAutocompletePopup();
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        meterConditionField.grabFocus();
        meterConditionField.requestFocusInWindow();
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            if (expandLabel != null) expandLabel.setIcon(IconLoader.getIcon("/icons/expand.svg"));
            closeLabel.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
            configPanel.setBackground(Color.decode("#252525"));

            if (!meterConditionField.getEditMode()) {
                meterConditionField.setBorder(new CompoundBorder(
                        new LineBorder(Color.darkGray, 0, true),
                        new EmptyBorder(2, 6, 0, 0)));
                meterConditionField.setBackground(Color.decode("#2B2B2B"));
                meterConditionField.setEditable(false);
            }
        });
    }

    private void setupAsActive() {
        LiveStatusManager.INSTANCE.addStatusBar(inlayMark, event -> {
            if (statusPanel == null) return;
            if (event.getEventType() == METER_REMOVED) {
                configLabel.setIcon(IconLoader.getIcon("/icons/eye-slash.svg"));

                LiveMeterRemoved removed = Json.decodeValue(event.getData(), LiveMeterRemoved.class);
                if (removed.getCause() == null) {
                    statusPanel.setStatus("Complete", Color.decode("#9876AA"));
                } else {
                    commandModel.insertRow(0, event);
                    statusPanel.setStatus("Error", Color.decode("#e1483b"));
                }
            }
        });
        statusPanel = new LiveBreakpointStatusPanel();
        statusPanel.setHitLimit(liveMeter.getHitLimit());

        meterConditionField.setEditMode(false);
        removeActiveDecorations();
        configDropdownLabel.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            mainPanel.removeAll();
            mainPanel.setLayout(new BorderLayout());
            statusPanel.setExpires(liveMeter.getExpiresAt());
            mainPanel.add(statusPanel);

            remove(closeLabel);
//                    JLabel searchLabel = new JLabel();
//                    searchLabel.setIcon(IconLoader.getIcon("/icons/search.svg"));
//                    add(searchLabel, "cell 2 0");
            expandLabel = new JLabel();
            expandLabel.setCursor(Cursor.getDefaultCursor());
            expandLabel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    expandLabel.setIcon(IconLoader.getIcon("/icons/expandHovered.svg"));
                }
            });
            addRecursiveMouseListener(expandLabel, new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!expanded) {
                        expanded = true;

                        panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        JBTable table = new JBTable();
                        JScrollPane scrollPane = new JBScrollPane(table);
                        table.setRowHeight(30);
                        table.setShowColumns(true);
                        table.setModel(commandModel);
                        table.setStriped(true);
                        table.setShowColumns(true);

                        table.setBackground(Color.decode("#252525"));
                        panel.add(scrollPane);
                        panel.setPreferredSize(new Dimension(0, 250));
                        wrapper.add(panel, BorderLayout.NORTH);
                    } else {
                        expanded = false;
                        wrapper.remove(panel);
                    }

                    JViewport viewport = editor.getScrollPane().getViewport();
                    viewport.dispatchEvent(new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED));
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    expandLabel.setIcon(IconLoader.getIcon("/icons/expandPressed.svg"));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    expandLabel.setIcon(IconLoader.getIcon("/icons/expandHovered.svg"));
                }
            }, () -> {
                removeActiveDecorations();
                return null;
            });
            expandLabel.setIcon(IconLoader.getIcon("/icons/expand.svg"));
            add(expandLabel, "cell 2 0");
            add(closeLabel);
        });
    }

    private void setupComponents() {
        meterTypeComboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    ApplicationManager.getApplication().runWriteAction(() -> saveLiveMeter());
                }
            }
        });
        meterTypeComboBox.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        meterConditionField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
            }
        });
        meterConditionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_TAB) {
                    //ignore tab; handled by auto-complete
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    meterTypeComboBox.requestFocus();
                }
            }
        });
        meterConditionField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                removeActiveDecorations();
            }
        });

        closeLabel.setCursor(Cursor.getDefaultCursor());
        closeLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        });
        addRecursiveMouseListener(closeLabel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        configPanel.setCursor(Cursor.getDefaultCursor());
        configPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (configDropdownLabel.isVisible()) {
                    configPanel.setBackground(Color.decode("#3C3C3C"));
                }
            }
        });

        AtomicLong popupLastOpened = new AtomicLong();
        addRecursiveMouseListener(configPanel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (configDropdownLabel.isVisible() && System.currentTimeMillis() - popupLastOpened.get() > 200) {
                    ApplicationManager.getApplication().runWriteAction(() -> showConfigurationPopup(popupLastOpened));
                }
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        timeLabel.setCursor(Cursor.getDefaultCursor());

        setCursor(Cursor.getDefaultCursor());
    }

    private void showConfigurationPopup(AtomicLong popupLastOpened) {
        popup = new JWindow(SwingUtilities.getWindowAncestor(MeterStatusBar.this));
        popup.setType(Window.Type.POPUP);
        popup.setAlwaysOnTop(true);

        if (configurationPanel == null) {
            configurationPanel = new LiveMeterConfigurationPanel(meterConditionField);
        }

        popup.add(configurationPanel);
        popup.setPreferredSize(new Dimension(MeterStatusBar.this.getWidth(), popup.getPreferredSize().height));
        popup.pack();
        popup.setLocation(configPanel.getLocationOnScreen().x - 1,
                configPanel.getLocationOnScreen().y + MeterStatusBar.this.getHeight() - 2);

        popup.setVisible(true);

        popup.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (popup != null) {
                    popup.dispose();
                    popup = null;

                    popupLastOpened.set(System.currentTimeMillis());
                }
            }
        });
    }

    private void saveLiveMeter() {
        meterConditionField.setShowSaveButton(false);

        String condition = null;
        if (!meterConditionField.getText().isEmpty()) {
            condition = conditionParser.getCondition(
                    meterConditionField.getText(), inlayMark.getPsiElement()
            );
        }

        long expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * 15);
        InstrumentThrottle throttle = InstrumentThrottle.Companion.getDEFAULT();
//        JFormattedTextField spinnerTextField = ((JSpinner.NumberEditor) meterTypeSpinner.getEditor()).getTextField();
//        int hitLimit = Integer.parseInt(spinnerTextField.getText());
        if (configurationPanel != null) {
            expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * configurationPanel.getExpirationInMinutes());
            throttle = new InstrumentThrottle(
                    configurationPanel.getRateLimitCount(),
                    ThrottleStep.valueOf(configurationPanel.getRateLimitStep().toUpperCase())
            );

            configurationPanel.setNewDefaults();
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("original_source_mark", inlayMark.getId());

        LiveInstrumentService instrumentService = Objects.requireNonNull(SourceMarkerServices.Instance.INSTANCE.getLiveInstrument());
        LiveMeter instrument = new LiveMeter(
                "tester",
                MeterType.COUNTER,
                new MetricValue(MetricValueType.NUMBER, "1", ""),
                sourceLocation,
                condition,
                expirationDate,
                -1,
                null,
                false,
                false,
                false,
                throttle,
                meta
        );
        instrumentService.addLiveInstrument(instrument, it -> {
            if (it.succeeded()) {
                liveMeter = (LiveMeter) it.result();
                inlayMark.putUserData(SourceMarkKeys.INSTANCE.getMETER_ID(), liveMeter.getId());
                LiveStatusManager.INSTANCE.addActiveLiveInstrument(liveMeter);
            } else {
                it.cause().printStackTrace();
            }
        });
    }

    private void dispose() {
        if (disposed) return;
        disposed = true;
        editor.getScrollingModel().removeVisibleAreaListener(this);
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
        inlayMark.dispose(true);
        List<SourceMark> groupedMarks = inlayMark.getUserData(SourceMarkKeys.INSTANCE.getGROUPED_MARKS());
        if (groupedMarks != null) groupedMarks.forEach(SourceMark::dispose);

        if (liveMeter != null) {
            SourceMarkerServices.Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(liveMeter.getId(), it -> {
                if (it.succeeded()) {
                    LiveStatusManager.INSTANCE.removeActiveLiveInstrument(liveMeter);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }
    }

    public static String substringAfterLast(String delimiter, String value) {
        int index = value.lastIndexOf(delimiter);
        if (index == -1) return value;
        else return value.substring(index + delimiter.length());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        configPanel = new JPanel();
        configLabel = new JLabel();
        configDropdownLabel = new JLabel();
        mainPanel = new JPanel();
        meterConditionField = new AutocompleteField(placeHolderText, scopeVars, lookup, inlayMark.getLineNumber(), false, false, Color.decode("#9876AA"));
        label1 = new JLabel();
        meterTypeComboBox = new JComboBox<>();
        timeLabel = new JLabel();
        separator1 = new JSeparator();
        closeLabel = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setBackground(new Color(43, 43, 43));
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "0[fill]" +
            "[grow,fill]" +
            "[fill]",
            // rows
            "0[grow]0"));

        //======== configPanel ========
        {
            configPanel.setBackground(new Color(37, 37, 37));
            configPanel.setPreferredSize(null);
            configPanel.setMinimumSize(null);
            configPanel.setMaximumSize(null);
            configPanel.setLayout(new MigLayout(
                "fill,insets 0,hidemode 3",
                // columns
                "5[fill]" +
                "[fill]4",
                // rows
                "[grow]"));

            //---- configLabel ----
            configLabel.setIcon(IconLoader.getIcon("/icons/eye.svg"));
            configPanel.add(configLabel, "cell 0 0");

            //---- configDropdownLabel ----
            configDropdownLabel.setIcon(IconLoader.getIcon("/icons/angle-down.svg"));
            configPanel.add(configDropdownLabel, "cell 1 0");
        }
        add(configPanel, "cell 0 0, grow");

        //======== mainPanel ========
        {
            mainPanel.setBackground(null);
            mainPanel.setLayout(new MigLayout(
                "novisualpadding,hidemode 3",
                // columns
                "[grow,fill]" +
                "[fill]",
                // rows
                "0[grow]0"));

            //---- meterConditionField ----
            meterConditionField.setBackground(new Color(37, 37, 37));
            meterConditionField.setBorder(new CompoundBorder(
                new LineBorder(Color.darkGray, 1, true),
                new EmptyBorder(2, 6, 0, 0)));
            meterConditionField.setFont(new Font("Roboto Light", Font.PLAIN, 14));
            meterConditionField.setMinimumSize(new Dimension(0, 27));
            mainPanel.add(meterConditionField, "cell 0 0");

            //---- label1 ----
            label1.setText("Type");
            label1.setForeground(Color.gray);
            label1.setFont(new Font("Roboto Light", Font.PLAIN, 15));
            mainPanel.add(label1, "cell 1 0");

            //---- meterTypeComboBox ----
            meterTypeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                "Counter",
                "Gauge",
                "Histogram"
            }));
            mainPanel.add(meterTypeComboBox, "cell 1 0");

            //---- timeLabel ----
            timeLabel.setIcon(IconLoader.getIcon("/icons/clock.svg"));
            timeLabel.setFont(new Font("Roboto Light", Font.PLAIN, 14));
            timeLabel.setIconTextGap(8);
            timeLabel.setVisible(false);
            mainPanel.add(timeLabel, "cell 1 0,gapx null 8");
        }
        add(mainPanel, "pad 0,cell 1 0,grow,gapx 0 0,gapy 0 0");

        //---- separator1 ----
        separator1.setPreferredSize(new Dimension(5, 20));
        separator1.setMinimumSize(new Dimension(5, 20));
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new Dimension(5, 20));
        separator1.setVisible(false);
        add(separator1, "cell 1 0");

        //---- closeLabel ----
        closeLabel.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        add(closeLabel, "cell 2 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel configPanel;
    private JLabel configLabel;
    private JLabel configDropdownLabel;
    private JPanel mainPanel;
    private AutocompleteField meterConditionField;
    private JLabel label1;
    private JComboBox<String> meterTypeComboBox;
    private JLabel timeLabel;
    private JSeparator separator1;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
