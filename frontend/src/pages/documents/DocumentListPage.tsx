import React, { useState } from 'react';
import {
  Download,
  AlertCircle,
  RefreshCw,
  Loader2,
  FileText
} from 'lucide-react';
import { downloadFile } from '@/lib/downloadFile';

interface GeneratedDoc {
  id: string;
  employeeName: string;
  docType: 'ATTESTATION' | 'CONTRAT' | 'BULLETIN_PAIE';
  date: string;
  endpoint: string;
  filename: string;
  isRealUuid: boolean;
}

// TODO (Étape 2 - Lot 1) : Ce raccordement avec les UUIDs du seed V3 est temporaire pour prouver le bon fonctionnement de l'étape 0.
// Il sera remplacé à l'étape 2 par la récupération dynamique des documents via l'API /api/v1/documents.
// En production (sans seed V3), le tableau est vide et affiche un état vide propre.
const demoDocuments: GeneratedDoc[] = import.meta.env.DEV
  ? [
      {
        id: 'real-doc-1',
        employeeName: 'Ousmane Diallo (EMP-004)',
        docType: 'ATTESTATION',
        date: '2026-09-30',
        endpoint: '/documents/attestation/55555555-5555-5555-5555-555555555504',
        filename: 'attestation-EMP-004.pdf',
        isRealUuid: true,
      },
      {
        id: 'real-doc-2',
        employeeName: 'Ousmane Diallo (EMP-004)',
        docType: 'BULLETIN_PAIE',
        date: '2026-09-30',
        endpoint: '/documents/payslip/99999999-9999-9999-9999-999999999901',
        filename: 'bulletin-paie-2026-09-EMP-004.pdf',
        isRealUuid: true,
      },
      {
        id: 'real-doc-3',
        employeeName: 'Ousmane Diallo (EMP-004)',
        docType: 'CONTRAT',
        date: '2022-06-01',
        endpoint: '/documents/contract/55555555-5555-5555-5555-555555555504',
        filename: 'contrat-EMP-004.pdf',
        isRealUuid: true,
      },
      {
        id: 'real-doc-4',
        employeeName: 'Mamadou Sow (EMP-003)',
        docType: 'ATTESTATION',
        date: '2026-09-30',
        endpoint: '/documents/attestation/55555555-5555-5555-5555-555555555503',
        filename: 'attestation-EMP-003.pdf',
        isRealUuid: true,
      },
      {
        id: 'mock-doc-fictif',
        employeeName: 'Exemple fictif non persisté',
        docType: 'ATTESTATION',
        date: '2025-01-01',
        endpoint: '/documents/attestation/mock-fictif',
        filename: 'attestation-fictive.pdf',
        isRealUuid: false,
      },
    ]
  : [];

export const DocumentListPage: React.FC = () => {
  const [filterType, setFilterType] = useState('ALL');
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [lastFailedDoc, setLastFailedDoc] = useState<GeneratedDoc | null>(null);

  const filtered = demoDocuments.filter(
    (d) => filterType === 'ALL' || d.docType === filterType
  );

  const handleDownload = async (doc: GeneratedDoc) => {
    if (!doc.isRealUuid) return;
    setDownloadingId(doc.id);
    setErrorMessage(null);
    setLastFailedDoc(null);

    try {
      await downloadFile(doc.endpoint, doc.filename);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Échec du téléchargement';
      setErrorMessage(msg);
      setLastFailedDoc(doc);
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Gestion des Documents RH & PDF</h1>
          <p>Édition certifiée conforme des attestations de travail, contrats et bulletins de paie (iTextPDF)</p>
        </div>
      </div>

      {errorMessage && (
        <div className="alert alert-danger flex items-center justify-between p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
          {lastFailedDoc && (
            <button
              type="button"
              onClick={() => handleDownload(lastFailedDoc)}
              className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 ml-4"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Réessayer
            </button>
          )}
        </div>
      )}

      <div className="card p-4">
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setFilterType('ALL')}
            className={`btn btn-sm ${filterType === 'ALL' ? 'btn-primary' : 'btn-secondary'} text-xs`}
          >
            Tous les documents
          </button>
          <button
            type="button"
            onClick={() => setFilterType('ATTESTATION')}
            className={`btn btn-sm ${filterType === 'ATTESTATION' ? 'btn-primary' : 'btn-secondary'} text-xs`}
          >
            Attestations de Travail
          </button>
          <button
            type="button"
            onClick={() => setFilterType('BULLETIN_PAIE')}
            className={`btn btn-sm ${filterType === 'BULLETIN_PAIE' ? 'btn-primary' : 'btn-secondary'} text-xs`}
          >
            Bulletins de Paie
          </button>
          <button
            type="button"
            onClick={() => setFilterType('CONTRAT')}
            className={`btn btn-sm ${filterType === 'CONTRAT' ? 'btn-primary' : 'btn-secondary'} text-xs`}
          >
            Contrats de Travail
          </button>
        </div>
      </div>

      <div className="table-container bg-white">
        <table className="table">
          <thead>
            <tr>
              <th>Collaborateur</th>
              <th>Type de Document</th>
              <th>Date d'émission</th>
              <th>Format</th>
              <th className="text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center py-8 text-xs text-gray-500">
                  Aucun document disponible pour le moment.
                </td>
              </tr>
            ) : (
              filtered.map((doc) => {
                const isLoading = downloadingId === doc.id;
                return (
                  <tr key={doc.id}>
                    <td className="font-semibold text-xs text-gray-900">{doc.employeeName}</td>
                    <td>
                      <span className="badge badge-primary font-mono text-[10px]">
                        {doc.docType}
                      </span>
                    </td>
                    <td className="text-xs text-gray-500">{doc.date}</td>
                    <td>
                      <span className="text-xs text-red-700 bg-red-50 px-2 py-0.5 rounded font-bold">
                        PDF iText
                      </span>
                    </td>
                    <td className="text-right">
                      {doc.isRealUuid ? (
                        <button
                          type="button"
                          onClick={() => handleDownload(doc)}
                          disabled={isLoading}
                          className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                        >
                          {isLoading ? (
                            <>
                              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
                              Génération...
                            </>
                          ) : (
                            <>
                              <Download className="w-3.5 h-3.5 text-[#1A6B3C]" />
                              Télécharger PDF
                            </>
                          )}
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled
                          title="Donnée de démonstration non persistée (non téléchargeable)"
                          className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 opacity-50 cursor-not-allowed"
                        >
                          <FileText className="w-3.5 h-3.5 text-gray-400" />
                          Désactivé (Mock)
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
    </div>
  );
};
export default DocumentListPage;
