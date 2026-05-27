/**
 * IRAgent v3 — Router
 * Screen loader: fetch HTML fragments, cache, inject into content area
 */
const Router = {
  _cache: {},
  _basePath: 'screens/',

  /** Params passed to the currently loading/loaded screen.
   *  Screen scripts read this on init. */
  currentParams: {},

  /**
   * Load a screen HTML fragment and inject into #screenContent
   * @param {string} screenName
   * @param {object} [params] - data passed to the target screen
   * @param {function} [callback] - called after DOM injection
   */
  async load(screenName, params, callback) {
    // Support 2-arg form: load(name, callback)
    if (typeof params === 'function') { callback = params; params = {}; }
    this.currentParams = params || {};

    const container = document.getElementById('screenContent');
    if (!container) return;

    // Show skeleton loading
    container.innerHTML = `
      <div style="padding: var(--space-8) var(--space-4);">
        <div class="skeleton" style="height: 120px; margin-bottom: 12px;"></div>
        <div class="skeleton" style="height: 60px; margin-bottom: 12px;"></div>
        <div class="skeleton" style="height: 80px; margin-bottom: 12px;"></div>
        <div class="skeleton" style="height: 60px;"></div>
      </div>
    `;

    try {
      const html = await this._fetch(screenName);
      container.innerHTML = html;

      // Execute any scripts embedded in the fragment
      this._executeScripts(container);

      if (callback) callback();
    } catch (err) {
      console.error(`Router: failed to load "${screenName}"`, err);
      const isFileProtocol = location.protocol === 'file:';
      const hint = isFileProtocol
        ? `<b>检测到你以 file:// 协议打开此文件</b><br><br>
           <b>解决方法（任选其一）：</b><br>
           1. VSCode 安装 Live Server 插件，右键 index.html → Open with Live Server<br>
           2. 终端运行：<code style="background:#eee;padding:1px 4px;border-radius:2px;">npx serve .</code>（需 Node.js）<br>
           3. 终端运行：<code style="background:#eee;padding:1px 4px;border-radius:2px;">python -m http.server 8080</code><br>
           <br>启动后浏览器访问对应端口即可`
        : `文件未找到：screens/${screenName}.html<br>请确认服务器根目录为 ui-prototype-v3/ 文件夹`;
      container.innerHTML = `
        <div style="padding:40px 24px;text-align:center;">
          <div style="font-size:48px;margin-bottom:16px;">⚠️</div>
          <div style="font-size:16px;font-weight:600;color:#374151;margin-bottom:8px;">页面加载失败</div>
          <div style="font-size:13px;color:#6B7280;line-height:1.7;">${hint}</div>
          <button class="btn btn--outline btn--md" style="margin-top:20px;" onclick="Router.load('${screenName}')">重试</button>
          <button class="btn btn--primary btn--md" style="margin-top:12px;display:block;width:100%;" onclick="navigator.clipboard.writeText('cd ui-prototype-v3 && npx serve .').then(()=>alert('已复制命令到剪贴板'))">复制启动命令</button>
        </div>
      `;
    }
  },

  /** Fetch with in-memory cache */
  async _fetch(screenName) {
    if (this._cache[screenName]) return this._cache[screenName];

    const url = this._basePath + screenName + '.html';
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const html = await res.text();
    this._cache[screenName] = html;
    return html;
  },

  /** Execute <script> tags within loaded content */
  _executeScripts(container) {
    const scripts = container.querySelectorAll('script');
    scripts.forEach(oldScript => {
      const newScript = document.createElement('script');
      Array.from(oldScript.attributes).forEach(attr => {
        newScript.setAttribute(attr.name, attr.value);
      });
      newScript.textContent = oldScript.textContent;
      oldScript.parentNode.replaceChild(newScript, oldScript);
    });
  },

  /** Preload screens in background */
  preload(screenNames) {
    screenNames.forEach(name => {
      if (!this._cache[name]) {
        this._fetch(name).catch(() => {});
      }
    });
  },

  /** Clear cache (for dev reload) */
  clearCache() {
    this._cache = {};
  }
};
