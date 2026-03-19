package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UgcContentRequest {

    @NotNull(message = "authorProfilId est obligatoire")
    private Long authorProfilId;

    @NotBlank(message = "title est obligatoire")
    @Size(max = 120, message = "title est trop long")
    private String title;

    @NotBlank(message = "body est obligatoire")
    @Size(max = 2000, message = "body est trop long")
    private String body;

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
}
