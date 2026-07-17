package com.example.event;

import org.springframework.context.ApplicationEvent;

/**
 * Publié lorsqu'un contenu UGC passe à l'état PUBLISHED.
 * La modération peut écouter cet événement pour déclencher
 * une vérification automatique si nécessaire.
 */
public class UgcPublishedEvent extends ApplicationEvent {

    private final Long contentId;
    private final Long authorProfilId;

    public UgcPublishedEvent(Object source, Long contentId, Long authorProfilId) {
        super(source);
        this.contentId = contentId;
        this.authorProfilId = authorProfilId;
    }

    public Long getContentId() { return contentId; }
    public Long getAuthorProfilId() { return authorProfilId; }
}
