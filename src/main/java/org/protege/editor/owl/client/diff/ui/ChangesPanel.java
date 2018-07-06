package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.model.OWLModelManager;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangesPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -6318728935982700515L;
    private OWLEditorKit editorKit;
    private LogDiffManager diffManager;
    private ChangesTableModel diffTableModel;
    private JTable table;
    private LogDiff diff;

    /**
     * Constructor
     *
     * @param modelManager  OWL model manager
     * @param editorKit OWL editor kit
     */
    public ChangesPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);
        diff = diffManager.getDiffEngine();

        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);

        createDiffTable();
    }

    private ListSelectionListener rowSelectionListener = e -> {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel lsm = (ListSelectionModel)e.getSource();
        Change complexEditChange = null;
        if (!lsm.isSelectionEmpty()) {
            List<Change> selectedChanges = new ArrayList<>();
            int selectedRowCount = table.getSelectedRowCount();
            int selectedComplexEditCount = 0;
            for(int i = 0; i < selectedRowCount; i++) {
                Change change = diffTableModel.getChange(table.convertRowIndexToModel(table.getSelectedRows()[i]));
                selectedChanges.add(change);
                String comment = change.getCommitMetadata().getComment();
                if (comment.endsWith(" - SPLIT") || comment.endsWith(" - MERGE") || comment.endsWith(" - RETIRE")) {
                	complexEditChange = change;
                	selectedComplexEditCount++;
                }
                		
            }
            if (complexEditChange != null) {
            	List<Change> changes = diffManager.getDiffEngine().getChangesForCommit(complexEditChange.getCommitMetadata());
            	if (changes != null && changes.size() != selectedComplexEditCount) {
            		diffManager.setAllComplexEditChangesSelected(false);
            	} else {
            		diffManager.setAllComplexEditChangesSelected(true);
            	}
            } else {
            	diffManager.setAllComplexEditChangesSelected(true);
            }
            diffManager.setSelectedChanges(selectedChanges);
        }
    };

    private LogDiffListener diffListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if(event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) || event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED)) {
                diffManager.clearSelectedChanges();
                updateDiff(event);
            } else if(event.equals(LogDiffEvent.COMMIT_OCCURRED)) {
                updateDiff(event);
                diffTableModel.clear();
            }
            else if(event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) { // TODO incrementally update change indices
                diff.clear();
                diff.initDiff();
                diffTableModel.setChanges(Collections.emptyList());
            } else if(event.equals(LogDiffEvent.CHANGE_REVIEWED)) {
                revalidate(); repaint();
            } else if(event.equals(LogDiffEvent.RESET)) {
                diffTableModel.clear();
            }
        }
    };

    private void updateDiff(LogDiffEvent event) {
        if(diff.getChanges().isEmpty()) {
            diff.initDiff();
        }
        List<Change> changesToDisplay = diff.getChangesToDisplay(event);
        Collections.sort(changesToDisplay);
        diffTableModel.setChanges(changesToDisplay);
    }

    private void createDiffTable() {
        // create diff table model
        diffTableModel = new ChangesTableModel(editorKit);

        // create diff table
        table = new ChangesTable(diffTableModel, editorKit);
        setColumnsWidth(table, 3, 13, 15, 15, 10, 7, 27, 5, 5);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        add(scrollPane, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(rowSelectionListener);

        // allow sorting columns (sort initially by the date column)
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(diffTableModel);
        sorter.setComparator(ChangesTableModel.Column.CHANGE_SUBJECT.ordinal(), new MyComparator<Object>(diffTableModel));
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(ChangesTableModel.Column.DATE.ordinal(), SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        table.setRowSorter(sorter);
    }
    
    private class MyComparator<Object> implements Comparator<Object> {
    	
    	private AbstractTableModel model = null;

    	public MyComparator(AbstractTableModel mod) {
    		model = mod;
    	}
		public int compare(Object o1, Object o2) {
			// TODO Auto-generated method stub
			return ((String) o1).compareToIgnoreCase((String) o2);
		}
    	
    };
    
    private void setColumnsWidth(JTable table, double... values) {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int)values[i]*100);
        }
    }

    @Override
    public void dispose() {
        diffManager.removeListener(diffListener);
    }
}
