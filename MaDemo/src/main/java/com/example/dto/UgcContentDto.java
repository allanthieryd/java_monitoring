package com.example.dto;

import java.time.Instant;

import com.example.entity.ContentStatus;

public class UgcContentDto {

    private Long id;
    private Long authorProfilId;
    private String title;
    private String body;
    private ContentStatus status;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorProfilId() {
        return authorProfilId;
    }

    public void setAuthorProfilId(Long authorProfilId) {
        this.authorProfilId = authorProfilId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
