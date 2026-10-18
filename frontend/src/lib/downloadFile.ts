import api from './axios';

export interface DownloadOptions {
  /** Nom de fichier par défaut si Content-Disposition est absent */
  defaultFilename?: string;
  /** Callback optionnel en cas d'erreur */
  onError?: (errorMessage: string) => void;
  /** Callback optionnel au démarrage du téléchargement */
  onStart?: () => void;
  /** Callback optionnel à la fin (succès ou erreur) */
  onFinish?: () => void;
}

/**
 * Extrait le nom de fichier à partir de l'en-tête Content-Disposition.
 */
function extractFilename(contentDisposition?: string, fallback = 'document.pdf'): string {
  if (!contentDisposition) return fallback;

  // Format RFC 5987 : filename*=UTF-8''mon_fichier.pdf
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim().replace(/["']/g, ''));
    } catch {
      // ignore
    }
  }

  // Format standard : filename="mon_fichier.pdf"
  const standardMatch = contentDisposition.match(/filename=["']?([^"';]+)["']?/i);
  if (standardMatch && standardMatch[1]) {
    return standardMatch[1].trim();
  }

  return fallback;
}

/**
 * Télécharge un fichier de manière sécurisée via l'instance Axios (JWT inclus),
 * gère la création/libération d'URL blob et le parsing des erreurs (JSON reçu au lieu d'un PDF).
 *
 * @param url Chemin de l'endpoint API (ex: '/documents/attestation/55555555-5555-...')
 * @param defaultFilename Nom de fichier de repli (ex: 'attestation.pdf')
 * @param options Options supplémentaires (callbacks, etc.)
 */
export async function downloadFile(
  url: string,
  defaultFilename = 'document.pdf',
  options?: DownloadOptions
): Promise<void> {
  options?.onStart?.();

  try {
    const response = await api.get(url, {
      responseType: 'blob',
    });

    const blob = response.data as Blob;
    const rawContentType = response.headers['content-type'] || blob.type || '';
    const contentType = String(rawContentType);

    // Si le serveur a renvoyé du JSON (cas d'erreur retourné sous responseType: 'blob')
    if (contentType.includes('application/json') || contentType.includes('text/plain')) {
      const text = await blob.text();
      let errorMsg = 'Le document demandé est indisponible.';
      try {
        const json = JSON.parse(text);
        errorMsg = json.message || json.error || errorMsg;
      } catch {
        if (text) errorMsg = text;
      }
      throw new Error(errorMsg);
    }

    // Vérification sommaire du format binaire PDF si attendu
    if (defaultFilename.endsWith('.pdf') && !contentType.includes('application/pdf')) {
      // Certains serveurs renvoient application/octet-stream, on vérifie les premiers octets
      const buffer = await blob.slice(0, 5).arrayBuffer();
      const headerStr = new TextDecoder().decode(buffer);
      if (!headerStr.startsWith('%PDF-')) {
        throw new Error('Le fichier reçu n\'est pas un document PDF valide.');
      }
    }

    // Extraction du nom de fichier
    const contentDisposition = response.headers['content-disposition'];
    const filename = extractFilename(contentDisposition, defaultFilename);

    // Déclenchement du téléchargement dans le navigateur
    const objectUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Libération de la mémoire
    setTimeout(() => {
      window.URL.revokeObjectURL(objectUrl);
    }, 1000);

    options?.onFinish?.();
  } catch (err: unknown) {
    options?.onFinish?.();
    let message = 'Impossible de télécharger le fichier. Veuillez réessayer.';

    if (err && typeof err === 'object') {
      const axiosErr = err as {
        response?: { status?: number; data?: Blob | { message?: string } };
        message?: string;
      };

      if (axiosErr.response?.status === 403) {
        message = 'Accès refusé : vous n\'avez pas les droits pour télécharger ce document.';
      } else if (axiosErr.response?.status === 404) {
        message = 'Document introuvable sur le serveur.';
      } else if (axiosErr.response?.status === 401) {
        message = 'Votre session a expiré. Veuillez vous reconnecter.';
      } else if (axiosErr.message) {
        message = axiosErr.message;
      }
    }

    options?.onError?.(message);
    throw new Error(message);
  }
}

export default downloadFile;
