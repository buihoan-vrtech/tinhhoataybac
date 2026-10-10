document.addEventListener('DOMContentLoaded', () => {
  const quantity = document.querySelector('#quantity');
  if (quantity) {
    for (const [selector, direction] of [['.quantity-btn.minus', -1], ['.quantity-btn.plus', 1]]) {
      document.querySelector(selector)?.addEventListener('click', () => {
        if (!quantity.value || !Number.isFinite(quantity.valueAsNumber)) quantity.value = quantity.min || '1';
        if (direction > 0) quantity.stepUp(); else quantity.stepDown();
        quantity.dispatchEvent(new Event('input', {bubbles: true}));
      });
    }
  }
  const slides = [...document.querySelectorAll('.slide')];
  const dots = [...document.querySelectorAll('.dot')];
  if (!slides.length) return;
  let current = 0;
  let timer;
  const show = index => {
    current = (index + slides.length) % slides.length;
    slides.forEach((slide, i) => slide.classList.toggle('active', i === current));
    dots.forEach((dot, i) => {
      dot.classList.toggle('active', i === current);
      dot.setAttribute('aria-pressed', String(i === current));
    });
  };
  const pause = () => clearInterval(timer);
  const start = () => {
    pause();
    if (slides.length > 1 && !document.hidden && !window.matchMedia('(prefers-reduced-motion: reduce)').matches)
      timer = setInterval(() => show(current + 1), 5000);
  };
  const move = delta => {show(current + delta); start();};
  document.querySelector('.next')?.addEventListener('click', () => move(1));
  document.querySelector('.prev')?.addEventListener('click', () => move(-1));
  dots.forEach((dot, i) => dot.addEventListener('click', () => {show(i); start();}));
  const slider = slides[0].parentElement;
  slider?.addEventListener('mouseenter', pause);
  slider?.addEventListener('mouseleave', start);
  slider?.addEventListener('focusin', pause);
  slider?.addEventListener('focusout', start);
  document.addEventListener('visibilitychange', start);
  show(0);
  start();
});
document.addEventListener('DOMContentLoaded', () => {
  const keys = {favorites: 'thtb.favorites.v1', recent: 'thtb.recent.v1'};
  const validId = id => /^\d{1,15}$/.test(String(id)) && Number(id) > 0;
  function read(mode) {
    try {
      const value = JSON.parse(localStorage.getItem(keys[mode]) || '[]');
      return Array.isArray(value) ? [...new Set(value.map(String).filter(validId))].slice(0, mode === 'recent' ? 10 : 100) : [];
    } catch (_) { return []; }
  }
  function write(mode, ids) {
    try { localStorage.setItem(keys[mode], JSON.stringify(ids)); return true; }
    catch (_) { return false; }
  }
  let notice;
  function announce(message) {
    if (!notice) {
      notice = document.createElement('div');
      notice.className = 'saved-toast'; notice.setAttribute('role', 'status');
      document.body.append(notice);
    }
    notice.textContent = message; notice.hidden = false;
    clearTimeout(notice.timer); notice.timer = setTimeout(() => {notice.hidden = true;}, 3500);
  }
  function sync() {
    const ids = read('favorites');
    document.querySelectorAll('[data-favorites-count]').forEach(el => {el.textContent = ids.length;});
    document.querySelectorAll('[data-favorite-id]').forEach(button => {
      const saved = ids.includes(button.dataset.favoriteId);
      button.classList.toggle('is-saved', saved);
      button.setAttribute('aria-pressed', String(saved));
      button.setAttribute('aria-label', (saved ? 'Bỏ yêu thích ' : 'Lưu yêu thích ') + (button.dataset.productName || 'sản phẩm'));
      button.textContent = button.classList.contains('favorite-detail') ? (saved ? '♥ Đã lưu yêu thích' : '♡ Lưu vào yêu thích') : (saved ? '♥' : '♡');
    });
  }
  const viewed = document.querySelector('[data-viewed-id]')?.dataset.viewedId;
  if (validId(viewed)) write('recent', [viewed, ...read('recent').filter(id => id !== viewed)].slice(0, 10));

  async function fetchCards(ids) {
    if (!ids.length) return [];
    const response = await fetch('/products/collection-data?ids=' + encodeURIComponent(ids.join(',')));
    if (!response.ok) throw new Error('Unavailable');
    const result = await response.json();
    if (!Array.isArray(result)) throw new Error('Invalid response');
    return result.filter(p => validId(p.id) && typeof p.name === 'string' && Number.isFinite(p.price));
  }
  function card(p) {
    const article = document.createElement('article'); article.className = 'product-card';
    const imageBox = document.createElement('div'); imageBox.className = 'product-image';
    const url = '/products/' + p.id;
    const link = document.createElement('a'); link.href = url;
    const image = document.createElement('img'); image.alt = p.name; image.loading = 'lazy';
    try { const src = new URL(p.image, location.origin); if (['http:', 'https:'].includes(src.protocol)) image.src = src.href; } catch (_) {}
    link.append(image); imageBox.append(link);
    const button = document.createElement('button'); button.type = 'button'; button.className = 'favorite-button';
    button.dataset.favoriteId = String(p.id); button.dataset.productName = p.name;
    imageBox.append(button);
    const info = document.createElement('div'); info.className = 'product-info';
    const category = document.createElement('span'); category.className = 'product-category'; category.textContent = p.category || 'Đặc sản Tây Bắc';
    const heading = document.createElement('h3'); const title = document.createElement('a'); title.href = url; title.textContent = p.name; heading.append(title);
    const price = document.createElement('p'); price.className = 'price'; price.textContent = new Intl.NumberFormat('vi-VN').format(p.price) + 'đ';
    const stock = document.createElement('p'); stock.className = p.available ? 'product-stock' : 'catalog-soldout'; stock.textContent = p.available ? 'Còn hàng' : 'Tạm hết hàng';
    info.append(category, heading, price, stock); article.append(imageBox, info);
    return article;
  }
  const collection = document.querySelector('[data-collection]');
  let revision = 0;
  async function loadCollection() {
    if (!collection) return;
    const mode = collection.dataset.collection;
    const request = ++revision;
    const grid = collection.querySelector('.saved-grid');
    const loading = collection.querySelector('.saved-loading');
    const empty = collection.querySelector('.saved-empty');
    const error = collection.querySelector('.saved-error');
    const retry = collection.querySelector('.saved-retry');
    loading.hidden = false; empty.hidden = true; error.hidden = true; retry.hidden = true;
    try {
      const products = await fetchCards(read(mode));
      if (request !== revision) return;
      grid.replaceChildren(...products.map(card));
      collection.querySelector('.saved-count').textContent = products.length + ' sản phẩm';
      empty.hidden = products.length !== 0;
      sync();
    } catch (_) {
      if (request === revision) {error.hidden = false; retry.hidden = false;}
    } finally {if (request === revision) loading.hidden = true;}
  }
  collection?.querySelector('.saved-retry')?.addEventListener('click', loadCollection);
  document.addEventListener('click', event => {
    const button = event.target.closest('[data-favorite-id]');
    if (!button || !validId(button.dataset.favoriteId)) return;
    const id = button.dataset.favoriteId, ids = read('favorites'), exists = ids.includes(id);
    if (!exists && ids.length >= 100) {announce('Danh sách đã đủ 100 sản phẩm. Hãy bỏ một sản phẩm trước khi thêm.'); return;}
    const next = exists ? ids.filter(value => value !== id) : [id, ...ids];
    if (!write('favorites', next)) {announce('Trình duyệt chưa lưu được danh sách. Vui lòng kiểm tra quyền lưu dữ liệu.'); return;}
    sync(); announce(exists ? 'Đã bỏ sản phẩm khỏi yêu thích.' : 'Đã lưu sản phẩm vào yêu thích.');
    if (collection?.dataset.collection === 'favorites') loadCollection();
  });
  window.addEventListener('storage', event => {
    if (Object.values(keys).includes(event.key) || event.key === null) {sync(); loadCollection();}
  });
  const recent = document.querySelector('.recent-products');
  if (recent) fetchCards(read('recent').filter(id => id !== viewed).slice(0, 4))
    .then(products => {recent.hidden = !products.length; recent.querySelector('.recent-grid').replaceChildren(...products.map(card)); sync();})
    .catch(() => {recent.hidden = true;});
  sync(); loadCollection();
});
