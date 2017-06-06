package org.protege.editor.owl.client.diff.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class CommitMetadataImpl implements CommitMetadata {
    private final CommitId commitId;
    private final String userId;
    private final Date date;
    private final String comment;
    private final int conflictCount;
    private final String conflictAuthor;

    /**
     * Constructor
     *
     * @param commitId  Commit identifier
     * @param userId    User identifier
     * @param date  Commit date
     * @param comment   Commit comment
     */
    public CommitMetadataImpl(CommitId commitId, String userId, Date date, String comment) {
        this.commitId = checkNotNull(commitId);
        this.userId = checkNotNull(userId);
        this.date = checkNotNull(date);
        this.comment = checkNotNull(comment);
        this.conflictCount = 0;
        this.conflictAuthor = "";
    }
    
    public CommitMetadataImpl(CommitId commitId, String userId, Date date, String comment, int conflictCount, String conflictAuthor) {
        this.commitId = checkNotNull(commitId);
        this.userId = checkNotNull(userId);
        this.date = checkNotNull(date);
        this.comment = checkNotNull(comment);
        this.conflictCount = conflictCount;
        this.conflictAuthor = conflictAuthor;
    }

    @Override
    public CommitId getCommitId() {
        return commitId;
    }

    @Override
    public String getAuthor() {
        return userId;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public int getConflictCount() {
    	return conflictCount;
    }
    
    @Override
    public String getConflictAuthor() {
    	return conflictAuthor;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitMetadataImpl commit = (CommitMetadataImpl) o;
        return Objects.equal(commitId, commit.commitId) &&
                Objects.equal(userId, commit.userId) &&
                Objects.equal(date, commit.date) &&
                Objects.equal(comment, commit.comment)&&
                Objects.equal(conflictCount, commit.conflictCount);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(commitId, userId, date, comment, conflictCount);
    }

    @Override
    public String toString() {
    	
        return MoreObjects.toStringHelper(this)
                .add("commitId", commitId)
                .add("userId", userId)
                .add("date", date)
                .add("comment", comment)
                .add((conflictCount>0 ? "conflict" : ""), (conflictCount>0 ? conflictCount : ""))
                .toString();
    }

    @Override
    public int compareTo(CommitMetadata that) {
        return that.getDate().compareTo(this.date); // compare w.r.t. date only
    }
}
