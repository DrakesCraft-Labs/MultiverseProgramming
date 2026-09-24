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
      --bg: #0b0f19;
      --card-bg: #151d30;
      --card-border: #263353;
      --card-border-focus: #38bdf8;
      --primary: #38bdf8;
      --primary-hover: #0284c7;
      --accent: #10b981;
      --accent-hover: #059669;
      --warning: #f59e0b;
      --danger: #ef4444;
      --text: #f1f5f9;
      --text-muted: #94a3b8;
      --grid-line: #1e293b;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
    body { background: var(--bg); color: var(--text); min-height: 100vh; display: flex; flex-direction: column; }
    
    header { background: #0f172a; border-bottom: 1px solid var(--card-border); padding: 14px 28px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
    .brand { display: flex; align-items: center; gap: 12px; font-weight: 700; font-size: 1.25rem; letter-spacing: 0.5px; }
    .brand-badge { background: linear-gradient(135deg, #0284c7, #38bdf8); color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 0.8rem; text-transform: uppercase; font-weight: 800; letter-spacing: 1px; }
    
    .header-controls { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
    .server-conn-bar { display: flex; align-items: center; gap: 8px; background: #0b0f19; padding: 4px 10px; border-radius: 8px; border: 1px solid var(--card-border); font-size: 0.85rem; }
    .server-conn-bar input { background: transparent; border: none; color: #fff; font-size: 0.82rem; outline: none; width: 190px; }
    .status-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--warning); box-shadow: 0 0 8px var(--warning); transition: 0.3s; flex-shrink: 0; }
    .status-dot.online { background: var(--accent); box-shadow: 0 0 10px var(--accent); }
    .status-dot.offline { background: var(--danger); box-shadow: 0 0 10px var(--danger); }
    
    .server-stats { display: flex; align-items: center; gap: 12px; font-size: 0.85rem; color: var(--text-muted); }
    
    #mixedContentAlert { display: none; background: rgba(245, 158, 11, 0.15); border-bottom: 1px solid var(--warning); color: #fde68a; padding: 10px 24px; font-size: 0.84rem; text-align: center; }
    #mixedContentAlert a { color: #fff; font-weight: 700; text-decoration: underline; cursor: pointer; }

    .container { max-width: 1440px; margin: 0 auto; padding: 24px; display: grid; grid-template-columns: 360px 1fr 380px; gap: 24px; flex: 1; width: 100%; }
    @media (max-width: 1180px) { .container { grid-template-columns: 1fr; } }

    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
    .card-title { font-size: 1.1rem; font-weight: 600; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--card-border); padding-bottom: 12px; }
    
    .dropzone { border: 2px dashed #38bdf8; border-radius: 10px; padding: 28px 16px; text-align: center; background: rgba(56, 189, 248, 0.04); cursor: pointer; transition: 0.2s; }
    .dropzone:hover, .dropzone.dragover { background: rgba(56, 189, 248, 0.12); border-color: #7dd3fc; }
    .dropzone-icon { font-size: 2.2rem; margin-bottom: 8px; }
    .dropzone-text { font-size: 0.95rem; font-weight: 600; color: #fff; }
    .dropzone-sub { font-size: 0.8rem; color: var(--text-muted); margin-top: 4px; }
    input[type="file"] { display: none; }

    .bp-list { display: flex; flex-direction: column; gap: 10px; max-height: 480px; overflow-y: auto; padding-right: 4px; }
    .bp-item { background: #1a233b; border: 1px solid var(--card-border); border-radius: 8px; padding: 12px; cursor: pointer; transition: 0.15s; }
    .bp-item:hover, .bp-item.selected { border-color: var(--primary); background: #1e2c4d; }
    .bp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
    .bp-name { font-weight: 600; font-size: 0.95rem; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 210px; }
    .badge { font-size: 0.72rem; padding: 2px 7px; border-radius: 4px; font-weight: 700; text-transform: uppercase; }
    .badge-litematic { background: #7c3aed; color: #fff; }
    .badge-nbt { background: #2563eb; color: #fff; }
    .bp-meta { font-size: 0.8rem; color: var(--text-muted); display: flex; gap: 12px; }

    .preview-canvas-container { background: #0b0f19; border: 1px solid var(--card-border); border-radius: 8px; height: 320px; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; }
    canvas { background: #050811; border-radius: 6px; }
    .slider-row { display: flex; align-items: center; gap: 12px; }
    input[type="range"] { flex: 1; accent-color: var(--primary); }

    .mat-table-container { max-height: 180px; overflow-y: auto; border: 1px solid var(--card-border); border-radius: 8px; }
    table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
    th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid var(--card-border); }
    th { background: #111827; color: var(--text-muted); position: sticky; top: 0; }
    tr:hover { background: rgba(255,255,255,0.02); }

    .turtle-list { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; }
    .turtle-item { background: #1a233b; border: 1px solid var(--card-border); border-radius: 8px; padding: 10px; display: flex; justify-content: space-between; align-items: center; }
    .turtle-status-IDLE { color: var(--accent); }
    .turtle-status-BUILDING { color: var(--warning); }
    .turtle-status-PAUSED { color: #f97316; }
    .turtle-status-ERROR { color: var(--danger); }

    .form-group { display: flex; flex-direction: column; gap: 6px; }
    label { font-size: 0.85rem; color: var(--text-muted); }
    input[type="text"], input[type="number"], select { background: #0b0f19; border: 1px solid var(--card-border); color: #fff; padding: 10px 12px; border-radius: 6px; font-size: 0.9rem; }
    input:focus, select:focus { outline: none; border-color: var(--primary); }
    .coord-inputs { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }

    .btn { background: var(--primary); color: #000; border: none; padding: 12px; border-radius: 8px; font-weight: 700; cursor: pointer; transition: 0.15s; font-size: 0.95rem; text-align: center; }
    .btn:hover { background: var(--primary-hover); color: #fff; }
    .btn-accent { background: var(--accent); color: #fff; }
    .btn-accent:hover { background: var(--accent-hover); }
    .btn-danger { background: var(--danger); color: #fff; }
    .btn-sm { padding: 6px 12px; font-size: 0.8rem; border-radius: 4px; }

    #toast { position: fixed; bottom: 24px; right: 24px; background: #1e293b; color: #fff; padding: 14px 20px; border-radius: 8px; border-left: 4px solid var(--primary); box-shadow: 0 4px 20px rgba(0,0,0,0.5); display: none; z-index: 100; font-size: 0.9rem; }
  </style>
</head>
<body>
  <header>
    <div class="brand">
      <span class="brand-badge">TURTLE MATRIX</span>
      <span>Multiverse Programming // Blueprint Nexus</span>
    </div>
    
    <div class="header-controls">
      <div class="server-conn-bar" title="Minecraft Server Webhook API Base">
        <div class="status-dot" id="serverStatusDot"></div>
        <input type="text" id="serverUrlInput" placeholder="http://localhost:8080" />
        <button class="btn btn-sm" onclick="saveServerUrl()">Connect</button>
      </div>

      <div class="server-stats">
        <span id="stat-turtles">Turtles: 0</span>
        <span>•</span>
        <span id="stat-blueprints">Blueprints: 0</span>
      </div>
    </div>
  </header>

  <div id="mixedContentAlert">
    ⚠️ <strong>Connecting to a local/HTTP server from HTTPS (GitHub Pages)?</strong> If requests fail, allow Insecure Content in your browser (Site Settings &gt; Insecure content &gt; Allow) or connect via an HTTPS reverse proxy/tunnel.
  </div>

  <main class="container">
    <!-- COLUMN 1: UPLOAD & BLUEPRINTS -->
    <div class="card">
      <div class="card-title">
        <span>Blueprint Library</span>
        <button class="btn btn-sm" onclick="fetchBlueprints()">⟳ Refresh</button>
      </div>

      <div class="dropzone" id="dropzone" onclick="document.getElementById('fileInput').click()">
        <div class="dropzone-icon">📥</div>
        <div class="dropzone-text">Drop .litematic or .nbt file here</div>
        <div class="dropzone-sub">or click to browse from your computer</div>
      </div>
      <input type="file" id="fileInput" accept=".litematic,.nbt" onchange="handleFileSelect(event)">

      <div class="bp-list" id="bpList">
        <div style="text-align:center; padding: 20px; color: var(--text-muted);">No blueprints uploaded yet.</div>
      </div>
    </div>

    <!-- COLUMN 2: 3D / LAYER VISUALIZER -->
    <div class="card">
      <div class="card-title">
        <span id="visualizerTitle">Design Visualizer</span>
        <span id="visualizerDims" style="font-size:0.85rem; color:var(--text-muted);">Select a blueprint</span>
      </div>

      <div class="preview-canvas-container">
        <canvas id="previewCanvas" width="400" height="300"></canvas>
      </div>

      <div class="slider-row">
        <label>Layer (Y):</label>
        <input type="range" id="layerSlider" min="0" max="0" value="0" oninput="onLayerChange(this.value)">
        <span id="layerDisplay" style="font-weight: 700; min-width: 40px;">0 / 0</span>
      </div>

      <div class="card-title" style="margin-top: 8px;">
        <span>Required Materials</span>
      </div>
      <div class="mat-table-container">
        <table id="matTable">
          <thead><tr><th>Item / Block</th><th>Count</th></tr></thead>
          <tbody id="matTableBody">
            <tr><td colspan="2" style="text-align: center; color: var(--text-muted);">Select a blueprint to view materials</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- COLUMN 3: TURTLES & DISPATCH -->
    <div class="card">
      <div class="card-title">
        <span>Active Turtles</span>
        <button class="btn btn-sm" onclick="fetchTurtles()">⟳ Refresh</button>
      </div>

      <div class="turtle-list" id="turtleList">
        <div style="text-align:center; padding: 20px; color: var(--text-muted);">No active turtles online. Place one in-game!</div>
      </div>

      <div class="card-title" style="margin-top: 12px;">
        <span>Dispatch Construction</span>
      </div>

      <div class="form-group">
        <label>Selected Blueprint</label>
        <select id="dispatchBpSelect" onchange="onDispatchBpSelect(this.value)">
          <option value="">-- Choose Blueprint --</option>
        </select>
      </div>

      <div class="form-group">
        <label>Target Turtle</label>
        <select id="dispatchTurtleSelect" onchange="onDispatchTurtleSelect(this.value)">
          <option value="">-- Choose Turtle --</option>
        </select>
      </div>

      <div class="form-group">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <label>Target Coordinates (X, Y, Z)</label>
          <a href="#" style="font-size:0.78rem; color:var(--primary); text-decoration:none;" onclick="useTurtleCoords()">Use Turtle Coords</a>
        </div>
        <div class="coord-inputs">
          <input type="number" id="coordX" placeholder="X">
          <input type="number" id="coordY" placeholder="Y">
          <input type="number" id="coordZ" placeholder="Z">
        </div>
      </div>

      <button class="btn btn-accent" onclick="startDispatchBuild()">⚡ INITIATE CONSTRUCTION</button>

      <!-- Live Job Progress -->
      <div id="liveJobCard" style="display:none; background:#111827; border: 1px solid var(--card-border); border-radius: 8px; padding: 12px; margin-top: 10px;">
        <div style="display:flex; justify-content:space-between; font-weight:600; font-size:0.9rem; margin-bottom: 6px;">
          <span id="jobTurtleName">Turtle T-001</span>
          <span id="jobPercent" style="color:var(--accent);">0%</span>
        </div>
        <div style="background:#1e293b; height: 8px; border-radius: 4px; overflow:hidden; margin-bottom: 8px;">
          <div id="jobProgressBar" style="background:var(--accent); width: 0%; height: 100%; transition: 0.2s;"></div>
        </div>
        <div style="display:flex; justify-content:space-between; font-size:0.8rem; color:var(--text-muted); margin-bottom: 10px;">
          <span id="jobBlocks">0 / 0 blocks</span>
          <span id="jobStatus">Building</span>
        </div>
        <div style="display:flex; gap: 8px;">
          <button class="btn btn-sm" style="flex:1;" onclick="togglePauseJob()">Pause / Resume</button>
          <button class="btn btn-sm btn-danger" style="flex:1;" onclick="cancelJob()">Cancel Job</button>
        </div>
      </div>
    </div>
  </main>

  <div id="toast"></div>

  <script>
    let blueprints = [];
    let turtles = [];
    let selectedBlueprint = null;

    function getApiBase() {
      const stored = localStorage.getItem('mp_server_url');
      if (stored !== null && stored.trim() !== '') {
        return stored.trim().replace(/\\/+$/, '');
      }
      if (window.location.protocol === 'file:' || window.location.hostname.endsWith('github.io')) {
        return 'http://localhost:8080';
      }
      return window.location.origin;
    }

    function initServerUrl() {
      const input = document.getElementById('serverUrlInput');
      const base = getApiBase();
      input.value = base;
      if (window.location.protocol === 'https:' && base.startsWith('http:')) {
        document.getElementById('mixedContentAlert').style.display = 'block';
      }
    }

    function saveServerUrl() {
      const input = document.getElementById('serverUrlInput');
      let val = input.value.trim().replace(/\\/+$/, '');
      if (val) {
        localStorage.setItem('mp_server_url', val);
        showToast('Server URL set to: ' + val);
        if (window.location.protocol === 'https:' && val.startsWith('http:')) {
          document.getElementById('mixedContentAlert').style.display = 'block';
        } else {
          document.getElementById('mixedContentAlert').style.display = 'none';
        }
        fetchBlueprints();
        fetchTurtles();
      }
    }

    function updateServerStatusDot(online) {
      const dot = document.getElementById('serverStatusDot');
      dot.className = 'status-dot ' + (online ? 'online' : 'offline');
    }

    // Init
    window.addEventListener('DOMContentLoaded', () => {
      initServerUrl();
      fetchBlueprints();
      fetchTurtles();
      setInterval(fetchTurtles, 3000);
      setupDropzone();
    });

    function showToast(msg, isError = false) {
      const toast = document.getElementById('toast');
      toast.textContent = msg;
      toast.style.borderColor = isError ? 'var(--danger)' : 'var(--accent)';
      toast.style.display = 'block';
      setTimeout(() => { toast.style.display = 'none'; }, 3500);
    }

    // Drag and Drop
    function setupDropzone() {
      const dz = document.getElementById('dropzone');
      dz.addEventListener('dragover', (e) => { e.preventDefault(); dz.classList.add('dragover'); });
      dz.addEventListener('dragleave', () => { dz.classList.remove('dragover'); });
      dz.addEventListener('drop', (e) => {
        e.preventDefault();
        dz.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
          uploadFile(e.dataTransfer.files[0]);
        }
      });
    }

    function handleFileSelect(e) {
      if (e.target.files.length > 0) {
        uploadFile(e.target.files[0]);
      }
    }

    function uploadFile(file) {
      if (!file.name.endsWith('.litematic') && !file.name.endsWith('.nbt')) {
        showToast('Invalid file format. Please upload .litematic or .nbt', true);
        return;
      }
      showToast('Uploading ' + file.name + '…');
      const reader = new FileReader();
      reader.onload = function(evt) {
        const arrayBuffer = evt.target.result;
        const bytes = new Uint8Array(arrayBuffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
          binary += String.fromCharCode(bytes[i]);
        }
        const base64Data = btoa(binary);

        fetch(getApiBase() + '/api/upload', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ filename: file.name, data: base64Data })
        })
        .then(res => res.json())
        .then(data => {
          if (data.ok) {
            showToast('✔ Blueprint uploaded: ' + data.blueprint.name);
            fetchBlueprints();
            selectBlueprint(data.blueprint.id);
          } else {
            showToast('Upload failed: ' + (data.error || 'Unknown error'), true);
          }
        })
        .catch(err => {
          showToast('Upload network error: ' + err, true);
          updateServerStatusDot(false);
        });
      };
      reader.readAsArrayBuffer(file);
    }

    // Fetch Blueprints
    function fetchBlueprints() {
      fetch(getApiBase() + '/api/blueprints')
        .then(res => res.json())
        .then(data => {
          blueprints = data;
          updateServerStatusDot(true);
          document.getElementById('stat-blueprints').textContent = 'Blueprints: ' + blueprints.length;
          renderBlueprintList();
          updateDispatchDropdowns();
        })
        .catch(err => {
          console.error(err);
          updateServerStatusDot(false);
        });
    }

    function renderBlueprintList() {
      const list = document.getElementById('bpList');
      if (blueprints.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--text-muted);">No blueprints uploaded yet.</div>';
        return;
      }
      list.innerHTML = blueprints.map(bp => `
        <div class="bp-item ${selectedBlueprint && selectedBlueprint.id === bp.id ? 'selected' : ''}" onclick="selectBlueprint('${bp.id}')">
          <div class="bp-header">
            <span class="bp-name" title="${escapeHtml(bp.name)}">${escapeHtml(bp.name)}</span>
            <span class="badge ${bp.format === 'LITEMATIC' ? 'badge-litematic' : 'badge-nbt'}">${bp.format}</span>
          </div>
          <div class="bp-meta">
            <span>📐 ${bp.sizeX}×${bp.sizeY}×${bp.sizeZ}</span>
            <span>🧱 ${bp.totalBlocks} blocks</span>
          </div>
        </div>
      `).join('');
    }

    function selectBlueprint(id) {
      fetch(getApiBase() + '/api/blueprints/' + id)
        .then(res => res.json())
        .then(bp => {
          selectedBlueprint = bp;
          renderBlueprintList();
          document.getElementById('visualizerTitle').textContent = bp.name;
          document.getElementById('visualizerDims').textContent = `${bp.sizeX} × ${bp.sizeY} × ${bp.sizeZ} (${bp.totalBlocks} blocks)`;

          const slider = document.getElementById('layerSlider');
          slider.max = Math.max(0, bp.sizeY - 1);
          slider.value = 0;
          document.getElementById('layerDisplay').textContent = `0 / ${slider.max}`;

          const tbody = document.getElementById('matTableBody');
          const mats = bp.materialCounts || {};
          const keys = Object.keys(mats).sort((a,b) => mats[b] - mats[a]);
          if (keys.length === 0) {
            tbody.innerHTML = '<tr><td colspan="2" style="text-align:center; color:var(--text-muted);">No blocks in this blueprint.</td></tr>';
          } else {
            tbody.innerHTML = keys.map(k => `
              <tr>
                <td><code>${escapeHtml(k.replace('minecraft:', ''))}</code></td>
                <td style="font-weight:700;">${mats[k]}</td>
              </tr>
            `).join('');
          }

          document.getElementById('dispatchBpSelect').value = bp.id;
          drawLayer(0);
        })
        .catch(err => console.error(err));
    }

    function onLayerChange(val) {
      document.getElementById('layerDisplay').textContent = `${val} / ${document.getElementById('layerSlider').max}`;
      drawLayer(parseInt(val, 10));
    }

    function drawLayer(layerY) {
      const canvas = document.getElementById('previewCanvas');
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      if (!selectedBlueprint || !selectedBlueprint.blocks) return;

      const layerBlocks = selectedBlueprint.blocks.filter(b => b.y === layerY);
      const sx = selectedBlueprint.sizeX || 1;
      const sz = selectedBlueprint.sizeZ || 1;

      const cellSize = Math.min(Math.floor(canvas.width / sx), Math.floor(canvas.height / sz), 24);
      const offsetX = Math.floor((canvas.width - sx * cellSize) / 2);
      const offsetY = Math.floor((canvas.height - sz * cellSize) / 2);

      ctx.strokeStyle = '#1e293b';
      ctx.strokeRect(offsetX, offsetY, sx * cellSize, sz * cellSize);

      layerBlocks.forEach(b => {
        ctx.fillStyle = hashColor(b.blockState);
        ctx.fillRect(offsetX + b.x * cellSize, offsetY + b.z * cellSize, Math.max(1, cellSize - 1), Math.max(1, cellSize - 1));
      });
    }

    function hashColor(str) {
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
      }
      const c = (hash & 0x00FFFFFF).toString(16).toUpperCase();
      return '#' + '00000'.substring(0, 6 - c.length) + c;
    }

    function fetchTurtles() {
      fetch(getApiBase() + '/api/turtles')
        .then(res => res.json())
        .then(data => {
          turtles = data;
          updateServerStatusDot(true);
          document.getElementById('stat-turtles').textContent = 'Turtles: ' + turtles.length;
          renderTurtleList();
          updateDispatchDropdowns();
          updateActiveJob();
        })
        .catch(err => {
          console.error(err);
          updateServerStatusDot(false);
        });
    }

    function renderTurtleList() {
      const list = document.getElementById('turtleList');
      if (turtles.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--text-muted);">No active turtles online. Place one in-game!</div>';
        return;
      }
      list.innerHTML = turtles.map(t => `
        <div class="turtle-item">
          <div>
            <div style="font-weight:700;">${t.id} <span style="font-weight:400; font-size:0.8rem; color:var(--text-muted);">[${t.x}, ${t.y}, ${t.z}]</span></div>
            <div style="font-size:0.75rem; color:var(--text-muted);">Facing: ${t.facing} • Fuel: ${t.fuel}</div>
          </div>
          <div class="turtle-status-${t.status}" style="font-weight:700; font-size:0.85rem;">
            ${t.status}
          </div>
        </div>
      `).join('');
    }

    function updateDispatchDropdowns() {
      const tSelect = document.getElementById('dispatchTurtleSelect');
      const curT = tSelect.value;
      tSelect.innerHTML = '<option value="">-- Choose Turtle --</option>' +
        turtles.map(t => `<option value="${t.id}" ${t.id === curT ? 'selected' : ''}>${t.id} at [${t.x}, ${t.y}, ${t.z}] (${t.status})</option>`).join('');

      const bpSelect = document.getElementById('dispatchBpSelect');
      const curBp = bpSelect.value;
      bpSelect.innerHTML = '<option value="">-- Choose Blueprint --</option>' +
        blueprints.map(b => `<option value="${b.id}" ${b.id === curBp ? 'selected' : ''}>${escapeHtml(b.name)} (${b.totalBlocks} blocks)</option>`).join('');
    }

    function onDispatchBpSelect(bpId) {
      if (bpId) selectBlueprint(bpId);
    }

    function onDispatchTurtleSelect(turtleId) {
      const t = turtles.find(x => x.id === turtleId);
      if (t) {
        document.getElementById('coordX').value = t.x;
        document.getElementById('coordY').value = t.y;
        document.getElementById('coordZ').value = t.z;
      }
    }

    function useTurtleCoords() {
      const tId = document.getElementById('dispatchTurtleSelect').value;
      onDispatchTurtleSelect(tId);
    }

    function startDispatchBuild() {
      const bpId = document.getElementById('dispatchBpSelect').value;
      const turtleId = document.getElementById('dispatchTurtleSelect').value;
      const x = parseInt(document.getElementById('coordX').value, 10);
      const y = parseInt(document.getElementById('coordY').value, 10);
      const z = parseInt(document.getElementById('coordZ').value, 10);

      if (!bpId || !turtleId || isNaN(x) || isNaN(y) || isNaN(z)) {
        showToast('Please select Blueprint, Turtle, and valid coordinates.', true);
        return;
      }

      fetch(getApiBase() + '/api/build', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ blueprintId: bpId, turtleId, x, y, z })
      })
      .then(res => res.json())
      .then(data => {
        if (data.ok) {
          showToast('⚡ Build dispatched to ' + turtleId);
          fetchTurtles();
        } else {
          showToast('Dispatch failed: ' + data.error, true);
        }
      })
      .catch(err => showToast('Error: ' + err, true));
    }

    function updateActiveJob() {
      const buildingTurtle = turtles.find(t => t.status === 'BUILDING' || t.status === 'PAUSED');
      const jobCard = document.getElementById('liveJobCard');
      if (buildingTurtle && buildingTurtle.progress) {
        jobCard.style.display = 'block';
        document.getElementById('jobTurtleName').textContent = 'Turtle ' + buildingTurtle.id;
        document.getElementById('jobPercent').textContent = buildingTurtle.progress.percentage.toFixed(1) + '%';
        document.getElementById('jobProgressBar').style.width = buildingTurtle.progress.percentage + '%';
        document.getElementById('jobBlocks').textContent = `${buildingTurtle.progress.current} / ${buildingTurtle.progress.total} blocks`;
        document.getElementById('jobStatus').textContent = buildingTurtle.statusMessage || buildingTurtle.status;
      } else {
        jobCard.style.display = 'none';
      }
    }

    function togglePauseJob() {
      const buildingTurtle = turtles.find(t => t.status === 'BUILDING' || t.status === 'PAUSED');
      if (!buildingTurtle) return;
      fetch(getApiBase() + '/api/pause', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ turtleId: buildingTurtle.id })
      }).then(() => fetchTurtles());
    }

    function cancelJob() {
      const buildingTurtle = turtles.find(t => t.status === 'BUILDING' || t.status === 'PAUSED');
      if (!buildingTurtle) return;
      fetch(getApiBase() + '/api/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ turtleId: buildingTurtle.id })
      }).then(() => fetchTurtles());
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
