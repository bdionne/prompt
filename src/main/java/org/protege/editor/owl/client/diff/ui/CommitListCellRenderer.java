package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.CommitMetadata;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setBorder(new EmptyBorder(7, 7, 7, 3));
        label.setIcon(GuiUtils.getIcon(GuiUtils.COMMIT_ICON_FILENAME, 17, 17));
        label.setIconTextGap(9);

        CommitMetadata c = (CommitMetadata) value;
        label.setToolTipText("Comment: " + c.getComment());
        
        String dateStr = GuiUtils.getShortenedFormattedDate(c.getDate()); 
        label.setText("<html><strong>" + dateStr + " · " + c.getAuthor().toString() + // TODO: To review later
                "</strong><br><p style=\"padding-top:3;" + (!isSelected ? "color:gray;" : "") + "\"><nobr>" + c.getComment() + 
                "</nobr></p></html>");
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(label);
        panel.setBackground(Color.WHITE);
        
        if (c.getConflictCount() > 0) {
	        JLabel conflictLabel = new JLabel();
	        conflictLabel.setBorder(new EmptyBorder(7, 3, 23, 3));
	        conflictLabel.setIcon(GuiUtils.getIcon(GuiUtils.WARNING_ICON_FILENAME, 23, 23));
	        conflictLabel.setIconTextGap(6);
	        conflictLabel.setBackground(Color.WHITE);
	        conflictLabel.setText("<html><strong><font color='red'>" + c.getConflictAuthor() + "(" + c.getConflictCount() + ")" +
	                "</font></strong></html>");
	        
	        panel.add(conflictLabel);
        }
        return panel;
    }

}
