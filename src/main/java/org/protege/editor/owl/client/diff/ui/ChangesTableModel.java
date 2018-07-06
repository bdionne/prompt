package org.protege.editor.owl.client.diff.ui;

import edu.stanford.protege.metaproject.api.UserId;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.Change;
import org.protege.editor.owl.client.diff.model.ChangeMode;
import org.protege.editor.owl.client.diff.model.ChangeType;
import org.protege.editor.owl.client.diff.model.Review;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.semanticweb.owlapi.search.Searcher.annotationObjects;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangesTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 2145701527431928323L;
    private List<Change> changes = new ArrayList<>();
    private OWLOntology ontology; 

    /**
     * No-args constructor
     */
    public ChangesTableModel(OWLEditorKit editorKit) { 
    	ontology = editorKit.getOWLModelManager().getActiveOntology();    	
    }

    public void setChanges(List<Change> changes) {
        this.changes = checkNotNull(changes);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return (changes != null ? changes.size() : 0);
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Change change = getChange(rowIndex);
        switch (Column.values()[columnIndex]) {
            case MODE:
                return change.getMode();
            case DATE:
                return change.getCommitMetadata().getDate();
            case AUTHOR:
                return change.getCommitMetadata().getAuthor();
            case CHANGE_SUBJECT:
            	OWLObject obj = change.getDetails().getSubject();
            	if (obj instanceof IRI) {
            		OWLEntity entity = getEntity((IRI)obj);
            		if (entity != null) {
            			if (entity instanceof OWLClass)
            				return getRDFSLabel((OWLClass) entity);
            			else if (entity instanceof OWLProperty) {
            				return ((OWLAnnotationProperty)entity).getIRI().getShortForm();
            			}
            		}
            		return ((IRI) obj).getShortForm();	
            	} else if (obj instanceof OWLClass) {
            		return getRDFSLabel((OWLClass)obj);
            	} else if (!(obj instanceof OWLObject)) {
            		return obj.toString();
            	}
            case CHANGE_TYPE:
                return change.getDetails().getType();
            case REVISION_TAG:
                return change.getDetails().getRevisionTag().getTag();
            case COMMENT:
                return change.getCommitMetadata().getComment();
            case CONFLICT:
                return change.isConflicting();
            case REVIEW:
                return change.getReview();
            default:
                throw new IllegalStateException();
        }
    }
    
    private OWLEntity getEntity(IRI iri) {
    	Set<OWLEntity> classes = ontology.getEntitiesInSignature(iri);
		for (OWLEntity et : classes) {
			if (et instanceof OWLClass) {
				return et.asOWLClass();
			}
			return et;
		}
		return null;
    }

    private String getRDFSLabel(OWLClass cls) {
    	String rdfsLabel = null;
		
		for (OWLAnnotation annotation : annotationObjects(ontology.getAnnotationAssertionAxioms(cls.getIRI()), ontology.getOWLOntologyManager().getOWLDataFactory()
				.getRDFSLabel())) {
			OWLAnnotationValue av = annotation.getValue();
			com.google.common.base.Optional<OWLLiteral> ol = av.asLiteral();
			if (ol.isPresent()) {
				rdfsLabel = ol.get().getLiteral();
			}
		}
    	return rdfsLabel;
    }
    
    public Change getChange(int rowIndex) {
        return changes.get(rowIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        switch (col) {
            case DATE:
                return Date.class;
            case AUTHOR:
                return UserId.class;
            case MODE:
                return ChangeMode.class;
            case COMMENT:
                return String.class;
            case CHANGE_TYPE:
                return ChangeType.class;
            case CHANGE_SUBJECT:
                return OWLObject.class;
            case CONFLICT:
                return Boolean.class;
            case REVIEW:
                return Review.class;
            case REVISION_TAG:
                return String.class;
            default:
                throw new IllegalStateException("Programmer Error: a case was missed");
        }
    }

    public String getColumnName(int column) {
        return Column.values()[column].toString();
    }

    public void clear() {
        changes.clear();
        fireTableDataChanged();
    }

    public enum Column {
        MODE("Mode"),
        DATE("Date"),
        AUTHOR("Author"),
        CHANGE_SUBJECT("Change Subject"),
        CHANGE_TYPE("Type"),
        REVISION_TAG("Revision Tag"),
        COMMENT("Comment"),
        CONFLICT("Conflict"),
        REVIEW("Review");

        private String name;

        Column(String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
