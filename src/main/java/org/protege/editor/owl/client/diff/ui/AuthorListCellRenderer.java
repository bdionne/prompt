package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.LogDiffManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String user = (String) value;
        List<Integer> countList = LogDiffManager.currentMan().getCount(user);
        if(user.equals(LogDiffManager.ALL_AUTHORS)) {
            label.setIcon(GuiUtils.getIcon(GuiUtils.USERS_ICON_FILENAME, 20, 20));
            label.setFont(getFont().deriveFont(Font.BOLD));
        }
        else {        	
            label.setIcon(GuiUtils.getIcon(GuiUtils.USER_ICON_FILENAME, 20, 20));
        }
        label.setText(user + "(" +  countList.get(0) + ")");
        label.setBorder(new EmptyBorder(0, 7, 0, 0));
        label.setIconTextGap(7);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(label);
        panel.setBackground(Color.WHITE);
        
        int conflictCount = countList.size() > 1? countList.get(1) : 0;
        if (conflictCount > 0) {
	        JLabel conflictLabel = new JLabel();
	        conflictLabel.setBorder(new EmptyBorder(20, 7, 23, 20));
	        conflictLabel.setIcon(GuiUtils.getIcon(GuiUtils.WARNING_ICON_FILENAME, 23, 23));
	        conflictLabel.setIconTextGap(7);
	        if (user.equals(LogDiffManager.ALL_AUTHORS)) {
	        	conflictLabel.setFont(getFont().deriveFont(Font.BOLD));
	        }
	        conflictLabel.setForeground(Color.red);
	        conflictLabel.setText("(" + conflictCount + ")");
	        panel.add(conflictLabel);
        }
        return panel;
        
        
        //return label;
    }

}
