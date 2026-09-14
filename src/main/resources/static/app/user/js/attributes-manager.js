/**
 * Access Sphere - Attributes Manager (Material Expressive 3)
 * Full support for primitive properties and nested JSON objects (e.g. money_stats_settings).
 * Bi-directional sync with JSON code editor and form submission.
 */

let attributesData = [];
let currentAttributesMode = "visual"; // 'visual' | 'code'

const EXAMPLE_ATTRIBUTES_JSON = {
  "api-token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample_jwt_token_payload_access_sphere_secret_key_example",
  "app_settings": {
    "theme": "DARK",
    "notificationsEnabled": true,
    "preferredLanguage": "it",
    "maxDailyTransactions": 100,
    "currency": "EUR"
  }
};

function escapeHtmlAttr(str) {
  if (str == null) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function switchAttributesMode(mode) {
  currentAttributesMode = mode;
  const visualContainer = document.getElementById("attributes-visual-view");
  const codeContainer = document.getElementById("attributes-code-view");
  const tabVisualBtn = document.getElementById("tab-attr-visual");
  const tabCodeBtn = document.getElementById("tab-attr-code");

  if (mode === "visual") {
    // Se passiamo a visuale da codice, facciamo il parse del codice
    const codeEditor = document.getElementById("attributes-json-editor");
    if (codeEditor) {
      try {
        const val = codeEditor.value.trim();
        if (val && val !== "{}") {
          const parsed = JSON.parse(val);
          if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
            loadAttributesIntoRepeater(parsed);
          }
        } else {
          loadAttributesIntoRepeater({});
        }
      } catch (e) {
        if (typeof sweetalert === "function") {
          sweetalert("error", "JSON Non Valido", "Correggi gli errori di sintassi prima di tornare alla visuale strutturata.");
        }
        return;
      }
    }

    if (visualContainer) visualContainer.style.display = "block";
    if (codeContainer) codeContainer.style.display = "none";
    if (tabVisualBtn) {
      tabVisualBtn.classList.remove("m3-btn-outline");
      tabVisualBtn.classList.add("m3-btn-primary");
    }
    if (tabCodeBtn) {
      tabCodeBtn.classList.remove("m3-btn-primary");
      tabCodeBtn.classList.add("m3-btn-outline");
    }
    const actionButtons = document.getElementById("attributes-action-buttons");
    if (actionButtons) actionButtons.style.display = "";
    renderAttributesTable();
  } else {
    // Passiamo alla vista codice
    syncAttributesToTextarea();
    const textarea = document.getElementById("attributes");
    const codeEditor = document.getElementById("attributes-json-editor");
    if (codeEditor && textarea) {
      codeEditor.value = textarea.value.trim() === "" ? "{}" : textarea.value;
      validateJsonCodeEditor();
    }
    if (visualContainer) visualContainer.style.display = "none";
    if (codeContainer) codeContainer.style.display = "block";
    if (tabVisualBtn) {
      tabVisualBtn.classList.remove("m3-btn-primary");
      tabVisualBtn.classList.add("m3-btn-outline");
    }
    if (tabCodeBtn) {
      tabCodeBtn.classList.remove("m3-btn-outline");
      tabCodeBtn.classList.add("m3-btn-primary");
    }
    const actionButtons = document.getElementById("attributes-action-buttons");
    if (actionButtons) actionButtons.style.display = "none";
  }
}

function renderAttributesTable() {
  const container = document.getElementById("attributes-items-container");
  const tbody = document.getElementById("attributes-tbody"); // fallback per vecchia tabella se presente
  
  if (!container) {
    if (tbody) {
      renderLegacyAttributesTable(tbody);
    }
    return;
  }

  if (attributesData.length === 0) {
    const emptyDesc = typeof t === "function" ? t("attr_empty_desc", "Nessun attributo configurato. Puoi aggiungere proprietà semplici o caricare un oggetto inestato.") : "Nessun attributo configurato. Puoi aggiungere proprietà semplici o caricare un oggetto inestato.";
    const btnProp = typeof t === "function" ? t("attr_btn_add_prop", "Aggiungi Proprietà") : "Aggiungi Proprietà";
    const btnNested = typeof t === "function" ? t("attr_btn_add_nested", "Aggiungi Oggetto Inestato") : "Aggiungi Oggetto Inestato";
    const btnExample = typeof t === "function" ? t("user_attr_btn_example", "Carica Esempio") : "Carica Esempio";

    container.innerHTML = `
      <div class="p-6 text-center text-purple-300/80 rounded-2xl border border-purple-500/20 bg-[#161124]/40">
        <i class="fa-solid fa-sliders text-2xl text-purple-400/60 mb-2 block"></i>
        <p class="text-xs">${emptyDesc}</p>
        <div class="flex flex-wrap items-center justify-center gap-2 mt-3">
          <button type="button" class="m3-btn-primary text-xs" onclick="addAttributeRow()">
            <i class="fa-solid fa-plus mr-1"></i> ${btnProp}
          </button>
          <button type="button" class="m3-btn-outline text-xs" onclick="addNestedObjectRow()">
            <i class="fa-solid fa-layer-group mr-1"></i> ${btnNested}
          </button>
          <button type="button" class="m3-btn-outline text-xs" onclick="loadExampleJson()">
            <i class="fa-solid fa-wand-magic-sparkles mr-1"></i> ${btnExample}
          </button>
        </div>
      </div>
    `;
    return;
  }

  let html = "";

  const subKeyPlaceholder = typeof t === "function" ? t("attr_sub_key_placeholder", "Chiave interna (es. currency, liveWallets)") : "Chiave interna (es. currency, liveWallets)";
  const subValPlaceholder = typeof t === "function" ? t("attr_sub_val_placeholder", "Valore interno (es. EUR, ACTIVE, €)") : "Valore interno (es. EUR, ACTIVE, €)";
  const delPropTitle = typeof t === "function" ? t("attr_del_prop", "Elimina attributo") : "Elimina attributo";
  const nestedNamePlaceholder = typeof t === "function" ? t("attr_nested_name_placeholder", "Nome oggetto (es. money_stats_settings)") : "Nome oggetto (es. money_stats_settings)";
  const addSubPropTitle = typeof t === "function" ? t("attr_add_sub_prop", "Proprietà") : "Proprietà";
  const delNestedTitle = typeof t === "function" ? t("attr_del_nested", "Elimina intero oggetto") : "Elimina intero oggetto";
  const subKeyLabel = typeof t === "function" ? t("attr_sub_key_label", "Sotto-Chiave") : "Sotto-Chiave";
  const subValLabel = typeof t === "function" ? t("attr_sub_val_label", "Sotto-Valore") : "Sotto-Valore";
  const noSubPropsText = typeof t === "function" ? t("attr_no_sub_props", "Nessuna proprietà interna. Clicca su \"+ Proprietà\" per aggiungerne una.") : "Nessuna proprietà interna. Clicca su \"+ Proprietà\" per aggiungerne una.";
  const propNamePlaceholder = typeof t === "function" ? t("attr_prop_name_placeholder", "Nome attributo (es. strapi-token)") : "Nome attributo (es. strapi-token)";
  const propValPlaceholder = typeof t === "function" ? t("attr_prop_val_placeholder", "Valore (stringa, numero o booleano)") : "Valore (stringa, numero o booleano)";

  attributesData.forEach((item, index) => {
    if (item.type === "object") {
      // Scheda oggetto inestato (es. money_stats_settings)
      let subRowsHtml = "";
      (item.subItems || []).forEach((sub, subIdx) => {
        subRowsHtml += `
          <div class="flex items-center gap-2 mb-2">
            <input type="text" class="form-control font-mono text-xs flex-1"
              value="${escapeHtmlAttr(sub.key)}"
              oninput="updateSubItemKey(${index}, ${subIdx}, this.value)"
              placeholder="${escapeHtmlAttr(subKeyPlaceholder)}" />
            <input type="text" class="form-control font-mono text-xs flex-1"
              value="${escapeHtmlAttr(sub.value)}"
              oninput="updateSubItemValue(${index}, ${subIdx}, this.value)"
              placeholder="${escapeHtmlAttr(subValPlaceholder)}" />
            <button type="button" class="m3-icon-btn m3-icon-btn-danger flex-shrink-0"
              onclick="removeSubItem(${index}, ${subIdx})" title="${escapeHtmlAttr(delPropTitle)}">
              <i class="fa-solid fa-trash text-xs"></i>
            </button>
          </div>
        `;
      });

      html += `
        <div class="m3-nested-card">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-3 mb-3 border-b border-purple-500/15 gap-2">
            <div class="flex items-center gap-2 flex-1">
              <div class="w-7 h-7 rounded-lg bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-300 flex-shrink-0">
                <i class="fa-solid fa-layer-group text-xs"></i>
              </div>
              <input type="text" class="form-control font-mono text-xs font-bold text-purple-200 flex-1 max-w-xs"
                value="${escapeHtmlAttr(item.key)}"
                oninput="updateAttributeKey(${index}, this.value)"
                placeholder="${escapeHtmlAttr(nestedNamePlaceholder)}" />
              <span class="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30 flex-shrink-0">
                ${escapeHtmlAttr(item.key || "Object")} (${(item.subItems || []).length})
              </span>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <button type="button" class="m3-btn-outline text-xs" onclick="addSubItem(${index})" title="${escapeHtmlAttr(addSubPropTitle)}">
                <i class="fa-solid fa-plus mr-1"></i> ${escapeHtmlAttr(addSubPropTitle)}
              </button>
              <button type="button" class="m3-icon-btn m3-icon-btn-danger" onclick="removeAttributeRow(${index})" title="${escapeHtmlAttr(delNestedTitle)}">
                <i class="fa-solid fa-trash text-xs"></i>
              </button>
            </div>
          </div>

          <div class="space-y-1 pl-2 sm:pl-3">
            <div class="flex items-center gap-2 text-[10px] font-semibold text-purple-300/70 uppercase tracking-wider mb-1.5 px-1">
              <span class="flex-1">${escapeHtmlAttr(subKeyLabel)}</span>
              <span class="flex-1">${escapeHtmlAttr(subValLabel)}</span>
              <span style="width: 36px;"></span>
            </div>
            ${subRowsHtml || `<p class="text-xs text-gray-400 italic py-2">${escapeHtmlAttr(noSubPropsText)}</p>`}
          </div>
        </div>
      `;
    } else {
      // Proprietà primitiva singola (es. strapi-token)
      const isToken = item.key.toLowerCase().includes("token") || (item.value && item.value.length > 30);
      html += `
        <div class="p-3 mb-2 rounded-xl border border-purple-500/15 bg-[#161124]/60 flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
          <div class="flex-1">
            <input type="text" class="form-control font-mono text-xs"
              value="${escapeHtmlAttr(item.key)}"
              oninput="updateAttributeKey(${index}, this.value)"
              placeholder="${escapeHtmlAttr(propNamePlaceholder)}" />
          </div>
          <div class="flex-1 flex items-center gap-2">
            <input type="text" class="form-control font-mono text-xs flex-1"
              value="${escapeHtmlAttr(item.value)}"
              oninput="updateAttributeValue(${index}, this.value)"
              placeholder="${escapeHtmlAttr(propValPlaceholder)}" />
            ${isToken ? `
              <span class="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-500/15 text-amber-300 border border-amber-500/30 flex-shrink-0">
                Token
              </span>
            ` : ""}
          </div>
          <div class="flex items-center justify-end gap-1 flex-shrink-0">
            <button type="button" class="m3-icon-btn m3-icon-btn-danger" onclick="removeAttributeRow(${index})" title="${escapeHtmlAttr(delPropTitle)}">
              <i class="fa-solid fa-trash text-xs"></i>
            </button>
          </div>
        </div>
      `;
    }
  });

  container.innerHTML = html;
}

function renderLegacyAttributesTable(tbody) {
  tbody.innerHTML = "";
  if (attributesData.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="3" class="repeater-empty-state">
          Nessun attributo configurato.
        </td>
      </tr>
    `;
    return;
  }
  attributesData.forEach((item, idx) => {
    const val = item.type === "object" ? JSON.stringify(buildSubObject(item)) : item.value;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>
        <input type="text" class="form-control font-mono text-xs" value="${escapeHtmlAttr(item.key)}"
          oninput="updateAttributeKey(${idx}, this.value)" placeholder="Nome attributo" />
      </td>
      <td>
        <input type="text" class="form-control font-mono text-xs" value="${escapeHtmlAttr(val)}"
          oninput="updateAttributeValue(${idx}, this.value)" placeholder="Valore" />
      </td>
      <td class="text-center">
        <button type="button" class="m3-icon-btn m3-icon-btn-danger" onclick="removeAttributeRow(${idx})" title="Elimina">
          <i class="fa-solid fa-trash text-xs"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function buildSubObject(item) {
  const subObj = {};
  (item.subItems || []).forEach(sub => {
    const subK = (sub.key || "").trim();
    if (!subK) return;
    let subV = sub.value;
    if (subV === "true") subV = true;
    else if (subV === "false") subV = false;
    else if (subV !== "" && !isNaN(subV) && !isNaN(parseFloat(subV)) && !String(subV).startsWith("0") && String(subV).length < 15) {
      subV = Number(subV);
    }
    subObj[subK] = subV;
  });
  return subObj;
}

function addAttributeRow() {
  attributesData.push({ type: "primitive", key: "", value: "" });
  renderAttributesTable();
  syncAttributesToTextarea();
}

function addNestedObjectRow() {
  attributesData.push({
    type: "object",
    key: "impostazioni",
    subItems: [
      { key: "tema", value: "DARK" },
      { key: "notifiche", value: "true" }
    ]
  });
  renderAttributesTable();
  syncAttributesToTextarea();
}

function addSubItem(parentIdx) {
  if (attributesData[parentIdx] && attributesData[parentIdx].type === "object") {
    if (!attributesData[parentIdx].subItems) attributesData[parentIdx].subItems = [];
    attributesData[parentIdx].subItems.push({ key: "", value: "" });
    renderAttributesTable();
    syncAttributesToTextarea();
  }
}

function removeSubItem(parentIdx, subIdx) {
  if (attributesData[parentIdx] && attributesData[parentIdx].subItems) {
    attributesData[parentIdx].subItems.splice(subIdx, 1);
    renderAttributesTable();
    syncAttributesToTextarea();
  }
}

function updateSubItemKey(parentIdx, subIdx, val) {
  if (attributesData[parentIdx] && attributesData[parentIdx].subItems && attributesData[parentIdx].subItems[subIdx]) {
    attributesData[parentIdx].subItems[subIdx].key = val;
    syncAttributesToTextarea();
  }
}

function updateSubItemValue(parentIdx, subIdx, val) {
  if (attributesData[parentIdx] && attributesData[parentIdx].subItems && attributesData[parentIdx].subItems[subIdx]) {
    attributesData[parentIdx].subItems[subIdx].value = val;
    syncAttributesToTextarea();
  }
}

function removeAttributeRow(idx) {
  attributesData.splice(idx, 1);
  renderAttributesTable();
  syncAttributesToTextarea();
}

function updateAttributeKey(idx, val) {
  if (attributesData[idx]) {
    attributesData[idx].key = val;
    syncAttributesToTextarea();
  }
}

function updateAttributeValue(idx, val) {
  if (attributesData[idx]) {
    attributesData[idx].value = val;
    syncAttributesToTextarea();
  }
}

function buildJsonFromAttributesData() {
  const result = {};
  attributesData.forEach(item => {
    const k = (item.key || "").trim();
    if (!k) return;

    if (item.type === "object") {
      result[k] = buildSubObject(item);
    } else {
      let v = item.value;
      if (typeof v === "string") {
        const trimmed = v.trim();
        if (trimmed === "true") v = true;
        else if (trimmed === "false") v = false;
        else if (trimmed !== "" && !isNaN(trimmed) && !isNaN(parseFloat(trimmed)) && !trimmed.startsWith("0") && trimmed.length < 15) {
          v = Number(trimmed);
        } else if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
          try {
            v = JSON.parse(trimmed);
          } catch (e) {}
        }
      }
      result[k] = v;
    }
  });
  return result;
}

function syncAttributesToTextarea() {
  const textarea = document.getElementById("attributes");
  if (!textarea) return;

  const obj = buildJsonFromAttributesData();
  const hasKeys = Object.keys(obj).length > 0;
  textarea.value = hasKeys ? JSON.stringify(obj, null, 2) : "";

  const codeEditor = document.getElementById("attributes-json-editor");
  if (codeEditor && currentAttributesMode === "visual") {
    codeEditor.value = hasKeys ? JSON.stringify(obj, null, 2) : "{}";
    validateJsonCodeEditor();
  }

  const errDiv = document.getElementById("validationAttributes");
  if (errDiv) errDiv.innerText = "";
  textarea.classList.remove("is-invalid");
}

function loadAttributesIntoRepeater(attributes) {
  attributesData = [];
  if (attributes && typeof attributes === "object" && !Array.isArray(attributes)) {
    for (const [k, v] of Object.entries(attributes)) {
      if (v && typeof v === "object" && !Array.isArray(v)) {
        const subItems = [];
        for (const [subK, subV] of Object.entries(v)) {
          subItems.push({
            key: subK,
            value: typeof subV === "object" ? JSON.stringify(subV) : String(subV)
          });
        }
        attributesData.push({
          type: "object",
          key: k,
          subItems: subItems
        });
      } else {
        attributesData.push({
          type: "primitive",
          key: k,
          value: typeof v === "object" ? JSON.stringify(v) : (v != null ? String(v) : "")
        });
      }
    }
  }
  renderAttributesTable();
  syncAttributesToTextarea();
}

function loadExampleJson() {
  loadAttributesIntoRepeater(EXAMPLE_ATTRIBUTES_JSON);
  const codeEditor = document.getElementById("attributes-json-editor");
  if (codeEditor) {
    codeEditor.value = JSON.stringify(EXAMPLE_ATTRIBUTES_JSON, null, 2);
    validateJsonCodeEditor();
  }
  if (typeof sweetalert === "function") {
    sweetalert("success", "Esempio Caricato", "Configurazione attributi di esempio caricata con successo.");
  }
}

function validateJsonCodeEditor() {
  const codeEditor = document.getElementById("attributes-json-editor");
  const feedbackEl = document.getElementById("json-editor-feedback");
  const hiddenTextarea = document.getElementById("attributes");
  if (!codeEditor || !feedbackEl) return;

  const rawVal = codeEditor.value.trim();
  if (rawVal === "" || rawVal === "{}") {
    feedbackEl.innerHTML = `<span class="text-gray-400 text-xs"><i class="fa-solid fa-circle-info mr-1"></i> JSON vuoto. Nessun attributo salvato.</span>`;
    codeEditor.style.borderColor = "rgba(208, 188, 255, 0.25)";
    if (hiddenTextarea) hiddenTextarea.value = "";
    return;
  }

  try {
    const parsed = JSON.parse(rawVal);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      throw new Error("Il JSON principale deve essere un oggetto racchiuso tra parentesi graffe { }");
    }
    const count = Object.keys(parsed).length;
    feedbackEl.innerHTML = `<span class="text-emerald-400 font-medium text-xs"><i class="fa-solid fa-circle-check mr-1"></i> JSON valido (${count} ${count === 1 ? 'attributo' : 'attributi'})</span>`;
    codeEditor.style.borderColor = "rgba(16, 185, 129, 0.4)";
    if (hiddenTextarea) hiddenTextarea.value = JSON.stringify(parsed, null, 2);
  } catch (e) {
    feedbackEl.innerHTML = `<span class="text-rose-400 font-medium text-xs"><i class="fa-solid fa-triangle-exclamation mr-1"></i> Sintassi non valida: ${e.message}</span>`;
    codeEditor.style.borderColor = "rgba(244, 63, 94, 0.5)";
  }
}

function formatJsonCodeEditor() {
  const codeEditor = document.getElementById("attributes-json-editor");
  if (!codeEditor) return;
  try {
    const parsed = JSON.parse(codeEditor.value);
    codeEditor.value = JSON.stringify(parsed, null, 2);
    validateJsonCodeEditor();
  } catch (e) {
    if (typeof sweetalert === "function") {
      sweetalert("error", "Errore di Formattazione", "Correggi la sintassi JSON prima di formattare: " + e.message);
    }
  }
}

function copyJsonCodeEditor() {
  const codeEditor = document.getElementById("attributes-json-editor");
  if (!codeEditor) return;
  navigator.clipboard.writeText(codeEditor.value).then(() => {
    if (typeof sweetalert === "function") {
      sweetalert("success", "Copiato!", "JSON copiato negli appunti.");
    }
  }).catch(() => {
    if (typeof sweetalert === "function") {
      sweetalert("info", "Info", "Seleziona e copia manualmente il testo del riquadro.");
    }
  });
}

function toggleAttributesJsonModal() {
  switchAttributesMode("code");
}

// Esporta esplicitamente su window per garantire la raggiungibilità globale
window.switchAttributesMode = switchAttributesMode;
window.renderAttributesTable = renderAttributesTable;
window.addAttributeRow = addAttributeRow;
window.addNestedObjectRow = addNestedObjectRow;
window.addSubItem = addSubItem;
window.removeSubItem = removeSubItem;
window.updateSubItemKey = updateSubItemKey;
window.updateSubItemValue = updateSubItemValue;
window.removeAttributeRow = removeAttributeRow;
window.updateAttributeKey = updateAttributeKey;
window.updateAttributeValue = updateAttributeValue;
window.syncAttributesToTextarea = syncAttributesToTextarea;
window.loadAttributesIntoRepeater = loadAttributesIntoRepeater;
window.loadExampleJson = loadExampleJson;
window.validateJsonCodeEditor = validateJsonCodeEditor;
window.formatJsonCodeEditor = formatJsonCodeEditor;
window.copyJsonCodeEditor = copyJsonCodeEditor;
window.toggleAttributesJsonModal = toggleAttributesJsonModal;

document.addEventListener("DOMContentLoaded", function () {
  const textarea = document.getElementById("attributes");
  if (textarea && textarea.value && textarea.value.trim() !== "") {
    try {
      const parsed = JSON.parse(textarea.value);
      if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
        loadAttributesIntoRepeater(parsed);
      }
    } catch (e) {}
  } else {
    renderAttributesTable();
  }
});

// Ascolta il cambio lingua globale per ri-renderizzare la tabella con i testi localizzati
window.addEventListener("languageChanged", function () {
  if (currentAttributesMode === "visual") {
    renderAttributesTable();
  }
});

