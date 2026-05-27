/**
 * IRAgent v3 — App Core
 * Tab switching, navigation stack, global state, toast, utils
 */
const App = {

  // ---- State ----
  state: {
    activeTab: 'knowledge',
    currentScreen: null,    // sub-screen within a tab
    navStack: [],           // [{tab, screen}]
    isFirstLaunch: false,   // set to false so default shows main app
    user: MOCK.user
  },

  // ---- Init ----
  init() {
    this._bindTabs();
    this._bindGlobalClicks();
    // Start with auth screen, tab bar hidden
    Router.load('auth');
    // Preload other screens in background
    Router.preload(['onboarding', 'knowledge-list', 'knowledge-detail', 'study-chat', 'study-deeplearn', 'study-video', 'practice-hub', 'errors-list', 'errors-detail', 'profile-dashboard']);
    console.log('%c📚 IRAgent v3 Prototype Ready %c| %cAuth → 5 Tabs',
      'font-size:14px;color:#6366F1;', '', 'color:#6B7280;');
  },

  /** Show tab bar (called after login) */
  showTabBar() {
    const bar = document.getElementById('tabBar');
    if (bar) bar.style.display = 'flex';
  },

  /** Hide tab bar (called on logout) */
  hideTabBar() {
    const bar = document.getElementById('tabBar');
    if (bar) bar.style.display = 'none';
  },

  // ---- Tab Switching ----
  _bindTabs() {
    document.querySelectorAll('.tab-bar__item').forEach(tab => {
      tab.addEventListener('click', () => {
        const tabId = tab.dataset.tab;
        const defaultScreen = tab.dataset.defaultScreen || tabId + '-list';
        this.switchTab(tabId, defaultScreen);
      });
    });
  },

  switchTab(tabId, screenName) {
    this.state.activeTab = tabId;
    this.state.currentScreen = screenName;

    // Update tab bar active state
    document.querySelectorAll('.tab-bar__item').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });

    // Load screen
    Router.load(screenName, () => {
      this._afterScreenLoad(tabId, screenName);
    });
  },

  _afterScreenLoad(tabId, screenName) {
    // Scroll content to top
    const body = document.getElementById('screenContent');
    if (body) body.scrollTop = 0;
  },

  /**
   * Navigate to a sub-screen within current tab
   * @param {string} screenName
   * @param {object} [params] - passed to the target screen, read via Router.currentParams
   */
  navigateTo(screenName, params) {
    this.state.navStack.push({
      tab: this.state.activeTab,
      screen: this.state.currentScreen,
      params: Router.currentParams || {}
    });
    this.state.currentScreen = screenName;
    Router.load(screenName, params, () => {
      this._afterScreenLoad(this.state.activeTab, screenName);
    });
  },

  goBack() {
    if (this.state.navStack.length === 0) return;
    const prev = this.state.navStack.pop();
    this.state.currentScreen = prev.screen;
    // Preserve previous params if any (stored on the stack entry)
    Router.load(prev.screen, prev.params || {}, () => {
      this._afterScreenLoad(prev.tab, prev.screen);
    });
  },

  // ---- Onboarding ----
  goOnboarding() {
    Router.load('onboarding', () => {
      // Onboarding script will init itself
    });
  },

  // ---- Sub-screen navigation within a tab (no tab switch) ----
  openSubScreen(screenName, params) {
    this.navigateTo(screenName, params);
  },

  // ---- Global click bindings ----
  _bindGlobalClicks() {
    // Delegate: back buttons
    document.addEventListener('click', (e) => {
      const backBtn = e.target.closest('[data-back]');
      if (backBtn) {
        e.preventDefault();
        if (this.state.navStack.length > 0) {
          this.goBack();
        } else {
          // Go to tab default
          const tab = document.querySelector('.tab-bar__item.active');
          if (tab) {
            this.switchTab(tab.dataset.tab, tab.dataset.defaultScreen);
          }
        }
      }
    });
  },

  // ---- Toast ----
  toast(message) {
    const el = document.getElementById('globalToast');
    if (!el) return;
    el.textContent = message;
    el.classList.add('active');
    clearTimeout(el._timeout);
    el._timeout = setTimeout(() => el.classList.remove('active'), 2000);
  }
};

// ---- Utility ----
function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now - d;
  if (diff < 3600000) return Math.floor(diff/60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff/3600000) + '小时前';
  if (diff < 604800000) return Math.floor(diff/86400000) + '天前';
  return dateStr.slice(0, 10);
}

// ---- Init on DOM ready ----
document.addEventListener('DOMContentLoaded', () => App.init());
