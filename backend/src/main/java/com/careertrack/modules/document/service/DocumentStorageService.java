package com.careertrack.modules.document.service;

import java.util.UUID;

public interface DocumentStorageService {

    /**
     * Enregistre le contenu binaire d'un document.
     */
    void store(UUID documentId, byte[] data);

    /**
     * Récupère le contenu binaire d'un document.
     */
    byte[] load(UUID documentId);

    /**
     * Supprime le contenu binaire d'un document.
     */
    void delete(UUID documentId);
}
