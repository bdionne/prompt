package org.protege.editor.owl.client.diff.ui;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.protege.editor.owl.client.diff.model.Change;
import org.protege.editor.owl.client.diff.model.ChangeDetails;
import org.protege.editor.owl.client.diff.model.CommitMetadata;
import org.protege.editor.owl.client.diff.model.LogDiff;
import org.protege.editor.owl.client.diff.model.LogDiffEvent;
import org.protege.editor.owl.client.diff.model.LogDiffListener;
import org.protege.editor.owl.client.diff.model.LogDiffManager;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.tree.UserRendering;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassHierarchyViewComponent;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

public class TreeView extends AbstractOWLClassHierarchyViewComponent /*AbstractOWLViewComponent*/ {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3417030023003101000L;
	//private TreePanel treePanel;
	private LogDiffManager diffManager;
	private LogDiff diff;
	
	public TreeView() {}

    /*@Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        treePanel = new TreePanel(getOWLModelManager(), getOWLEditorKit());
        add(treePanel, BorderLayout.CENTER);
    }*/

    @Override
    public void disposeView() {
		//treePanel.dispose();
    }

	@Override
	protected void performExtraInitialisation() throws Exception {
		diffManager = LogDiffManager.get(this.getOWLModelManager(), this.getOWLEditorKit());
        diffManager.addListener(diffListener);
        diff = diffManager.getDiffEngine();
	}

	private LogDiffListener diffListener = new LogDiffListener() {
		@Override
		public void statusChanged(LogDiffEvent event) {
			// TODO Auto-generated method stub
			if(event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED)) {
                diffManager.clearSelectedChanges();
                updateDiff(event);
                
            } else {
            	
            }
		}
    };
    
    private void updateDiff(LogDiffEvent event) {
    	if(diff.getChanges().isEmpty()) {
            diff.initDiff();
        }
    	CommitMetadata metadata = diffManager.getSelectedCommit();
        //this.getTree().setSelectedOWLObject(metadata.getClass());
    	List<Change> changesToDisplay = diff.getChangesToDisplay(event);
        Collections.sort(changesToDisplay);
        for ( Change changeToDisplay : changesToDisplay ) {
        	if ( changeToDisplay != null ) {
        		ChangeDetails changeDetails = changeToDisplay.getDetails();
        		OWLObject changeObj = changeDetails.getSubject();
        		if ( changeObj instanceof IRI ) {
	        		Set<OWLEntity> owlEntitySet = this.getOWLModelManager().getActiveOntology().getEntitiesInSignature((IRI)changeObj);
	        		
	    			for ( OWLEntity owlEntity : owlEntitySet ) {
	    				if (owlEntity.isOWLClass()) {
							OWLClass subj = owlEntity.asOWLClass();
							this.getTree().setSelectedOWLObject(subj);
	    				}
	    			}
        		}
        		
        	}
        }
        
    }
    
	@Override
	protected UserRendering getUserRenderer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OWLObjectHierarchyProvider<OWLClass> getHierarchyProvider() {
        return getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
    }

	@Override
	protected Optional<OWLObjectHierarchyProvider<OWLClass>> getInferredHierarchyProvider() {
		return Optional.of(getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider());
	}
}
