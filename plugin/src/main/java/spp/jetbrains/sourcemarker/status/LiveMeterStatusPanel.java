package spp.jetbrains.sourcemarker.status;

import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.marker.source.mark.gutter.GutterMark;
import spp.jetbrains.sourcemarker.PluginIcons;
import spp.jetbrains.sourcemarker.PluginUI;
import spp.jetbrains.sourcemarker.service.InstrumentEventListener;
import spp.protocol.instrument.LiveMeter;
import spp.protocol.instrument.event.LiveInstrumentEvent;
import spp.protocol.instrument.meter.MeterType;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static spp.jetbrains.sourcemarker.PluginUI.LABEL_FOREGROUND_COLOR;
import static spp.jetbrains.sourcemarker.PluginUI.ROBOTO_LIGHT_PLAIN_15;
import static spp.jetbrains.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;
import static spp.protocol.SourceMarkerServices.Instance.INSTANCE;

public class LiveMeterStatusPanel extends JPanel implements InstrumentEventListener {

    private final LiveMeter liveMeter;
    private final GutterMark gutterMark;

    public LiveMeterStatusPanel(LiveMeter liveMeter, GutterMark gutterMark) {
        this.liveMeter = liveMeter;
        this.gutterMark = gutterMark;

        initComponents();
        setupComponents();

        UIManager.addPropertyChangeListener(propertyChangeEvent -> {
            setBackground(UIUtil.getPanelBackground());
            minuteLabel.setForeground(UIUtil.getLabelForeground());
            minuteValueLabel.setForeground(UIUtil.getLabelForeground());
            hourLabel.setForeground(UIUtil.getLabelForeground());
            hourValueLabel.setForeground(UIUtil.getLabelForeground());
            dayLabel.setForeground(UIUtil.getLabelForeground());
            dayValueLabel.setForeground(UIUtil.getLabelForeground());
        });

        meterDescriptionTextField.setText(liveMeter.getMeterName());

        String meterType = liveMeter.getMeterType().name().toLowerCase();
        meterType = meterType.substring(0, 1).toUpperCase() + meterType.substring(1);
        meterTypeValueLabel.setText(meterType);

        if (liveMeter.getMeterType() == MeterType.GAUGE) {
            minuteLabel.setText("Value");
            hourLabel.setVisible(false);
            hourValueLabel.setVisible(false);
            dayLabel.setVisible(false);
            dayValueLabel.setVisible(false);
        }
    }

    @Override
    public void accept(@NotNull LiveInstrumentEvent event) {
        JsonObject rawMetrics = new JsonObject(new JsonObject(event.getData()).getString("metricsData"));
        minuteValueLabel.setText(getShortNumber(rawMetrics.getString("last_minute")));
        hourValueLabel.setText(getShortNumber(rawMetrics.getString("last_hour")));
        dayValueLabel.setText(getShortNumber(rawMetrics.getString("last_day")));
    }

    private String getShortNumber(String number) {
        long value = Long.parseLong(number);
        if (value > 1000000) {
            return String.format("%.1fM", value / 1000000.0);
        } else if (value > 1000) {
            return String.format("%.1fK", value / 1000.0);
        }
        return String.valueOf(value);
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            closeLabel.setIcon(PluginIcons.close);
        });
    }

    private void setupComponents() {
        closeLabel.setCursor(Cursor.getDefaultCursor());
        closeLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        });
        addRecursiveMouseListener(closeLabel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                INSTANCE.getLiveInstrument().removeLiveInstrument(liveMeter.getId()).onComplete(it -> {
                    if (it.succeeded()) {
                        gutterMark.dispose();
                        LiveStatusManager.INSTANCE.removeActiveLiveInstrument(liveMeter);
                    } else {
                        it.cause().printStackTrace();
                    }
                });
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closePressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel4 = new JPanel();
        panel1 = new JPanel();
        meterTypeValueLabel = new JLabel();
        separator1 = new JSeparator();
        minuteLabel = new JLabel();
        minuteValueLabel = new JLabel();
        hourLabel = new JLabel();
        hourValueLabel = new JLabel();
        panel3 = new JPanel();
        dayLabel = new JLabel();
        dayValueLabel = new JLabel();
        panel2 = new JPanel();
        meterDescriptionTextField = new JTextField();
        closeLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EtchedBorder());
        setFont(ROBOTO_LIGHT_PLAIN_15);
        setMinimumSize(new Dimension(385, 70));
        setPreferredSize(new Dimension(385, 70));
        setLayout(new FormLayout(
            "default:grow",
            "fill:default:grow"));

        //======== panel4 ========
        {
            panel4.setBackground(null);
            panel4.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow, 1dlu, default"));

            //======== panel1 ========
            {
                panel1.setBackground(null);
                panel1.setFont(ROBOTO_LIGHT_PLAIN_15);
                panel1.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.CENTER, Sizes.DLUX4, FormSpec.NO_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.LEFT, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                    },
                    RowSpec.decodeSpecs("fill:default:grow")));

                //---- meterTypeValueLabel ----
                meterTypeValueLabel.setText("Count");
                meterTypeValueLabel.setFont(PluginUI.ROBOTO_LIGHT_PLAIN_16);
                meterTypeValueLabel.setForeground(LABEL_FOREGROUND_COLOR);
                meterTypeValueLabel.setMinimumSize(new Dimension(46, 25));
                panel1.add(meterTypeValueLabel, cc.xy(1, 1));

                //---- separator1 ----
                separator1.setOrientation(SwingConstants.VERTICAL);
                separator1.setMinimumSize(new Dimension(2, 1));
                separator1.setMaximumSize(new Dimension(2, 1));
                separator1.setPreferredSize(new Dimension(2, 1));
                separator1.setForeground(Color.darkGray);
                panel1.add(separator1, new CellConstraints(3, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(5, 0, 5, 0)));

                //---- minuteLabel ----
                minuteLabel.setText("Minute");
                minuteLabel.setFont(ROBOTO_LIGHT_PLAIN_15);
                panel1.add(minuteLabel, cc.xy(5, 1));

                //---- minuteValueLabel ----
                minuteValueLabel.setText("n/a");
                minuteValueLabel.setFont(PluginUI.ROBOTO_LIGHT_PLAIN_16);
                panel1.add(minuteValueLabel, cc.xy(7, 1));

                //---- hourLabel ----
                hourLabel.setText("Hour");
                hourLabel.setFont(ROBOTO_LIGHT_PLAIN_15);
                panel1.add(hourLabel, cc.xy(9, 1));

                //---- hourValueLabel ----
                hourValueLabel.setText("n/a");
                hourValueLabel.setFont(PluginUI.ROBOTO_LIGHT_PLAIN_16);
                panel1.add(hourValueLabel, cc.xy(11, 1));

                //======== panel3 ========
                {
                    panel3.setBackground(null);
                    panel3.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                        },
                        RowSpec.decodeSpecs("fill:default:grow")));

                    //---- dayLabel ----
                    dayLabel.setText("Day");
                    dayLabel.setFont(ROBOTO_LIGHT_PLAIN_15);
                    panel3.add(dayLabel, cc.xy(1, 1));

                    //---- dayValueLabel ----
                    dayValueLabel.setText("n/a");
                    dayValueLabel.setFont(PluginUI.ROBOTO_LIGHT_PLAIN_16);
                    panel3.add(dayValueLabel, cc.xy(3, 1));
                }
                panel1.add(panel3, cc.xy(13, 1));
            }
            panel4.add(panel1, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(2, 4, 2, 2)));

            //======== panel2 ========
            {
                panel2.setBackground(null);
                panel2.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                    },
                    RowSpec.decodeSpecs("default")));

                //---- meterDescriptionTextField ----
                meterDescriptionTextField.setEditable(false);
                panel2.add(meterDescriptionTextField, cc.xy(1, 1));

                //---- closeLabel ----
                closeLabel.setIcon(PluginIcons.close);
                panel2.add(closeLabel, cc.xy(3, 1));
            }
            panel4.add(panel2, cc.xy(1, 3));
        }
        add(panel4, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(5, 5, 5, 10)));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel4;
    private JPanel panel1;
    private JLabel meterTypeValueLabel;
    private JSeparator separator1;
    private JLabel minuteLabel;
    private JLabel minuteValueLabel;
    private JLabel hourLabel;
    private JLabel hourValueLabel;
    private JPanel panel3;
    private JLabel dayLabel;
    private JLabel dayValueLabel;
    private JPanel panel2;
    private JTextField meterDescriptionTextField;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
