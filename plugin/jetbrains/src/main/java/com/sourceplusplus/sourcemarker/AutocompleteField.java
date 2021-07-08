package com.sourceplusplus.sourcemarker;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class AutocompleteField extends JTextField implements FocusListener, DocumentListener, KeyListener {

    private final Function<String, List<String>> lookup;
    private final List<String> results;
    private final JWindow popup;
    private final JList<String> list;
    private final ListModel<String> model;
    private final String placeHolderText;

    public AutocompleteField(String placeHolderText, Function<String, List<String>> lookup) {
        this.placeHolderText = placeHolderText;
        this.lookup = lookup;
        this.results = new ArrayList<>();

        Window parent = SwingUtilities.getWindowAncestor(this);
        popup = new JWindow(parent);
        popup.setType(Window.Type.POPUP);
        popup.setFocusableWindowState(false);
        popup.setAlwaysOnTop(true);

        model = new ListModel<>();
        list = new JBList<>(model);
        list.setBackground(JBColor.decode("#252525"));
        list.setBorder(JBUI.Borders.empty());

        JScrollPane scroll = new JScrollPane(list) {
            @Override
            public Dimension getPreferredSize() {
                final Dimension ps = super.getPreferredSize();
                ps.width = AutocompleteField.this.getWidth();
                return ps;
            }
        };
        scroll.setBorder(JBUI.Borders.empty());
        popup.add(scroll);

        addFocusListener(this);
        getDocument().addDocumentListener(this);
        addKeyListener(this);
    }

    private void showAutocompletePopup() {
        final Point los = AutocompleteField.this.getLocationOnScreen();
        popup.setLocation(los.x, los.y + getHeight() + 6);
        popup.setVisible(true);
    }

    private void hideAutocompletePopup() {
        popup.setVisible(false);
    }

    @Override
    public void focusGained(final FocusEvent e) {
        SwingUtilities.invokeLater(() -> {
            if (results.size() > 0) {
                showAutocompletePopup();
            }
        });
    }

    private void documentChanged() {
        SwingUtilities.invokeLater(() -> {
            // Updating results list
            results.clear();
            results.addAll(lookup.apply(getText()));

            // Updating list view
            model.updateView();
            list.setVisibleRowCount(Math.min(results.size(), 10));

            // Selecting first result
            if (results.size() > 0) {
                list.setSelectedIndex(0);
            }

            // Ensure autocomplete popup has correct size
            popup.pack();

            // Display or hide popup depending on the results
            if (results.size() > 0) {
                showAutocompletePopup();
            } else {
                hideAutocompletePopup();
            }
        });
    }

    public String getSelectedText() {
        return list.getSelectedValue();
    }

    @Override
    public void focusLost(final FocusEvent e) {
        SwingUtilities.invokeLater(this::hideAutocompletePopup);
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            final int index = list.getSelectedIndex();
            if (index > 0) {
                list.setSelectedIndex(index - 1);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            final int index = list.getSelectedIndex();
            if (index != -1 && list.getModel().getSize() > index + 1) {
                list.setSelectedIndex(index + 1);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
            final String text = list.getSelectedValue();
            setText(text);
            setCaretPosition(text.length());
        }
    }

    @Override
    public void insertUpdate(final DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void removeUpdate(final DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void changedUpdate(final DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void keyTyped(final KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        // Do nothing
    }

    private class ListModel<T> extends AbstractListModel<T> {
        @Override
        public int getSize() {
            return results.size();
        }

        @Override
        public T getElementAt(final int index) {
            return (T) results.get(index);
        }

        public void updateView() {
            super.fireContentsChanged(AutocompleteField.this, 0, getSize());
        }
    }

    @Override
    protected void paintComponent(final Graphics pG) {
        super.paintComponent(pG);

        if (getText().length() > 0 || placeHolderText == null) {
            return;
        }

        final Graphics2D g = (Graphics2D) pG;
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

//        //        g.setColor(Color.decode("#252525"));
////        g.drawString(placeHolderText, getInsets().left + 6, pG.getFontMetrics()
////                .getMaxAscent() + getInsets().top + 2);
//        g.setColor(new Color(225, 72, 59));
//        g.drawString(placeHolderText.substring(6), ((float) g.getFontMetrics().getStringBounds(placeHolderText.substring(0, 6), g).getWidth()) + ((float) getInsets().left + 6), pG.getFontMetrics()
//                .getMaxAscent() + getInsets().top + 2);

        g.setColor(new Color(85, 85, 85, 200));
        g.drawString(placeHolderText, getInsets().left + 6, pG.getFontMetrics()
                .getMaxAscent() + getInsets().top + 2);
    }
}
