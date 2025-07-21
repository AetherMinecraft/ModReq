package dev.bwmp.modReq.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ModRequestNote {

    private int id;
    private int requestId;
    private UUID authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    public ModRequestNote() {
        this.createdAt = LocalDateTime.now();
    }

    public ModRequestNote(int requestId, UUID authorId, String authorName, String content) {
        this();
        this.requestId = requestId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("ModRequestNote{id=%d, requestId=%d, author=%s, content='%s'}",
                id, requestId, authorName, content);
    }
}
