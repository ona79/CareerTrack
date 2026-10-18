import React, { useState, useEffect, useMemo } from 'react';
import {
  Download,
  AlertCircle,
  RefreshCw,
  Loader2,
  FileText,
  Upload,
  Trash2,
  Plus,
  CheckCircle2,
  X,
  FileCheck
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import api from '@/lib/axios';
import { downloadFile } from '@/lib/downloadFile';
import type { DocumentResponseDto, DocumentType, Employee } from '@/types';

export const DocumentListPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const isDrh = hasRole('ROLE_DRH') || hasRole('DRH');
  const isEmploye = hasRole('ROLE_EMPLOYE') || hasRole('EMPLOYE');
  const isManager = hasRole('ROLE_MANAGER') || hasRole('MANAGER');
  const isCollaborator = isEmploye || isManager;

  // État des collaborateurs (pour la sélection par la DRH)
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('');
  const [isLoadingEmployees, setIsLoadingEmployees] = useState(false);

  // État des documents
  const [documents, setDocuments] = useState<DocumentResponseDto[]>([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Filtres
  const [filterType, setFilterType] = useState<string>('ALL');

  // Téléchargements en cours
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // Modale d'envoi
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadDocType, setUploadDocType] = useState<DocumentType>('CNI');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Modale de confirmation de suppression
  const [documentToDelete, setDocumentToDelete] = useState<DocumentResponseDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // 1. Initialiser le collaborateur cible
  useEffect(() => {
    if (isDrh) {
      setIsLoadingEmployees(true);
      api.get<{ data?: Employee[] | { content: Employee[] } }>('/employees')
        .then((res) => {
          const payload = res.data?.data;
          let list: Employee[] = [];
          if (Array.isArray(payload)) {
            list = payload;
          } else if (payload && 'content' in payload && Array.isArray(payload.content)) {
            list = payload.content;
          }
          setEmployees(list);
          if (list.length > 0) {
            setSelectedEmployeeId(list[0].id);
          } else if (user?.employeeId) {
            setSelectedEmployeeId(user.employeeId);
          }
        })
        .catch(() => {
          if (user?.employeeId) {
            setSelectedEmployeeId(user.employeeId);
          }
        })
        .finally(() => setIsLoadingEmployees(false));
    } else if (user?.employeeId) {
      setSelectedEmployeeId(user.employeeId);
    }
  }, [isDrh, user?.employeeId]);

  // 2. Charger les documents du collaborateur sélectionné
  const fetchDocuments = async (empId: string) => {
    if (!empId) return;
    setIsLoadingDocs(true);
    setErrorMessage(null);
    try {
      const res = await api.get<{ data: DocumentResponseDto[] }>(`/documents/employee/${empId}`);
      setDocuments(res.data?.data || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Impossible de récupérer la liste des documents.';
      setErrorMessage(msg);
    } finally {
      setIsLoadingDocs(false);
    }
  };

  useEffect(() => {
    if (selectedEmployeeId) {
      fetchDocuments(selectedEmployeeId);
    }
  }, [selectedEmployeeId]);

  // Collaborateur actuellement affiché
  const currentEmployee = useMemo(() => {
    if (isDrh && employees.length > 0) {
      return employees.find((e) => e.id === selectedEmployeeId);
    }
    return null;
  }, [isDrh, employees, selectedEmployeeId]);

  // Filtrage local
  const filteredDocs = useMemo(() => {
    if (filterType === 'ALL') return documents;
    return documents.filter((d) => d.documentType === filterType);
  }, [documents, filterType]);

  // Formatage de la taille
  const formatFileSize = (bytes?: number) => {
    if (!bytes || bytes === 0) return '0 Ko';
    const k = 1024;
    if (bytes < k) return `${bytes} o`;
    if (bytes < k * k) return `${(bytes / k).toFixed(1)} Ko`;
    return `${(bytes / (k * k)).toFixed(2)} Mo`;
  };

  // Formatage date
  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  // Téléchargement sécurisé
  const handleDownload = async (endpoint: string, filename: string, id: string) => {
    setDownloadingId(id);
    setErrorMessage(null);
    try {
      await downloadFile(endpoint, filename);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Échec du téléchargement du fichier';
      setErrorMessage(msg);
    } finally {
      setDownloadingId(null);
    }
  };

  // Gestion de la sélection de fichier avec contrôles stricts côté client
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUploadError(null);
    const file = e.target.files?.[0];
    if (!file) {
      setSelectedFile(null);
      return;
    }

    // Contrôle taille (max 5 Mo)
    if (file.size > 5 * 1024 * 1024) {
      setUploadError('Le fichier dépasse la taille maximale autorisée de 5 Mo.');
      setSelectedFile(null);
      e.target.value = '';
      return;
    }

    // Contrôle extension client
    const allowedExtensions = ['.pdf', '.jpg', '.jpeg', '.png'];
    const fileNameLower = file.name.toLowerCase();
    const hasValidExt = allowedExtensions.some((ext) => fileNameLower.endsWith(ext));
    if (!hasValidExt) {
      setUploadError('Format de fichier non supporté. Seuls les formats PDF, JPEG et PNG sont acceptés.');
      setSelectedFile(null);
      e.target.value = '';
      return;
    }

    setSelectedFile(file);
    if (!uploadTitle.trim()) {
      // Auto-compléter le titre avec le nom du fichier sans extension
      const baseName = file.name.substring(0, file.name.lastIndexOf('.')) || file.name;
      setUploadTitle(baseName.replace(/[_-]/g, ' '));
    }
  };

  // Soumission de l'upload
  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setUploadError(null);

    if (!selectedFile) {
      setUploadError('Veuillez sélectionner un fichier à téléverser.');
      return;
    }
    if (!uploadTitle.trim()) {
      setUploadError('Le libellé du document est obligatoire.');
      return;
    }
    if (!selectedEmployeeId) {
      setUploadError('Aucun collaborateur sélectionné.');
      return;
    }

    setIsSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('employeeId', selectedEmployeeId);
      formData.append('type', uploadDocType);
      formData.append('title', uploadTitle.trim());

      await api.post('/documents', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      setSuccessMessage('Document téléversé avec succès !');
      setIsUploadOpen(false);
      setSelectedFile(null);
      setUploadTitle('');
      setUploadDocType('CNI');
      // Rafraîchir la liste
      await fetchDocuments(selectedEmployeeId);
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erreur lors du téléversement du document.';
      setUploadError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Confirmation et exécution de suppression
  const handleConfirmDelete = async () => {
    if (!documentToDelete) return;
    setIsDeleting(true);
    try {
      await api.delete(`/documents/${documentToDelete.id}`);
      setSuccessMessage('Document supprimé avec succès.');
      setDocumentToDelete(null);
      await fetchDocuments(selectedEmployeeId);
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erreur lors de la suppression du document.';
      setErrorMessage(msg);
    } finally {
      setIsDeleting(false);
    }
  };

  // Vérifier si l'utilisateur a le droit de supprimer le document
  const canDeleteDoc = (doc: DocumentResponseDto) => {
    if (isDrh) return true;
    if (isCollaborator && doc.uploadedBy && doc.uploadedBy === user?.id) return true;
    return false;
  };

  return (
    <div className="space-y-6">
      {/* En-tête de page */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1 className="text-xl font-bold text-gray-900">Gestion des Documents RH & Dossier</h1>
          <p className="text-xs text-gray-500">
            Dossier administratif certifié (CNI, diplômes, contrats) et attestations officielles (iTextPDF)
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Bouton d'upload disponible pour DRH, EMPLOYE et MANAGER */}
          {(isDrh || isCollaborator) && selectedEmployeeId && (
            <button
              type="button"
              onClick={() => {
                setUploadError(null);
                setIsUploadOpen(true);
              }}
              className="btn btn-primary btn-sm inline-flex items-center gap-1.5 text-xs py-2 px-3 shadow-sm"
            >
              <Plus className="w-4 h-4" />
              Téléverser un document
            </button>
          )}
        </div>
      </div>

      {/* Bannière de succès */}
      {successMessage && (
        <div className="alert alert-success flex items-center justify-between p-3 rounded-lg border border-green-200 bg-green-50 text-green-800 text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-green-600 flex-shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button type="button" onClick={() => setSuccessMessage(null)} className="text-green-600 hover:text-green-900">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Bannière d'erreur avec bouton Réessayer */}
      {errorMessage && (
        <div className="alert alert-danger flex items-center justify-between p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => fetchDocuments(selectedEmployeeId)}
            className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 ml-4"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Réessayer
          </button>
        </div>
      )}

      {/* Barre de contexte collaborateur (Sélection DRH ou badge Employé) */}
      <div className="card p-4 bg-white border border-gray-100 rounded-lg shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        {isDrh ? (
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 w-full sm:w-auto">
            <label htmlFor="employee-select" className="text-xs font-semibold text-gray-700 whitespace-nowrap">
              Collaborateur concerné :
            </label>
            {isLoadingEmployees ? (
              <div className="inline-flex items-center gap-2 text-xs text-gray-500">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
                Chargement des salariés...
              </div>
            ) : (
              <select
                id="employee-select"
                value={selectedEmployeeId}
                onChange={(e) => setSelectedEmployeeId(e.target.value)}
                className="input input-sm text-xs rounded border-gray-300 py-1.5 px-3 bg-white font-medium text-gray-800 focus:border-[#1A6B3C] focus:ring-[#1A6B3C]"
              >
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.fullName || `${emp.firstName} ${emp.lastName}`} ({emp.employeeNumber})
                  </option>
                ))}
              </select>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">Espace personnel :</span>
            <span className="text-xs font-bold text-gray-900 bg-gray-100 px-2.5 py-1 rounded">
              {user?.username} (Matricule lié)
            </span>
          </div>
        )}

        {/* Raccourcis de génération directe des documents officiels certifiés pour ce salarié */}
        {selectedEmployeeId && (
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() =>
                handleDownload(
                  `/documents/attestation/${selectedEmployeeId}`,
                  `attestation-${currentEmployee?.employeeNumber || 'salarie'}.pdf`,
                  `cert-${selectedEmployeeId}`
                )
              }
              disabled={downloadingId === `cert-${selectedEmployeeId}`}
              className="btn btn-secondary btn-sm text-[11px] py-1.5 px-2.5 inline-flex items-center gap-1.5 border-gray-200 hover:border-[#1A6B3C]"
              title="Générer l'attestation de travail certifiée conforme iTextPDF"
            >
              {downloadingId === `cert-${selectedEmployeeId}` ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
              ) : (
                <FileCheck className="w-3.5 h-3.5 text-[#1A6B3C]" />
              )}
              Attestation PDF
            </button>

            {/* Contrat officiel généré (réservé DRH ou salarié concerné) */}
            <button
              type="button"
              onClick={() =>
                handleDownload(
                  `/documents/contract/${selectedEmployeeId}`,
                  `contrat-${currentEmployee?.employeeNumber || 'salarie'}.pdf`,
                  `contract-${selectedEmployeeId}`
                )
              }
              disabled={downloadingId === `contract-${selectedEmployeeId}`}
              className="btn btn-secondary btn-sm text-[11px] py-1.5 px-2.5 inline-flex items-center gap-1.5 border-gray-200 hover:border-[#1A6B3C]"
              title="Générer le contrat de travail certifié conforme iTextPDF"
            >
              {downloadingId === `contract-${selectedEmployeeId}` ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
              ) : (
                <FileText className="w-3.5 h-3.5 text-blue-600" />
              )}
              Contrat PDF
            </button>
          </div>
        )}
      </div>

      {/* Barre d'onglets de filtres */}
      <div className="card p-3 bg-white border border-gray-100 rounded-lg shadow-sm">
        <div className="flex flex-wrap gap-2">
          {[
            { key: 'ALL', label: 'Tous les documents' },
            { key: 'CNI', label: "Pièce d'Identité (CNI)" },
            { key: 'DIPLOME', label: 'Diplômes' },
            { key: 'CONTRAT', label: 'Contrats signés' },
            { key: 'AUTRE', label: 'Autres justificatifs' },
          ].map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilterType(tab.key)}
              className={`btn btn-sm text-xs py-1 px-3 rounded ${
                filterType === tab.key ? 'btn-primary bg-[#1A6B3C] text-white' : 'btn-secondary bg-gray-50 text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tableau des pièces jointes réelles du collaborateur */}
      <div className="table-container bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <table className="table w-full">
          <thead>
            <tr className="bg-gray-50 text-left text-[11px] font-semibold text-gray-500 uppercase tracking-wider">
              <th className="py-3 px-4">Libellé / Document</th>
              <th className="py-3 px-4">Type</th>
              <th className="py-3 px-4">Nom de fichier</th>
              <th className="py-3 px-4">Taille</th>
              <th className="py-3 px-4">Date d'ajout</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 text-xs">
            {isLoadingDocs ? (
              <tr>
                <td colSpan={6} className="text-center py-12 text-gray-400">
                  <Loader2 className="w-6 h-6 animate-spin mx-auto text-[#1A6B3C] mb-2" />
                  Chargement des documents du dossier...
                </td>
              </tr>
            ) : filteredDocs.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center py-12 text-gray-500">
                  <div className="max-w-xs mx-auto space-y-2">
                    <FileText className="w-8 h-8 text-gray-300 mx-auto" />
                    <p className="font-medium text-gray-700">Aucun document disponible</p>
                    <p className="text-[11px] text-gray-400">
                      Aucune pièce jointe correspondant à cette catégorie n'a été trouvée pour ce collaborateur.
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              filteredDocs.map((doc) => {
                const isDownloading = downloadingId === doc.id;
                const userCanDelete = canDeleteDoc(doc);
                return (
                  <tr key={doc.id} className="hover:bg-gray-50 transition-colors">
                    <td className="py-3 px-4 font-semibold text-gray-900">{doc.title}</td>
                    <td className="py-3 px-4">
                      <span className="badge badge-primary font-mono text-[10px] px-2 py-0.5 rounded bg-green-50 text-[#1A6B3C] border border-green-200 font-bold">
                        {doc.documentType}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-[11px] text-gray-600 truncate max-w-[200px]" title={doc.fileName}>
                      {doc.fileName}
                    </td>
                    <td className="py-3 px-4 text-gray-500">{formatFileSize(doc.fileSize)}</td>
                    <td className="py-3 px-4 text-gray-500">{formatDate(doc.uploadedAt)}</td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <button
                        type="button"
                        onClick={() => handleDownload(doc.downloadUrl, doc.fileName, doc.id)}
                        disabled={isDownloading}
                        className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 border-gray-200 hover:border-[#1A6B3C]"
                        title="Télécharger la pièce jointe"
                      >
                        {isDownloading ? (
                          <>
                            <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
                            Téléchargement...
                          </>
                        ) : (
                          <>
                            <Download className="w-3.5 h-3.5 text-[#1A6B3C]" />
                            Télécharger
                          </>
                        )}
                      </button>

                      {userCanDelete && (
                        <button
                          type="button"
                          onClick={() => setDocumentToDelete(doc)}
                          className="btn btn-sm text-[11px] py-1 px-2 inline-flex items-center gap-1 text-red-600 hover:bg-red-50 border border-transparent hover:border-red-200 rounded"
                          title="Supprimer ce document"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modale d'Upload */}
      {isUploadOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Upload className="w-5 h-5 text-[#1A6B3C]" />
                <h3 className="font-bold text-gray-900 text-sm">Téléverser une pièce justificative</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsUploadOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {uploadError && (
              <div className="p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
                <span>{uploadError}</span>
              </div>
            )}

            <form onSubmit={handleUploadSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-medium text-gray-700 mb-1">Collaborateur cible</label>
                <input
                  type="text"
                  disabled
                  value={
                    currentEmployee
                      ? `${currentEmployee.fullName} (${currentEmployee.employeeNumber})`
                      : user?.username || selectedEmployeeId
                  }
                  className="input w-full bg-gray-50 text-gray-500 cursor-not-allowed border-gray-200"
                />
              </div>

              <div>
                <label htmlFor="upload-doc-type" className="block font-medium text-gray-700 mb-1">
                  Type de document <span className="text-red-500">*</span>
                </label>
                <select
                  id="upload-doc-type"
                  value={uploadDocType}
                  onChange={(e) => setUploadDocType(e.target.value as DocumentType)}
                  className="input w-full text-xs"
                >
                  <option value="CNI">Carte Nationale d'Identité (CNI)</option>
                  <option value="DIPLOME">Diplôme universitaire / professionnel</option>
                  <option value="AUTRE">Autre pièce justificative</option>
                  {/* Règle 8(c) : CONTRAT disponible uniquement pour la DRH */}
                  {isDrh && <option value="CONTRAT">Contrat de travail signé (DRH)</option>}
                </select>
              </div>

              <div>
                <label htmlFor="upload-title" className="block font-medium text-gray-700 mb-1">
                  Libellé / Titre explicatif <span className="text-red-500">*</span>
                </label>
                <input
                  id="upload-title"
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  placeholder="Ex : CNI 2026, Diplôme Master 2, etc."
                  required
                  className="input w-full text-xs"
                />
              </div>

              <div>
                <label htmlFor="upload-file" className="block font-medium text-gray-700 mb-1">
                  Fichier joint (PDF, JPEG, PNG - Max 5 Mo) <span className="text-red-500">*</span>
                </label>
                <input
                  id="upload-file"
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                  onChange={handleFileChange}
                  required
                  className="block w-full text-xs text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-xs file:font-semibold file:bg-green-50 file:text-[#1A6B3C] hover:file:bg-green-100 cursor-pointer"
                />
                {selectedFile && (
                  <p className="mt-1 text-[11px] text-gray-500">
                    Fichier prêt : <span className="font-medium text-gray-700">{selectedFile.name}</span> ({formatFileSize(selectedFile.size)})
                  </p>
                )}
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <button
                  type="button"
                  onClick={() => setIsUploadOpen(false)}
                  disabled={isSubmitting}
                  className="btn btn-secondary btn-sm text-xs py-1.5 px-3"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || !selectedFile}
                  className="btn btn-primary btn-sm text-xs py-1.5 px-4 inline-flex items-center gap-1.5"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      Envoi en cours...
                    </>
                  ) : (
                    <>
                      <Upload className="w-3.5 h-3.5" />
                      Téléverser
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modale de confirmation de suppression */}
      {documentToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-sm w-full p-6 space-y-4">
            <div className="flex items-center gap-3 text-red-600">
              <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-sm">Confirmer la suppression</h3>
                <p className="text-xs text-gray-500">Cette action est définitive.</p>
              </div>
            </div>

            <p className="text-xs text-gray-600">
              Êtes-vous certain de vouloir supprimer le document{' '}
              <span className="font-semibold text-gray-900">"{documentToDelete.title}"</span> ? Le fichier binaire et ses
              métadonnées seront définitivement effacés.
            </p>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setDocumentToDelete(null)}
                disabled={isDeleting}
                className="btn btn-secondary btn-sm text-xs py-1.5 px-3"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="btn btn-sm text-xs py-1.5 px-3 bg-red-600 hover:bg-red-700 text-white rounded font-medium inline-flex items-center gap-1.5"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Suppression...
                  </>
                ) : (
                  'Supprimer'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentListPage;
