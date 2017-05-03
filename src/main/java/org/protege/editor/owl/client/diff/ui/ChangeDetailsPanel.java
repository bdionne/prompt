package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.model.OWLModelManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -5433982406494139888L;
    private OWLEditorKit editorKit;
    private LogDiffManager diffManager;

    /**
     * Constructor
     *
     * @param modelManager OWL model manager
     * @param editorKit    OWL editor kit
     */
    public ChangeDetailsPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);

        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);

        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        setBackground(GuiUtils.WHITE_BACKGROUND);

        createContents();
    }

    private LogDiffListener diffListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if (event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if(!diffManager.getSelectedChanges().isEmpty()) {
                    removeAll();
                    createContents();
                }
            }
            else if(event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) || event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED) ||
                    event.equals(LogDiffEvent.ONTOLOGY_UPDATED) || event.equals(LogDiffEvent.COMMIT_OCCURRED)) {
                removeAll();
                repaint();
            }
            // rpc: when review view is active and a change is reviewed, recreate the review panel
        }
    };

    private void createContents() {
        if(!diffManager.getSelectedChanges().isEmpty()) {
            Change change = diffManager.getFirstSelectedChange();
            if (change != null) {
                addDetailsTable(change);
                revalidate();
            }
        }
    }

    private void addDetailsTable(Change change) {
        ChangeDetailsTableModel tableModel;
        if(change.getBaselineChange().isPresent()) {
            tableModel = new MatchingChangeDetailsTableModel();
        }
        else {
            tableModel = new MultipleChangeDetailsTableModel();
        }
        
        ChangeDetailsTable table = new ChangeDetailsTable(tableModel, editorKit);
        
        ColumnListener cl = new ColumnListener(){

            @Override
            public void columnMoved(int oldLocation, int newLocation) {
            	
            }

            @Override
            public void columnResized(int column, int newWidth) {
            	TableColumn c = table.getColumnModel().getColumn(column);
                updateRowHeights(column, c.getWidth(), table);
            }

        };

        table.getColumnModel().addColumnModelListener(cl);
        table.getTableHeader().addMouseListener(cl);
		table.getModel().addTableModelListener(new TableModelListener() {

			public void tableChanged(TableModelEvent e) {
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						int columnCount = table.getColumnModel().getColumnCount();
						for (int i = 0; i < columnCount; i++) {
							TableColumn c = table.getColumnModel().getColumn(i);
							updateRowHeights(i, c.getWidth(), table);
						}
					}

				});

			}
		});
        
        tableModel.setChange(change);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        add(scrollPane, BorderLayout.CENTER);
    }
    
	public static void updateRowHeights(int column, int width, JTable table){
	    for (int row = 0; row < table.getRowCount(); row++) {
	        int rowHeight = table.getRowHeight();
	        Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
	        Dimension d = comp.getPreferredSize();
	        comp.setSize(new Dimension(width, d.height));
	        d = comp.getPreferredSize();
	        rowHeight = Math.max(rowHeight, d.height);
	        table.setRowHeight(row, rowHeight);
	    }
	}
    abstract class ColumnListener extends MouseAdapter implements TableColumnModelListener {

        private int oldIndex = -1;
        private int newIndex = -1;
        private boolean dragging = false;

        private boolean resizing = false;
        private int resizingColumn = -1;
        private int oldWidth = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            // capture start of resize
            if(e.getSource() instanceof JTableHeader) {
                JTableHeader header = (JTableHeader)e.getSource();
                TableColumn tc = header.getResizingColumn();
                if(tc != null) {
                    resizing = true;
                    JTable table = header.getTable();
                    resizingColumn = table.convertColumnIndexToView( tc.getModelIndex());
                    oldWidth = tc.getPreferredWidth();
                } else {
                    resizingColumn = -1;
                    oldWidth = -1;
                }
            }   
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // column moved
            if(dragging && oldIndex != newIndex) {
                columnMoved(oldIndex, newIndex);
            }
            dragging = false;
            oldIndex = -1;
            newIndex = -1;

            // column resized
            if(resizing) {
                if(e.getSource() instanceof JTableHeader) {
                    JTableHeader header = (JTableHeader)e.getSource();
                    TableColumn tc = header.getColumnModel().getColumn(resizingColumn);
                    if(tc != null) {
                        int newWidth = tc.getPreferredWidth();
                        if(newWidth != oldWidth) {
                            columnResized(resizingColumn, newWidth);
                        }
                    }
                }   
            }
            resizing = false;
            resizingColumn = -1;
            oldWidth = -1;
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {      
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {        
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            // capture dragging
            dragging = true;
            if(oldIndex == -1){
                oldIndex = e.getFromIndex();
            }

            newIndex = e.getToIndex();  
                 
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }

        public abstract void columnMoved(int oldLocation, int newLocation);
        public abstract void columnResized(int column, int newWidth);
    }

    @SuppressWarnings("unused") // rpc
    private void addReview(Change change) {
        JPanel reviewPanel = new JPanel(new BorderLayout());
        JLabel reviewLbl = new JLabel();
        Icon icon = null;
        String statusStr = "";
        ReviewStatus status = change.getReviewStatus();
        switch(status) {
            case ACCEPTED:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_ACCEPTED_ICON_FILENAME, 40, 40);
                statusStr = "Accepted"; break;
            case REJECTED:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_REJECTED_ICON_FILENAME, 40, 40);
                statusStr = "Rejected"; break;
            case PENDING:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_PENDING_ICON_FILENAME, 40, 40);
                statusStr = "Pending Review"; break;
        }
        if(icon != null) {
            reviewLbl.setIcon(icon);
            reviewLbl.setIconTextGap(10);
        }
        reviewLbl.setBorder(new EmptyBorder(10, 13, 10, 1));
        reviewLbl.setText(getReviewText(change.getReview(), statusStr));
        reviewPanel.add(reviewLbl);
        add(reviewPanel, BorderLayout.SOUTH);
    }

    private String getReviewText(Review review, String statusStr) {
        String dateStr = "", author = "", comment = "";
        if(review != null) {
            if (review.getDate().isPresent()) {
                dateStr = GuiUtils.getShortenedFormattedDate(review.getDate().get());
            }
            author = (review.getAuthor().isPresent() ? review.getAuthor().get().toString() : ""); // TODO: To review later
            comment = (review.getComment().isPresent() ? review.getComment().get() : "");
        }
        String reviewText = "<html><p style=\"font-size:14\"><strong><i><u>" + statusStr + "</u></i></strong></p>";
        if(!author.isEmpty() && !dateStr.isEmpty()) {
            reviewText += "<p style=\"padding-top:7px\">Reviewed by <strong>" + author + "</strong> on <strong>" + dateStr + "</strong></p>";
        }
        if(!comment.isEmpty()) {
            reviewText += "<p style=\"padding-top:4px\">Comment: \"" + comment + "\"</p>";
        }
        reviewText += "</html>";
        return reviewText;
    }

    @Override
    public void dispose() {
        diffManager.removeListener(diffListener);
    }
}
