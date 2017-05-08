package org.protege.editor.owl.client.diff.model;

import java.util.Comparator;

public class ChangeCommitDateCompare implements Comparator<CommitMetadata> {
	boolean ascending;
	
	public ChangeCommitDateCompare(boolean ascending) {
		this.ascending = ascending;
	}
	
	@Override
	public int compare(CommitMetadata data1, CommitMetadata data2) {
		
		if (ascending) {
			return data1.getDate().compareTo(data2.getDate());
		} else {
			return data2.getDate().compareTo(data1.getDate());
		}
	}

}
