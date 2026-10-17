import React, { useState } from 'react';
import {
  FileText,
  Download,
  Search,
  Filter,
  CheckCircle,
  FileCheck
} from 'lucide-react';

interface GeneratedDoc {
  id: string;
  employeeName: string;
  docType: 'ATTESTATION' | 'CONTRAT' | 'BULLETIN_PAIE';
  date: string;
  url: string;
}

const mockDocs: GeneratedDoc[] = [
  {
    id: 'doc-1',
    employeeName: 'Amadou Diop',
    docType: 'ATTESTATION',
    date: '2026-03-25',
    url: '/api/v1/documents/attestation/1',
  },
  {
    id: 'doc-2',
    employeeName: 'Amadou Diop',
    docType: 'BULLETIN_PAIE',
    date: '2026-03-25',
    url: '/api/v1/documents/payslip/1',
  },
  {
    id: 'doc-3',
    employeeName: 'Fatou Ndiaye',
    docType: 'ATTESTATION',
    date: '2026-03-20',
    url: '/api/v1/documents/attestation/2',
  },
  {
    id: 'doc-4',
    employeeName: 'Aïssatou Sow',
    docType: 'CONTRAT',
    date: '2025-09-01',
    url: '/api/v1/documents/attestation/4',
  },
];

export const DocumentListPage: React.FC = () => {
  const [filterType, setFilterType] = useState('ALL');

  const filtered = mockDocs.filter(
    (d) => filterType === 'ALL' || d.docType === filterType
  );

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Gestion des Documents RH & PDF</h1>
          <p>Édition certifiée conforme des attestations de travail, contrats et bulletins de paie (iTextPDF)</p>
        </div>
      </div>

      <div className="card p-4">
        <div className="flex gap-2">
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
            {filtered.map((doc) => (
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
                  <a
                    href={doc.url}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                  >
                    <Download className="w-3.5 h-3.5 text-[#1A6B3C]" />
                    Télécharger
                  </a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
export default DocumentListPage;
