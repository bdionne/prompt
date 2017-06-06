package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.Change;
import org.protege.editor.owl.client.diff.model.LogDiff;
import org.protege.editor.owl.client.diff.model.LogDiffEvent;
import org.protege.editor.owl.client.diff.model.LogDiffListener;
import org.protege.editor.owl.client.diff.model.LogDiffManager;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = 8377485189413724504L;
    private LogDiffManager diffManager;
    private LogDiff diff;
    private JList<String> authorsList = new JList<>();

    /**
     * Constructor
     *
     * @param modelManager  OWL model manager
     * @param editorKit OWL editor kit
     */
    public AuthorPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);
        diff = diffManager.getDiffEngine();
        setLayout(new BorderLayout(20, 20));
        setupList();

        JScrollPane scrollPane = new JScrollPane(authorsList);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        add(scrollPane, BorderLayout.CENTER);
        listAuthors();
    }

    private ListSelectionListener listSelectionListener = e -> {
        String selection = authorsList.getSelectedValue();
        if (selection != null && !e.getValueIsAdjusting()) {
            diffManager.setSelectedAuthor(selection);
        }
    };

    private LogDiffListener diffListener = event -> {
        if (event.equals(LogDiffEvent.ONTOLOGY_UPDATED) || event.equals(LogDiffEvent.COMMIT_OCCURRED) || 
        		event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
            listAuthors();
        }
    };

    private void setupList() {
        authorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        authorsList.addListSelectionListener(listSelectionListener);
        authorsList.setCellRenderer(new AuthorListCellRenderer());
        authorsList.setFixedCellHeight(35);
        authorsList.setBorder(GuiUtils.MATTE_BORDER);
    }

    private void listAuthors() {
        if(diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOWLOntology vont = diffManager.getVersionedOntologyDocument().get();
            ChangeHistory changes = vont.getChangeHistory();
            List<String> users = new ArrayList<>();
            DocumentRevision base = changes.getBaseRevision();
            DocumentRevision head = changes.getHeadRevision();
            for (DocumentRevision rev = base.next(); rev.behindOrSameAs(head); rev = rev.next()) {
                RevisionMetadata metaData = changes.getMetadataForRevision(rev);
                String user = metaData.getAuthorId();
                
                List<Integer> userCountList = new ArrayList<Integer>();
                if (!users.contains(user)) {
                    users.add(user);
                    userCountList.add(1);
                    userCountList.add(getConflictCountForUser(user));
                    user_cnts.put(user, userCountList);
                } else {
                	Integer c_cnt = user_cnts.get(user).get(0);
                	user_cnts.get(user).set(0, c_cnt + 1);
                }
            }
            Collections.sort(users);
            if(!users.isEmpty()) {
                users.add(0, LogDiffManager.ALL_AUTHORS);
            }
            
            diffManager.setUserCounts(user_cnts);
            
            authorsList.setListData(users.toArray(new String[users.size()]));
        }
        else {
            authorsList.setListData(new String[0]);
        }
    }
    
    private int getConflictCountForUser(String user) {
    	List<Change> userChanges = diff.getChangesForUser(user);
        int conflictCount = 0;
        for (Change userChange : userChanges) {
        	if (userChange.isConflicting()) {
        		conflictCount++;
        	}
        }
    	return conflictCount;
    }
    
    private HashMap<String, List<Integer>> user_cnts = new HashMap<String, List<Integer>>();

    @Override
    public void dispose() {
        authorsList.removeListSelectionListener(listSelectionListener);
        diffManager.removeListener(diffListener);
    }
}
