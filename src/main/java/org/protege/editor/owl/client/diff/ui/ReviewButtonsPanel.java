package org.protege.editor.owl.client.diff.ui;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ProjectId;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.SessionRecorder;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.client.event.CommitOperationEvent;
import org.protege.editor.owl.client.ui.UserLoginPanel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.server.util.SnapShot;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.protege.editor.owl.ui.util.ProgressDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ReviewButtonsPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -5575738883023751161L;
    private LogDiffManager diffManager;
    private ReviewManager reviewManager;
    private OWLEditorKit editorKit;
    private JButton rejectBtn, clearBtn, commitBtn, squashHistoryBtn, conceptHistoryBtn, restartPelletBtn;
    private boolean read_only = false;
    private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private ProgressDialog dlg = new ProgressDialog();

    /**
     * Constructor
     *
     * @param modelManager  OWL model manager
     * @param editorKit OWL editor kit
     */
    public ReviewButtonsPanel(OWLModelManager modelManager, OWLEditorKit editorKit, boolean read_only) {
        this.editorKit = checkNotNull(editorKit);
        this.diffManager = LogDiffManager.get(modelManager, editorKit);
        this.reviewManager = diffManager.getReviewManager();
        this.read_only = read_only;
        setLayout(new FlowLayout(FlowLayout.CENTER, 2, 3));
        addButtons();
    }

    private void addButtons() {
        
        rejectBtn = getButton("Reject", rejectBtnListener);
        rejectBtn.setToolTipText("Reject selected change(s); rejected changes are undone");

        clearBtn = getButton("Clear", clearBtnListener);
        clearBtn.setToolTipText("Reset selected change(s) to review-pending status");

        commitBtn = getButton("Commit", commitBtnListener);
        commitBtn.setToolTipText("Commit all change reviews");

        squashHistoryBtn = getButton("Squash History", squashHistoryBtnListener);
        squashHistoryBtn.setToolTipText("Squash and Archive history on server");
        
        conceptHistoryBtn = getButton("History", conceptHistoryBtnListener);
        conceptHistoryBtn.setToolTipText("Push the concept history to the server");

        restartPelletBtn = getButton("Restart Pellet", restartPelletBtnListener);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(20, 0));

        add(rejectBtn); add(clearBtn); add(separator); add(commitBtn); add(squashHistoryBtn);
        add(conceptHistoryBtn);
        add(restartPelletBtn);
        diffManager.addListener(changeSelectionListener);
        enable(true, squashHistoryBtn);
        enable(true, conceptHistoryBtn);

        boolean isWorkflowManager = false;
        ClientSession sess = ClientSession.getInstance(editorKit);
        try {
            ((LocalHttpClient) sess.getActiveClient()).isWorkFlowManager(sess.getActiveProject());
        } catch (Exception e) { } // session and client can be null when this view is created
        enable(isWorkflowManager, restartPelletBtn);
    }

    private LogDiffListener changeSelectionListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if(event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if(!diffManager.getSelectedChanges().isEmpty() && diffManager.isAllComplexEditChangesSelected()) {
                    enable(true, clearBtn, rejectBtn, commitBtn);
                }
                else {
                    enable(false, clearBtn, rejectBtn, commitBtn);
                }
            }
            if(event.equals(LogDiffEvent.CHANGE_REVIEWED) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                if(reviewManager.hasUncommittedReviews()) {
                    enable(true, commitBtn);
                }
                else {
                    enable(false, commitBtn);
                }
            }
        }
    };

    private ActionListener rejectBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.setReviewStatus(c, ReviewStatus.REJECTED);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener clearBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.setReviewStatus(c, ReviewStatus.PENDING);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener squashHistoryBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent _e) {
            ClientSession clientSession = ClientSession.getInstance(editorKit);
            SnapShot snapshot = new SnapShot(clientSession.getActiveVersionOntology().getOntology());
            ProjectId projectId = clientSession.getActiveProject();
            LocalHttpClient client = (LocalHttpClient) clientSession.getActiveClient();
            
            OWLOntologyManager manager = clientSession.getActiveVersionOntology().getOntology().getOWLOntologyManager();

            dlg.setMessage("Squashing history.");

            ListenableFuture<Boolean> squashFuture = service.submit(
                    new Callable<Boolean>() {
                        public Boolean call() {
                            try {
                                client.squashHistory(snapshot, projectId);

                                dlg.setVisible(false);
                                return Boolean.TRUE;
                            }
                            catch (ClientRequestException e) {
                                dlg.setVisible(false);
                                return Boolean.FALSE;
                            }
                        }});
            dlg.setVisible(true);

            try {
                if (squashFuture.get()) {
                    info("History squashed and archived on server, resetting ontology");

                    SessionRecorder.getInstance(editorKit).stopRecording();

                    clientSession.reset();                    
                    
                    try {
						ServerDocument serverDocument = client.openProject(projectId).serverDocument;
						VersionedOWLOntology vont = client.buildVersionedOntology(
			                    serverDocument, manager, projectId);
						clientSession.setActiveProject(projectId, vont);
					} catch (AuthorizationException | ClientRequestException e) {
						throw new ExecutionException(e);
					}  
					
                    SessionRecorder.getInstance(editorKit).startRecording();
                    
                    // ClientSession.setActiveProject also fires an event, so this isn't needed?
                    //editorKit.getModelManager().fireEvent(EventType.ACTIVE_ONTOLOGY_CHANGED);
                    
                } else {
                    warn("Unable to squash history.");
                }
            } catch (InterruptedException | ExecutionException e) {
                warn("Unable to squash history." + e);

            } finally {
                SessionRecorder.getInstance(editorKit).startRecording();

            }
        }
    };
    
    private void warn(String msg) {
		JOptionPane.showMessageDialog(this, 
				msg, "Warning", JOptionPane.WARNING_MESSAGE);
	}
    
    private void info(String msg) {
		JOptionPane.showMessageDialog(this, 
				msg, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
    
    private ActionListener conceptHistoryBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
        	ClientSession sess = ClientSession.getInstance(editorKit);
        	try {
        		if (((LocalHttpClient) sess.getActiveClient()).isWorkFlowManager(sess.getActiveProject())) {
        			((LocalHttpClient) sess.getActiveClient()).genConceptHistory(sess.getActiveProject());
        			info("Concept history recorded on server.");
                } else {
                	warn("Only Workflow Managers can generate history.");
                }
				
			} catch (LoginTimeoutException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (AuthorizationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClientRequestException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	 
        }
    };

    private ActionListener restartPelletBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ClientSession sess = ClientSession.getInstance(editorKit);
            LocalHttpClient client = (LocalHttpClient) sess.getActiveClient();

            if (!client.isWorkFlowManager(sess.getActiveProject())) return;

            // TODO: implement this
//            pelletService.restart();
        }
    };

    public List<String> getCommitComments() {
        VersionedOWLOntology vont = ClientSession.getInstance(editorKit).getActiveVersionOntology();
        ChangeHistory changes = vont.getChangeHistory();
        DocumentRevision base = changes.getBaseRevision();
        DocumentRevision head = changes.getHeadRevision();
        List<String> comments = new ArrayList<String>();
        for (DocumentRevision rev = base.next(); rev.behindOrSameAs(head); rev = rev.next()) {
            RevisionMetadata metaData = changes.getMetadataForRevision(rev);
            comments.add(metaData.getComment());
        }
       
        return comments;
    }

    private ActionListener commitBtnListener = e -> {
        Container owner = SwingUtilities.getAncestorOfClass(Frame.class, editorKit.getOWLWorkspace());
        int answer = JOptionPane.showOptionDialog(owner, "Committing these reviews may involve undoing or redoing previous changes.\n" +
                "Are you sure you would like to proceed?", "Confirm reviews", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if(answer == JOptionPane.OK_OPTION) {
            List<OWLOntologyChange> changes = reviewManager.getReviewOntologyChanges();
            reviewManager.clearUncommittedReviews();
            enable(false, commitBtn);
            if(!changes.isEmpty()) {
                VersionedOWLOntology vont = diffManager.getVersionedOntologyDocument().get();
                String commitComment = JOptionPane.showInputDialog(owner, "Comment for the review: ", "Commit reviews");
                if (vont == null) {
                    JOptionPane.showMessageDialog(owner, "Commit ignored because the ontology is not associated with a server");
                    return;
                }
                if (commitComment == null) {
                    return; // user pressed cancel
                }
                boolean success = commit(vont, changes, commitComment);
                if(success) {
                    JOptionPane.showMessageDialog(owner, "The reviews have been successfully committed", "Reviews committed", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    };

    private boolean commit(VersionedOWLOntology vont, List<OWLOntologyChange> changes, String commitComment) {
        try {
            ClientSession clientSession = ClientSession.getInstance(editorKit);
            Client client = clientSession.getActiveClient();
            RevisionMetadata metaData = new RevisionMetadata(
                    client.getUserInfo().getId(),
                    client.getUserInfo().getName(),
                    client.getUserInfo().getEmailAddress(), "[Review] " + commitComment);

            diffManager.applyOntologyChanges(changes); // apply changes to active ontology
            Commit commit = new Commit(metaData, changes);
            CommitBundle bundle = new CommitBundleImpl(vont.getHeadRevision(), commit);
            ChangeHistory history = client.commit(clientSession.getActiveProject(), bundle);
            vont.update(history);
            clientSession.fireCommitPerformedEvent(new CommitOperationEvent(
                    history.getHeadRevision(),
                    history.getMetadataForRevision(history.getHeadRevision()),
                    history.getChangesForRevision(history.getHeadRevision())));
            return true;
        } catch (LoginTimeoutException ex1) {
            // TODO timeouts would ideally be dealt with in a central client handler, rather than forcing all client code to deal with them
            Optional<AuthToken> authToken = UserLoginPanel.showDialog(editorKit, this);
            return authToken.isPresent() && authToken.get().isAuthorized() && commit(vont, changes, commitComment);
        } catch (AuthorizationException | ClientRequestException ex2) {
            ErrorLogPanel.showErrorDialog(ex2);
            return false;
        }
    }

    private JButton getButton(String text, ActionListener listener) {
        JButton button = new JButton();
        button.setText(text);
        button.setPreferredSize(new Dimension(95, 32));
        button.setFocusable(false);
        button.addActionListener(listener);
        button.setEnabled(false);
        return button;
    }

    private void enable(boolean enable, JComponent... components) {
    	for(JComponent c : components) {
    		if (read_only) {
    			c.setEnabled(false);
    		} else {
    			c.setEnabled(enable);
    		}
    	}
    }

    @Override
    public void dispose() {
        rejectBtn.removeActionListener(rejectBtnListener);
        clearBtn.removeActionListener(clearBtnListener);
        commitBtn.removeActionListener(commitBtnListener);
        squashHistoryBtn.removeActionListener(squashHistoryBtnListener);
        restartPelletBtn.removeActionListener(restartPelletBtnListener);
        diffManager.removeListener(changeSelectionListener);
    }
}
