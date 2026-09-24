// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.web;

/**
 * Embedded single-page web dashboard HTML, CSS, and JS for the Blueprint Web Portal.
 * Self-contained, responsive, zero-dependency, works fully offline or connected.
 */
public final class WebDashboardHtml {

    private WebDashboardHtml() {
    }

    public static final String DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Multiverse Programming // Blueprint Nexus</title>
  <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🐢</text></svg>">
  <style>
    :root {
      --bg: #070a13;
      --card-bg: #0f172a;
      --card-hover: #141f36;
      --card-border: #1e293b;
      --card-border-glow: #38bdf8;
      --primary: #38bdf8;
      --primary-hover: #0284c7;
      --accent: #10b981;
      --accent-hover: #059669;
      --violet: #8b5cf6;
      --warning: #f59e0b;
      --danger: #ef4444;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --text-dim: #64748b;
      --grid-line: #1e293b;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
    body { background: var(--bg); color: var(--text); min-height: 100vh; display: flex; flex-direction: column; background-image: radial-gradient(circle at 50% 0%, #172554 0%, transparent 60%); }
    
    header { background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(12px); border-bottom: 1px solid var(--card-border); padding: 14px 28px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; position: sticky; top: 0; z-index: 50; }
    .brand { display: flex; align-items: center; gap: 12px; font-weight: 800; font-size: 1.2rem; letter-spacing: 0.5px; }
    .brand-badge { background: linear-gradient(135deg, #0284c7, #38bdf8); color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; text-transform: uppercase; font-weight: 800; letter-spacing: 1.5px; box-shadow: 0 0 15px rgba(56, 189, 248, 0.4); }
    
    .header-controls { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
    .repo-badge { display: flex; align-items: center; gap: 6px; background: #070a13; border: 1px solid var(--card-border); padding: 6px 12px; border-radius: 8px; font-size: 0.8rem; color: var(--text-muted); text-decoration: none; }
    .repo-badge:hover { border-color: var(--primary); color: #fff; }

    .help-btn { background: transparent; border: 1px solid var(--card-border); color: var(--text-muted); border-radius: 50%; width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; font-size: 0.85rem; font-weight: 700; }
    .help-btn:hover { color: #fff; border-color: var(--primary); }

    /* Modal */
    .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.75); backdrop-filter: blur(4px); z-index: 1000; align-items: center; justify-content: center; padding: 20px; }
    .modal-content { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; max-width: 580px; width: 100%; padding: 24px; display: flex; flex-direction: column; gap: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.6); }
    .modal-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--card-border); padding-bottom: 12px; }
    .modal-title { font-weight: 700; font-size: 1.15rem; color: #fff; }
    .close-btn { background: transparent; border: none; color: var(--text-muted); font-size: 1.3rem; cursor: pointer; }
    .close-btn:hover { color: #fff; }

    .container { max-width: 1440px; margin: 0 auto; padding: 24px; display: grid; grid-template-columns: 360px 1fr 380px; gap: 24px; flex: 1; width: 100%; }
    @media (max-width: 1180px) { .container { grid-template-columns: 1fr; } }

    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
    .card-title { font-size: 1.05rem; font-weight: 700; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--card-border); padding-bottom: 12px; }
    
    /* Upload Zone */
    .dropzone { border: 2px dashed #38bdf8; border-radius: 12px; padding: 26px 16px; text-align: center; background: rgba(56, 189, 248, 0.03); cursor: pointer; transition: 0.2s; position: relative; }
    .dropzone:hover, .dropzone.dragover { background: rgba(56, 189, 248, 0.1); border-color: #7dd3fc; box-shadow: 0 0 20px rgba(56, 189, 248, 0.2); }
    .dropzone-icon { font-size: 2.2rem; margin-bottom: 8px; }
    .dropzone-text { font-size: 0.95rem; font-weight: 600; color: #fff; }
    .dropzone-sub { font-size: 0.78rem; color: var(--text-muted); margin-top: 4px; }
    input[type="file"] { display: none; }

    /* Library Search & Filter */
    .search-bar { display: flex; gap: 8px; }
    .search-input { flex: 1; background: #070a13; border: 1px solid var(--card-border); border-radius: 6px; padding: 8px 12px; color: #fff; font-size: 0.85rem; outline: none; }
    .search-input:focus { border-color: var(--primary); }

    /* Blueprints List */
    .bp-list { display: flex; flex-direction: column; gap: 10px; max-height: 480px; overflow-y: auto; padding-right: 4px; }
    .bp-item { background: #0b1120; border: 1px solid var(--card-border); border-radius: 8px; padding: 12px; cursor: pointer; transition: 0.15s; display: flex; flex-direction: column; gap: 6px; }
    .bp-item:hover, .bp-item.selected { border-color: var(--primary); background: #131d33; box-shadow: 0 0 12px rgba(56, 189, 248, 0.15); }
    .bp-header { display: flex; justify-content: space-between; align-items: center; }
    .bp-name { font-weight: 700; font-size: 0.95rem; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 190px; }
    .badges-row { display: flex; gap: 6px; align-items: center; }
    .badge { font-size: 0.7rem; padding: 2px 7px; border-radius: 4px; font-weight: 700; text-transform: uppercase; }
    .badge-litematic { background: #6d28d9; color: #fff; }
    .badge-nbt { background: #1d4ed8; color: #fff; }
    .badge-community { background: #059669; color: #fff; }
    .bp-meta { font-size: 0.78rem; color: var(--text-muted); display: flex; justify-content: space-between; align-items: center; }

    /* Visualizer & Slicer */
    .preview-canvas-container { background: #04060c; border: 1px solid var(--card-border); border-radius: 8px; height: 350px; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; }
    canvas { background: #04060c; border-radius: 6px; cursor: grab; }
    canvas:active { cursor: grabbing; }
    
    .canvas-overlay-controls { position: absolute; top: 12px; right: 12px; display: flex; flex-direction: column; gap: 6px; z-index: 10; }
    .canvas-btn { background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(8px); border: 1px solid var(--card-border); color: #fff; width: 32px; height: 32px; border-radius: 6px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 0.9rem; transition: 0.15s; }
    .canvas-btn:hover { border-color: var(--primary); color: var(--primary); }

    .view-mode-tabs { display: flex; gap: 8px; }
    .tab-btn { background: #070a13; border: 1px solid var(--card-border); color: var(--text-muted); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
    .tab-btn.active { background: rgba(56, 189, 248, 0.15); border-color: var(--primary); color: #fff; }

    .slider-row { display: flex; align-items: center; gap: 10px; }
    input[type="range"] { flex: 1; accent-color: var(--primary); cursor: pointer; }

    /* Materials Table */
    .mat-table-container { max-height: 180px; overflow-y: auto; border: 1px solid var(--card-border); border-radius: 8px; }
    table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
    th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid var(--card-border); }
    th { background: #0b1120; color: var(--text-muted); position: sticky; top: 0; font-weight: 600; }
    tr:hover { background: rgba(255,255,255,0.02); }

    /* Turtle Pastebin / Code Box */
    .code-box { background: #070a13; border: 1px solid var(--card-border); border-radius: 8px; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
    .code-header { display: flex; justify-content: space-between; align-items: center; font-size: 0.82rem; color: var(--text-muted); }
    .code-display { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 1rem; font-weight: 700; color: var(--primary); letter-spacing: 0.5px; user-select: all; }
    .copy-btn { background: #1e293b; border: 1px solid var(--card-border); color: #fff; padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: 0.15s; display: flex; align-items: center; gap: 6px; }
    .copy-btn:hover { background: var(--primary); color: #000; border-color: var(--primary); }

    .instruction-step { display: flex; gap: 10px; align-items: flex-start; font-size: 0.85rem; color: var(--text-muted); line-height: 1.45; }
    .step-number { background: #1e293b; color: var(--primary); border-radius: 50%; width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.75rem; flex-shrink: 0; margin-top: 1px; }

    .btn { background: var(--primary); color: #000; border: none; padding: 11px; border-radius: 8px; font-weight: 700; cursor: pointer; transition: 0.15s; font-size: 0.9rem; text-align: center; }
    .btn:hover { background: var(--primary-hover); color: #fff; }
    .btn-sm { padding: 5px 10px; font-size: 0.78rem; border-radius: 5px; }
    .btn-ghost { background: transparent; border: 1px solid var(--card-border); color: var(--text-muted); }
    .btn-ghost:hover { color: #fff; border-color: var(--text); }

    /* Toast */
    #toast { position: fixed; bottom: 24px; right: 24px; background: #0f172a; color: #fff; padding: 14px 20px; border-radius: 8px; border-left: 4px solid var(--primary); box-shadow: 0 4px 25px rgba(0,0,0,0.6); display: none; z-index: 1000; font-size: 0.88rem; font-weight: 500; }
  </style>
</head>
<body>
  <header>
    <div class="brand">
      <span class="brand-badge">TURTLE MATRIX</span>
      <span>Multiverse Programming // Blueprint Nexus</span>
    </div>
    
    <div class="header-controls">
      <a href="https://github.com/DrakesCraft-Labs/MultiverseProgramming" target="_blank" class="repo-badge">
        <span>📦 GitHub Repository</span>
      </a>
      <button class="help-btn" onclick="openHelpModal()" title="In-Game Usage Guide">?</button>
    </div>
  </header>

  <!-- Help / Instructions Modal -->
  <div class="modal-overlay" id="helpModal" onclick="closeHelpModal(event)">
    <div class="modal-content" onclick="event.stopPropagation()">
      <div class="modal-header">
        <span class="modal-title">How to Build with Turtles In-Game</span>
        <button class="close-btn" onclick="closeHelpModal()">&times;</button>
      </div>
      <div style="font-size:0.88rem; color:var(--text-muted); display:flex; flex-direction:column; gap:12px; line-height:1.5;">
        <p><strong style="color:#fff;">1. Universal Blueprint Code (Pastebin Style):</strong> Every design has a unique Blueprint Code (like <code style="color:var(--primary);">BP-NETHER-PORTAL</code>). You don't need to configure coordinates on this page!</p>
        <p><strong style="color:#fff;">2. Position Your Turtle:</strong> Place your Turtle directly in front of where you want the structure to be built. The Turtle builds forward and upward relative to its facing direction.</p>
        <p><strong style="color:#fff;">3. Execute the Build:</strong> In-game, run the command:</p>
        <div style="background:#070a13; padding:10px 14px; border-radius:6px; font-family:monospace; color:var(--primary);">/pc build &lt;CODE&gt; [turtleId]</div>
        <p>Or inside a Lua floppy disk program:</p>
        <div style="background:#070a13; padding:10px 14px; border-radius:6px; font-family:monospace; color:var(--accent);">turtle.build("&lt;CODE&gt;")</div>
        <p><strong style="color:#fff;">4. Server Storage &amp; 15 MB Quota:</strong> Structures are saved once in the server. If two players use the same design, it is not duplicated. Player quotas only apply when uploading new custom files inside the server.</p>
      </div>
      <button class="btn btn-sm" style="margin-top:8px;" onclick="closeHelpModal()">Got it!</button>
    </div>
  </div>

  <main class="container">
    <!-- COLUMN 1: UPLOAD & BLUEPRINTS -->
    <div class="card">
      <div class="card-title">
        <span>Blueprint Library</span>
        <button class="btn btn-sm btn-ghost" onclick="syncLibrary()">⟳ Refresh</button>
      </div>

      <!-- Drag & Drop Zone -->
      <div class="dropzone" id="dropzone" onclick="document.getElementById('fileInput').click()">
        <div class="dropzone-icon">📥</div>
        <div class="dropzone-text">Drop .litematic or .nbt file here</div>
        <div class="dropzone-sub">Parsed client-side • Instant preview</div>
      </div>
      <input type="file" id="fileInput" accept=".litematic,.nbt" onchange="handleFileSelect(event)">

      <!-- Search & Filter -->
      <div class="search-bar">
        <input type="text" class="search-input" id="searchInput" placeholder="Search blueprints..." oninput="filterBlueprints(this.value)">
      </div>

      <!-- Blueprint List -->
      <div class="bp-list" id="bpList">
        <div style="text-align:center; padding: 20px; color: var(--text-muted);">Loading blueprint catalog...</div>
      </div>
    </div>

    <!-- COLUMN 2: 3D / LAYER VISUALIZER -->
    <div class="card">
      <div class="card-title">
        <div>
          <span id="visualizerTitle">Design Visualizer</span>
          <div id="visualizerDims" style="font-size:0.8rem; color:var(--text-muted); font-weight:400; margin-top:2px;">Select a blueprint to begin</div>
        </div>

        <div class="view-mode-tabs">
          <button class="tab-btn active" id="tab3D" onclick="setViewMode('3D')">3D Isometric</button>
          <button class="tab-btn" id="tab2D" onclick="setViewMode('2D')">2D Slice</button>
        </div>
      </div>

      <!-- Interactive Canvas -->
      <div class="preview-canvas-container" id="canvasContainer">
        <canvas id="previewCanvas" width="460" height="340"></canvas>
        <div class="canvas-overlay-controls">
          <button class="canvas-btn" onclick="zoomIn()" title="Zoom In">+</button>
          <button class="canvas-btn" onclick="zoomOut()" title="Zoom Out">-</button>
          <button class="canvas-btn" onclick="rotateView()" title="Rotate 90°">⟳</button>
          <button class="canvas-btn" onclick="resetView()" title="Reset View">⌂</button>
        </div>
      </div>

      <!-- Layer Slider & Player Controls -->
      <div class="slider-row">
        <button class="btn btn-sm btn-ghost" onclick="stepLayer(-1)" title="Previous Layer">◀</button>
        <button class="btn btn-sm btn-ghost" id="playAnimBtn" onclick="togglePlayAnimation()" title="Play Layer Animation">▶</button>
        <input type="range" id="layerSlider" min="0" max="0" value="0" oninput="onLayerChange(this.value)">
        <button class="btn btn-sm btn-ghost" onclick="stepLayer(1)" title="Next Layer">▶</button>
        <span id="layerDisplay" style="font-weight: 700; font-size:0.85rem; min-width: 55px; text-align:right;">0 / 0</span>
      </div>

      <!-- Materials Table -->
      <div class="card-title" style="margin-top: 4px;">
        <span>Required Materials</span>
        <span id="totalMaterialsCount" style="font-size:0.8rem; color:var(--primary); font-weight:600;">0 types</span>
      </div>
      <div class="mat-table-container">
        <table id="matTable">
          <thead><tr><th>Block / Item</th><th>Count</th><th>Stacks</th></tr></thead>
          <tbody id="matTableBody">
            <tr><td colspan="3" style="text-align: center; color: var(--text-muted); padding:20px;">Select a blueprint to calculate materials</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- COLUMN 3: TURTLE IN-GAME INTEGRATION (PASTEBIN STYLE) -->
    <div class="card">
      <div class="card-title">
        <span>In-Game Construction Code</span>
      </div>

      <!-- Blueprint Share Code -->
      <div class="code-box">
        <div class="code-header">
          <span>📋 Blueprint Code</span>
          <span style="font-size:0.75rem; color:var(--accent);">Pastebin Style</span>
        </div>
        <div class="code-display" id="bpCodeDisplay">BP-NETHER-PORTAL</div>
        <button class="copy-btn" onclick="copyBlueprintCode()">
          <span>📋 Copy Code</span>
        </button>
      </div>

      <!-- In-Game Command -->
      <div class="code-box">
        <div class="code-header">
          <span>🎮 Chat Command</span>
        </div>
        <div class="code-display" id="inGameCommandDisplay" style="font-size:0.88rem; color:#fff;">/pc build BP-NETHER-PORTAL</div>
        <button class="copy-btn" onclick="copyInGameCommand()">
          <span>⚡ Copy Chat Command</span>
        </button>
      </div>

      <!-- Lua Script Code -->
      <div class="code-box">
        <div class="code-header">
          <span>💻 Turtle Lua Code</span>
        </div>
        <div class="code-display" id="luaCodeDisplay" style="font-size:0.85rem; color:var(--accent);">turtle.build("BP-NETHER-PORTAL")</div>
        <button class="copy-btn" onclick="copyLuaCode()">
          <span>💾 Copy Lua Code</span>
        </button>
      </div>

      <!-- In-Game Instructions Guide -->
      <div class="card-title" style="margin-top: 6px;">
        <span>How to Build in Minecraft</span>
      </div>

      <div style="display:flex; flex-direction:column; gap:12px;">
        <div class="instruction-step">
          <div class="step-number">1</div>
          <div><strong>Place your Turtle:</strong> Place the Turtle block in your world facing where you want the build to start.</div>
        </div>
        <div class="instruction-step">
          <div class="step-number">2</div>
          <div><strong>Provide Fuel / Materials:</strong> If configured on your server, put fuel (coal) and required blocks inside the Turtle.</div>
        </div>
        <div class="instruction-step">
          <div class="step-number">3</div>
          <div><strong>Run Command:</strong> Type <code style="color:var(--primary);">/pc build &lt;CODE&gt;</code> in chat or run <code style="color:var(--accent);">turtle.build("&lt;CODE&gt;")</code> in a floppy disk.</div>
        </div>
      </div>

      <!-- Download Original File -->
      <button class="btn btn-ghost" id="downloadFileBtn" style="margin-top:8px;" onclick="downloadSelectedFile()">
        ⬇ Download Blueprint File
      </button>
    </div>
  </main>

  <div id="toast"></div>

  <!-- Client-Side Engine Script -->
  <script>
    let blueprints = [];
    let selectedBlueprint = null;
    let viewMode = '3D'; // '3D' or '2D'
    let currentRotation = 0; // 0, 90, 180, 270 degrees
    let zoomLevel = 1.0;
    let animInterval = null;

    window.addEventListener('DOMContentLoaded', async () => {
      loadLocalLibrary();
      await loadCommunityCatalog();
      syncLibrary();
      setupDropzone();
      setupCanvasInteraction();
    });

    function showToast(msg, isError = false) {
      const toast = document.getElementById('toast');
      toast.textContent = msg;
      toast.style.borderColor = isError ? 'var(--danger)' : 'var(--accent)';
      toast.style.display = 'block';
      setTimeout(() => { toast.style.display = 'none'; }, 3500);
    }

    function openHelpModal() { document.getElementById('helpModal').style.display = 'flex'; }
    function closeHelpModal(e) { if (!e || e.target.id === 'helpModal' || e.target.tagName === 'BUTTON') document.getElementById('helpModal').style.display = 'none'; }

    // =========================================================================
    // COMMUNITY CATALOG PRELOAD
    // =========================================================================
    async function loadCommunityCatalog() {
      const sample = {
        id: 'BP-NETHER-PORTAL',
        name: 'Monumental Nether Portal',
        author: 'elmagra',
        format: 'litematic',
        sizeX: 66,
        sizeY: 148,
        sizeZ: 113,
        totalBlocks: 49298,
        owner: 'Repository',
        isCommunity: true,
        fileSizeBytes: 39476,
        materialCounts: {
          'POLISHED_DEEPSLATE': 22716,
          'DEEPSLATE_TILES': 14694,
          'POLISHED_BLACKSTONE': 7362,
          'NETHER_PORTAL': 1998,
          'DEEPSLATE_BRICKS': 1056,
          'OBSIDIAN': 562,
          'DEEPSLATE_BRICK_STAIRS': 528,
          'DEEPSLATE': 377,
          'REDSTONE_BLOCK': 4,
          'GLASS': 1
        },
        sampleUrl: 'blueprints/nether_portal.litematic'
      };

      if (!blueprints.some(b => b.id === sample.id)) {
        blueprints.push(sample);
      }
    }

    function loadLocalLibrary() {
      try {
        const raw = localStorage.getItem('mp_user_blueprints');
        if (raw) {
          const localList = JSON.parse(raw);
          localList.forEach(bp => {
            if (!blueprints.some(b => b.id === bp.id)) {
              blueprints.push(bp);
            }
          });
        }
      } catch (e) {
        console.error('Failed to load local library:', e);
      }
    }

    function saveToLocalLibrary(bp, fileSizeBytes) {
      bp.fileSizeBytes = fileSizeBytes;
      bp.isLocal = true;
      bp.owner = bp.owner || 'Custom';

      blueprints = blueprints.filter(b => b.id !== bp.id);
      blueprints.unshift(bp);

      try {
        const localsOnly = blueprints.filter(b => b.isLocal);
        localStorage.setItem('mp_user_blueprints', JSON.stringify(localsOnly));
      } catch (e) {
        console.warn('LocalStorage caching:', e);
      }
      return true;
    }

    // =========================================================================
    // CLIENT-SIDE NBT & LITEMATICA DECOMPRESSOR / PARSER
    // =========================================================================
    async function decompressGzip(arrayBuffer) {
      const bytes = new Uint8Array(arrayBuffer);
      if (bytes[0] === 0x1f && bytes[1] === 0x8b) {
        if (typeof DecompressionStream !== 'undefined') {
          const ds = new DecompressionStream('gzip');
          const decompressedStream = new Response(new Blob([arrayBuffer])).body.pipeThrough(ds);
          return await new Response(decompressedStream).arrayBuffer();
        }
      }
      return arrayBuffer;
    }

    function parseNbtBuffer(buffer) {
      let offset = 0;
      const view = new DataView(buffer);
      const decoder = new TextDecoder('utf-8');

      function readString() {
        const len = view.getUint16(offset);
        offset += 2;
        const str = decoder.decode(new Uint8Array(buffer, offset, len));
        offset += len;
        return str;
      }

      function readTag(type) {
        switch (type) {
          case 0: return null;
          case 1: { const v = view.getInt8(offset); offset += 1; return v; }
          case 2: { const v = view.getInt16(offset); offset += 2; return v; }
          case 3: { const v = view.getInt32(offset); offset += 4; return v; }
          case 4: { const v = view.getBigInt64(offset); offset += 8; return v; }
          case 5: { const v = view.getFloat32(offset); offset += 4; return v; }
          case 6: { const v = view.getFloat64(offset); offset += 8; return v; }
          case 7: {
            const len = view.getInt32(offset); offset += 4;
            const arr = new Int8Array(buffer, offset, len);
            offset += len;
            return arr;
          }
          case 8: return readString();
          case 9: {
            const itemType = view.getInt8(offset++);
            const len = view.getInt32(offset); offset += 4;
            const list = [];
            for (let i = 0; i < len; i++) list.push(readTag(itemType));
            return list;
          }
          case 10: {
            const comp = {};
            while (offset < buffer.byteLength) {
              const t = view.getInt8(offset++);
              if (t === 0) break;
              const name = readString();
              comp[name] = readTag(t);
            }
            return comp;
          }
          case 11: {
            const len = view.getInt32(offset); offset += 4;
            const arr = [];
            for (let i = 0; i < len; i++) { arr.push(view.getInt32(offset)); offset += 4; }
            return arr;
          }
          case 12: {
            const len = view.getInt32(offset); offset += 4;
            const arr = [];
            for (let i = 0; i < len; i++) { arr.push(view.getBigInt64(offset)); offset += 8; }
            return arr;
          }
          default: throw new Error('Unknown NBT tag: ' + type);
        }
      }

      const rootType = view.getInt8(offset++);
      if (rootType !== 10) throw new Error('Root must be Compound (10), got ' + rootType);
      readString();
      return readTag(10);
    }

    function parseLitematicData(id, fileName, buffer) {
      const root = parseNbtBuffer(buffer);
      const metadata = root.Metadata || {};
      const name = metadata.Name || fileName.replace(/\\.litematic$/i, '');
      const author = metadata.Author || 'Unknown';
      const size = metadata.EnclosingSize || { x: 1, y: 1, z: 1 };
      const sizeX = Math.abs(size.x || 1);
      const sizeY = Math.abs(size.y || 1);
      const sizeZ = Math.abs(size.z || 1);

      const materials = {};
      const blocks = [];

      const regions = root.Regions || {};
      for (const regKey of Object.keys(regions)) {
        const reg = regions[regKey];
        const rSize = reg.Size || { x: 1, y: 1, z: 1 };
        const rx = Math.abs(rSize.x || 1);
        const ry = Math.abs(rSize.y || 1);
        const rz = Math.abs(rSize.z || 1);

        const palette = (reg.BlockStatePalette || []).map(p => (p.Name || 'minecraft:air').replace('minecraft:', '').toUpperCase());
        const blockStates = reg.BlockStates || [];

        if (palette.length <= 1) continue;

        const bitsPerEntry = Math.max(2, Math.ceil(Math.log2(palette.length)));
        const mask = (1n << BigInt(bitsPerEntry)) - 1n;
        const total = rx * ry * rz;

        for (let i = 0; i < total; i++) {
          const bitIndex = BigInt(i * bitsPerEntry);
          const startLong = Number(bitIndex / 64n);
          const startBit = Number(bitIndex % 64n);

          if (startLong >= blockStates.length) break;

          const curLong = BigInt.asUintN(64, blockStates[startLong]);
          let val = curLong >> BigInt(startBit);

          if (startBit + bitsPerEntry > 64 && startLong + 1 < blockStates.length) {
            const nextLong = BigInt.asUintN(64, blockStates[startLong + 1]);
            val |= (nextLong << BigInt(64 - startBit));
          }

          const pIdx = Number(val & mask);
          if (pIdx >= 0 && pIdx < palette.length) {
            const mat = palette[pIdx];
            if (mat !== 'AIR' && mat !== 'CAVE_AIR' && mat !== 'VOID_AIR') {
              const y = Math.floor(i / (rx * rz)) % ry;
              const z = Math.floor(i / rx) % rz;
              const x = i % rx;
              blocks.push({ x, y, z, material: mat });
              materials[mat] = (materials[mat] || 0) + 1;
            }
          }
        }
      }

      return {
        id,
        name,
        author,
        format: 'litematic',
        sizeX,
        sizeY,
        sizeZ,
        totalBlocks: blocks.length,
        materialCounts: materials,
        blocks
      };
    }

    function parseVanillaNbtData(id, fileName, buffer) {
      const root = parseNbtBuffer(buffer);
      const name = fileName.replace(/\\.nbt$/i, '');
      const author = root.author || 'Unknown';

      let sizeX = 1, sizeY = 1, sizeZ = 1;
      if (root.size && root.size.length >= 3) {
        sizeX = Math.max(1, root.size[0]);
        sizeY = Math.max(1, root.size[1]);
        sizeZ = Math.max(1, root.size[2]);
      }

      const palette = (root.palette || []).map(p => (p.Name || 'minecraft:air').replace('minecraft:', '').toUpperCase());
      const blocks = [];
      const materials = {};

      if (root.blocks) {
        root.blocks.forEach(b => {
          const state = b.state || 0;
          const mat = palette[state] || 'AIR';
          if (mat !== 'AIR' && mat !== 'CAVE_AIR' && mat !== 'VOID_AIR' && b.pos && b.pos.length >= 3) {
            blocks.push({ x: b.pos[0], y: b.pos[1], z: b.pos[2], material: mat });
            materials[mat] = (materials[mat] || 0) + 1;
          }
        });
      }

      return {
        id,
        name,
        author,
        format: 'nbt',
        sizeX,
        sizeY,
        sizeZ,
        totalBlocks: blocks.length,
        materialCounts: materials,
        blocks
      };
    }

    // =========================================================================
    // DRAG AND DROP & UPLOAD
    // =========================================================================
    function setupDropzone() {
      const dz = document.getElementById('dropzone');
      dz.addEventListener('dragover', (e) => { e.preventDefault(); dz.classList.add('dragover'); });
      dz.addEventListener('dragleave', () => { dz.classList.remove('dragover'); });
      dz.addEventListener('drop', (e) => {
        e.preventDefault();
        dz.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
          processBlueprintFile(e.dataTransfer.files[0]);
        }
      });
    }

    function handleFileSelect(e) {
      if (e.target.files.length > 0) {
        processBlueprintFile(e.target.files[0]);
      }
    }

    async function processBlueprintFile(file) {
      const lower = file.name.toLowerCase();
      if (!lower.endsWith('.litematic') && !lower.endsWith('.nbt')) {
        showToast('Invalid format. Please drop .litematic or .nbt', true);
        return;
      }

      showToast('⚡ Processing ' + file.name + '…');
      try {
        const rawBuffer = await file.arrayBuffer();
        const decompressed = await decompressGzip(rawBuffer);
        const generatedId = 'BP-' + Math.random().toString(36).substring(2, 6).toUpperCase();

        let parsedBp;
        if (lower.endsWith('.litematic')) {
          parsedBp = parseLitematicData(generatedId, file.name, decompressed);
        } else {
          parsedBp = parseVanillaNbtData(generatedId, file.name, decompressed);
        }

        saveToLocalLibrary(parsedBp, file.size);
        showToast(`✔ Loaded "${parsedBp.name}" (${parsedBp.totalBlocks.toLocaleString()} blocks)`);
        renderBlueprintList();
        selectBlueprint(parsedBp.id);
      } catch (err) {
        console.error(err);
        showToast('Parsing error: ' + err.message, true);
      }
    }

    // =========================================================================
    // BLUEPRINT LIST & SELECTION
    // =========================================================================
    function syncLibrary() {
      renderBlueprintList();
      if (blueprints.length > 0 && !selectedBlueprint) {
        selectBlueprint(blueprints[0].id);
      }
    }

    function renderBlueprintList(filterText = '') {
      const list = document.getElementById('bpList');
      const filtered = blueprints.filter(bp => {
        if (!filterText) return true;
        const q = filterText.toLowerCase();
        return bp.name.toLowerCase().includes(q) || (bp.id && bp.id.toLowerCase().includes(q));
      });

      if (filtered.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding: 24px; color: var(--text-muted);">No matching blueprints.</div>';
        return;
      }

      list.innerHTML = filtered.map(bp => `
        <div class="bp-item ${selectedBlueprint && selectedBlueprint.id === bp.id ? 'selected' : ''}" onclick="selectBlueprint('${bp.id}')">
          <div class="bp-header">
            <span class="bp-name" title="${escapeHtml(bp.name)}">${escapeHtml(bp.name)}</span>
            <div class="badges-row">
              <span class="badge ${bp.format === 'litematic' || bp.format === 'LITEMATIC' ? 'badge-litematic' : 'badge-nbt'}">${bp.format}</span>
              ${bp.isCommunity ? '<span class="badge badge-community">Featured</span>' : ''}
            </div>
          </div>
          <div class="bp-meta">
            <span>📐 ${bp.sizeX}×${bp.sizeY}×${bp.sizeZ} • 🧱 ${bp.totalBlocks.toLocaleString()}</span>
            <span style="color:var(--primary); font-weight:700;">${bp.id}</span>
          </div>
        </div>
      `).join('');
    }

    function filterBlueprints(query) {
      renderBlueprintList(query);
    }

    async function selectBlueprint(id) {
      let bp = blueprints.find(b => b.id === id);
      if (!bp) return;

      if (bp.sampleUrl && (!bp.blocks || bp.blocks.length === 0)) {
        showToast('Loading full 3D voxels…');
        try {
          const res = await fetch(bp.sampleUrl);
          const buf = await res.arrayBuffer();
          const decomp = await decompressGzip(buf);
          const full = parseLitematicData(bp.id, bp.name, decomp);
          bp.blocks = full.blocks;
          bp.materialCounts = full.materialCounts;
        } catch (e) {
          console.warn('Could not load sample blocks:', e);
        }
      }

      selectedBlueprint = bp;
      renderBlueprintList();

      document.getElementById('visualizerTitle').textContent = bp.name;
      document.getElementById('visualizerDims').textContent = `${bp.sizeX} × ${bp.sizeY} × ${bp.sizeZ} (${bp.totalBlocks.toLocaleString()} blocks) • Author: ${bp.author || 'Unknown'}`;

      // Update In-Game Code Display & Copy Boxes
      document.getElementById('bpCodeDisplay').textContent = bp.id;
      document.getElementById('inGameCommandDisplay').textContent = `/pc build ${bp.id}`;
      document.getElementById('luaCodeDisplay').textContent = `turtle.build("${bp.id}")`;

      // Slider setup
      const slider = document.getElementById('layerSlider');
      slider.max = Math.max(0, bp.sizeY - 1);
      slider.value = 0;
      document.getElementById('layerDisplay').textContent = `0 / ${slider.max}`;

      // Materials table setup
      const tbody = document.getElementById('matTableBody');
      const mats = bp.materialCounts || {};
      const keys = Object.keys(mats).sort((a,b) => mats[b] - mats[a]);
      document.getElementById('totalMaterialsCount').textContent = keys.length + ' types';

      if (keys.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding:15px; color:var(--text-muted);">No blocks recorded.</td></tr>';
      } else {
        tbody.innerHTML = keys.map(k => {
          const count = mats[k];
          const stacks = Math.floor(count / 64);
          const rem = count % 64;
          const stackStr = stacks > 0 ? `${stacks}s + ${rem}` : `${rem}`;
          return `
            <tr>
              <td><code style="color:var(--primary);">${escapeHtml(k)}</code></td>
              <td style="font-weight:700;">${count.toLocaleString()}</td>
              <td style="color:var(--text-muted);">${stackStr}</td>
            </tr>
          `;
        }).join('');
      }

      renderCanvas();
    }

    // =========================================================================
    // COPY CODE & COMMANDS
    // =========================================================================
    function copyBlueprintCode() {
      if (!selectedBlueprint) return;
      navigator.clipboard.writeText(selectedBlueprint.id).then(() => {
        showToast('📋 Copied Code: ' + selectedBlueprint.id);
      });
    }

    function copyInGameCommand() {
      if (!selectedBlueprint) return;
      const cmd = `/pc build ${selectedBlueprint.id}`;
      navigator.clipboard.writeText(cmd).then(() => {
        showToast('⚡ Copied Chat Command: ' + cmd);
      });
    }

    function copyLuaCode() {
      if (!selectedBlueprint) return;
      const lua = `turtle.build("${selectedBlueprint.id}")`;
      navigator.clipboard.writeText(lua).then(() => {
        showToast('💻 Copied Lua Script: ' + lua);
      });
    }

    function downloadSelectedFile() {
      if (!selectedBlueprint) return;
      if (selectedBlueprint.sampleUrl) {
        window.open(selectedBlueprint.sampleUrl, '_blank');
      } else {
        const jsonStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(selectedBlueprint));
        const downloadAnchor = document.createElement('a');
        downloadAnchor.setAttribute("href", jsonStr);
        downloadAnchor.setAttribute("download", `${selectedBlueprint.id}_${selectedBlueprint.name}.json`);
        document.body.appendChild(downloadAnchor);
        downloadAnchor.click();
        downloadAnchor.remove();
      }
    }

    // =========================================================================
    // 3D ISOMETRIC & 2D SLICE CANVAS RENDERER
    // =========================================================================
    function setViewMode(mode) {
      viewMode = mode;
      document.getElementById('tab3D').className = 'tab-btn' + (mode === '3D' ? ' active' : '');
      document.getElementById('tab2D').className = 'tab-btn' + (mode === '2D' ? ' active' : '');
      renderCanvas();
    }

    function zoomIn() { zoomLevel = Math.min(3.5, zoomLevel + 0.2); renderCanvas(); }
    function zoomOut() { zoomLevel = Math.max(0.4, zoomLevel - 0.2); renderCanvas(); }
    function rotateView() { currentRotation = (currentRotation + 90) % 360; renderCanvas(); }
    function resetView() { zoomLevel = 1.0; currentRotation = 0; renderCanvas(); }

    function stepLayer(delta) {
      const slider = document.getElementById('layerSlider');
      let val = parseInt(slider.value, 10) + delta;
      val = Math.max(0, Math.min(parseInt(slider.max, 10), val));
      slider.value = val;
      onLayerChange(val);
    }

    function togglePlayAnimation() {
      const btn = document.getElementById('playAnimBtn');
      if (animInterval) {
        clearInterval(animInterval);
        animInterval = null;
        btn.textContent = '▶';
      } else {
        btn.textContent = '⏸';
        animInterval = setInterval(() => {
          const slider = document.getElementById('layerSlider');
          let val = parseInt(slider.value, 10) + 1;
          if (val > parseInt(slider.max, 10)) val = 0;
          slider.value = val;
          onLayerChange(val);
        }, 120);
      }
    }

    function onLayerChange(val) {
      document.getElementById('layerDisplay').textContent = `${val} / ${document.getElementById('layerSlider').max}`;
      renderCanvas();
    }

    function renderCanvas() {
      const canvas = document.getElementById('previewCanvas');
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      if (!selectedBlueprint || !selectedBlueprint.blocks || selectedBlueprint.blocks.length === 0) {
        ctx.strokeStyle = '#1e293b';
        ctx.lineWidth = 1;
        for (let i = 0; i < canvas.width; i += 30) {
          ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height); ctx.stroke();
        }
        for (let j = 0; j < canvas.height; j += 30) {
          ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(canvas.width, j); ctx.stroke();
        }
        return;
      }

      const curLayer = parseInt(document.getElementById('layerSlider').value, 10);
      const sx = selectedBlueprint.sizeX || 1;
      const sy = selectedBlueprint.sizeY || 1;
      const sz = selectedBlueprint.sizeZ || 1;

      if (viewMode === '2D') {
        render2DSlice(ctx, canvas.width, canvas.height, curLayer, sx, sz);
      } else {
        render3DIsometric(ctx, canvas.width, canvas.height, curLayer, sx, sy, sz);
      }
    }

    function render2DSlice(ctx, w, h, layerY, sx, sz) {
      const layerBlocks = selectedBlueprint.blocks.filter(b => b.y === layerY);
      const baseCell = Math.min(Math.floor(w / sx), Math.floor(h / sz), 30);
      const cellSize = Math.max(2, Math.floor(baseCell * zoomLevel));

      const offX = Math.floor((w - sx * cellSize) / 2);
      const offY = Math.floor((h - sz * cellSize) / 2);

      ctx.strokeStyle = '#1e293b';
      ctx.lineWidth = 1;
      ctx.strokeRect(offX, offY, sx * cellSize, sz * cellSize);

      layerBlocks.forEach(b => {
        let x = b.x, z = b.z;
        if (currentRotation === 90) { const tx = x; x = sz - 1 - z; z = tx; }
        else if (currentRotation === 180) { x = sx - 1 - x; z = sz - 1 - z; }
        else if (currentRotation === 270) { const tx = x; x = z; z = sx - 1 - tx; }

        ctx.fillStyle = hashColor(b.material || b.blockState);
        ctx.fillRect(offX + x * cellSize, offY + z * cellSize, Math.max(1, cellSize - 1), Math.max(1, cellSize - 1));
      });
    }

    function render3DIsometric(ctx, w, h, maxLayer, sx, sy, sz) {
      const blocks = selectedBlueprint.blocks.filter(b => b.y <= maxLayer);
      
      const maxDim = Math.max(sx, sy, sz);
      const tileSize = Math.max(3, Math.floor((w / (maxDim * 2.2)) * zoomLevel));
      const tileWidth = tileSize * 2;
      const tileHeight = tileSize;
      const blockHeight = tileSize * 1.2;

      const originX = w / 2;
      const originY = h * 0.7;

      blocks.sort((a, b) => {
        if (a.y !== b.y) return a.y - b.y;
        return (a.x + a.z) - (b.x + b.z);
      });

      blocks.forEach(b => {
        let x = b.x, z = b.z;
        if (currentRotation === 90) { const tx = x; x = sz - 1 - z; z = tx; }
        else if (currentRotation === 180) { x = sx - 1 - x; z = sz - 1 - z; }
        else if (currentRotation === 270) { const tx = x; x = z; z = sx - 1 - tx; }

        const px = originX + (x - z) * (tileWidth / 2);
        const py = originY + (x + z) * (tileHeight / 2) - b.y * blockHeight;

        drawIsoVoxel(ctx, px, py, tileWidth, tileHeight, blockHeight, hashColor(b.material || b.blockState));
      });
    }

    function drawIsoVoxel(ctx, x, y, tw, th, bh, hexColor) {
      const hw = tw / 2;
      const hh = th / 2;

      // Top face
      ctx.fillStyle = hexColor;
      ctx.beginPath();
      ctx.moveTo(x, y - bh);
      ctx.lineTo(x + hw, y - bh + hh);
      ctx.lineTo(x, y - bh + th);
      ctx.lineTo(x - hw, y - bh + hh);
      ctx.closePath();
      ctx.fill();

      // Left face
      ctx.fillStyle = shadeColor(hexColor, -25);
      ctx.beginPath();
      ctx.moveTo(x - hw, y - bh + hh);
      ctx.lineTo(x, y - bh + th);
      ctx.lineTo(x, y + th);
      ctx.lineTo(x - hw, y + hh);
      ctx.closePath();
      ctx.fill();

      // Right face
      ctx.fillStyle = shadeColor(hexColor, -45);
      ctx.beginPath();
      ctx.moveTo(x, y - bh + th);
      ctx.lineTo(x + hw, y - bh + hh);
      ctx.lineTo(x + hw, y + hh);
      ctx.lineTo(x, y + th);
      ctx.closePath();
      ctx.fill();
    }

    function hashColor(str) {
      if (!str) return '#38bdf8';
      let hash = 0;
      for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
      const c = (hash & 0x00FFFFFF).toString(16).toUpperCase();
      return '#' + '00000'.substring(0, 6 - c.length) + c;
    }

    function shadeColor(color, percent) {
      let R = parseInt(color.substring(1,3), 16);
      let G = parseInt(color.substring(3,5), 16);
      let B = parseInt(color.substring(5,7), 16);
      R = Math.max(0, Math.min(255, parseInt(R * (100 + percent) / 100)));
      G = Math.max(0, Math.min(255, parseInt(G * (100 + percent) / 100)));
      B = Math.max(0, Math.min(255, parseInt(B * (100 + percent) / 100)));
      return '#' + ((1 << 24) + (R << 16) + (G << 8) + B).toString(16).slice(1);
    }

    function setupCanvasInteraction() {
      const canvas = document.getElementById('previewCanvas');
      canvas.addEventListener('wheel', (e) => {
        e.preventDefault();
        if (e.deltaY < 0) zoomIn(); else zoomOut();
      });
    }

    function escapeHtml(str) {
      if (!str) return '';
      return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }
  </script>
</body>
</html>

""";
}